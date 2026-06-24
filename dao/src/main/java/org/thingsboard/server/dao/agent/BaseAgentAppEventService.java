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

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
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
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class BaseAgentAppEventService implements AgentAppEventService {

    private static final String INCORRECT_AGENT_APP_EVENT_ID = "Incorrect agentAppEventId ";

    @Autowired
    private AgentAppEventDao agentAppEventDao;

    @Autowired
    private DataValidator<AgentAppEvent> agentAppEventValidator;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public AgentAppEvent save(TenantId tenantId, AgentAppEvent event) {
        return save(tenantId, event, true);
    }

    @Override
    public AgentAppEvent save(TenantId tenantId, AgentAppEvent event, boolean doValidate) {
        log.trace("Executing saveAgentAppEvent [{}]", event);
        if (doValidate) {
            agentAppEventValidator.validate(event, AgentAppEvent::getTenantId);
        }
        AgentAppEvent saved = agentAppEventDao.save(event.getTenantId(), event);
        eventPublisher.publishEvent(SaveEntityEvent.builder()
                .tenantId(saved.getTenantId())
                .entityId(saved.getId())
                .entity(saved)
                .created(event.getId() == null)
                .build());
        return saved;
    }

    @Override
    public AgentAppEvent findById(TenantId tenantId, AgentAppEventId id) {
        log.trace("Executing findAgentAppEventById [{}]", id);
        validateId(id, i -> INCORRECT_AGENT_APP_EVENT_ID + i);
        return agentAppEventDao.findById(tenantId, id.getId());
    }

    @Override
    public Optional<AgentAppEvent> findOldestPendingByApplicationId(AgentApplicationId applicationId) {
        log.trace("Executing findOldestPendingByApplicationId [{}]", applicationId);
        return agentAppEventDao.findOldestPendingByApplicationId(applicationId.getId());
    }

    @Override
    public boolean hasActiveEventForApplication(AgentApplicationId applicationId) {
        return agentAppEventDao.hasActiveEventForApplication(applicationId.getId());
    }

    @Override
    public boolean hasActiveOrPendingEventForApplication(AgentApplicationId applicationId) {
        return agentAppEventDao.hasActiveOrPendingEventForApplication(applicationId.getId());
    }

    @Override
    public boolean existsByApplicationIdAndBulkActionId(AgentApplicationId applicationId, UUID bulkActionId) {
        return agentAppEventDao.existsByApplicationIdAndBulkActionId(applicationId.getId(), bulkActionId);
    }

    @Override
    public Optional<AgentAppEvent> findActiveDeliveredByApplicationId(AgentApplicationId applicationId) {
        return agentAppEventDao.findActiveDeliveredByApplicationId(applicationId.getId());
    }

    @Override
    public boolean markDelivered(AgentAppEventId id) {
        log.trace("Executing markDelivered [{}]", id);
        return agentAppEventDao.markDelivered(id.getId());
    }

    @Override
    public boolean updateStatus(AgentAppEventId id, AgentAppEventStatusUpdate update) {
        log.trace("Executing updateStatus [{}] update [{}]", id, update);
        return agentAppEventDao.updateStatus(id.getId(), update);
    }

    @Override
    public boolean updateResolvedArguments(AgentAppEventId id, Map<String, String> resolvedArguments) {
        log.trace("Executing updateResolvedArguments [{}]", id);
        return agentAppEventDao.updateResolvedArguments(id.getId(), resolvedArguments);
    }

    @Override
    public void deleteAllPendingByApplicationId(AgentApplicationId applicationId) {
        log.trace("Executing deleteAllPendingByApplicationId [{}]", applicationId);
        agentAppEventDao.deleteAllPendingByApplicationId(applicationId.getId());
    }

    @Override
    public int cleanUpExpiredEvents(long expirationTs) {
        log.trace("Executing cleanUpExpiredEvents before [{}]", expirationTs);
        return agentAppEventDao.cleanUpExpiredEvents(expirationTs);
    }

    @Override
    public PageData<AgentAppEvent> findByBulkActionId(AgentBulkActionId bulkActionId, AgentAppEventActionType actionType,
                                                      AgentAppEventStatus status, PageLink pageLink) {
        log.trace("Executing findByBulkActionId [{}] actionType [{}] status [{}]", bulkActionId, actionType, status);
        return agentAppEventDao.findByBulkActionId(bulkActionId.getId(), actionType, status, pageLink);
    }

    @Override
    public PageData<AgentAppEventInfo> findInfosByBulkActionId(AgentBulkActionId bulkActionId, AgentAppEventActionType actionType,
                                                               AgentAppEventStatus status, PageLink pageLink) {
        log.trace("Executing findInfosByBulkActionId [{}] actionType [{}] status [{}]", bulkActionId, actionType, status);
        return agentAppEventDao.findInfosByBulkActionId(bulkActionId.getId(), actionType, status, pageLink);
    }

    @Override
    public PageData<AgentAppEvent> findByFilter(AgentAppEventFilter filter, PageLink pageLink) {
        log.trace("Executing findAgentAppEventsByFilter [{}]", filter);
        return agentAppEventDao.findByFilter(filter, pageLink);
    }

    @Override
    public PageData<AgentAppEvent> findByAgentId(TenantId tenantId, AgentId agentId, PageLink pageLink) {
        log.trace("Executing findAgentAppEventsByAgentId [{}]", agentId);
        return agentAppEventDao.findByTenantIdAndAgentId(tenantId, agentId, pageLink);
    }

    @Override
    public PageData<AgentAppEventInfo> findInfosByAgentId(TenantId tenantId, AgentId agentId,
                                                          AgentAppEventActionType actionType,
                                                          AgentAppEventStatus status,
                                                          PageLink pageLink) {
        log.trace("Executing findAgentAppEventInfosByAgentId [{}] actionType [{}] status [{}]",
                agentId, actionType, status);
        return agentAppEventDao.findInfosByTenantIdAndAgentId(tenantId, agentId, actionType, status, pageLink);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findById(tenantId, new AgentAppEventId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        ListenableFuture<AgentAppEvent> future = agentAppEventDao.findByIdAsync(tenantId, entityId.getId());
        return FluentFuture.from(future).transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_APP_EVENT;
    }
}
