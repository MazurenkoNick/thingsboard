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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventDeliveryState;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.ErrorOrigin;
import org.thingsboard.server.common.data.agent.RollbackEventMeta;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentApplicationService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentEventErrorHandlerTest {

    @Mock
    private AgentAppEventService appEventService;
    @Mock
    private AgentApplicationService appService;
    @Mock
    private AgentEventWatchdog eventWatchdog;
    @Mock
    private AgentEventProcessor agentEventProcessor;
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private AgentEventErrorHandler errorHandler;

    @BeforeEach
    void setUp() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                invocation.<org.springframework.transaction.support.TransactionCallback<?>>getArgument(0)
                        .doInTransaction(null));
    }

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentId AGENT_ID = new AgentId(UUID.randomUUID());
    private static final AgentAppEventId EVENT_ID = new AgentAppEventId(UUID.randomUUID());
    private static final AgentApplicationId APP_ID = new AgentApplicationId(UUID.randomUUID());

    // ==================== Common behavior ====================

    @Test
    void onFailure_alwaysMarksErrorAndCancelsWatchdog() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(newEvent(AgentAppEventActionType.INSTALL));
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(newApplication());

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.SERVER);

        verify(appEventService).updateStatus(EVENT_ID, AgentAppEventStatus.ERROR, null);
        verify(eventWatchdog).cancel(AGENT_ID, EVENT_ID);
    }

    // ==================== Dispatch next (queue must never stall) ====================

    @Test
    void onFailure_install_serverError_dispatchesNext() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(newEvent(AgentAppEventActionType.INSTALL));
        AgentApplication app = newApplication();
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(app);

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.SERVER);

        verify(agentEventProcessor).processNextEventForApp(TENANT_ID, AGENT_ID, app);
    }

    @Test
    void onFailure_install_agentError_dispatchesNext() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(newEvent(AgentAppEventActionType.INSTALL));
        AgentApplication app = newApplication();
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(app);

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.AGENT);

        verify(agentEventProcessor).processNextEventForApp(TENANT_ID, AGENT_ID, app);
    }

    @Test
    void onFailure_restart_dispatchesNext() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(newEvent(AgentAppEventActionType.RESTART));
        AgentApplication app = newApplication();
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(app);

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.AGENT);

        verify(agentEventProcessor).processNextEventForApp(TENANT_ID, AGENT_ID, app);
    }

    @Test
    void onFailure_eventNotFound_dispatchesNext() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(null);
        AgentApplication app = newApplication();
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(app);

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.SERVER);

        verify(agentEventProcessor).processNextEventForApp(TENANT_ID, AGENT_ID, app);
    }

    @Test
    void onFailure_applicationNotFound_doesNotThrow() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(newEvent(AgentAppEventActionType.INSTALL));
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(null);

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.SERVER);

        verify(agentEventProcessor, never()).processNextEventForApp(any(), any(), any());
    }

    // ==================== DELETE → rollback pending deletion ====================

    @Test
    void onFailure_delete_rollbacksPendingDeletion() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(newEvent(AgentAppEventActionType.DELETE));
        AgentApplication app = newApplication();
        app.setPendingDeletion(true);
        when(appService.findById(TENANT_ID, APP_ID)).thenReturn(app);
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(app);

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.AGENT);

        assertThat(app.isPendingDeletion()).isFalse();
        verify(appService).save(TENANT_ID, app);
    }

    @Test
    void onFailure_delete_dispatchesNext() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(newEvent(AgentAppEventActionType.DELETE));
        AgentApplication app = newApplication();
        when(appService.findById(TENANT_ID, APP_ID)).thenReturn(app);
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(app);

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.SERVER);

        verify(agentEventProcessor).processNextEventForApp(TENANT_ID, AGENT_ID, app);
    }

    @Test
    void onFailure_delete_doesNotCreateRollbackEvent() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(newEvent(AgentAppEventActionType.DELETE));
        AgentApplication app = newApplication();
        when(appService.findById(TENANT_ID, APP_ID)).thenReturn(app);
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(app);

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.SERVER);

        verify(appEventService, never()).save(any(), any());
    }

    // ==================== SERVER + UPDATE → enqueue rollback event ====================

    @Test
    void onFailure_serverUpdate_createsRollbackEvent() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(newEvent(AgentAppEventActionType.UPDATE));

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.SERVER);

        ArgumentCaptor<AgentAppEvent> captor = ArgumentCaptor.forClass(AgentAppEvent.class);
        verify(appEventService).save(eq(TENANT_ID), captor.capture());

        AgentAppEvent rollback = captor.getValue();
        assertThat(rollback.getApplicationId()).isEqualTo(APP_ID);
        assertThat(rollback.getActionType()).isEqualTo(AgentAppEventActionType.ROLLBACK);
        assertThat(rollback.getDeliveryState()).isEqualTo(AgentAppEventDeliveryState.DELIVERED);
        assertThat(rollback.getStatus()).isEqualTo(AgentAppEventStatus.PENDING);
        assertThat(rollback.getMetadata()).isInstanceOf(RollbackEventMeta.class);
        assertThat(((RollbackEventMeta) rollback.getMetadata()).getFailedEventId()).isEqualTo(EVENT_ID);
    }

    @Test
    void onFailure_serverUpdate_doesNotDispatchNext() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(newEvent(AgentAppEventActionType.UPDATE));

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.SERVER);

        verify(agentEventProcessor, never()).processNextEventForApp(any(), any(), any());
    }

    // ==================== AGENT + UPDATE → no rollback, just dispatch ====================

    @Test
    void onFailure_agentUpdate_doesNotCreateRollbackEvent() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(newEvent(AgentAppEventActionType.UPDATE));
        AgentApplication app = newApplication();
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(app);

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.AGENT);

        verify(appEventService, never()).save(any(), any());
        verify(agentEventProcessor).processNextEventForApp(TENANT_ID, AGENT_ID, app);
    }

    // ==================== ROLLBACK failure → no recursive rollback ====================

    @Test
    void onFailure_rollbackEvent_serverError_doesNotCreateAnotherRollback() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(newEvent(AgentAppEventActionType.ROLLBACK));
        AgentApplication app = newApplication();
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(app);

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.SERVER);

        verify(appEventService, never()).save(any(), any());
        verify(agentEventProcessor).processNextEventForApp(TENANT_ID, AGENT_ID, app);
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
}
