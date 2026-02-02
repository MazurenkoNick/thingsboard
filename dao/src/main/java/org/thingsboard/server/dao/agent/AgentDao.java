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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.UUID;

public interface AgentDao extends Dao<Agent>, TenantEntityDao<Agent> {

    AgentInfo findAgentInfoById(TenantId tenantId, UUID agentId);
    PageData<Agent> findAgentsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);
    PageData<AgentInfo> findAgentInfosByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);
    PageData<Agent> findAgentsByTenantId(UUID id, PageLink pageLink);
    PageData<AgentInfo> findAgentInfosByTenantId(UUID id, PageLink pageLink);
    Long countByTenantId(TenantId tenantId);
}
