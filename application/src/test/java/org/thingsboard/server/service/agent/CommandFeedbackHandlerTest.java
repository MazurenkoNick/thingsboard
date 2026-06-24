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
package org.thingsboard.server.service.agent;

import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.AgentAppEventStatusUpdate;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.gen.agent.v1.CommandId;
import org.thingsboard.server.gen.agent.v1.CommandResult;
import org.thingsboard.server.gen.agent.v1.StepId;
import org.thingsboard.server.service.agent.event.AgentEventErrorHandler;
import org.thingsboard.server.service.agent.event.AgentEventProcessor;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandFeedbackHandlerTest {

    @Mock
    private AgentAppEventService appEventService;
    @Mock
    private AgentEventProcessor agentEventProcessor;
    @Mock
    private AgentEventErrorHandler eventErrorHandler;
    @Mock
    private AgentContextComponent agentCtx;

    @InjectMocks
    private CommandFeedbackHandler handler;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentId AGENT_ID = new AgentId(UUID.randomUUID());
    private static final AgentAppEventId EVENT_ID = new AgentAppEventId(UUID.randomUUID());
    private static final UUID STEP_1_ID = UUID.randomUUID();
    private static final UUID STEP_2_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(agentCtx.getAgentEventExecutor()).thenReturn(MoreExecutors.newDirectExecutorService());
    }

    @Test
    void onCommandResult_successForCurrentStep_advances() {
        AgentAppEvent event = newEvent(AgentAppEventStatus.PROCESSING, STEP_1_ID);
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(event);

        handler.onCommandResult(TENANT_ID, AGENT_ID, successResult(STEP_1_ID));

        verify(appEventService).updateStatus(eq(EVENT_ID), any(AgentAppEventStatusUpdate.class));
        verify(agentEventProcessor).processNextStepOrFinish(TENANT_ID, AGENT_ID, event);
    }

    @Test
    void onCommandResult_duplicateForAlreadyAdvancedStep_skips() {
        // Event has already advanced to STEP_2, a duplicate success for STEP_1 must not advance again.
        AgentAppEvent event = newEvent(AgentAppEventStatus.PENDING, STEP_2_ID);
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(event);

        handler.onCommandResult(TENANT_ID, AGENT_ID, successResult(STEP_1_ID));

        verify(appEventService, never()).updateStatus(any(), any());
        verify(agentEventProcessor, never()).processNextStepOrFinish(any(), any(), any());
        verify(eventErrorHandler, never()).onFailure(any(), any(), any(), any(), any());
    }

    @Test
    void onCommandResult_terminatedEvent_skips() {
        AgentAppEvent event = newEvent(AgentAppEventStatus.FINISHED, STEP_1_ID);
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(event);

        handler.onCommandResult(TENANT_ID, AGENT_ID, successResult(STEP_1_ID));

        verify(appEventService, never()).updateStatus(any(), any());
        verify(agentEventProcessor, never()).processNextStepOrFinish(any(), any(), any());
        verify(eventErrorHandler, never()).onFailure(any(), any(), any(), any(), any());
    }

    private AgentAppEvent newEvent(AgentAppEventStatus status, UUID currentStepId) {
        AgentAppEvent event = new AgentAppEvent();
        event.setId(EVENT_ID);
        event.setTenantId(TENANT_ID);
        event.setActionType(AgentAppEventActionType.INSTALL);
        event.setStatus(status);
        event.setCurrentStepId(currentStepId);
        return event;
    }

    private CommandResult successResult(UUID stepId) {
        return CommandResult.newBuilder()
                .setCommandId(CommandId.newBuilder()
                        .setIdMSB(EVENT_ID.getId().getMostSignificantBits())
                        .setIdLSB(EVENT_ID.getId().getLeastSignificantBits())
                        .build())
                .setSuccess(true)
                .setStep(StepId.newBuilder()
                        .setIdMSB(stepId.getMostSignificantBits())
                        .setIdLSB(stepId.getLeastSignificantBits())
                        .build())
                .build();
    }
}
