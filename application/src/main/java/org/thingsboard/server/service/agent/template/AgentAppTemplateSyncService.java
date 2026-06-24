/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.service.agent.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.RegexUtils;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.ComposeTypeChoiceStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.agent.AgentAppTemplateService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.install.ProjectInfo;
import org.thingsboard.server.service.sync.GitSyncService;
import org.thingsboard.server.service.sync.vc.GitRepository.FileType;
import org.thingsboard.server.service.sync.vc.GitRepository.RepoFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "agents.appTemplates.sync.enabled", havingValue = "true")
public class AgentAppTemplateSyncService {

    private static final Pattern FOLDER_TB_VERSION = Pattern.compile(
            "^templates/(?<tbVersion>[0-9.]+)/.*$"
    );
    private static final int TB_VERSION_GROUP_NUM = 1;

    private static final Pattern TEMPLATE_FILE_PATTERN = Pattern.compile(
            "^template-(?<appType>[A-Z0-9_]+)-(?<configType>[A-Z0-9_]+)-(?<version>\\d+(?:\\.\\d+)*[a-zA-Z0-9]*(?:-[a-zA-Z0-9]+)?)\\.json$"
    );
    private static final int APP_TYPE_GROUP_NUM = 1;
    private static final int CONFIG_TYPE_GROUP_NUM = 2;
    private static final int APP_VERSION_GROUP_NUM = 3;
    private static final String REPO_KEY = "agent-app-templates";

    private final GitSyncService gitSyncService;
    private final AgentAppTemplateService agentAppTemplateService;
    private final PartitionService partitionService;
    private final ProjectInfo projectInfo;

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @Value("${agents.appTemplates.sync.repoUri:}")
    private String repoUri;
    @Value("${agents.appTemplates.sync.branch:}")
    private String branch;
    @Value("${agents.appTemplates.sync.basePath:templates}")
    private String basePath;
    @Value("${agents.appTemplates.sync.fetchFrequencyMs:3600000}")
    private long fetchFrequencyMs;

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void init() throws Exception {
        if (StringUtils.isBlank(repoUri)) {
            log.warn("Agent app template sync is enabled but repoUri is empty");
            return;
        }
        if (StringUtils.isBlank(branch)) {
            branch = "main";
        }
        gitSyncService.registerSync(REPO_KEY, repoUri, branch, fetchFrequencyMs, this::update);
    }

    private void update() {
        if (!partitionService.isMyPartition(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID)) {
            return;
        }

        for (RepoFile f : getTemplateFiles()) {
            try {
                if (!isValidFolderVersion(f)) {
                    continue;
                }
                ParsedTemplateFile parsedFile = getParsedTemplateFile(f);

                AgentAppTemplate template = loadExistingTemplate(parsedFile.getId())
                        .orElseGet(() -> new AgentAppTemplate(new AgentAppTemplateId(parsedFile.getId())));
                template.setTenantId(TenantId.SYS_TENANT_ID);
                template.setAppType(parsedFile.getAppType());
                template.setConfigType(parsedFile.getConfigType());
                template.setImageDigest(parsedFile.getImageDigest());
                template.setCurrentVersion(parsedFile.getCurrentVersion());
                template.setNextVersion(parsedFile.getNextVersion());

                convertComposeTemplatesFromYamlToJson(f.name(), parsedFile.getStartSteps());
                convertComposeTemplatesFromYamlToJson(f.name(), parsedFile.getRestartSteps());
                template.setStartSteps(parsedFile.getStartSteps());
                template.setUpgradeSteps(parsedFile.getUpgradeSteps());
                template.setDeleteSteps(parsedFile.getDeleteSteps());
                template.setRollbackSteps(parsedFile.getRollbackSteps());
                template.setRestartSteps(parsedFile.getRestartSteps());

                agentAppTemplateService.save(TenantId.SYS_TENANT_ID, template);
            } catch (Exception e) {
                log.error("Failed to sync agent app template file: {}", f.name(), e);
            }
        }

        log.info("Agent app template sync completed");
    }

    private boolean isValidFolderVersion(RepoFile f) {
        String folderTbVersion = RegexUtils.getMatch(f.path(), FOLDER_TB_VERSION, TB_VERSION_GROUP_NUM);
        if (StringUtils.isBlank(folderTbVersion)) {
            return false;
        }
        String truncatedFolderTbVersion = folderTbVersion.replaceAll("[^\\d.]", "");
        return compareVersions(truncatedFolderTbVersion, projectInfo.getProjectVersion()) <= 0;
    }

    private static int compareVersions(String a, String b) {
        String[] as = a.split("\\.");
        String[] bs = b.split("\\.");
        for (int i = 0; i < Math.max(as.length, bs.length); i++) {
            int ai = i < as.length ? Integer.parseInt(as[i]) : 0;
            int bi = i < bs.length ? Integer.parseInt(bs[i]) : 0;
            if (ai != bi) {
                return Integer.compare(ai, bi);
            }
        }
        return 0;
    }

