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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventDeliveryState;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.step.ComposeDownStep;
import org.thingsboard.server.common.data.agent.step.ComposeStep;
import org.thingsboard.server.common.data.agent.step.state.ComposeDownStepState;
import org.thingsboard.server.common.data.agent.step.state.ComposeStepState;
import org.thingsboard.server.common.data.agent.step.state.RollBackStepState;
import org.thingsboard.server.common.data.agent.step.state.StepField;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventStepsResolver;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentAppEventDataValidatorTest {

    @Mock
    private AgentApplicationDao agentApplicationDao;

    @Mock
    private AgentAppEventStepsResolver stepsResolver;

    @InjectMocks
    private AgentAppEventDataValidator validator;


    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentApplicationId APP_ID = new AgentApplicationId(UUID.randomUUID());

    @Test
    void validate_nullTenantId_throws() {
        AgentAppEvent event = validEvent();
        event.setTenantId(null);

        assertThatThrownBy(() -> validator.validate(event, AgentAppEvent::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    void validate_nullApplicationId_throws() {
        AgentAppEvent event = validEvent();
        event.setApplicationId(null);

        assertThatThrownBy(() -> validator.validate(event, AgentAppEvent::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("applicationId");
    }

    @Test
    void validate_nullActionType_throws() {
        AgentAppEvent event = validEvent();
        event.setActionType(null);

        assertThatThrownBy(() -> validator.validate(event, AgentAppEvent::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("actionType");
    }

    @Test
    void validate_nullDeliveryState_throws() {
        AgentAppEvent event = validEvent();
        event.setDeliveryState(null);

        assertThatThrownBy(() -> validator.validate(event, AgentAppEvent::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("deliveryState");
    }

    @Test
    void validate_deliveredWithNullStatus_throws() {
        AgentAppEvent event = validEvent();
        event.setDeliveryState(AgentAppEventDeliveryState.DELIVERED);
        event.setStatus(null);

        assertThatThrownBy(() -> validator.validate(event, AgentAppEvent::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("status");
    }

    @Test
    void validate_pendingWithNullStatus_doesNotThrowForStatus() {
        AgentAppEvent event = validEvent();
        event.setDeliveryState(AgentAppEventDeliveryState.PENDING);
        event.setStatus(null);
        when(agentApplicationDao.findById(any(), eq(APP_ID.getId()))).thenReturn(new AgentApplication());

        // Should not throw - status is not set when deliveryStatus = PENDING
        validator.validate(event, AgentAppEvent::getTenantId);
    }

    @Test
    void validate_nonExistentApplication_throws() {
        AgentAppEvent event = validEvent();
        when(agentApplicationDao.findById(any(), eq(APP_ID.getId()))).thenReturn(null);

        assertThatThrownBy(() -> validator.validate(event, AgentAppEvent::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("non-existent application");
    }

    @Test
    void validate_rollbackWithNullFailedEventId_throws() {
        AgentAppEvent event = validEvent();
        event.setActionType(AgentAppEventActionType.ROLLBACK);
        event.setStepStates(Map.of(UUID.randomUUID(), new RollBackStepState()));
        when(agentApplicationDao.findById(any(), eq(APP_ID.getId()))).thenReturn(new AgentApplication());

        assertThatThrownBy(() -> validator.validate(event, AgentAppEvent::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("failedEventId");
    }

    @Test
    void validate_rollbackWithFailedEventId_passes() {
        AgentAppEvent event = validEvent();
        event.setActionType(AgentAppEventActionType.ROLLBACK);
        event.setStepStates(Map.of(UUID.randomUUID(), new RollBackStepState(new AgentAppEventId(UUID.randomUUID()))));
        when(agentApplicationDao.findById(any(), eq(APP_ID.getId()))).thenReturn(new AgentApplication());

        validator.validate(event, AgentAppEvent::getTenantId);
    }

    @Test
    void validate_validInstallEvent_passes() {
        AgentAppEvent event = validEvent();
        when(agentApplicationDao.findById(any(), eq(APP_ID.getId()))).thenReturn(new AgentApplication());

        validator.validate(event, AgentAppEvent::getTenantId);
    }

    @Test
    void validate_userChoiceFalseField_noStepInputs_valid() {
        UUID id = UUID.randomUUID();
        ComposeStep composeStep = new ComposeStep();
        composeStep.setId(id);
        composeStep.setState(composeState(false));

        AgentAppEvent event = validEvent();
        event.setStepStates(null);
        when(agentApplicationDao.findById(any(), eq(APP_ID.getId()))).thenReturn(new AgentApplication());
        when(stepsResolver.resolveSteps(any(), eq(AgentAppEventActionType.INSTALL))).thenReturn(List.of(composeStep));

        assertDoesNotThrow(() -> validator.validate(event, AgentAppEvent::getTenantId));
    }

    @Test
    void validate_userChoiceTrueField_withStepInputs_valid() {
        UUID id = UUID.randomUUID();
        ComposeStep composeStep = new ComposeStep();
        composeStep.setId(id);
        composeStep.setState(composeState(true));

        AgentAppEvent event = validEvent();
        event.setStepStates(Map.of(id, composeState(true)));
        when(agentApplicationDao.findById(any(), eq(APP_ID.getId()))).thenReturn(new AgentApplication());
        when(stepsResolver.resolveSteps(any(), eq(AgentAppEventActionType.INSTALL))).thenReturn(List.of(composeStep));

        assertDoesNotThrow(() -> validator.validate(event, AgentAppEvent::getTenantId));
    }

    @Test
    void validate_userChoiceTrueField_noStepInputs_throws() {
        UUID id = UUID.randomUUID();
        ComposeStep composeStep = new ComposeStep();
        composeStep.setId(id);
        composeStep.setState(composeState(true));

        AgentAppEvent event = validEvent();
        event.setStepStates(null);
        when(agentApplicationDao.findById(any(), eq(APP_ID.getId()))).thenReturn(new AgentApplication());
        when(stepsResolver.resolveSteps(any(), eq(AgentAppEventActionType.INSTALL))).thenReturn(List.of(composeStep));

        assertThatThrownBy(() -> validator.validate(event, AgentAppEvent::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("step states must not be null");
    }

    @Test
    void validate_mixedSteps_onlyUserChoiceStepRequiresState() {
        UUID composeStepId = UUID.randomUUID();
        ComposeStep composeStep = new ComposeStep();
        composeStep.setId(composeStepId);
        composeStep.setState(composeState(false));

        UUID composeDownStepId = UUID.randomUUID();
        ComposeDownStep composeDownStep = new ComposeDownStep();
        composeDownStep.setId(composeDownStepId);
        composeDownStep.setState(composeDownState(true));

        AgentAppEvent event = validEvent();
        // provide state only for the step whose field is a user choice (ComposeDownStep)
        event.setStepStates(Map.of(composeDownStepId, composeDownState(true)));
        when(agentApplicationDao.findById(any(), eq(APP_ID.getId()))).thenReturn(new AgentApplication());
        when(stepsResolver.resolveSteps(any(), eq(AgentAppEventActionType.INSTALL)))
                .thenReturn(List.of(composeStep, composeDownStep));

        assertDoesNotThrow(() -> validator.validate(event, AgentAppEvent::getTenantId));
    }

    @Test
    void validate_mixedSteps_missingStateForUserChoiceStep_throws() {
        UUID composeStepId = UUID.randomUUID();
        ComposeStep composeStep = new ComposeStep();
        composeStep.setId(composeStepId);
        composeStep.setState(composeState(false));

        UUID composeDownStepId = UUID.randomUUID();
        ComposeDownStep composeDownStep = new ComposeDownStep();
        composeDownStep.setId(composeDownStepId);
        composeDownStep.setState(composeDownState(true));

        AgentAppEvent event = validEvent();
        // provide state only for compose step (not a user choice), not for compose down (which is)
        event.setStepStates(Map.of(composeStepId, composeDownState(true)));
        when(agentApplicationDao.findById(any(), eq(APP_ID.getId()))).thenReturn(new AgentApplication());
        when(stepsResolver.resolveSteps(any(), eq(AgentAppEventActionType.INSTALL)))
                .thenReturn(List.of(composeStep, composeDownStep));

        assertThatThrownBy(() -> validator.validate(event, AgentAppEvent::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("Step state is missing");
    }

    @Test
    void validate_userChoiceTrueField_nullValueSubmitted_throws() {
        UUID id = UUID.randomUUID();
        ComposeStep composeStep = new ComposeStep();
        composeStep.setId(id);
        composeStep.setState(composeState(true));

        ComposeStepState submitted = new ComposeStepState();
        submitted.setPullImages(new StepField<>(null, true)); // userChoice field present but no value

        AgentAppEvent event = validEvent();
        event.setStepStates(Map.of(id, submitted));
        when(agentApplicationDao.findById(any(), eq(APP_ID.getId()))).thenReturn(new AgentApplication());
        when(stepsResolver.resolveSteps(any(), eq(AgentAppEventActionType.INSTALL))).thenReturn(List.of(composeStep));

        assertThatThrownBy(() -> validator.validate(event, AgentAppEvent::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("missing required user inputs")
                .hasMessageContaining("pullImages");
    }

    private static ComposeStepState composeState(boolean userChoice) {
        ComposeStepState state = new ComposeStepState();
        state.setPullImages(new StepField<>(false, userChoice));
        return state;
    }

    private static ComposeDownStepState composeDownState(boolean userChoice) {
        ComposeDownStepState state = new ComposeDownStepState();
        state.setRemoveVolumes(new StepField<>(false, userChoice));
        return state;
    }

    private AgentAppEvent validEvent() {
        AgentAppEvent event = new AgentAppEvent();
        event.setTenantId(TENANT_ID);
        event.setApplicationId(APP_ID);
        event.setAgentId(new AgentId(UUID.randomUUID()));
        event.setActionType(AgentAppEventActionType.INSTALL);
        event.setDeliveryState(AgentAppEventDeliveryState.PENDING);
        event.setStatus(AgentAppEventStatus.QUEUED);
        return event;
    }

}
