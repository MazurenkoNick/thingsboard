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

import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentInfo;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

public interface AgentService extends EntityDaoService {
    Agent saveAgent(Agent agent);
    Agent findAgentById(TenantId tenantId, AgentId agentId);
    AgentInfo findAgentInfoById(TenantId tenantId, AgentId agentId);
    PageData<Agent> findAgentsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);
    PageData<AgentInfo> findAgentInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);
    PageData<Agent> findAgentsByTenantId(TenantId tenantId, PageLink pageLink);
    PageData<AgentInfo> findAgentInfosByTenantId(TenantId tenantId, PageLink pageLink);
    void deleteAgent(TenantId tenantId, AgentId agentId);
    void unassignCustomerAgents(TenantId tenantId, CustomerId customerId);
    Agent unassignAgentFromCustomer(TenantId tenantId, AgentId agentId);
    Agent assignAgentToCustomer(TenantId tenantId, AgentId agentId, CustomerId customerId);
}
