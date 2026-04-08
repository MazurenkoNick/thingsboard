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
package org.thingsboard.server.service.agent.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.AgentAppEventStatusUpdate;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.ErrorOrigin;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.AgentAppStepType;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.gen.agent.v1.ServerToAgent;
import org.thingsboard.server.dao.agent.AgentAppEventStepsResolver;
import org.thingsboard.server.gen.transport.TransportProtos.AgentAppEventNotificationProto;
import org.thingsboard.server.service.agent.AgentMsgConstructorUtils;
import org.thingsboard.server.service.agent.AgentRpcService;
import org.thingsboard.server.service.agent.session.AgentSessionRegistry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultAgentEventProcessorTest {

    @Mock
    private AgentRpcService agentRpcService;
    @Mock
    private AgentAppEventService appEventService;
    @Mock
    private AgentApplicationService appService;
    @Mock
    private AgentEventWatchdog eventWatchdog;
    @Mock
    private AgentAppEventStepsResolver eventStepsResolver;
    @Mock
    private AgentEventErrorHandler eventErrorHandler;
    @Mock
    private AgentSessionRegistry sessions;

    @InjectMocks
    private DefaultAgentEventProcessor processor;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentId AGENT_ID = new AgentId(UUID.randomUUID());
    private static final AgentAppEventId EVENT_ID = new AgentAppEventId(UUID.randomUUID());
    private static final AgentApplicationId APP_ID = new AgentApplicationId(UUID.randomUUID());

    private static final UUID STEP_1_ID = UUID.randomUUID();
    private static final UUID STEP_2_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(processor, "retrySendDelayMs", 1000L);
    }

    // ==================== processNextStepOrFinish ====================

    @Test
    void processNextStepOrFinish_hasNextStep_sendsIt() throws Exception {
        AgentAppEvent event = newEvent(AgentAppEventActionType.INSTALL);
        event.setCurrentStepId(STEP_1_ID);
        AgentApplication app = newApplication();
        AgentAppStep step1 = newStep(STEP_1_ID, STEP_2_ID);
        AgentAppStep step2 = newStep(STEP_2_ID, null);

        when(appService.findById(TENANT_ID, APP_ID)).thenReturn(app);
        when(eventStepsResolver.resolveSteps(app, AgentAppEventActionType.INSTALL)).thenReturn(List.of(step1, step2));

        try (MockedStatic<AgentMsgConstructorUtils> utils = mockStatic(AgentMsgConstructorUtils.class)) {
            utils.when(() -> AgentMsgConstructorUtils.buildAppCommand(any(), any(), any(), eq(2)))
                    .thenReturn(ServerToAgent.getDefaultInstance());
            when(agentRpcService.push(eq(AGENT_ID), any())).thenReturn(true);

            processor.processNextStepOrFinish(TENANT_ID, AGENT_ID, event);
        }

        verify(appEventService).updateStatus(EVENT_ID, AgentAppEventStatusUpdate.builder()
                .status(AgentAppEventStatus.PENDING).currentStepId(STEP_2_ID).build());
    }

    @Test
    void processNextStepOrFinish_noNextStep_finishesEvent() {
        AgentAppEvent event = newEvent(AgentAppEventActionType.INSTALL);
        event.setCurrentStepId(STEP_1_ID);
        AgentApplication app = newApplication();
        AgentAppStep step1 = newStep(STEP_1_ID, null); // last step

        when(appService.findById(TENANT_ID, APP_ID)).thenReturn(app);
        when(eventStepsResolver.resolveSteps(app, AgentAppEventActionType.INSTALL)).thenReturn(List.of(step1));
        when(appEventService.hasActiveEventForApplication(APP_ID)).thenReturn(false);
        when(appEventService.findOldestPendingByApplicationId(APP_ID)).thenReturn(Optional.empty());

        processor.processNextStepOrFinish(TENANT_ID, AGENT_ID, event);

        verify(appEventService).updateStatus(EVENT_ID, AgentAppEventStatusUpdate.builder()
                .status(AgentAppEventStatus.FINISHED).currentStepId(STEP_1_ID).build());
        verify(eventWatchdog).cancel(AGENT_ID, EVENT_ID);
    }

    @Test
    void processNextStepOrFinish_deleteEvent_finishDeletesApp() {
        AgentAppEvent event = newEvent(AgentAppEventActionType.DELETE);
        event.setCurrentStepId(STEP_1_ID);
        AgentApplication app = newApplication();
        AgentAppStep step = newStep(STEP_1_ID, null);

        when(appService.findById(TENANT_ID, APP_ID)).thenReturn(app);
        when(eventStepsResolver.resolveSteps(app, AgentAppEventActionType.DELETE)).thenReturn(List.of(step));

        processor.processNextStepOrFinish(TENANT_ID, AGENT_ID, event);

        verify(appEventService).updateStatus(EVENT_ID, AgentAppEventStatusUpdate.builder()
                .status(AgentAppEventStatus.FINISHED).currentStepId(STEP_1_ID).build());
        verify(appService).delete(TENANT_ID, APP_ID);
    }

    @Test
    void processNextStepOrFinish_appNotFound_marksError() {
        AgentAppEvent event = newEvent(AgentAppEventActionType.INSTALL);
        event.setCurrentStepId(STEP_1_ID);
        when(appService.findById(TENANT_ID, APP_ID)).thenReturn(null);

        processor.processNextStepOrFinish(TENANT_ID, AGENT_ID, event);

        verify(appEventService).updateStatus(EVENT_ID, AgentAppEventStatusUpdate.builder()
                .status(AgentAppEventStatus.ERROR).currentStepId(STEP_1_ID).build());
    }

    @Test
    void processNextStepOrFinish_exception_callsOnFailure() {
        AgentAppEvent event = newEvent(AgentAppEventActionType.INSTALL);
        when(appService.findById(TENANT_ID, APP_ID)).thenThrow(new RuntimeException("db error"));

        processor.processNextStepOrFinish(TENANT_ID, AGENT_ID, event);

        verify(eventErrorHandler).onFailure(eq(TENANT_ID), eq(AGENT_ID), eq(EVENT_ID), eq(ErrorOrigin.SERVER), any());
    }

    // ==================== processNextEventForApp (dispatch) ====================

    @Test
    void processNextEventForApp_activeEventExists_skips() {
        AgentApplication app = newApplication();
        when(appEventService.hasActiveEventForApplication(APP_ID)).thenReturn(true);

        processor.processNextEventForApp(TENANT_ID, AGENT_ID, app);

        verify(appEventService, never()).findOldestPendingByApplicationId(any());
    }

    @Test
    void processNextEventForApp_noPendingEvents_doesNothing() {
        AgentApplication app = newApplication();
        when(appEventService.hasActiveEventForApplication(APP_ID)).thenReturn(false);
        when(appEventService.findOldestPendingByApplicationId(APP_ID)).thenReturn(Optional.empty());

        processor.processNextEventForApp(TENANT_ID, AGENT_ID, app);

        verify(appEventService, never()).markDelivered(any());
    }

    @Test
    void processNextEventForApp_markDeliveredFails_skips() {
        AgentApplication app = newApplication();
        AgentAppEvent pendingEvent = newEvent(AgentAppEventActionType.INSTALL);

        when(appEventService.hasActiveEventForApplication(APP_ID)).thenReturn(false);
        when(appEventService.findOldestPendingByApplicationId(APP_ID)).thenReturn(Optional.of(pendingEvent));
        when(appEventService.markDelivered(EVENT_ID)).thenReturn(false);

        processor.processNextEventForApp(TENANT_ID, AGENT_ID, app);

        verify(eventStepsResolver, never()).resolveSteps(any(), any());
    }

    @Test
    void processNextEventForApp_dispatchesFirstStep() throws Exception {
        AgentApplication app = newApplication();
        AgentAppEvent pendingEvent = newEvent(AgentAppEventActionType.INSTALL);
        AgentAppStep step = newStep(STEP_1_ID, null);

        when(appEventService.hasActiveEventForApplication(APP_ID)).thenReturn(false);
        when(appEventService.findOldestPendingByApplicationId(APP_ID)).thenReturn(Optional.of(pendingEvent));
        when(appEventService.markDelivered(EVENT_ID)).thenReturn(true);
        when(eventStepsResolver.resolveSteps(app, AgentAppEventActionType.INSTALL)).thenReturn(List.of(step));

        try (MockedStatic<AgentMsgConstructorUtils> utils = mockStatic(AgentMsgConstructorUtils.class)) {
            utils.when(() -> AgentMsgConstructorUtils.buildAppCommand(any(), any(), any(), eq(1)))
                    .thenReturn(ServerToAgent.getDefaultInstance());
            when(agentRpcService.push(eq(AGENT_ID), any())).thenReturn(true);

            processor.processNextEventForApp(TENANT_ID, AGENT_ID, app);
        }

        verify(appEventService).updateStatus(EVENT_ID, AgentAppEventStatusUpdate.builder()
                .status(AgentAppEventStatus.PENDING).currentStepId(STEP_1_ID).build());
    }

    @Test
    void processNextEventForApp_dispatchException_callsOnFailure() {
        AgentApplication app = newApplication();
        AgentAppEvent pendingEvent = newEvent(AgentAppEventActionType.INSTALL);

        when(appEventService.hasActiveEventForApplication(APP_ID)).thenReturn(false);
        when(appEventService.findOldestPendingByApplicationId(APP_ID)).thenReturn(Optional.of(pendingEvent));
        when(appEventService.markDelivered(EVENT_ID)).thenReturn(true);
        when(eventStepsResolver.resolveSteps(app, AgentAppEventActionType.INSTALL)).thenThrow(new RuntimeException("bad steps"));

        processor.processNextEventForApp(TENANT_ID, AGENT_ID, app);

        verify(eventErrorHandler).onFailure(eq(TENANT_ID), eq(AGENT_ID), eq(EVENT_ID), eq(ErrorOrigin.SERVER), any());
    }

    // ==================== resumeEventsOnReconnect ====================

    @Test
    void resumeEventsOnReconnect_hasInFlightEvent_resumesIt() throws Exception {
        AgentApplication app = newApplication();
        AgentAppEvent inFlight = newEvent(AgentAppEventActionType.INSTALL);
        inFlight.setCurrentStepId(STEP_1_ID);
        AgentAppStep step = newStep(STEP_1_ID, null);

        when(appService.findByAgentId(eq(TENANT_ID), eq(AGENT_ID), any(PageLink.class)))
                .thenReturn(new PageData<>(List.of(app), 1, 1, false));
        when(appEventService.findActiveDeliveredByApplicationId(APP_ID)).thenReturn(Optional.of(inFlight));
        when(eventStepsResolver.resolveSteps(app, AgentAppEventActionType.INSTALL)).thenReturn(List.of(step));

        try (MockedStatic<AgentMsgConstructorUtils> utils = mockStatic(AgentMsgConstructorUtils.class)) {
            utils.when(() -> AgentMsgConstructorUtils.buildAppCommand(any(), any(), any(), eq(1)))
                    .thenReturn(ServerToAgent.getDefaultInstance());
            when(agentRpcService.push(eq(AGENT_ID), any())).thenReturn(true);

            processor.resumeEventsOnReconnect(TENANT_ID, AGENT_ID);
        }

        verify(appEventService).updateStatus(EVENT_ID, AgentAppEventStatusUpdate.builder()
                .status(AgentAppEventStatus.PENDING).currentStepId(STEP_1_ID).build());
    }

    @Test
    void resumeEventsOnReconnect_noInFlightEvent_dispatchesNext() {
        AgentApplication app = newApplication();

        when(appService.findByAgentId(eq(TENANT_ID), eq(AGENT_ID), any(PageLink.class)))
                .thenReturn(new PageData<>(List.of(app), 1, 1, false));
        when(appEventService.findActiveDeliveredByApplicationId(APP_ID)).thenReturn(Optional.empty());
        when(appEventService.hasActiveEventForApplication(APP_ID)).thenReturn(false);
        when(appEventService.findOldestPendingByApplicationId(APP_ID)).thenReturn(Optional.empty());

        processor.resumeEventsOnReconnect(TENANT_ID, AGENT_ID);

        verify(appEventService).findOldestPendingByApplicationId(APP_ID);
    }

    @Test
    void resumeEventsOnReconnect_resumeException_callsOnFailure() {
        AgentApplication app = newApplication();
        AgentAppEvent inFlight = newEvent(AgentAppEventActionType.INSTALL);

        when(appService.findByAgentId(eq(TENANT_ID), eq(AGENT_ID), any(PageLink.class)))
                .thenReturn(new PageData<>(List.of(app), 1, 1, false));
        when(appEventService.findActiveDeliveredByApplicationId(APP_ID)).thenReturn(Optional.of(inFlight));
        when(eventStepsResolver.resolveSteps(app, AgentAppEventActionType.INSTALL)).thenThrow(new RuntimeException("bad"));

        processor.resumeEventsOnReconnect(TENANT_ID, AGENT_ID);

        verify(eventErrorHandler).onFailure(eq(TENANT_ID), eq(AGENT_ID), eq(EVENT_ID), eq(ErrorOrigin.SERVER), any());
    }

    @Test
    void resumeEventsOnReconnect_exceptionOnOneApp_continuesWithOthers() {
        AgentApplication app1 = newApplication();
        AgentApplicationId app2Id = new AgentApplicationId(UUID.randomUUID());
        AgentApplication app2 = newApplication();
        app2.setId(app2Id);

        AgentAppEvent inFlight = newEvent(AgentAppEventActionType.INSTALL);

        when(appService.findByAgentId(eq(TENANT_ID), eq(AGENT_ID), any(PageLink.class)))
                .thenReturn(new PageData<>(List.of(app1, app2), 2, 1, false));
        when(appEventService.findActiveDeliveredByApplicationId(APP_ID)).thenReturn(Optional.of(inFlight));
        when(eventStepsResolver.resolveSteps(app1, AgentAppEventActionType.INSTALL)).thenThrow(new RuntimeException("bad"));
        when(appEventService.findActiveDeliveredByApplicationId(app2Id)).thenReturn(Optional.empty());
        when(appEventService.hasActiveEventForApplication(app2Id)).thenReturn(false);
        when(appEventService.findOldestPendingByApplicationId(app2Id)).thenReturn(Optional.empty());

        processor.resumeEventsOnReconnect(TENANT_ID, AGENT_ID);

        // app1 failed but app2 was still processed
        verify(appEventService).findOldestPendingByApplicationId(app2Id);
    }

    // ==================== onEventNotification ====================

    @Test
    void onEventNotification_noLocalSession_skips() {
        when(sessions.hasSession(AGENT_ID)).thenReturn(false);

        AgentAppEventNotificationProto notification = AgentAppEventNotificationProto.newBuilder()
                .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                .setAgentIdMSB(AGENT_ID.getId().getMostSignificantBits())
                .setAgentIdLSB(AGENT_ID.getId().getLeastSignificantBits())
                .setApplicationIdMSB(APP_ID.getId().getMostSignificantBits())
                .setApplicationIdLSB(APP_ID.getId().getLeastSignificantBits())
                .setEventIdMSB(EVENT_ID.getId().getMostSignificantBits())
                .setEventIdLSB(EVENT_ID.getId().getLeastSignificantBits())
                .setActionType(AgentAppEventActionType.INSTALL.name())
                .build();

        processor.onEventNotification(notification);

        verify(appService, never()).findById(any(), any());
        verify(appEventService, never()).findOldestPendingByApplicationId(any());
        verify(eventErrorHandler, never()).onFailure(any(), any(), any(), any(), any());
    }

    @Test
    void onEventNotification_hasLocalSession_processes() {
        when(sessions.hasSession(AGENT_ID)).thenReturn(true);

        AgentAppEventNotificationProto notification = AgentAppEventNotificationProto.newBuilder()
                .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                .setAgentIdMSB(AGENT_ID.getId().getMostSignificantBits())
                .setAgentIdLSB(AGENT_ID.getId().getLeastSignificantBits())
                .setApplicationIdMSB(APP_ID.getId().getMostSignificantBits())
                .setApplicationIdLSB(APP_ID.getId().getLeastSignificantBits())
                .setEventIdMSB(EVENT_ID.getId().getMostSignificantBits())
                .setEventIdLSB(EVENT_ID.getId().getLeastSignificantBits())
                .setActionType(AgentAppEventActionType.INSTALL.name())
                .build();

        AgentApplication app = newApplication();
        when(appService.findById(TENANT_ID, APP_ID)).thenReturn(app);
        when(appEventService.hasActiveEventForApplication(APP_ID)).thenReturn(false);
        when(appEventService.findOldestPendingByApplicationId(APP_ID)).thenReturn(Optional.empty());

        processor.onEventNotification(notification);

        verify(appService).findById(TENANT_ID, APP_ID);
    }

    // ==================== Helpers ====================

    private AgentAppEvent newEvent(AgentAppEventActionType actionType) {
        AgentAppEvent event = new AgentAppEvent();
        event.setId(EVENT_ID);
        event.setTenantId(TENANT_ID);
        event.setApplicationId(APP_ID);
        event.setActionType(actionType);
        return event;
    }

    private AgentApplication newApplication() {
        AgentApplication app = new AgentApplication();
        app.setId(APP_ID);
        app.setTenantId(TENANT_ID);
        app.setAgentId(AGENT_ID);
        return app;
    }

    private AgentAppStep newStep(UUID id, UUID nextId) {
        return new AgentAppStep(nextId, id, "Step " + id, false) {
            @Override
            public AgentAppStepType getType() {
                return AgentAppStepType.COMPOSE_START;
            }
        };
    }
}