    private ParsedTemplateFile getParsedTemplateFile(RepoFile f) {
        ParsedTemplateFile parsedFile = parseTemplateFileContent(f.path());

        if (parsedFile.getId() == null) {
            throw new IllegalStateException("Template id is missing in file " + f.name());
        }

        String appTypeStr = RegexUtils.getMatch(f.name(), TEMPLATE_FILE_PATTERN, APP_TYPE_GROUP_NUM);
        AgentApplicationType appType = AgentApplicationType.valueOf(appTypeStr);
        parsedFile.setAppType(appType);

        String configTypeStr = RegexUtils.getMatch(f.name(), TEMPLATE_FILE_PATTERN, CONFIG_TYPE_GROUP_NUM);
        AgentAppConfigType fileNameConfigType = AgentAppConfigType.valueOf(configTypeStr);
        if (parsedFile.getConfigType() != fileNameConfigType) {
            throw new IllegalStateException("Config type mismatch for file " + f.name()
                    + ": filename says " + fileNameConfigType + ", content says " + parsedFile.getConfigType());
        }

        String templateVersion = RegexUtils.getMatch(f.name(), TEMPLATE_FILE_PATTERN, APP_VERSION_GROUP_NUM);
        parsedFile.setCurrentVersion(templateVersion);

        return parsedFile;
    }

    private ParsedTemplateFile parseTemplateFileContent(String path) {
        try {
            byte[] data = gitSyncService.getFileContent(REPO_KEY, path);
            return JacksonUtil.IGNORE_UNKNOWN_PROPERTIES_JSON_MAPPER.readValue(data, ParsedTemplateFile.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse application template file, path: " + path, e);
        }
    }

    private List<RepoFile> getTemplateFiles() {
        return gitSyncService.listFiles(REPO_KEY, basePath, 2, FileType.FILE).stream()
                .filter(file -> RegexUtils.matches(file.name(), TEMPLATE_FILE_PATTERN))
                .collect(Collectors.toList());
    }

    private void convertComposeTemplatesFromYamlToJson(String fileName, List<AgentAppStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return;
        }
        for (AgentAppStep step : steps) {
            if (!(step instanceof ComposeTypeChoiceStep composeTypeChoiceStep)) {
                continue;
            }
            Map<String, JsonNode> composeTemplates = composeTypeChoiceStep.getComposeTemplates();
            if (CollectionUtils.isEmpty(composeTemplates)) {
                log.warn("Compose templates are null or empty for the step [{}] in [{}]", step, fileName);
                continue;
            }

            Map<String, JsonNode> composeJsonTemplates = new HashMap<>();
            for (Map.Entry<String, JsonNode> entry : composeTemplates.entrySet()) {
                JsonNode composeVal = entry.getValue();
                if (!composeVal.isTextual()) {
                    log.warn("Compose value is not textual for the step [{}] in [{}]", step, fileName);
                    continue;
                }
                String value = composeVal.asText();
                String yamlContent;
                if (value.endsWith(".yml") || value.endsWith(".yaml")) {
                    yamlContent = readComposeFile(value);
                } else {
                    yamlContent = value;
                }
                JsonNode compose = readComposeYaml(yamlContent);
                if (compose != null) {
                    composeJsonTemplates.put(entry.getKey(), compose);
                }
            }
            composeTypeChoiceStep.setComposeTemplates(composeJsonTemplates);
        }
    }

    private String readComposeFile(String filePath) {
        byte[] data = gitSyncService.getFileContent(REPO_KEY, filePath);
        return new String(data, StandardCharsets.UTF_8);
    }

    private JsonNode readComposeYaml(String composeYaml) {
        if (StringUtils.isBlank(composeYaml)) {
            return null;
        }
        try {
            return yamlMapper.readTree(composeYaml);
        } catch (IOException e) {
            log.warn("Failed to parse compose yaml to json: {}", composeYaml, e);
            return null;
        }
    }

    private Optional<AgentAppTemplate> loadExistingTemplate(UUID id) {
        return Optional.ofNullable(agentAppTemplateService.findById(TenantId.SYS_TENANT_ID, new AgentAppTemplateId(id)));
    }

    @Data
    private static class ParsedTemplateFile {
        private UUID id;
        private AgentApplicationType appType;
        private AgentAppConfigType configType;
        private String imageDigest;
        private String currentVersion;
        private String nextVersion;
        private List<AgentAppStep> startSteps = new ArrayList<>();
        private List<AgentAppStep> upgradeSteps = new ArrayList<>();
        private List<AgentAppStep> deleteSteps = new ArrayList<>();
        private List<AgentAppStep> rollbackSteps = new ArrayList<>();
        private List<AgentAppStep> restartSteps = new ArrayList<>();
    }
}
