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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppUnitService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.gen.agent.v1.ProjectStateSync;
import org.thingsboard.server.service.agent.AgentInboundMsgCtx;

@Component
@Slf4j
@RequiredArgsConstructor
public class ComposeProjectRemovedMessageHandler implements AgentInboundMessageHandler {

    private final AgentAppUnitService agentAppUnitService;
    private final AgentApplicationService appService;

    @Override
    public boolean canHandle(AgentInboundMsgCtx msgCtx) {
        var msg = msgCtx.msg();
        return msg.hasProjectSync() && msg.getProjectSync().getRemoved();
    }

    @Override
    public void handle(AgentInboundMsgCtx msgCtx) {
        TenantId tenantId = msgCtx.sessionState().getTenantId();
        AgentId agentId = msgCtx.sessionState().getAgentId();
        ProjectStateSync projectSync = msgCtx.msg().getProjectSync();

        try {
            AgentApplication app = appService.findByProjectName(tenantId, agentId, projectSync.getProjectName());
            if (app == null) {
                return;
            }
            log.trace("[{}][{}] Processing project removal [{}]", tenantId, agentId, projectSync.getProjectName());
            agentAppUnitService.deleteByAgentApplicationId(tenantId, app.getId());
        } catch (Exception e) {
            log.error("[{}][{}] Failed to process project removal for project: {}", tenantId, agentId, projectSync.getProjectName(), e);
        }
    }
}
