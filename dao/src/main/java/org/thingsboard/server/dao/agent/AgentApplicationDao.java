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

    AgentApplication findByIdForUpdate(TenantId tenantId, UUID id);

    List<AgentApplication> findByAgentId(TenantId tenantId, UUID agentId);

    PageData<AgentApplication> findByAgentId(TenantId tenantId, UUID agentId, PageLink pageLink);

    List<AgentApplication> findByTemplateId(TenantId tenantId, UUID templateId);

    AgentApplication findByProjectName(TenantId tenantId, AgentId agentId, String projectName);

    AgentApplication findByEventId(TenantId tenantId, UUID eventId);

    AgentApplicationInfo findInfoById(TenantId tenantId, UUID id);

    PageData<AgentApplicationInfo> findInfosByAgentId(TenantId tenantId, UUID agentId, PageLink pageLink);

    PageData<AgentApplicationInfo> findByApplicationProfileIdAndAgentProfileId(TenantId tenantId, UUID profileId, UUID agentProfileId, PageLink pageLink);

    PageData<AgentApplication> findByEntityGroupId(UUID groupId, PageLink pageLink);

    PageData<AgentApplication> findByEntityGroupIds(List<UUID> groupIds, PageLink pageLink);

    AgentApplication findByRelatedEntity(TenantId tenantId, UUID relatedEntityId);

    List<UUID> findManagedRelatedEntityIds(TenantId tenantId, String relatedEntityType);

    int promoteDesiredTemplate(TenantId tenantId, AgentApplicationId applicationId, AgentAppTemplateId templateId);

}
