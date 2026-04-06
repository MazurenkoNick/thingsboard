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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.AgentBulkActionStatus;
import org.thingsboard.server.common.data.agent.BulkOperationRequest;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.common.data.id.AgentGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.agent.AgentBulkActionService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.transport.TransportProtos.AgentBulkOperationMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.agent.bulk.DefaultAgentBulkActionProcessingService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultAgentBulkActionProcessingServiceTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentGroupId GROUP_ID = new AgentGroupId(UUID.randomUUID());
    private static final AgentAppProfileId PROFILE_ID = new AgentAppProfileId(UUID.randomUUID());
    private static final AgentAppTemplateId TEMPLATE_ID = new AgentAppTemplateId(UUID.randomUUID());
    private static final User USER = new User();

    @Mock
    private AgentAppProfileService profileService;
    @Mock
    private AgentApplicationDao applicationDao;
    @Mock
    private AgentAppEventService agentAppEventService;
    @Mock
    private AgentBulkActionService agentBulkActionService;
    @Mock
    private TbClusterService clusterService;
    @Mock
    private TbAgentApplicationService tbAgentApplicationService;
    @Mock
    private PartitionService partitionService;

    private DefaultAgentBulkActionProcessingService service;

    @BeforeAll
    static void beforeAll() {
        USER.setId(new UserId(UUID.randomUUID()));
    }

    @BeforeEach
    void setUp() {
        service = new DefaultAgentBulkActionProcessingService(tbAgentApplicationService, profileService, applicationDao, agentAppEventService,
                agentBulkActionService, clusterService, partitionService);
        ReflectionTestUtils.setField(service, "stuckActionThresholdMs", 600_000L);

        lenient().when(agentBulkActionService.save(any(), any())).thenAnswer(invocation -> {
            AgentBulkAction action = invocation.getArgument(1);
            if (action.getId() == null) {
                action.setId(new AgentBulkActionId(UUID.randomUUID()));
            }
            return action;
        });
    }

    // ==================== enqueueBulkOperation ====================

    @Test
    void enqueueBulkOperation_rejectsNullActionType() {
        BulkOperationRequest request = new BulkOperationRequest();
        request.setActionType(null);
        request.setForce(false);

        assertThatThrownBy(() -> service.enqueueBulkOperation(TENANT_ID, GROUP_ID, PROFILE_ID, request))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("not allowed");
        verifyNoInteractions(clusterService);
    }

    @ParameterizedTest
    @EnumSource(value = AgentAppEventActionType.class, names = {"INSTALL"})
    void enqueueBulkOperation_restrictedActionTypes(AgentAppEventActionType actionType) {
        BulkOperationRequest request = new BulkOperationRequest();
        request.setActionType(actionType);
        request.setForce(false);

        assertThatThrownBy(() -> service.enqueueBulkOperation(TENANT_ID, GROUP_ID, PROFILE_ID, request))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("not allowed");
        verifyNoInteractions(clusterService);
    }

    @ParameterizedTest
    @EnumSource(value = AgentAppEventActionType.class, names = {"UPDATE", "DELETE", "RESTART", "ROLLBACK", "UPGRADE"})
    void enqueueBulkOperation_allowedActionTypes(AgentAppEventActionType actionType) {
        BulkOperationRequest request = new BulkOperationRequest();
        request.setActionType(actionType);
        request.setForce(false);

        AgentBulkAction result = service.enqueueBulkOperation(TENANT_ID, GROUP_ID, PROFILE_ID, request);

        assertThat(result.getStatus()).isEqualTo(AgentBulkActionStatus.QUEUED);
        verify(clusterService).pushMsgToAgentBulkOps(any(), eq(request));
    }

    @Test
    void enqueueBulkOperation_savesWithQueuedStatusAndPublishes() {
        BulkOperationRequest request = createRequest(AgentAppEventActionType.UPDATE);
        request.setForce(true);

        AgentBulkAction result = service.enqueueBulkOperation(TENANT_ID, GROUP_ID, PROFILE_ID, request);

        assertThat(result.getStatus()).isEqualTo(AgentBulkActionStatus.QUEUED);
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(result.getGroupId()).isEqualTo(GROUP_ID.getId());
        assertThat(result.getProfileId()).isEqualTo(PROFILE_ID.getId());
        assertThat(result.getActionType()).isEqualTo(AgentAppEventActionType.UPDATE);

        ArgumentCaptor<AgentBulkAction> captor = ArgumentCaptor.forClass(AgentBulkAction.class);
        verify(agentBulkActionService).save(eq(TENANT_ID), captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AgentBulkActionStatus.QUEUED);

        verify(clusterService).pushMsgToAgentBulkOps(any(), eq(request));
    }

    // ==================== processBulkOperation ====================

    @Test
    void processBulkOperation_skipsWhenBulkActionNotFound() {
        AgentBulkOperationMsg msg = buildProtoMsg(AgentAppEventActionType.UPDATE);

        service.processBulkOperation(msg);

        verify(agentBulkActionService).findById(any(), any());
        verify(agentBulkActionService, never()).save(any(), any());
    }

    @Test
    void processBulkOperation_setsInProgressAndExecutes() {
        AgentBulkActionId bulkActionId = new AgentBulkActionId(UUID.randomUUID());
        AgentBulkAction bulkAction = createBulkAction(bulkActionId, AgentBulkActionStatus.QUEUED);
        AgentAppProfile profile = createProfile();

        AgentBulkOperationMsg msg = buildProtoMsg(bulkActionId, AgentAppEventActionType.UPDATE);

        when(agentBulkActionService.findById(any(), eq(bulkActionId))).thenReturn(bulkAction);
        when(profileService.findProfileById(any(), any())).thenReturn(profile);
        when(applicationDao.findByApplicationProfileIdAndAgentGroupId(any(), any(), any()))
                .thenReturn(new PageData<>(Collections.emptyList(), 0, 0, false));

        service.processBulkOperation(msg);

        // save is called at least twice: first for IN_PROGRESS, then for COMPLETED.
        // Since the same object is mutated, we verify processingStartedTime was set (during IN_PROGRESS)
        // and final status is COMPLETED.
        ArgumentCaptor<AgentBulkAction> captor = ArgumentCaptor.forClass(AgentBulkAction.class);
        verify(agentBulkActionService, atLeastOnce()).save(any(), captor.capture());
        AgentBulkAction lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(lastSaved.getProcessingStartedTime()).isNotNull();
        assertThat(lastSaved.getStatus()).isEqualTo(AgentBulkActionStatus.COMPLETED);
    }

    @Test
    void processBulkOperation_completesWithNoEligibleApps() {
        AgentBulkActionId bulkActionId = new AgentBulkActionId(UUID.randomUUID());
        AgentBulkAction bulkAction = createBulkAction(bulkActionId, AgentBulkActionStatus.QUEUED);
        AgentAppProfile profile = createProfile();

        AgentBulkOperationMsg msg = buildProtoMsg(bulkActionId, AgentAppEventActionType.UPDATE);

        when(agentBulkActionService.findById(any(), eq(bulkActionId))).thenReturn(bulkAction);
        when(profileService.findProfileById(any(), any())).thenReturn(profile);
        when(applicationDao.findByApplicationProfileIdAndAgentGroupId(any(), any(), any()))
                .thenReturn(new PageData<>(Collections.emptyList(), 0, 0, false));

        service.processBulkOperation(msg);

        ArgumentCaptor<AgentBulkAction> captor = ArgumentCaptor.forClass(AgentBulkAction.class);
        verify(agentBulkActionService, atLeastOnce()).save(any(), captor.capture());
        List<AgentBulkAction> savedActions = captor.getAllValues();
        AgentBulkAction lastSaved = savedActions.get(savedActions.size() - 1);
        assertThat(lastSaved.getStatus()).isEqualTo(AgentBulkActionStatus.COMPLETED);
    }

    @Test
    void processBulkOperation_executesForEligibleApps() throws Exception {
        AgentBulkActionId bulkActionId = new AgentBulkActionId(UUID.randomUUID());
        AgentBulkAction bulkAction = createBulkAction(bulkActionId, AgentBulkActionStatus.QUEUED);
        AgentAppProfile profile = createProfile();

        AgentApplication app = createApplication();

        AgentBulkOperationMsg msg = buildProtoMsg(bulkActionId, AgentAppEventActionType.UPDATE);

        when(agentBulkActionService.findById(any(), eq(bulkActionId))).thenReturn(bulkAction);
        when(profileService.findProfileById(any(), any())).thenReturn(profile);
        when(applicationDao.findByApplicationProfileIdAndAgentGroupId(any(), any(), any()))
                .thenReturn(new PageData<>(List.of(app), 1, 1, false));

        service.processBulkOperation(msg);

        verify(tbAgentApplicationService).execActionEvent(any(), eq(app.getId()), any(), eq(true));

        ArgumentCaptor<AgentBulkAction> captor = ArgumentCaptor.forClass(AgentBulkAction.class);
        verify(agentBulkActionService, atLeastOnce()).save(any(), captor.capture());
        List<AgentBulkAction> savedActions = captor.getAllValues();
        AgentBulkAction lastSaved = savedActions.get(savedActions.size() - 1);
        assertThat(lastSaved.getStatus()).isEqualTo(AgentBulkActionStatus.COMPLETED);
        assertThat(lastSaved.getSubmitted()).isEqualTo(1);
    }

    // ==================== failStuckBulkActions ====================

    @Test
    void failStuckBulkActions_skipsWhenNotMyPartition() {
        TopicPartitionInfo tpi = new TopicPartitionInfo("topic", null, 0, false);
        when(partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID)).thenReturn(tpi);

        service.failStuckBulkActions();

        verifyNoInteractions(agentBulkActionService);
    }

    @Test
    void failStuckBulkActions_failsStuckInProgressAction() {
        TopicPartitionInfo tpi = new TopicPartitionInfo("topic", null, 0, true);
        when(partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID)).thenReturn(tpi);

        AgentBulkAction stuckAction = createBulkAction(new AgentBulkActionId(UUID.randomUUID()), AgentBulkActionStatus.IN_PROGRESS);
        when(agentBulkActionService.findStuckBulkActions(anyLong(), any(PageLink.class)))
                .thenReturn(new PageData<>(List.of(stuckAction), 1, 1, false));

        service.failStuckBulkActions();

        ArgumentCaptor<AgentBulkAction> captor = ArgumentCaptor.forClass(AgentBulkAction.class);
        verify(agentBulkActionService).save(eq(TENANT_ID), captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AgentBulkActionStatus.FAILED);
        assertThat(captor.getValue().getErrorMsg()).contains("Stuck in IN_PROGRESS");
    }

    @Test
    void failStuckBulkActions_failsStuckQueuedAction() {
        TopicPartitionInfo tpi = new TopicPartitionInfo("topic", null, 0, true);
        when(partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID)).thenReturn(tpi);

        AgentBulkAction queuedAction = createBulkAction(new AgentBulkActionId(UUID.randomUUID()), AgentBulkActionStatus.QUEUED);
        when(agentBulkActionService.findStuckBulkActions(anyLong(), any(PageLink.class)))
                .thenReturn(new PageData<>(List.of(queuedAction), 1, 1, false));

        service.failStuckBulkActions();

        ArgumentCaptor<AgentBulkAction> captor = ArgumentCaptor.forClass(AgentBulkAction.class);
        verify(agentBulkActionService).save(eq(TENANT_ID), captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AgentBulkActionStatus.FAILED);
        assertThat(captor.getValue().getErrorMsg()).contains("Stuck in QUEUED");
    }

    // ==================== processBulkOperation error handling ====================

    @Test
    void processBulkOperation_unexpectedException_savesFailedStatus() {
        AgentBulkActionId bulkActionId = new AgentBulkActionId(UUID.randomUUID());
        AgentBulkAction bulkAction = createBulkAction(bulkActionId, AgentBulkActionStatus.QUEUED);

        AgentBulkOperationMsg msg = buildProtoMsg(bulkActionId, AgentAppEventActionType.UPDATE);

        when(agentBulkActionService.findById(any(), eq(bulkActionId))).thenReturn(bulkAction);
        when(profileService.findProfileById(any(), any())).thenThrow(new RuntimeException("DB connection lost"));

        service.processBulkOperation(msg);

        ArgumentCaptor<AgentBulkAction> captor = ArgumentCaptor.forClass(AgentBulkAction.class);
        verify(agentBulkActionService, atLeastOnce()).save(any(), captor.capture());
        List<AgentBulkAction> saved = captor.getAllValues();
        AgentBulkAction lastSaved = saved.get(saved.size() - 1);
        assertThat(lastSaved.getStatus()).isEqualTo(AgentBulkActionStatus.FAILED);
        assertThat(lastSaved.getErrorMsg()).contains("DB connection lost");
    }

    // ==================== Helpers ====================

    private BulkOperationRequest createRequest(AgentAppEventActionType actionType) {
        BulkOperationRequest request = new BulkOperationRequest();
        request.setActionType(actionType);
        return request;
    }

    private AgentAppProfile createProfile() {
        AgentAppProfile profile = new AgentAppProfile();
        profile.setId(PROFILE_ID);
        profile.setTemplateId(TEMPLATE_ID);
        return profile;
    }

    private AgentBulkAction createBulkAction(AgentBulkActionId id, AgentBulkActionStatus status) {
        AgentBulkAction action = new AgentBulkAction(id);
        action.setTenantId(TENANT_ID);
        action.setGroupId(GROUP_ID.getId());
        action.setProfileId(PROFILE_ID.getId());
        action.setActionType(AgentAppEventActionType.UPDATE);
        action.setStatus(status);
        return action;
    }

    private AgentApplication createApplication() {
        AgentApplication app = new AgentApplication();
        app.setId(new AgentApplicationId(UUID.randomUUID()));
        app.setTenantId(TENANT_ID);
        app.setName("test-app");
        app.setTemplateId(TEMPLATE_ID);
        return app;
    }

    private AgentBulkOperationMsg buildProtoMsg(AgentAppEventActionType actionType) {
        return buildProtoMsg(new AgentBulkActionId(UUID.randomUUID()), actionType);
    }

    private AgentBulkOperationMsg buildProtoMsg(AgentBulkActionId bulkActionId, AgentAppEventActionType actionType) {
        UUID tenantUuid = TENANT_ID.getId();
        UUID bulkUuid = bulkActionId.getId();
        UUID groupUuid = GROUP_ID.getId();
        UUID profileUuid = PROFILE_ID.getId();

        return AgentBulkOperationMsg.newBuilder()
                .setTenantIdMSB(tenantUuid.getMostSignificantBits())
                .setTenantIdLSB(tenantUuid.getLeastSignificantBits())
                .setBulkActionIdMSB(bulkUuid.getMostSignificantBits())
                .setBulkActionIdLSB(bulkUuid.getLeastSignificantBits())
                .setGroupIdMSB(groupUuid.getMostSignificantBits())
                .setGroupIdLSB(groupUuid.getLeastSignificantBits())
                .setProfileIdMSB(profileUuid.getMostSignificantBits())
                .setProfileIdLSB(profileUuid.getLeastSignificantBits())
                .setActionType(actionType.name())
                .setForce(false)
                .build();
    }
}
