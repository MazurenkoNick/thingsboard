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
package org.thingsboard.server.service.entitiy.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventDeliveryState;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentAppInstallResponse;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.state.ComposeDownStepState;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.agent.config.AgentAppConfigMergeOrchestrator;
import org.thingsboard.server.service.agent.AgentEventRateLimiter;
import org.thingsboard.server.service.agent.action.DeleteAppActionHandler;
import org.thingsboard.server.service.agent.action.UpdateActionHandler;
import org.thingsboard.server.service.agent.action.UpgradeActionHandler;
import org.thingsboard.server.service.entitiy.TbLogEntityActionService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultTbAgentApplicationServiceTest {

    @Mock
    private AgentAppConfigMergeOrchestrator templateMergeOrchestrator;
    @Mock
    private AgentApplicationService agentApplicationService;
    @Mock
    private AgentAppEventService agentAppEventService;
    @Mock
    private TbClusterService tbClusterService;
    @Mock
    private TbLogEntityActionService logEntityActionService;
    @Mock
    private UpdateActionHandler updateActionHandler;
    @Mock
    private UpgradeActionHandler upgradeActionHandler;
    @Mock
    private DeleteAppActionHandler deleteAppActionHandler;
    @Mock
    private AgentEventRateLimiter agentEventRateLimiter;

    private DefaultTbAgentApplicationService service;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentApplicationId APP_ID = new AgentApplicationId(UUID.randomUUID());
    private static final AgentAppEventId EVENT_ID = new AgentAppEventId(UUID.randomUUID());
    private static final AgentId AGENT_ID = new AgentId(UUID.randomUUID());
    private static final User USER;

    static {
        USER = new User();
        USER.setTenantId(TENANT_ID);
    }

    @BeforeEach
    void setUp() {
        when(updateActionHandler.getActionType()).thenReturn(AgentAppEventActionType.UPDATE);
        when(upgradeActionHandler.getActionType()).thenReturn(AgentAppEventActionType.UPGRADE);
        when(deleteAppActionHandler.getActionType()).thenReturn(AgentAppEventActionType.DELETE);

        service = new DefaultTbAgentApplicationService(
                templateMergeOrchestrator, agentApplicationService, agentAppEventService, tbClusterService, agentEventRateLimiter);
        service.setActionHandlers(List.of(updateActionHandler, upgradeActionHandler, deleteAppActionHandler));
        ReflectionTestUtils.setField(service, "logEntityActionService", logEntityActionService);
    }

    // ==================== update() ====================

    @Test
    void update_newApplication_throws() {
        AgentApplication app = newApplication(null);

        assertThatThrownBy(() -> service.update(app, USER))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void update_existingApplication_savesAndLogsUpdate() throws Exception {
        AgentApplication app = newApplication(APP_ID);
        when(agentApplicationService.save(eq(TENANT_ID), eq(app))).thenReturn(app);

        AgentApplication result = service.update(app, USER);

        assertThat(result.getId()).isEqualTo(APP_ID);
        verify(agentAppEventService, never()).save(any(), any());
    }

    // ==================== install() ====================

    @Test
    void install() throws Exception {
        AgentApplication app = newApplication(null);
        AgentApplication savedApp = newApplication(APP_ID);
        when(agentApplicationService.saveWithRelatedEntity(eq(TENANT_ID), eq(app), isNull())).thenReturn(savedApp);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.INSTALL);
        request.setApplication(app);

        AgentAppInstallResponse result = service.install(TENANT_ID, request, USER);

        assertThat(result.getApplication().getId()).isEqualTo(APP_ID);

        ArgumentCaptor<AgentAppEvent> captor = ArgumentCaptor.forClass(AgentAppEvent.class);
        verify(agentAppEventService).save(eq(TENANT_ID), captor.capture());
        AgentAppEvent event = captor.getValue();
        assertThat(event.getActionType()).isEqualTo(AgentAppEventActionType.INSTALL);
        assertThat(event.getApplicationId()).isEqualTo(APP_ID);
        assertThat(event.getDeliveryState()).isEqualTo(AgentAppEventDeliveryState.PENDING);
    }

    @Test
    void install_setsOriginToInstalled() throws Exception {
        AgentApplication app = newApplication(null);
        AgentApplication savedApp = newApplication(APP_ID);
        when(agentApplicationService.saveWithRelatedEntity(eq(TENANT_ID), any(AgentApplication.class), isNull())).thenReturn(savedApp);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.INSTALL);
        request.setApplication(app);

        service.install(TENANT_ID, request, USER);

        ArgumentCaptor<AgentApplication> appCaptor = ArgumentCaptor.forClass(AgentApplication.class);
        verify(agentApplicationService).saveWithRelatedEntity(eq(TENANT_ID), appCaptor.capture(), isNull());
        assertThat(appCaptor.getValue().getOrigin()).isEqualTo(AgentApplicationOrigin.INSTALLED);
    }

    @Test
    void install_noApplication_throws() {
        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.INSTALL);

        assertThatThrownBy(() -> service.install(TENANT_ID, request, USER))
                .isInstanceOf(NullPointerException.class);
    }

    // ==================== execActionEvent() ====================

    @Test
    void execActionEvent_createsEvent() throws Exception {
        AgentApplication app = newApplication(APP_ID);
        when(agentApplicationService.findByIdForUpdate(TENANT_ID, APP_ID)).thenReturn(app);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.UPDATE);

        service.execActionEvent(TENANT_ID, APP_ID, request);

        ArgumentCaptor<AgentAppEvent> captor = ArgumentCaptor.forClass(AgentAppEvent.class);
        verify(agentAppEventService).save(eq(TENANT_ID), captor.capture());
        assertThat(captor.getValue().getActionType()).isEqualTo(AgentAppEventActionType.UPDATE);
    }

    @Test
    void execActionEvent_delegatesToActionHandler() throws Exception {
        AgentApplication app = newApplication(APP_ID);
        when(agentApplicationService.findByIdForUpdate(TENANT_ID, APP_ID)).thenReturn(app);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.DELETE);

        service.execActionEvent(TENANT_ID, APP_ID, request);

        verify(deleteAppActionHandler).handle(any(), eq(request), any());
    }

    @Test
    void execActionEvent_withStepInputs_setsStepStates() throws Exception {
        AgentApplication app = newApplication(APP_ID);
        when(agentApplicationService.findByIdForUpdate(TENANT_ID, APP_ID)).thenReturn(app);

        UUID stepId = UUID.randomUUID();
        ComposeDownStepState stepState = new ComposeDownStepState();
        stepState.setRemoveVolumes(true);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.DELETE);
        request.setStepInputs(Map.of(stepId, stepState));

        service.execActionEvent(TENANT_ID, APP_ID, request);

        ArgumentCaptor<AgentAppEvent> captor = ArgumentCaptor.forClass(AgentAppEvent.class);
        verify(agentAppEventService).save(eq(TENANT_ID), captor.capture());
        assertThat(captor.getValue().getStepStates()).containsKey(stepId);
    }

    @Test
    void execActionEvent_activeOrPendingEventExists_throws() {
        when(agentApplicationService.findByIdForUpdate(TENANT_ID, APP_ID)).thenReturn(newApplication(APP_ID));
        when(agentAppEventService.hasActiveOrPendingEventForApplication(APP_ID)).thenReturn(true);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.RESTART);

        assertThatThrownBy(() -> service.execActionEvent(TENANT_ID, APP_ID, request))
                .isInstanceOf(ThingsboardException.class);
        verify(agentAppEventService, never()).save(eq(TENANT_ID), any(AgentAppEvent.class));
    }

    @Test
    void execActionEvent_skipActiveEventCheck_bypassesGuard() throws Exception {
        AgentApplication app = newApplication(APP_ID);
        when(agentApplicationService.findByIdForUpdate(TENANT_ID, APP_ID)).thenReturn(app);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.RESTART);

        service.execActionEvent(TENANT_ID, APP_ID, request, true);

        verify(agentAppEventService, never()).hasActiveOrPendingEventForApplication(any());
        verify(agentAppEventService).save(eq(TENANT_ID), any(AgentAppEvent.class));
    }

    @Test
    void execActionEvent_clearsDesiredTemplateId() throws Exception {
        AgentAppTemplateId desiredTemplateId = new AgentAppTemplateId(UUID.randomUUID());
        AgentApplication app = newApplication(APP_ID);
        app.setDesiredTemplateId(desiredTemplateId);
        when(agentApplicationService.findByIdForUpdate(TENANT_ID, APP_ID)).thenReturn(app);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.RESTART);

        service.execActionEvent(TENANT_ID, APP_ID, request);

        ArgumentCaptor<AgentApplication> appCaptor = ArgumentCaptor.forClass(AgentApplication.class);
        verify(agentApplicationService).save(eq(TENANT_ID), appCaptor.capture());
        assertThat(appCaptor.getValue().getDesiredTemplateId()).isNull();
    }

    // ==================== cancelEvent() ====================

    @Test
    void cancelEvent_success() throws Exception {
        AgentAppEvent event = new AgentAppEvent(EVENT_ID);
        event.setTenantId(TENANT_ID);
        event.setApplicationId(APP_ID);
        event.setStatus(AgentAppEventStatus.PROCESSING);
        when(agentAppEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(event);

        AgentApplication app = newApplication(APP_ID);
        app.setAgentId(AGENT_ID);
        when(agentApplicationService.findById(TENANT_ID, APP_ID)).thenReturn(app);

        service.cancelEvent(TENANT_ID, EVENT_ID);

        verify(tbClusterService).onAgentAppEventCancelled(TENANT_ID, AGENT_ID, event);
    }

    @Test
    void cancelEvent_alreadyFinished_throws() {
        AgentAppEvent event = new AgentAppEvent(EVENT_ID);
        event.setTenantId(TENANT_ID);
        event.setStatus(AgentAppEventStatus.FINISHED);
        when(agentAppEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(event);

        assertThatThrownBy(() -> service.cancelEvent(TENANT_ID, EVENT_ID))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining("terminal state");
    }

    @Test
    void cancelEvent_alreadyError_throws() {
        AgentAppEvent event = new AgentAppEvent(EVENT_ID);
        event.setTenantId(TENANT_ID);
        event.setStatus(AgentAppEventStatus.ERROR);
        when(agentAppEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(event);

        assertThatThrownBy(() -> service.cancelEvent(TENANT_ID, EVENT_ID))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining("terminal state");
    }

    @Test
    void cancelEvent_eventNotFound_throws() {
        when(agentAppEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.cancelEvent(TENANT_ID, EVENT_ID))
                .isInstanceOf(ThingsboardException.class);
    }

    // ==================== Helpers ====================

    private AgentApplication newApplication(AgentApplicationId id) {
        AgentApplication app = new AgentApplication();
        if (id != null) {
            app.setId(id);
        }
        app.setTenantId(TENANT_ID);
        return app;
    }

    private DockerComposeConfig createDockerComposeConfig(String composeContent) {
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(new ObjectMapper().valueToTree(composeContent));
        return config;
    }
}
