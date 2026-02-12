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
package org.thingsboard.server.service.agent.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentEventSourcingListener {

    private final TbClusterService tbClusterService;
    private final AgentApplicationService agentApplicationService;

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(SaveEntityEvent<?> event) {
        if (!(event.getEntity() instanceof AgentAppEvent agentAppEvent)) {
            return;
        }
        try {
            log.trace("[{}] AgentAppEvent SaveEntityEvent: {}", event.getTenantId(), agentAppEvent);
            var application = agentApplicationService.findById(
                    agentAppEvent.getTenantId(),
                    agentAppEvent.getApplicationId());
            if (application != null) {
                tbClusterService.onAgentAppEvent(
                        agentAppEvent.getTenantId(),
                        application.getAgentId(),
                        agentAppEvent);
            }
        } catch (Exception e) {
            log.error("[{}] Failed to process AgentAppEvent SaveEntityEvent: {}", event.getTenantId(), event, e);
        }
    }
}
