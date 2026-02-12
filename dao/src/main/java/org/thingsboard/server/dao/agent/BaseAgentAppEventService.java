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
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;

import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class BaseAgentAppEventService implements AgentAppEventService {

    private static final String INCORRECT_AGENT_APP_EVENT_ID = "Incorrect agentAppEventId ";

    @Autowired
    private AgentAppEventDao agentAppEventDao;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public AgentAppEvent save(TenantId tenantId, AgentAppEvent event) {
        log.trace("Executing saveAgentAppEvent [{}]", event);
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
    public List<AgentAppEvent> findPendingEventsByAgentId(AgentId agentId) {
        log.trace("Executing findPendingEventsByAgentId [{}]", agentId);
        return agentAppEventDao.findPendingEventsByAgentId(agentId.getId());
    }

    @Override
    public boolean markDelivered(AgentAppEventId id) {
        log.trace("Executing markDelivered [{}]", id);
        return agentAppEventDao.markDelivered(id.getId());
    }

    @Override
    public void updateStatus(AgentAppEventId id, AgentAppEventStatus status, String currentStepId) {
        log.trace("Executing updateStatus [{}] status [{}] stepId [{}]", id, status, currentStepId);
        agentAppEventDao.updateStatus(id.getId(), status, currentStepId);
    }

    @Override
    public List<AgentAppEvent> findStaleDeliveredEvents(long updatedTimeBefore) {
        return agentAppEventDao.findStaleDeliveredEvents(updatedTimeBefore);
    }

    @Override
    public void revertToPending(AgentAppEventId id) {
        log.trace("Executing revertToPending [{}]", id);
        agentAppEventDao.revertToPending(id.getId());
    }

    @Override
    public void deleteAllPendingByApplicationId(AgentApplicationId applicationId) {
        log.trace("Executing deleteAllPendingByApplicationId [{}]", applicationId);
        agentAppEventDao.deleteAllPendingByApplicationId(applicationId.getId());
    }
}
