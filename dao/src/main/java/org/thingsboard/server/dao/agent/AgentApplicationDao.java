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

import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

public interface AgentApplicationDao extends Dao<AgentApplication> {

    void removeByAgentId(TenantId tenantId, UUID agentId);

    void removeByTemplateId(TenantId tenantId, UUID templateId);

    List<AgentApplication> findByAgentId(TenantId tenantId, UUID agentId);

    PageData<AgentApplication> findByAgentId(TenantId tenantId, UUID agentId, PageLink pageLink);

    List<AgentApplication> findByTemplateId(TenantId tenantId, UUID templateId);

    AgentApplication findByProjectName(TenantId tenantId, AgentId agentId, String projectName);

    AgentApplication findByEventId(TenantId tenantId, UUID eventId);

    AgentApplicationInfo findInfoById(TenantId tenantId, UUID id);

    PageData<AgentApplicationInfo> findInfosByAgentId(TenantId tenantId, UUID agentId, PageLink pageLink);

    PageData<AgentApplicationInfo> findByApplicationProfileIdAndAgentGroupId(UUID profileId, UUID groupId, PageLink pageLink);

    AgentApplication findByRelatedEntity(TenantId tenantId, UUID relatedEntityId, String relatedEntityType);

    int promoteDesiredTemplate(TenantId tenantId, AgentApplicationId applicationId, AgentAppTemplateId templateId);

}
