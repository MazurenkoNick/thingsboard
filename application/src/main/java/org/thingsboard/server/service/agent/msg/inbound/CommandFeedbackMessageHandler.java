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
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.agent.v1.AgentToServer;
import org.thingsboard.server.service.agent.AgentInboundMsgCtx;
import org.thingsboard.server.service.agent.CommandFeedbackHandler;
import org.thingsboard.server.service.agent.session.AgentSessionState;

@Component
@Slf4j
@RequiredArgsConstructor
public class CommandFeedbackMessageHandler implements AgentInboundMessageHandler {

    private final CommandFeedbackHandler commandFeedbackHandler;

    @Override
    public boolean canHandle(AgentInboundMsgCtx msgCtx) {
        AgentToServer msg = msgCtx.msg();
        return msg.hasCommandAck() || msg.hasProgress() || msg.hasResult();
    }

    @Override
    public void handle(AgentInboundMsgCtx msgCtx) {
        AgentToServer msg = msgCtx.msg();
        AgentSessionState state = msgCtx.sessionState();
        AgentId agentId = state.getAgentId();
        TenantId tenantId = state.getAgent().getTenantId();

        if (msg.hasCommandAck()) {
            commandFeedbackHandler.onCommandAck(tenantId, agentId, msg.getCommandAck());
        } else if (msg.hasProgress()) {
            commandFeedbackHandler.onCommandProgress(tenantId, agentId, msg.getProgress());
        } else if (msg.hasResult()) {
            commandFeedbackHandler.onCommandResult(tenantId, agentId, msg.getResult());
        }
    }
}
