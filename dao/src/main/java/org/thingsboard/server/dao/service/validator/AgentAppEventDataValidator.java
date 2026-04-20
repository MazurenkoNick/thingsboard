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
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventDeliveryState;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventStepsResolver;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@AllArgsConstructor
public class AgentAppEventDataValidator extends DataValidator<AgentAppEvent> {

    private final AgentApplicationDao agentApplicationDao;
    private final AgentAppEventStepsResolver stepsResolver;

    @Override
    protected void validateDataImpl(TenantId tenantId, AgentAppEvent event) {
        if (event.getTenantId() == null) {
            throw new DataValidationException("Agent app event tenantId must not be null!");
        }
        if (event.getApplicationId() == null) {
            throw new DataValidationException("Agent app event applicationId must not be null!");
        }
        if (event.getAgentId() == null) {
            throw new DataValidationException("Agent app event agentId must not be null!");
        }
        if (event.getActionType() == null) {
            throw new DataValidationException("Agent app event actionType must not be null!");
        }
        if (event.getDeliveryState() == null) {
            throw new DataValidationException("Agent app event deliveryState must not be null!");
        }
        if (event.getDeliveryState() != AgentAppEventDeliveryState.PENDING && event.getStatus() == null) {
            throw new DataValidationException("Agent app event status must not be null!");
        }
        AgentApplication app = agentApplicationDao.findById(tenantId, event.getApplicationId().getId());
        if (app == null) {
            throw new DataValidationException("Agent app event references non-existent application!");
        }

        Map<UUID, AgentAppStepState> stepStates = event.getStepStates();
        if (!CollectionUtils.isEmpty(stepStates)) {
            stepStates.values().forEach(AgentAppStepState::validate);
        }

        List<AgentAppStep> statefulAppStepsWithoutDefaultState = stepsResolver.resolveSteps(app, event.getActionType())
                .stream()
                .filter(AgentAppStep::isStateful)
                .filter(AgentAppStep::hasNoDefaultState)
                .toList();

        if (!CollectionUtils.isEmpty(statefulAppStepsWithoutDefaultState) && CollectionUtils.isEmpty(stepStates)) {
                throw new DataValidationException("Agent app step states must not be null!");
        }

        statefulAppStepsWithoutDefaultState.stream()
                .filter(s -> !stepStates.containsKey(s.getId()))
                .findAny()
                .ifPresent(s -> {
                    log.trace("Step state is missing!, {}", s.getId());
                    throw new DataValidationException("Step state is missing!");
                });
    }
}
