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
package org.thingsboard.server.service.agent.msg.inbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.RegexUtils;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.gen.agent.v1.ComposeStateSync;
import org.thingsboard.server.service.agent.AgentInboundMsgCtx;
import org.thingsboard.server.service.agent.template.TbAgentAppTemplateService;

import java.util.Iterator;
import java.util.Map.Entry;

@Component
@Slf4j
@RequiredArgsConstructor
public class ComposeSyncMessageHandler implements AgentInboundMessageHandler {

    private final AgentApplicationService appService;
    private final TbAgentAppTemplateService templateService;

    @Override
    public boolean canHandle(AgentInboundMsgCtx msgCtx) {
        return msgCtx.msg().hasComposeSync();
    }

    @Override
    public void handle(AgentInboundMsgCtx msgCtx) {
        ComposeStateSync composeSync = msgCtx.msg().getComposeSync();
        TenantId tenantId = msgCtx.sessionState().getTenantId();
        AgentId agentId = msgCtx.sessionState().getAgentId();

        try {
            doHandle(tenantId, agentId, composeSync);
        } catch (Exception e) {
            log.error("[{}][{}] Couldn't process compose synchronization for project: {}", tenantId, agentId, composeSync.getProjectName(), e);
        }
    }

    private void doHandle(TenantId tenantId, AgentId agentId, ComposeStateSync composeSync) {
        AgentApplication app = appService.findByProjectName(tenantId, composeSync.getProjectName());
        if (app != null) {
            log.info("[{}][{}] Agent Application already exists for project [{}], skipping creation",
                    tenantId, agentId, composeSync.getProjectName());
            return;
        }

        JsonNode compose = extractCompose(composeSync);
        ComposeInfo composeInfo = getComposeInfo(compose);

        createApp(tenantId, agentId, composeSync.getProjectName(), composeInfo, compose);
    }

    private void createApp(TenantId tenantId, AgentId agentId, String projectName, ComposeInfo composeInfo, JsonNode compose) {
        AgentAppTemplate template = resolveTemplate(composeInfo);
        if (template == null) {
            log.warn("[{}][{}] No template found for appType [{}], version [{}]",
                    tenantId, agentId, composeInfo.appType(), composeInfo.version());
            return;
        }

        AgentApplication newApp = new AgentApplication();
        newApp.setTenantId(tenantId);
        newApp.setAgentId(agentId);
        newApp.setName(composeInfo.generatePlaceholderAppName(projectName));
        newApp.setProjectName(projectName);
        newApp.setAppType(composeInfo.appType());
        newApp.setTemplateId(template.getId());

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);
        newApp.setConfig(config);

        AgentApplication app = appService.save(tenantId, newApp);
        log.info("[{}][{}] Created agent application for project [{}], appType [{}], version [{}]",
                tenantId, agentId, app.getProjectName(), composeInfo.appType(), composeInfo.version());
    }

    private AgentAppTemplate resolveTemplate(ComposeInfo composeInfo) {
        return templateService.findByAppTypeAndConfigTypeAndCurrentVersion(
                composeInfo.appType(), AgentAppConfigType.DOCKER_COMPOSE, composeInfo.version());
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
            if (service.isObject() && service.has("image")) {
                String image = service.get("image").asText();
                for (var t : AgentApplicationType.values()) {
                    if (t.getMainImagePattern() != null && RegexUtils.matches(image, t.getMainImagePattern())) {
                        String version = extractVersion(image);
                        return new ComposeInfo(t, image, version);
                    }
                }
            }
        }
        return ComposeInfo.GENERIC;
    }

    private String extractVersion(String image) {
        int colonIdx = image.lastIndexOf(':');
        return colonIdx >= 0 ? image.substring(colonIdx + 1) : null;
    }

    private JsonNode extractCompose(ComposeStateSync composeSync) {
        try {
            return JacksonUtil.OBJECT_MAPPER.readTree(composeSync.getComposeJson());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Couldn't parse docker compose");
        }
    }

    private record ComposeInfo(AgentApplicationType appType, String mainServiceImage, String version) {

        public static final ComposeInfo GENERIC = new ComposeInfo(AgentApplicationType.GENERIC, null, null);

        public String generatePlaceholderAppName(String projectName) {
            return this.appType + "_" + mainServiceImage + "_" + projectName;
        }
    }
}
