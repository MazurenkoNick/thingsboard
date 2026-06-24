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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.step.ComposeTypeChoiceStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.agent.AgentAppTemplateService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.install.ProjectInfo;
import org.thingsboard.server.service.sync.GitSyncService;
import org.thingsboard.server.service.sync.vc.GitRepository.FileType;
import org.thingsboard.server.service.sync.vc.GitRepository.RepoFile;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentAppTemplateSyncServiceTest {

    private static final UUID TEST_TEMPLATE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEST_TEMPLATE_ID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private GitSyncService gitSyncService;
    @Mock
    private AgentAppTemplateService agentAppTemplateService;
    @Mock
    private PartitionService partitionService;
    @Mock
    private ProjectInfo projectInfo;

    @InjectMocks
    private AgentAppTemplateSyncService syncService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(syncService, "repoUri", "https://github.com/test/repo.git");
        ReflectionTestUtils.setField(syncService, "branch", "main");
        ReflectionTestUtils.setField(syncService, "basePath", "templates");
        ReflectionTestUtils.setField(syncService, "fetchFrequencyMs", 3600000L);
        lenient().when(projectInfo.getProjectVersion()).thenReturn("4.4.0");
    }

    // ==================== init() tests ====================

    @Test
    void init_shouldSkipRegistration_whenRepoUriIsBlank() throws Exception {
        ReflectionTestUtils.setField(syncService, "repoUri", "");

        syncService.init();

        verifyNoInteractions(gitSyncService);
    }

    @Test
    void init_shouldDefaultBranchToMain_whenBranchIsBlank() throws Exception {
        ReflectionTestUtils.setField(syncService, "branch", "");

        syncService.init();

        verify(gitSyncService).registerSync(eq("agent-app-templates"),
                eq("https://github.com/test/repo.git"), eq("main"), eq(3600000L), any());
    }

    @Test
    void init_shouldRegisterSync_withConfiguredValues() throws Exception {
        ReflectionTestUtils.setField(syncService, "branch", "develop");

        syncService.init();

        verify(gitSyncService).registerSync(eq("agent-app-templates"),
                eq("https://github.com/test/repo.git"), eq("develop"), eq(3600000L), any());
    }

    // ==================== update() - partition check ====================

    @Test
    void update_shouldSkip_whenNotMyPartition() {
        when(partitionService.isMyPartition(any(), any(), any())).thenReturn(false);

        invokeUpdate();

        verify(agentAppTemplateService, never()).save(any(), any());
    }

    // ==================== update() - happy path ====================

    @Test
    void update_shouldSaveTemplate_withFieldsFromFileNameAndContent() {
        setupPartitionOwnership();
        String fileName = "template-EDGE-DOCKER_COMPOSE-1.0.0.json";
        String filePath = "templates/4.0.0/" + fileName;
        RepoFile file = new RepoFile(filePath, fileName, FileType.FILE);
        when(gitSyncService.listFiles(eq("agent-app-templates"), eq("templates"), anyInt(), eq(FileType.FILE)))
                .thenReturn(List.of(file));
        when(gitSyncService.getFileContent("agent-app-templates", filePath))
                .thenReturn(templateJson("DOCKER_COMPOSE", "1.1.0", "[]"));
        when(agentAppTemplateService.findById(TenantId.SYS_TENANT_ID, new AgentAppTemplateId(TEST_TEMPLATE_ID)))
                .thenReturn(null);

        invokeUpdate();

        ArgumentCaptor<AgentAppTemplate> captor = ArgumentCaptor.forClass(AgentAppTemplate.class);
        verify(agentAppTemplateService).save(eq(TenantId.SYS_TENANT_ID), captor.capture());
        AgentAppTemplate saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(new AgentAppTemplateId(TEST_TEMPLATE_ID));
        assertThat(saved.getAppType()).isEqualTo(AgentApplicationType.EDGE);
        assertThat(saved.getConfigType()).isEqualTo(AgentAppConfigType.DOCKER_COMPOSE);
        assertThat(saved.getCurrentVersion()).isEqualTo("1.0.0");
        assertThat(saved.getNextVersion()).isEqualTo("1.1.0");
        assertThat(saved.getTenantId()).isEqualTo(TenantId.SYS_TENANT_ID);
    }

    @Test
    void update_shouldUpdateExistingTemplate_whenFound() {
        setupPartitionOwnership();
        String fileName = "template-GATEWAY-DOCKER_COMPOSE-2.0.0.json";
        String filePath = "templates/4.0.0/" + fileName;
        RepoFile file = new RepoFile(filePath, fileName, FileType.FILE);
        when(gitSyncService.listFiles(any(), any(), anyInt(), any())).thenReturn(List.of(file));
        when(gitSyncService.getFileContent("agent-app-templates", filePath))
                .thenReturn(templateJson("DOCKER_COMPOSE", null, "[]"));

        AgentAppTemplate existing = new AgentAppTemplate(new AgentAppTemplateId(TEST_TEMPLATE_ID));
        existing.setAppType(AgentApplicationType.GATEWAY);
        when(agentAppTemplateService.findById(TenantId.SYS_TENANT_ID, new AgentAppTemplateId(TEST_TEMPLATE_ID)))
                .thenReturn(existing);

        invokeUpdate();

        ArgumentCaptor<AgentAppTemplate> captor = ArgumentCaptor.forClass(AgentAppTemplate.class);
        verify(agentAppTemplateService).save(eq(TenantId.SYS_TENANT_ID), captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
        assertThat(existing.getCurrentVersion()).isEqualTo("2.0.0");
    }

    @Test
    void update_shouldProcessMultipleFiles() {
        setupPartitionOwnership();
        RepoFile file1 = new RepoFile("templates/4.0.0/template-EDGE-DOCKER_COMPOSE-1.0.0.json",
                "template-EDGE-DOCKER_COMPOSE-1.0.0.json", FileType.FILE);
        RepoFile file2 = new RepoFile("templates/4.0.0/template-GATEWAY-DOCKER_COMPOSE-1.0.0.json",
                "template-GATEWAY-DOCKER_COMPOSE-1.0.0.json", FileType.FILE);
        when(gitSyncService.listFiles(any(), any(), anyInt(), any())).thenReturn(List.of(file1, file2));
        when(gitSyncService.getFileContent("agent-app-templates", file1.path()))
                .thenReturn(templateJson(TEST_TEMPLATE_ID, "DOCKER_COMPOSE", null, "[]", "[]"));
        when(gitSyncService.getFileContent("agent-app-templates", file2.path()))
                .thenReturn(templateJson(TEST_TEMPLATE_ID_2, "DOCKER_COMPOSE", null, "[]", "[]"));
        when(agentAppTemplateService.findById(eq(TenantId.SYS_TENANT_ID), any(AgentAppTemplateId.class)))
                .thenReturn(null);

        invokeUpdate();

        verify(agentAppTemplateService, times(2)).save(eq(TenantId.SYS_TENANT_ID), any());
    }

    // ==================== update() - error isolation ====================

    @Test
    void update_shouldContinueProcessing_whenOneFileFails() {
        setupPartitionOwnership();
        RepoFile badFile = new RepoFile("templates/4.0.0/template-EDGE-DOCKER_COMPOSE-1.0.0.json",
                "template-EDGE-DOCKER_COMPOSE-1.0.0.json", FileType.FILE);
        RepoFile goodFile = new RepoFile("templates/4.0.0/template-GATEWAY-DOCKER_COMPOSE-2.0.0.json",
                "template-GATEWAY-DOCKER_COMPOSE-2.0.0.json", FileType.FILE);
        when(gitSyncService.listFiles(any(), any(), anyInt(), any())).thenReturn(List.of(badFile, goodFile));
        when(gitSyncService.getFileContent("agent-app-templates", badFile.path()))
                .thenThrow(new RuntimeException("git read error"));
        when(gitSyncService.getFileContent("agent-app-templates", goodFile.path()))
                .thenReturn(templateJson("DOCKER_COMPOSE", null, "[]", "[]"));
        when(agentAppTemplateService.findById(eq(TenantId.SYS_TENANT_ID), any(AgentAppTemplateId.class)))
                .thenReturn(null);

        invokeUpdate();

        verify(agentAppTemplateService, times(1)).save(eq(TenantId.SYS_TENANT_ID), any());
    }

    // ==================== template id validation ====================

    @Test
    void update_shouldSkipFile_whenTemplateIdIsMissingInJson() {
        setupPartitionOwnership();
        RepoFile noIdFile = new RepoFile("templates/4.0.0/template-EDGE-DOCKER_COMPOSE-1.0.0.json",
                "template-EDGE-DOCKER_COMPOSE-1.0.0.json", FileType.FILE);
        RepoFile goodFile = new RepoFile("templates/4.0.0/template-GATEWAY-DOCKER_COMPOSE-1.0.0.json",
                "template-GATEWAY-DOCKER_COMPOSE-1.0.0.json", FileType.FILE);
        when(gitSyncService.listFiles(any(), any(), anyInt(), any())).thenReturn(List.of(noIdFile, goodFile));
        when(gitSyncService.getFileContent("agent-app-templates", noIdFile.path()))
                .thenReturn("""
                        {"configType": "DOCKER_COMPOSE", "startSteps": [], "upgradeSteps": []}
                        """.getBytes());
        when(gitSyncService.getFileContent("agent-app-templates", goodFile.path()))
                .thenReturn(templateJson("DOCKER_COMPOSE", null, "[]", "[]"));
        when(agentAppTemplateService.findById(eq(TenantId.SYS_TENANT_ID), any(AgentAppTemplateId.class)))
                .thenReturn(null);

        invokeUpdate();

        verify(agentAppTemplateService, times(1)).save(eq(TenantId.SYS_TENANT_ID), any());
    }

    // ==================== config type validation ====================

    @Test
    void update_shouldSkipFile_whenConfigTypeMismatchesBetweenFilenameAndContent() {
        setupPartitionOwnership();
        String fileName = "template-EDGE-DOCKER_COMPOSE-1.0.0.json";

        RepoFile mismatchFile = new RepoFile("templates/4.0.0/template-EDGE-INVALID_TYPE-1.0.0.json",
                "template-EDGE-INVALID_TYPE-1.0.0.json", FileType.FILE);
        RepoFile goodFile = new RepoFile("templates/4.0.0/template-GATEWAY-DOCKER_COMPOSE-1.0.0.json",
                "template-GATEWAY-DOCKER_COMPOSE-1.0.0.json", FileType.FILE);
        when(gitSyncService.listFiles(any(), any(), anyInt(), any())).thenReturn(List.of(mismatchFile, goodFile));
        when(gitSyncService.getFileContent("agent-app-templates", mismatchFile.path()))
                .thenReturn(templateJson("DOCKER_COMPOSE", null, "[]", "[]"));
        when(gitSyncService.getFileContent("agent-app-templates", goodFile.path()))
                .thenReturn(templateJson("DOCKER_COMPOSE", null, "[]", "[]"));
        when(agentAppTemplateService.findById(eq(TenantId.SYS_TENANT_ID), any(AgentAppTemplateId.class)))
                .thenReturn(null);

        invokeUpdate();

        // Only the good file should be saved — mismatch file fails with IllegalArgumentException on valueOf
        verify(agentAppTemplateService, times(1)).save(eq(TenantId.SYS_TENANT_ID), any());
    }

    // ==================== file pattern matching ====================

    @Test
    void update_shouldIgnoreFilesThatDontMatchPattern() {
        setupPartitionOwnership();
        RepoFile nonMatching = new RepoFile("templates/readme.md", "readme.md", FileType.FILE);
        RepoFile wrongFormat = new RepoFile("templates/4.0.0/template-edge-1.0.0.json",
                "template-edge-1.0.0.json", FileType.FILE); // lowercase, missing configType
        when(gitSyncService.listFiles(any(), any(), anyInt(), any())).thenReturn(List.of(nonMatching, wrongFormat));

        invokeUpdate();

        verify(gitSyncService, never()).getFileContent(any(), any());
        verify(agentAppTemplateService, never()).save(any(), any());
    }

    @Test
    void update_shouldMatchPreReleaseVersions() {
        setupPartitionOwnership();
        String fileName = "template-EDGE-DOCKER_COMPOSE-1.0.0-beta1.json";
        RepoFile file = new RepoFile("templates/4.0.0/" + fileName, fileName, FileType.FILE);
        when(gitSyncService.listFiles(any(), any(), anyInt(), any())).thenReturn(List.of(file));
        when(gitSyncService.getFileContent("agent-app-templates", file.path()))
                .thenReturn(templateJson("DOCKER_COMPOSE", null, "[]", "[]"));
        when(agentAppTemplateService.findById(eq(TenantId.SYS_TENANT_ID), any(AgentAppTemplateId.class)))
                .thenReturn(null);

        invokeUpdate();

        ArgumentCaptor<AgentAppTemplate> captor = ArgumentCaptor.forClass(AgentAppTemplate.class);
        verify(agentAppTemplateService).save(eq(TenantId.SYS_TENANT_ID), captor.capture());
        assertThat(captor.getValue().getCurrentVersion()).isEqualTo("1.0.0-beta1");
    }

    // ==================== folder version gating ====================

    @Test
    void update_shouldSkipFile_whenFolderVersionIsNewerThanProjectVersion() {
        setupPartitionOwnership();
        String fileName = "template-EDGE-DOCKER_COMPOSE-1.0.0.json";
        RepoFile tooNewFile = new RepoFile("templates/9.0.0/" + fileName, fileName, FileType.FILE);
        when(gitSyncService.listFiles(any(), any(), anyInt(), any())).thenReturn(List.of(tooNewFile));

        invokeUpdate();

        verify(gitSyncService, never()).getFileContent(any(), any());
        verify(agentAppTemplateService, never()).save(any(), any());
    }

    @Test
    void update_shouldProcessFile_whenFolderVersionSmallerProjectVersion() {
        setupPartitionOwnership();
        String fileName = "template-EDGE-DOCKER_COMPOSE-1.0.0.json";
        RepoFile file = new RepoFile("templates/4.2.1.2/" + fileName, fileName, FileType.FILE);
        when(gitSyncService.listFiles(any(), any(), anyInt(), any())).thenReturn(List.of(file));
        when(gitSyncService.getFileContent("agent-app-templates", file.path()))
                .thenReturn(templateJson("DOCKER_COMPOSE", null, "[]", "[]"));
        when(agentAppTemplateService.findById(eq(TenantId.SYS_TENANT_ID), any(AgentAppTemplateId.class)))
                .thenReturn(null);

        invokeUpdate();

        verify(agentAppTemplateService).save(eq(TenantId.SYS_TENANT_ID), any());
    }

    @Test
    void update_shouldProcessFile_whenFolderVersionEqualsProjectVersion() {
        setupPartitionOwnership();
        String fileName = "template-EDGE-DOCKER_COMPOSE-1.0.0.json";
        RepoFile file = new RepoFile("templates/4.4.0/" + fileName, fileName, FileType.FILE);
        when(gitSyncService.listFiles(any(), any(), anyInt(), any())).thenReturn(List.of(file));
        when(gitSyncService.getFileContent("agent-app-templates", file.path()))
                .thenReturn(templateJson("DOCKER_COMPOSE", null, "[]", "[]"));
        when(agentAppTemplateService.findById(eq(TenantId.SYS_TENANT_ID), any(AgentAppTemplateId.class)))
                .thenReturn(null);

        invokeUpdate();

        verify(agentAppTemplateService).save(eq(TenantId.SYS_TENANT_ID), any());
    }

    // ==================== YAML compose conversion ====================

    @Test
    void update_shouldConvertYamlComposeTemplatesToJson() {
        setupPartitionOwnership();
        String fileName = "template-EDGE-DOCKER_COMPOSE-1.0.0.json";
        RepoFile file = new RepoFile("templates/4.0.0/" + fileName, fileName, FileType.FILE);
        when(gitSyncService.listFiles(any(), any(), anyInt(), any())).thenReturn(List.of(file));

        UUID stepId = UUID.randomUUID();
        String yamlCompose = "services:\n  tb-core:\n    image: thingsboard/tb-core";
        String startStepsJson = """
                [{"type": "COMPOSE_TEMPLATE", "id": "%s", "title": "Choose type", "composeTemplates": {"monolith": "%s"}}]
                """.formatted(stepId, yamlCompose.replace("\n", "\\n"));
        when(gitSyncService.getFileContent("agent-app-templates", file.path()))
                .thenReturn(templateJson("DOCKER_COMPOSE", null, startStepsJson, "[]"));
        when(agentAppTemplateService.findById(eq(TenantId.SYS_TENANT_ID), any(AgentAppTemplateId.class)))
                .thenReturn(null);

        invokeUpdate();

        ArgumentCaptor<AgentAppTemplate> captor = ArgumentCaptor.forClass(AgentAppTemplate.class);
        verify(agentAppTemplateService).save(eq(TenantId.SYS_TENANT_ID), captor.capture());
        AgentAppTemplate saved = captor.getValue();
        assertThat(saved.getStartSteps()).hasSize(1);
        ComposeTypeChoiceStep step = (ComposeTypeChoiceStep) saved.getStartSteps().get(0);
        JsonNode composeJson = step.getComposeTemplates().get("monolith");
        assertThat(composeJson).isNotNull();
        assertThat(composeJson.isObject()).isTrue();
        assertThat(composeJson.get("services").get("tb-core").get("image").asText())
                .isEqualTo("thingsboard/tb-core");
    }

    @Test
    void update_shouldReadComposeFromYamlFileReference() {
        setupPartitionOwnership();
        String fileName = "template-EDGE-DOCKER_COMPOSE-1.0.0.json";
        RepoFile file = new RepoFile("templates/4.0.0/" + fileName, fileName, FileType.FILE);
        when(gitSyncService.listFiles(any(), any(), anyInt(), any())).thenReturn(List.of(file));

        UUID stepId = UUID.randomUUID();
        String startStepsJson = """
                [{"type": "COMPOSE_TEMPLATE", "id": "%s", "title": "Choose type", "composeTemplates": {"monolith": "compose/edge/1.0.0/monolith.yml"}}]
                """.formatted(stepId);
        when(gitSyncService.getFileContent("agent-app-templates", file.path()))
                .thenReturn(templateJson("DOCKER_COMPOSE", null, startStepsJson, "[]"));
        when(gitSyncService.getFileContent("agent-app-templates", "compose/edge/1.0.0/monolith.yml"))
                .thenReturn("services:\n  tb-core:\n    image: thingsboard/tb-core".getBytes());
        when(agentAppTemplateService.findById(eq(TenantId.SYS_TENANT_ID), any(AgentAppTemplateId.class)))
                .thenReturn(null);

        invokeUpdate();

        ArgumentCaptor<AgentAppTemplate> captor = ArgumentCaptor.forClass(AgentAppTemplate.class);
        verify(agentAppTemplateService).save(eq(TenantId.SYS_TENANT_ID), captor.capture());
        AgentAppTemplate saved = captor.getValue();
        assertThat(saved.getStartSteps()).hasSize(1);
        ComposeTypeChoiceStep step = (ComposeTypeChoiceStep) saved.getStartSteps().get(0);
        JsonNode composeJson = step.getComposeTemplates().get("monolith");
        assertThat(composeJson).isNotNull();
        assertThat(composeJson.isObject()).isTrue();
        assertThat(composeJson.get("services").get("tb-core").get("image").asText())
                .isEqualTo("thingsboard/tb-core");
    }

    @Test
    void update_shouldHandleEmptyComposeTemplatesGracefully() {
        setupPartitionOwnership();
        String fileName = "template-EDGE-DOCKER_COMPOSE-1.0.0.json";
        RepoFile file = new RepoFile("templates/4.0.0/" + fileName, fileName, FileType.FILE);
        when(gitSyncService.listFiles(any(), any(), anyInt(), any())).thenReturn(List.of(file));

        UUID stepId = UUID.randomUUID();
        String startStepsJson = """
                [{"type": "COMPOSE_TEMPLATE", "id": "%s", "title": "Choose type", "composeTemplates": {}}]
                """.formatted(stepId);
        when(gitSyncService.getFileContent("agent-app-templates", file.path()))
                .thenReturn(templateJson("DOCKER_COMPOSE", null, "[]", startStepsJson));
        when(agentAppTemplateService.findById(eq(TenantId.SYS_TENANT_ID), any(AgentAppTemplateId.class)))
                .thenReturn(null);

        invokeUpdate();

        verify(agentAppTemplateService).save(eq(TenantId.SYS_TENANT_ID), any());
    }

    @Test
    void update_shouldNotConvertComposeTemplatesInUpgradeSteps() {
        setupPartitionOwnership();
        String fileName = "template-EDGE-DOCKER_COMPOSE-1.0.0.json";
        RepoFile file = new RepoFile("templates/4.0.0/" + fileName, fileName, FileType.FILE);
        when(gitSyncService.listFiles(any(), any(), anyInt(), any())).thenReturn(List.of(file));

        UUID stepId = UUID.randomUUID();
        String infoStepJson = """
                [{"type": "COMPOSE_START", "id": "%s", "title": "Upgrade info"}]
                """.formatted(stepId);
        when(gitSyncService.getFileContent("agent-app-templates", file.path()))
                .thenReturn(templateJson("DOCKER_COMPOSE", null, "[]", infoStepJson));
        when(agentAppTemplateService.findById(eq(TenantId.SYS_TENANT_ID), any(AgentAppTemplateId.class)))
                .thenReturn(null);

        invokeUpdate();

        ArgumentCaptor<AgentAppTemplate> captor = ArgumentCaptor.forClass(AgentAppTemplate.class);
        verify(agentAppTemplateService).save(eq(TenantId.SYS_TENANT_ID), captor.capture());
        assertThat(captor.getValue().getUpgradeSteps()).hasSize(1);
    }

    // ==================== Helper methods ====================

    private void setupPartitionOwnership() {
        when(partitionService.isMyPartition(eq(ServiceType.TB_CORE), eq(TenantId.SYS_TENANT_ID), eq(TenantId.SYS_TENANT_ID)))
                .thenReturn(true);
    }

    private void invokeUpdate() {
        try {
            syncService.init();
            ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
            verify(gitSyncService).registerSync(any(), any(), any(), anyLong(), callbackCaptor.capture());
            callbackCaptor.getValue().run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] templateJson(String configType, String nextVersion, String startStepsJson) {
        return templateJson(TEST_TEMPLATE_ID, configType, nextVersion, startStepsJson, "[]");
    }

    private byte[] templateJson(String configType, String nextVersion,
                                String startStepsJson, String upgradeStepsJson) {
        return templateJson(TEST_TEMPLATE_ID, configType, nextVersion, startStepsJson, upgradeStepsJson);
    }

    private byte[] templateJson(UUID id, String configType, String nextVersion,
                                String startStepsJson, String upgradeStepsJson) {
        return """
                {
                  "id": "%s",
                  "configType": "%s",
                  "nextVersion": %s,
                  "startSteps": %s,
                  "upgradeSteps": %s
                }
                """.formatted(
                id,
                configType,
                nextVersion != null ? "\"" + nextVersion + "\"" : "null",
                startStepsJson,
                upgradeStepsJson
        ).getBytes();
    }
}
