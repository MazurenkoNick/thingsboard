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
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.AgentGroup;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentGroupId;
import org.thingsboard.server.common.data.id.CustomerId;
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
            logEntityActionService.logEntityAction(tenantId, group.getId(), group, group.getCustomerId(), actionType, user, group.getId().toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.AGENT_GROUP), group, group.getCustomerId(), actionType, user, e, group.getId().toString());
            throw e;
        }
    }

    @Override
    public AgentGroup assignGroupToCustomer(TenantId tenantId, AgentGroupId groupId, Customer customer, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        CustomerId customerId = customer.getId();
        try {
            AgentGroup saved = checkNotNull(groupService.assignGroupToCustomer(tenantId, groupId, customerId));
            logEntityActionService.logEntityAction(tenantId, groupId, saved, customerId, actionType, user,
                    groupId.toString(), customerId.toString(), customer.getName());
            return saved;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.AGENT_GROUP), actionType, user, e,
                    groupId.toString(), customerId.toString());
            throw e;
        }
    }

    @Override
    public AgentGroup unassignGroupFromCustomer(TenantId tenantId, AgentGroupId groupId, Customer customer, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        try {
            AgentGroup saved = checkNotNull(groupService.unassignGroupFromCustomer(tenantId, groupId));
            CustomerId customerId = customer.getId();
            logEntityActionService.logEntityAction(tenantId, groupId, saved, customerId, actionType, user,
                    groupId.toString(), customerId.toString(), customer.getName());
            return saved;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.AGENT_GROUP), actionType, user, e, groupId.toString());
            throw e;
        }
    }
}
