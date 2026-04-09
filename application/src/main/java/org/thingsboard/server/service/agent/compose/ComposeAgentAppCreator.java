/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.agent.compose;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.RegexUtils;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.gen.agent.v1.ComposeState;
import org.thingsboard.server.service.agent.template.TbAgentAppTemplateService;

import java.util.Iterator;
import java.util.Map.Entry;

@Service
@Slf4j
@RequiredArgsConstructor
public class ComposeAgentAppCreator {

    private final AgentApplicationService appService;
    private final TbAgentAppTemplateService templateService;

    public AgentApplication createApp(TenantId tenantId, AgentId agentId, String projectName, ComposeState compose) {
        JsonNode composeJson = extractCompose(compose);
        ComposeInfo composeInfo = getComposeInfo(composeJson);

        AgentAppTemplate template = resolveTemplate(composeInfo);
        if (template == null) {
            log.warn("[{}][{}] No template found for appType [{}], version [{}]",
                    tenantId, agentId, composeInfo.appType(), composeInfo.version());
            return null;
        }

        AgentApplication newApp = new AgentApplication();
        newApp.setTenantId(tenantId);
        newApp.setAgentId(agentId);
        newApp.setName(composeInfo.generatePlaceholderAppName(projectName));
        newApp.setProjectName(projectName);
        newApp.setAppType(template.getAppType());
        newApp.setTemplateId(template.getId());
        newApp.setOrigin(AgentApplicationOrigin.DISCOVERED);

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(composeJson);
        newApp.setConfig(config);

        AgentApplication app = appService.save(tenantId, newApp);
        log.info("[{}][{}] Created agent application for project [{}], appType [{}], version [{}]",
                tenantId, agentId, app.getProjectName(), composeInfo.appType(), composeInfo.version());
        return app;
    }

    private AgentAppTemplate resolveTemplate(ComposeInfo composeInfo) {
        AgentAppTemplate byAppType = templateService.findByAppTypeAndConfigTypeAndCurrentVersion(
                composeInfo.appType(), AgentAppConfigType.DOCKER_COMPOSE, composeInfo.version());
        if (byAppType != null) {
            return byAppType;
        }
        AgentApplicationType genericType = AgentApplicationType.GENERIC;
        return templateService.findByAppTypeAndConfigTypeAndCurrentVersion(genericType, AgentAppConfigType.DOCKER_COMPOSE, genericType.getDefaultVersion());
    }

    private ComposeInfo getComposeInfo(JsonNode compose) {
        if (!compose.isObject()) {
            throw new IllegalStateException("Compose is not an object");
        }

        JsonNode services = compose.get("services");
        if (services == null || !services.isObject() || services.isEmpty()) {
            throw new IllegalStateException("Compose has no services defined");
        }

        Iterator<Entry<String, JsonNode>> it = services.fields();
        while (it.hasNext()) {
            JsonNode service = it.next().getValue();
            ComposeInfo t = resolveAppTypeAndVersion(service);
            if (t != null) return t;
        }
        return ComposeInfo.GENERIC;
    }

    private ComposeInfo resolveAppTypeAndVersion(JsonNode service) {
        if (service.isObject() && service.has("image")) {
            String image = service.get("image").asText();
            for (var t : AgentApplicationType.values()) {
                if (t.getMainImagePattern() != null && RegexUtils.matches(image, t.getMainImagePattern())) {
                    String version = extractVersion(image);
                    return new ComposeInfo(t, image, version);
                }
            }
        }
        return null;
    }

    private String extractVersion(String image) {
        int colonIdx = image.lastIndexOf(':');
        return colonIdx >= 0 ? image.substring(colonIdx + 1) : null;
    }

    private JsonNode extractCompose(ComposeState composeState) {
        try {
            return JacksonUtil.OBJECT_MAPPER.readTree(composeState.getComposeJson());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Couldn't parse docker compose");
        }
    }

    private record ComposeInfo(AgentApplicationType appType, String mainServiceImage, String version) {

        public static final ComposeInfo GENERIC = new ComposeInfo(AgentApplicationType.GENERIC, null, AgentApplicationType.GENERIC.getDefaultVersion());

        public String generatePlaceholderAppName(String projectName) {
            if (mainServiceImage != null) {
                return this.appType + "_" + mainServiceImage + "_" + projectName;
            }
            return this.appType + "_" + projectName;
        }
    }
}
