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
                .filter(step -> !step.hasDefaultState())
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
