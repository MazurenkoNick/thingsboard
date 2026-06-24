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
package org.thingsboard.server.dao.sql.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventFilter;
import org.thingsboard.server.common.data.agent.AgentAppEventInfo;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.AgentAppEventStatusUpdate;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.agent.AgentAppEventDao;
import org.thingsboard.server.dao.model.sql.AgentAppEventEntity;
import org.thingsboard.server.dao.model.sql.AgentAppEventInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@SqlDao
@Slf4j
public class JpaAgentAppEventDao extends JpaAbstractDao<AgentAppEventEntity, AgentAppEvent> implements AgentAppEventDao {

    @Autowired
    private AgentAppEventRepository repository;

    private static final Map<String, String> EVENT_COLUMN_MAP = Map.of(
            "createdTime", "created_time",
            "updatedTime", "updated_time",
            "actionType", "action_type",
            "deliveryState", "delivery_state",
            "status", "status"
    );

    @Override
    protected Class<AgentAppEventEntity> getEntityClass() {
        return AgentAppEventEntity.class;
    }

    @Override
    protected JpaRepository<AgentAppEventEntity, UUID> getRepository() {
        return repository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_APP_EVENT;
    }

    @Override
    public Optional<AgentAppEvent> findOldestPendingByApplicationId(UUID applicationId) {
        return repository.findOldestPendingByApplicationId(applicationId).map(AgentAppEventEntity::toData);
    }

    @Override
    public boolean hasActiveEventForApplication(UUID applicationId) {
        return repository.hasActiveEventForApplication(applicationId);
    }

    @Override
    public boolean hasActiveOrPendingEventForApplication(UUID applicationId) {
        return repository.hasActiveOrPendingEventForApplication(applicationId);
    }

    @Override
    public boolean existsByApplicationIdAndBulkActionId(UUID applicationId, UUID bulkActionId) {
        return repository.existsByApplicationIdAndBulkActionId(applicationId, bulkActionId);
    }

    @Override
    public Optional<AgentAppEvent> findActiveDeliveredByApplicationId(UUID applicationId) {
        return repository.findActiveDeliveredByApplicationId(applicationId).map(AgentAppEventEntity::toData);
    }

    @Override
    public boolean markDelivered(UUID eventId) {
        return repository.markDelivered(eventId, System.currentTimeMillis()) == 1;
    }

    @Override
    public boolean updateStatus(UUID eventId, AgentAppEventStatusUpdate update) {
        return repository.updateStatus(eventId, update.getStatus(), update.getCurrentStepId(),
                update.getCurrentActivity(), update.getErrorMessage(), System.currentTimeMillis()) == 1;
    }

    @Override
    public boolean updateResolvedArguments(UUID eventId, Map<String, String> resolvedArguments) {
        return repository.updateResolvedArguments(eventId, JacksonUtil.toString(resolvedArguments)) == 1;
    }

    @Override
    public void deleteAllPendingByApplicationId(UUID applicationId) {
        repository.deleteAllPendingByApplicationId(applicationId);
    }

    @Override
    public int cleanUpExpiredEvents(long expirationTs) {
        return repository.deleteEventsUpdatedBefore(expirationTs);
    }

    @Override
    public PageData<AgentAppEvent> findByBulkActionId(UUID bulkActionId, AgentAppEventActionType actionType,
                                                      AgentAppEventStatus status, PageLink pageLink) {
        return DaoUtil.pageToPageData(
                repository.findByBulkActionId(
                                bulkActionId,
                                actionType != null ? actionType.name() : null,
                                status != null ? status.name() : null,
                                normalizeTextSearch(pageLink),
                                DaoUtil.toPageable(pageLink, EVENT_COLUMN_MAP))
                        .map(AgentAppEventEntity::toData)
        );
    }

    @Override
    public PageData<AgentAppEventInfo> findInfosByBulkActionId(UUID bulkActionId, AgentAppEventActionType actionType,
                                                               AgentAppEventStatus status, PageLink pageLink) {
        return DaoUtil.pageToPageData(
                repository.findInfosByBulkActionId(
                                bulkActionId,
                                actionType,
                                status,
                                normalizeTextSearch(pageLink),
                                DaoUtil.toPageable(pageLink))
                        .map(AgentAppEventInfoEntity::toData)
        );
    }

    @Override
    public PageData<AgentAppEvent> findByFilter(AgentAppEventFilter filter, PageLink pageLink) {
        return DaoUtil.pageToPageData(
                repository.findByFilter(
                                filter.getTenantId().getId(),
                                filter.getApplicationId().getId(),
                                filter.getActionType() != null ? filter.getActionType().name() : null,
                                filter.getStatus() != null ? filter.getStatus().name() : null,
                                normalizeTextSearch(pageLink),
                                DaoUtil.toPageable(pageLink, EVENT_COLUMN_MAP))
                        .map(AgentAppEventEntity::toData)
        );
    }

    private static String normalizeTextSearch(PageLink pageLink) {
        String ts = pageLink.getTextSearch();
        return (ts == null || ts.isBlank()) ? null : ts.trim();
    }

    @Override
    public PageData<AgentAppEvent> findByTenantIdAndAgentId(TenantId tenantId, AgentId agentId, PageLink pageLink) {
        return DaoUtil.pageToPageData(
                repository.findByTenantIdAndAgentId(
                                tenantId.getId(),
                                agentId.getId(),
                                DaoUtil.toPageable(pageLink))
                        .map(AgentAppEventEntity::toData)
        );
    }

    @Override
    public PageData<AgentAppEventInfo> findInfosByTenantIdAndAgentId(TenantId tenantId, AgentId agentId,
                                                                     AgentAppEventActionType actionType,
                                                                     AgentAppEventStatus status,
                                                                     PageLink pageLink) {
        return DaoUtil.pageToPageData(
                repository.findInfosByTenantIdAndAgentId(
                                tenantId.getId(),
                                agentId.getId(),
                                actionType,
                                status,
                                normalizeTextSearch(pageLink),
                                DaoUtil.toPageable(pageLink))
                        .map(AgentAppEventInfoEntity::toData)
        );
    }
}
