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
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.gen.agent.v1.ComposeState;
import org.thingsboard.server.gen.agent.v1.ContainerInfo;
import org.thingsboard.server.gen.agent.v1.ProjectStateSync;
import org.thingsboard.server.service.agent.AgentInboundMsgCtx;
import org.thingsboard.server.service.agent.compose.ComposeAgentAppCreator;
import org.thingsboard.server.service.agent.compose.ComposeUnitsSynchronizer;
import org.thingsboard.server.service.agent.compose.ImageDigestChecker;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ComposeProjectSyncMessageHandler implements AgentInboundMessageHandler {

    private final AgentApplicationService appService;
    private final ComposeAgentAppCreator appCreator;
    private final ComposeUnitsSynchronizer unitsSynchronizer;
    private final ImageDigestChecker imageDigestChecker;

    @Override
    public boolean canHandle(AgentInboundMsgCtx msgCtx) {
        var msg = msgCtx.msg();
        return msg.hasProjectSync()
                && msg.getProjectSync().hasCompose()
                && !msg.getProjectSync().getRemoved();
    }

    @Override
    public void handle(AgentInboundMsgCtx msgCtx) {
        ProjectStateSync projectSync = msgCtx.msg().getProjectSync();
        TenantId tenantId = msgCtx.sessionState().getTenantId();
        AgentId agentId = msgCtx.sessionState().getAgentId();

        try {
            doHandle(tenantId, agentId, projectSync);
        } catch (Exception e) {
            log.error("[{}][{}] Failed to process compose sync for project: {}", tenantId, agentId, projectSync.getProjectName(), e);
        }
    }

    private void doHandle(TenantId tenantId, AgentId agentId, ProjectStateSync projectSync) {
        String projectName = projectSync.getProjectName();
        ComposeState externalCompose = projectSync.getCompose();
        log.trace("[{}][{}] Processing externalCompose sync for project [{}], hasComposeJson: {}, containerStates: {}",
                tenantId, agentId, projectName, externalCompose.hasComposeJson(), externalCompose.getContainerStatesMap().keySet());

        AgentApplication app = appService.findByProjectName(tenantId, projectName);
        if (app == null) {
            if (!externalCompose.hasComposeJson()) {
                log.warn("[{}][{}] No agent application found for project [{}] and no externalComposeJson provided, skipping", tenantId, agentId, projectName);
                return;
            }
            app = appCreator.createApp(tenantId, agentId, projectName, externalCompose);
            if (app == null) {
                return;
            }
        }

        Map<String, ContainerInfo> containerStates = externalCompose.getContainerStatesMap();
        JsonNode externalComposeJson = null;
        if (externalCompose.hasComposeJson()) {
            log.trace("[{}][{}] Compose JSON changed for project [{}], syncing units and state", tenantId, agentId, projectName);
            externalComposeJson = parseComposeJson(externalCompose.getComposeJson());
            unitsSynchronizer.syncUnitsAndState(tenantId, app.getId(), externalComposeJson, containerStates);
        } else {
            log.trace("[{}][{}] Syncing project container states only [{}]", tenantId, agentId, projectName);
            unitsSynchronizer.syncState(tenantId, app.getId(), containerStates);
        }

        imageDigestChecker.checkImageDigest(tenantId, app, containerStates, externalComposeJson);
    }

    private JsonNode parseComposeJson(String json) {
        try {
            return JacksonUtil.OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Couldn't parse docker compose JSON", e);
        }
    }
}
