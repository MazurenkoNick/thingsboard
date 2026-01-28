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
import org.thingsboard.server.service.agent.AgentInboundMsgCtx;
import org.thingsboard.server.service.agent.AgentWorkflowProcessorService;

@Component
@Slf4j
@RequiredArgsConstructor
public class AgentCommandResultInboundHandler implements AgentInboundMessageHandler {

    private final AgentWorkflowProcessorService agentWorkflowProcessorService;

    @Override
    public boolean canHandle(AgentInboundMsgCtx msgCtx) {
        return msgCtx != null && msgCtx.msg() != null && msgCtx.msg().hasResult();
    }

    @Override
    public void handle(AgentInboundMsgCtx msgCtx) {
        try {
            agentWorkflowProcessorService.onStepResult(msgCtx.session(), msgCtx.msg().getResult());
        } catch (Exception e) {
            log.warn("[{}] Failed to process agent CommandAck", msgCtx.sessionState().getAgentId(), e);
        }
    }
}


