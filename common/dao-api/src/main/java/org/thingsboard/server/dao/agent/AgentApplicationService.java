/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.dao.agent;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

public interface AgentApplicationService extends EntityDaoService {

    AgentApplication save(TenantId tenantId, AgentApplication agentApplication);
    AgentApplication saveWithRelatedEntity(TenantId tenantId, AgentApplication agentApplication, EntityId relatedEntityId);
    AgentApplication findById(TenantId tenantId, AgentApplicationId agentApplicationId);
    AgentApplication findByIdForUpdate(TenantId tenantId, AgentApplicationId agentApplicationId);
    AgentApplicationInfo findInfoById(TenantId tenantId, AgentApplicationId agentApplicationId);
    AgentApplication findByProjectName(TenantId tenantId, AgentId agentId, String projectName);
    AgentApplication findByEventId(TenantId tenantId, AgentAppEventId agentAppEventId);
    PageData<AgentApplication> findByAgentId(TenantId tenantId, AgentId agentId, PageLink pageLink);
    PageData<AgentApplicationInfo> findInfosByAgentId(TenantId tenantId, AgentId agentId, PageLink pageLink);
    PageData<AgentApplication> findByEntityGroupId(EntityGroupId groupId, PageLink pageLink);
    PageData<AgentApplication> findByEntityGroupIds(List<EntityGroupId> groupIds, PageLink pageLink);
    AgentApplication findByRelatedEntity(TenantId tenantId, EntityId entityId);
    List<EntityId> findManagedRelatedEntityIds(TenantId tenantId, EntityType relatedEntityType);
    void delete(TenantId tenantId, AgentApplicationId agentApplicationId);
    void deleteByAgentId(TenantId tenantId, AgentId agentId);
    void promoteDesiredTemplate(TenantId tenantId, AgentApplicationId agentApplicationId);
    AgentApplication assignRelatedEntity(TenantId tenantId, AgentApplicationId agentApplicationId, EntityId relatedEntityId);
    AgentApplication unassignRelatedEntity(TenantId tenantId, AgentApplicationId agentApplicationId);

}
