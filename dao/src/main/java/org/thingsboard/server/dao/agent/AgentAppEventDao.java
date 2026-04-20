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

import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventFilter;
import org.thingsboard.server.common.data.agent.AgentAppEventInfo;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.AgentAppEventStatusUpdate;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentAppEventDao extends Dao<AgentAppEvent> {

    Optional<AgentAppEvent> findOldestPendingByApplicationId(UUID applicationId);

    boolean hasActiveEventForApplication(UUID applicationId);

    boolean existsByApplicationIdAndBulkActionId(UUID applicationId, UUID bulkActionId);

    Optional<AgentAppEvent> findActiveDeliveredByApplicationId(UUID applicationId);

    boolean markDelivered(UUID eventId);

    void updateStatus(UUID eventId, AgentAppEventStatusUpdate update);

    void deleteAllPendingByApplicationId(UUID applicationId);

    int cleanUpExpiredEvents(long expirationTs);

    PageData<AgentAppEvent> findByBulkActionId(UUID bulkActionId, AgentAppEventStatus status, PageLink pageLink);

    PageData<AgentAppEvent> findByFilter(AgentAppEventFilter filter, PageLink pageLink);

    PageData<AgentAppEvent> findByTenantIdAndAgentId(TenantId tenantId, AgentId agentId, PageLink pageLink);

    PageData<AgentAppEventInfo> findInfosByTenantIdAndAgentId(TenantId tenantId, AgentId agentId,
                                                              AgentAppEventActionType actionType,
                                                              AgentAppEventStatus status,
                                                              PageLink pageLink);
}
