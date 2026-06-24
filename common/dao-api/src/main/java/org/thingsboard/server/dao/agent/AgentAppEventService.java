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

import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventFilter;
import org.thingsboard.server.common.data.agent.AgentAppEventInfo;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.AgentAppEventStatusUpdate;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface AgentAppEventService extends EntityDaoService {

    AgentAppEvent save(TenantId tenantId, AgentAppEvent event);

    AgentAppEvent save(TenantId tenantId, AgentAppEvent event, boolean doValidate);

    AgentAppEvent findById(TenantId tenantId, AgentAppEventId id);

    Optional<AgentAppEvent> findOldestPendingByApplicationId(AgentApplicationId applicationId);

    boolean hasActiveEventForApplication(AgentApplicationId applicationId);

    boolean hasActiveOrPendingEventForApplication(AgentApplicationId applicationId);

    boolean existsByApplicationIdAndBulkActionId(AgentApplicationId applicationId, UUID bulkActionId);

    Optional<AgentAppEvent> findActiveDeliveredByApplicationId(AgentApplicationId applicationId);

    boolean markDelivered(AgentAppEventId id);

    boolean updateStatus(AgentAppEventId id, AgentAppEventStatusUpdate update);

    boolean updateResolvedArguments(AgentAppEventId id, Map<String, String> resolvedArguments);

    void deleteAllPendingByApplicationId(AgentApplicationId applicationId);

    int cleanUpExpiredEvents(long expirationTs);

    PageData<AgentAppEvent> findByBulkActionId(AgentBulkActionId bulkActionId, AgentAppEventActionType actionType,
                                               AgentAppEventStatus status, PageLink pageLink);

    PageData<AgentAppEventInfo> findInfosByBulkActionId(AgentBulkActionId bulkActionId, AgentAppEventActionType actionType,
                                                        AgentAppEventStatus status, PageLink pageLink);

    PageData<AgentAppEvent> findByFilter(AgentAppEventFilter filter, PageLink pageLink);

    PageData<AgentAppEvent> findByAgentId(TenantId tenantId, AgentId agentId, PageLink pageLink);

    PageData<AgentAppEventInfo> findInfosByAgentId(TenantId tenantId, AgentId agentId,
                                                   AgentAppEventActionType actionType,
                                                   AgentAppEventStatus status,
                                                   PageLink pageLink);
}
