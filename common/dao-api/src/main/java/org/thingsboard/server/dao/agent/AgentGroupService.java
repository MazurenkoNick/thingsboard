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
package org.thingsboard.server.dao.agent;

import org.thingsboard.server.common.data.agent.AgentGroup;
import org.thingsboard.server.common.data.agent.AgentGroupInfo;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentGroupId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

public interface AgentGroupService extends EntityDaoService {

    AgentGroup saveGroup(AgentGroup group);

    AgentGroup findGroupById(TenantId tenantId, AgentGroupId groupId);

    AgentGroup findGroupByProvisionKey(String provisionKey);

    AgentGroupInfo findGroupInfoById(TenantId tenantId, AgentGroupId groupId);

    PageData<AgentGroup> findGroupsByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<AgentGroupInfo> findGroupInfosByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<AgentGroup> findGroupsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    void deleteGroup(TenantId tenantId, AgentGroupId groupId);

    AgentGroup assignGroupToCustomer(TenantId tenantId, AgentGroupId groupId, CustomerId customerId);

    AgentGroup unassignGroupFromCustomer(TenantId tenantId, AgentGroupId groupId);

    void assignProfileToGroup(TenantId tenantId, AgentGroupId groupId, AgentAppProfileId profileId);

    void unassignProfileFromGroup(TenantId tenantId, AgentGroupId groupId, AgentAppProfileId profileId);

    List<EntityRelation> findProfileRelations(TenantId tenantId, AgentGroupId groupId);
}
