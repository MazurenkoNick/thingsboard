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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.Optional;
import java.util.UUID;

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
        log.trace("Executing saveAgentAppEvent [{}]", event);
        agentAppEventValidator.validate(event, AgentAppEvent::getTenantId);
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
    public void updateStatus(AgentAppEventId id, AgentAppEventStatus status, UUID currentStepId) {
        log.trace("Executing updateStatus [{}] status [{}] stepId [{}]", id, status, currentStepId);
        agentAppEventDao.updateStatus(id.getId(), status, currentStepId);
    }

    @Override
    public void deleteAllPendingByApplicationId(AgentApplicationId applicationId) {
        log.trace("Executing deleteAllPendingByApplicationId [{}]", applicationId);
        agentAppEventDao.deleteAllPendingByApplicationId(applicationId.getId());
    }

    @Override
    public PageData<AgentAppEvent> findByBulkActionId(AgentBulkActionId bulkActionId, AgentAppEventStatus status, PageLink pageLink) {
        log.trace("Executing findByBulkActionId [{}] status [{}]", bulkActionId, status);
        return agentAppEventDao.findByBulkActionId(bulkActionId.getId(), status, pageLink);
    }

    @Override
    public PageData<AgentAppEvent> findByApplicationId(TenantId tenantId, AgentApplicationId applicationId, PageLink pageLink) {
        log.trace("Executing findAgentAppEventsByApplicationId [{}]", applicationId);
        return agentAppEventDao.findByTenantIdAndApplicationId(tenantId, applicationId, pageLink);
    }

    @Override
    public PageData<AgentAppEvent> findByAgentId(TenantId tenantId, AgentId agentId, PageLink pageLink) {
        log.trace("Executing findAgentAppEventsByAgentId [{}]", agentId);
        return agentAppEventDao.findByTenantIdAndAgentId(tenantId, agentId, pageLink);
    }
}
