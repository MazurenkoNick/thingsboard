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
package org.thingsboard.server.service.entitiy.agent;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@AllArgsConstructor
@TbCoreComponent
@Service
@Slf4j
public class DefaultTbAgentService extends AbstractTbEntityService implements TbAgentService {

    private final AgentService agentService;

    @Override
    public Agent save(Agent agent, User user) throws Exception {
        ActionType actionType = agent.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = agent.getTenantId();

        try {
            Agent savedAgent = checkNotNull(agentService.saveAgent(agent));
            logEntityActionService.logEntityAction(tenantId, savedAgent.getId(), agent, actionType, user);
            return savedAgent;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.AGENT), agent, actionType, user);
            throw e;
        }
    }
}
