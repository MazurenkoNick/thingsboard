/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.springframework.stereotype.Service;
import org.thingsboard.server.service.agent.AgentInboundMsgCtx;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BaseAgentInboundMessageDispatcher implements AgentInboundMessageDispatcher {

    private final List<AgentInboundMessageHandler> agentInboundMessageHandlerList;

    @Override
    public void process(AgentInboundMsgCtx ctx) {
        agentInboundMessageHandlerList.stream()
                .filter(h -> h.canHandle(ctx))
                .forEach(h -> h.handle(ctx));
    }
}
