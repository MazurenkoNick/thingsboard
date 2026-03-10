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
package org.thingsboard.server.service.entitiy.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventDeliveryState;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.state.ComposeDownStepState;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.service.agent.template.merge.AgentAppTemplateMergeOrchestrator;
import org.thingsboard.server.service.entitiy.TbLogEntityActionService;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultTbAgentApplicationServiceTest {

    @Mock
    private AgentAppTemplateMergeOrchestrator templateMergeOrchestrator;
    @Mock
    private AgentApplicationService agentApplicationService;
    @Mock
    private AgentAppEventService agentAppEventService;
    @Mock
    private TbClusterService tbClusterService;
    @Mock
    private TbLogEntityActionService logEntityActionService;

    @InjectMocks
    private DefaultTbAgentApplicationService service;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentApplicationId APP_ID = new AgentApplicationId(UUID.randomUUID());
    private static final AgentAppEventId EVENT_ID = new AgentAppEventId(UUID.randomUUID());
    private static final AgentId AGENT_ID = new AgentId(UUID.randomUUID());
    private static final User USER = new User();

    @BeforeEach
    void setUp() {
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

    // ==================== installEvent() ====================

    @Test
    void install() throws Exception {
        AgentApplication app = newApplication(null);
        AgentApplication savedApp = newApplication(APP_ID);
        when(agentApplicationService.save(eq(TENANT_ID), eq(app))).thenReturn(savedApp);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.INSTALL);
        request.setApplication(app);

        AgentApplication result = service.install(TENANT_ID, request, USER);

        assertThat(result.getId()).isEqualTo(APP_ID);

        ArgumentCaptor<AgentAppEvent> captor = ArgumentCaptor.forClass(AgentAppEvent.class);
        verify(agentAppEventService).save(eq(TENANT_ID), captor.capture());
        AgentAppEvent event = captor.getValue();
        assertThat(event.getActionType()).isEqualTo(AgentAppEventActionType.INSTALL);
        assertThat(event.getApplicationId()).isEqualTo(APP_ID);
        assertThat(event.getDeliveryState()).isEqualTo(AgentAppEventDeliveryState.PENDING);
    }

    @Test
    void install_noApplication_throws() {
        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.INSTALL);

        assertThatThrownBy(() -> service.install(TENANT_ID, request, USER))
                .isInstanceOf(NullPointerException.class);
    }

    // ==================== createEvent() ====================

    @Test
    void execActionEvent_update_createsEvent() throws Exception {
        AgentApplication app = newApplication(APP_ID);
        when(agentApplicationService.findById(TENANT_ID, APP_ID)).thenReturn(app);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.UPDATE);

        service.execActionEvent(TENANT_ID, APP_ID, request, USER);

        ArgumentCaptor<AgentAppEvent> captor = ArgumentCaptor.forClass(AgentAppEvent.class);
        verify(agentAppEventService).save(eq(TENANT_ID), captor.capture());
        assertThat(captor.getValue().getActionType()).isEqualTo(AgentAppEventActionType.UPDATE);
    }

    @Test
    void execActionEvent_delete_setsPendingDeletion() throws Exception {
        AgentApplication app = newApplication(APP_ID);
        when(agentApplicationService.findById(TENANT_ID, APP_ID)).thenReturn(app);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.DELETE);

        service.execActionEvent(TENANT_ID, APP_ID, request, USER);

        ArgumentCaptor<AgentApplication> appCaptor = ArgumentCaptor.forClass(AgentApplication.class);
        verify(agentApplicationService).save(eq(TENANT_ID), appCaptor.capture());
        assertThat(appCaptor.getValue().isPendingDeletion()).isTrue();
        verify(agentAppEventService).deleteAllPendingByApplicationId(APP_ID);

        ArgumentCaptor<AgentAppEvent> captor = ArgumentCaptor.forClass(AgentAppEvent.class);
        verify(agentAppEventService).save(eq(TENANT_ID), captor.capture());
        assertThat(captor.getValue().getActionType()).isEqualTo(AgentAppEventActionType.DELETE);
    }

    @Test
    void execActionEvent_restart_createsEvent() throws Exception {
        AgentApplication app = newApplication(APP_ID);
        when(agentApplicationService.findById(TENANT_ID, APP_ID)).thenReturn(app);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.RESTART);

        service.execActionEvent(TENANT_ID, APP_ID, request, USER);

        ArgumentCaptor<AgentAppEvent> captor = ArgumentCaptor.forClass(AgentAppEvent.class);
        verify(agentAppEventService).save(eq(TENANT_ID), captor.capture());
        assertThat(captor.getValue().getActionType()).isEqualTo(AgentAppEventActionType.RESTART);
    }

    @Test
    void execActionEvent_withStepInputs_setsStepStates() throws Exception {
        AgentApplication app = newApplication(APP_ID);
        when(agentApplicationService.findById(TENANT_ID, APP_ID)).thenReturn(app);

        UUID stepId = UUID.randomUUID();
        ComposeDownStepState stepState = new ComposeDownStepState();
        stepState.setRemoveVolumes(true);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.DELETE);
        request.setStepInputs(Map.of(stepId, stepState));

        service.execActionEvent(TENANT_ID, APP_ID, request, USER);

        ArgumentCaptor<AgentAppEvent> captor = ArgumentCaptor.forClass(AgentAppEvent.class);
        verify(agentAppEventService).save(eq(TENANT_ID), captor.capture());
        assertThat(captor.getValue().getStepStates()).containsKey(stepId);
    }

    @Test
    void execActionEvent_upgrade_savesUpdatedApplication() throws Exception {
        AgentAppTemplateId oldTemplateId = new AgentAppTemplateId(UUID.randomUUID());
        AgentAppTemplateId newTemplateId = new AgentAppTemplateId(UUID.randomUUID());

        AgentApplication app = newApplication(APP_ID);
        app.setTemplateId(oldTemplateId);
        when(agentApplicationService.findById(TENANT_ID, APP_ID)).thenReturn(app);

        AgentApplication upgradedApp = new AgentApplication();
        upgradedApp.setConfig(createDockerComposeConfig("new-compose"));
        upgradedApp.setTemplateId(newTemplateId);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.UPGRADE);
        request.setApplication(upgradedApp);

        service.execActionEvent(TENANT_ID, APP_ID, request, USER);

        verify(agentApplicationService).save(eq(TENANT_ID), eq(upgradedApp));
        assertThat(upgradedApp.getId()).isEqualTo(APP_ID);
        assertThat(upgradedApp.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(upgradedApp.getTemplateId()).isEqualTo(oldTemplateId);
        assertThat(upgradedApp.getDesiredTemplateId()).isEqualTo(newTemplateId);
    }

    @Test
    void execActionEvent_activeEventExists_throws() {
        when(agentAppEventService.hasActiveEventForApplication(APP_ID)).thenReturn(true);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.RESTART);

        assertThatThrownBy(() -> service.execActionEvent(TENANT_ID, APP_ID, request, USER))
                .isInstanceOf(ThingsboardException.class);
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
