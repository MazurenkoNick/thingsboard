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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.AgentGroup;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentGroupService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@RequiredArgsConstructor
@TbCoreComponent
@Service
@Slf4j
public class DefaultTbAgentGroupService extends AbstractTbEntityService implements TbAgentGroupService {

    private final AgentGroupService groupService;

    @Override
    public AgentGroup save(AgentGroup group, User user) throws Exception {
        ActionType actionType = group.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = group.getTenantId();
        try {
            AgentGroup saved = checkNotNull(groupService.saveGroup(group));
            logEntityActionService.logEntityAction(tenantId, saved.getId(), saved, actionType, user);
            return saved;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.AGENT_GROUP), group, actionType, user, e);
            throw e;
        }
    }

    @Transactional
    @Override
    public void delete(AgentGroup group, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = group.getTenantId();
        try {
            groupService.deleteGroup(tenantId, group.getId());
            logEntityActionService.logEntityAction(tenantId, group.getId(), group, actionType, user, group.getId().toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.AGENT_GROUP), group, actionType, user, e, group.getId().toString());
            throw e;
        }
    }
}
