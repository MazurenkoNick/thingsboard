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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.ComposeDownStep;
import org.thingsboard.server.common.data.agent.step.ComposeStep;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.service.agent.template.merge.AgentAppTemplateMergeOrchestrator;
import org.thingsboard.server.service.entitiy.TbLogEntityActionService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private static final User USER = new User();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "logEntityActionService", logEntityActionService);
    }

    // ==================== save() - new application ====================

    @Test
    void save_newApplication_createsInstallEvent() throws Exception {
        AgentApplication app = newApplication(null);
        when(agentApplicationService.save(eq(TENANT_ID), eq(app))).thenReturn(app);

        service.save(app, USER);

        ArgumentCaptor<AgentAppEvent> eventCaptor = ArgumentCaptor.forClass(AgentAppEvent.class);
        verify(agentAppEventService).save(eq(TENANT_ID), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getActionType()).isEqualTo(AgentAppEventActionType.INSTALL);
    }

    // ==================== save() - update ====================

    @Test
    void save_update_createsUpdateEvent() throws Exception {
        AgentApplicationId appId = new AgentApplicationId(UUID.randomUUID());

        AgentApplication existing = newApplication(appId);
        AgentApplication updated = newApplication(appId);

        when(agentApplicationService.findById(TENANT_ID, appId)).thenReturn(existing);
        when(agentApplicationService.save(eq(TENANT_ID), eq(updated))).thenReturn(updated);

        service.save(updated, USER);

        ArgumentCaptor<AgentAppEvent> eventCaptor = ArgumentCaptor.forClass(AgentAppEvent.class);
        verify(agentAppEventService).save(eq(TENANT_ID), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getActionType()).isEqualTo(AgentAppEventActionType.UPDATE);
    }

    @Test
    void save_updateWithActiveEvent_throwsException() {
        AgentApplicationId appId = new AgentApplicationId(UUID.randomUUID());
        AgentApplication updated = newApplication(appId);

        when(agentAppEventService.hasActiveEventForApplication(appId)).thenReturn(true);

        assertThatThrownBy(() -> service.save(updated, USER))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining("Cannot update application while an event is being processed");
    }

    @Test
    void save_updatePendingDeletion_throwsException() {
        AgentApplicationId appId = new AgentApplicationId(UUID.randomUUID());

        AgentApplication existing = newApplication(appId);
        existing.setPendingDeletion(true);

        AgentApplication updated = newApplication(appId);

        when(agentApplicationService.findById(TENANT_ID, appId)).thenReturn(existing);

        assertThatThrownBy(() -> service.save(updated, USER))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("pending for removal");
    }

    // ==================== updateDeleteSteps() ====================

    @Test
    void updateDeleteSteps_success() {
        AgentApplicationId appId = new AgentApplicationId(UUID.randomUUID());
        AgentApplication existing = newApplication(appId);
        List<AgentAppStep> deleteSteps = List.of(createComposeDownStep(true));

        when(agentApplicationService.findById(TENANT_ID, appId)).thenReturn(existing);
        when(agentApplicationService.save(eq(TENANT_ID), any())).thenAnswer(i -> i.getArgument(1));

        AgentApplication result = service.updateDeleteSteps(TENANT_ID, appId, deleteSteps);

        assertThat(result.getDeleteSteps()).isEqualTo(deleteSteps);
        verify(agentApplicationService).save(eq(TENANT_ID), any());
    }

    @Test
    void updateDeleteSteps_pendingDeletion_throwsException() {
        AgentApplicationId appId = new AgentApplicationId(UUID.randomUUID());
        AgentApplication existing = newApplication(appId);
        existing.setPendingDeletion(true);

        when(agentApplicationService.findById(TENANT_ID, appId)).thenReturn(existing);

        assertThatThrownBy(() -> service.updateDeleteSteps(TENANT_ID, appId, List.of(createComposeDownStep(false))))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("pending for removal");
    }

    @Test
    void updateDeleteSteps_doesNotCreateEvent() {
        AgentApplicationId appId = new AgentApplicationId(UUID.randomUUID());
        AgentApplication existing = newApplication(appId);

        when(agentApplicationService.findById(TENANT_ID, appId)).thenReturn(existing);
        when(agentApplicationService.save(eq(TENANT_ID), any())).thenAnswer(i -> i.getArgument(1));

        service.updateDeleteSteps(TENANT_ID, appId, List.of(createComposeDownStep(true)));

        verify(agentAppEventService, org.mockito.Mockito.never()).save(any(), any());
    }

    // ==================== delete() ====================

    @Test
    void delete_success() {
        AgentApplicationId appId = new AgentApplicationId(UUID.randomUUID());
        AgentApplication app = newApplication(appId);
        app.setDeleteSteps(List.of(createComposeDownStep(true)));

        service.delete(app, USER);

        verify(agentAppEventService).deleteAllPendingByApplicationId(appId);
        verify(agentApplicationService).save(TENANT_ID, app);
        assertThat(app.isPendingDeletion()).isTrue();

        ArgumentCaptor<AgentAppEvent> eventCaptor = ArgumentCaptor.forClass(AgentAppEvent.class);
        verify(agentAppEventService).save(eq(TENANT_ID), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getActionType()).isEqualTo(AgentAppEventActionType.DELETE);
    }

    @Test
    void delete_pendingDeletion_throwsException() {
        AgentApplication app = newApplication(new AgentApplicationId(UUID.randomUUID()));
        app.setPendingDeletion(true);
        app.setDeleteSteps(List.of(createComposeDownStep(false)));

        assertThatThrownBy(() -> service.delete(app, USER))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("pending for removal");
    }

    @Test
    void delete_noDeleteSteps_throwsValidationException() {
        AgentApplication app = newApplication(new AgentApplicationId(UUID.randomUUID()));

        assertThatThrownBy(() -> service.delete(app, USER))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("Delete steps must be configured");
    }

    @Test
    void delete_emptyDeleteSteps_throwsValidationException() {
        AgentApplication app = newApplication(new AgentApplicationId(UUID.randomUUID()));
        app.setDeleteSteps(List.of());

        assertThatThrownBy(() -> service.delete(app, USER))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("Delete steps must be configured");
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

    private ComposeDownStep createComposeDownStep(boolean removeVolumes) {
        ComposeDownStep step = new ComposeDownStep();
        step.setId(UUID.randomUUID());
        step.setRemoveVolumes(removeVolumes);
        return step;
    }
}
