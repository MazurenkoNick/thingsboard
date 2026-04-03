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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.BulkOperationRequest;
import org.thingsboard.server.common.data.agent.BulkOperationResult.SkipReason;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.common.data.id.AgentGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.agent.AgentBulkActionService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultAgentBulkOperationServiceTest {

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
    private TbAgentApplicationService tbAgentApplicationService;
    @Mock
    private AgentBulkActionService agentBulkActionService;

    private DefaultAgentBulkOperationService service;

    @BeforeEach
    void setUp() {
        service = new DefaultAgentBulkOperationService(profileService, applicationDao, agentAppEventService, tbAgentApplicationService, agentBulkActionService);
        service.init();

        lenient().when(agentBulkActionService.save(any(), any())).thenAnswer(invocation -> {
            AgentBulkAction action = invocation.getArgument(1);
            if (action.getId() == null) {
                action.setId(new AgentBulkActionId(UUID.randomUUID()));
            }
            return action;
        });
    }

    @AfterEach
    void tearDown() {
        service.destroy();
    }

    @Test
    void bulkOperation_rejectsNullActionType() {
        BulkOperationRequest request = new BulkOperationRequest();
        request.setActionType(null);

        assertThatThrownBy(() -> service.bulkOperation(TENANT_ID, GROUP_ID, PROFILE_ID, request, false, USER))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("not allowed");
    }

    @ParameterizedTest
    @EnumSource(value = AgentAppEventActionType.class, names = {"INSTALL"})
    void bulkOperation_restrictedActionTypes(AgentAppEventActionType actionType) {
        BulkOperationRequest request = new BulkOperationRequest();
        request.setActionType(actionType);

        assertThatThrownBy(() -> service.bulkOperation(TENANT_ID, GROUP_ID, PROFILE_ID, request, false, USER))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("not allowed");
    }

    @ParameterizedTest
    @EnumSource(value = AgentAppEventActionType.class, names = {"UPDATE", "DELETE", "RESTART", "ROLLBACK", "UPGRADE"})
    void bulkOperation_allowedActionTypes(AgentAppEventActionType actionType) {
        BulkOperationRequest request = new BulkOperationRequest();
        request.setActionType(actionType);

        AgentAppProfile profile = createProfile();
        when(profileService.findProfileById(TENANT_ID, PROFILE_ID)).thenReturn(profile);
        when(applicationDao.findByApplicationProfileIdAndAgentGroupId(eq(PROFILE_ID.getId()), eq(GROUP_ID.getId()), any()))
                .thenReturn(new PageData<>(Collections.emptyList(), 0, 0, false));

        AgentBulkAction result = service.bulkOperation(TENANT_ID, GROUP_ID, PROFILE_ID, request, false, USER);

        assertThat(result.getTotal()).isZero();
    }

    @Test
    void bulkOperation_noApps_returnsEmptyResult() {
        BulkOperationRequest request = createRequest(AgentAppEventActionType.UPDATE);

        AgentAppProfile profile = createProfile();
        when(profileService.findProfileById(TENANT_ID, PROFILE_ID)).thenReturn(profile);
        when(applicationDao.findByApplicationProfileIdAndAgentGroupId(eq(PROFILE_ID.getId()), eq(GROUP_ID.getId()), any()))
                .thenReturn(new PageData<>(Collections.emptyList(), 0, 0, false));

        AgentBulkAction result = service.bulkOperation(TENANT_ID, GROUP_ID, PROFILE_ID, request, false, USER);

        assertThat(result.getTotal()).isZero();
        assertThat(result.getSubmitted()).isZero();
        assertThat(result.getSkipCounts()).isNullOrEmpty();
    }

    @Test
    void bulkOperation_allEligible_submitsAll() {
        BulkOperationRequest request = createRequest(AgentAppEventActionType.UPDATE);

        AgentAppProfile profile = createProfile();
        when(profileService.findProfileById(TENANT_ID, PROFILE_ID)).thenReturn(profile);

        List<AgentApplication> apps = List.of(createApp(), createApp(), createApp());
        when(applicationDao.findByApplicationProfileIdAndAgentGroupId(eq(PROFILE_ID.getId()), eq(GROUP_ID.getId()), any()))
                .thenReturn(new PageData<>(apps, 1, apps.size(), false));

        AgentBulkAction result = service.bulkOperation(TENANT_ID, GROUP_ID, PROFILE_ID, request, true, USER);

        assertThat(result.getTotal()).isEqualTo(3);
        assertThat(result.getSubmitted()).isEqualTo(3);
        assertThat(result.getSkipCounts()).isNullOrEmpty();
    }

    @Test
    void bulkOperation_forceTrue_skipsBlockedApps() {
        BulkOperationRequest request = createRequest(AgentAppEventActionType.UPDATE);

        AgentAppProfile profile = createProfile();
        when(profileService.findProfileById(TENANT_ID, PROFILE_ID)).thenReturn(profile);

        AgentApplication eligible1 = createApp();
        AgentApplication eligible2 = createApp();
        AgentApplication blocked = createApp();
        when(agentAppEventService.hasActiveEventForApplication(any())).thenAnswer(invocation -> {
            AgentApplicationId id = invocation.getArgument(0);
            return id.equals(blocked.getId());
        });

        List<AgentApplication> apps = List.of(eligible1, blocked, eligible2);
        when(applicationDao.findByApplicationProfileIdAndAgentGroupId(eq(PROFILE_ID.getId()), eq(GROUP_ID.getId()), any()))
                .thenReturn(new PageData<>(apps, 1, apps.size(), false));

        AgentBulkAction result = service.bulkOperation(TENANT_ID, GROUP_ID, PROFILE_ID, request, true, USER);

        assertThat(result.getTotal()).isEqualTo(3);
        assertThat(result.getSubmitted()).isEqualTo(2);
        assertThat(result.getSkipCounts()).hasSize(1);
        assertThat(result.getSkipCounts()).containsKey(SkipReason.ACTIVE_EVENT);
    }

    @Test
    void bulkOperation_forceFalse_throwsOnBlocker() {
        BulkOperationRequest request = createRequest(AgentAppEventActionType.UPDATE);

        AgentAppProfile profile = createProfile();
        when(profileService.findProfileById(TENANT_ID, PROFILE_ID)).thenReturn(profile);

        AgentApplication blocked = createApp();
        when(agentAppEventService.hasActiveEventForApplication(blocked.getId())).thenReturn(true);

        when(applicationDao.findByApplicationProfileIdAndAgentGroupId(eq(PROFILE_ID.getId()), eq(GROUP_ID.getId()), any()))
                .thenReturn(new PageData<>(List.of(blocked), 1, 1, false));

        assertThatThrownBy(() -> service.bulkOperation(TENANT_ID, GROUP_ID, PROFILE_ID, request, false, USER))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("Bulk operation blocked");
    }

    @Test
    void bulkOperation_upgrade_versionMismatch_skipped() {
        BulkOperationRequest request = createRequest(AgentAppEventActionType.UPGRADE);

        AgentAppProfile profile = createProfile();
        when(profileService.findProfileById(TENANT_ID, PROFILE_ID)).thenReturn(profile);

        AgentApplication app = createApp();
        app.setTemplateId(new AgentAppTemplateId(UUID.randomUUID())); // different from profile's

        when(applicationDao.findByApplicationProfileIdAndAgentGroupId(eq(PROFILE_ID.getId()), eq(GROUP_ID.getId()), any()))
                .thenReturn(new PageData<>(List.of(app), 1, 1, false));

        AgentBulkAction result = service.bulkOperation(TENANT_ID, GROUP_ID, PROFILE_ID, request, true, USER);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getSubmitted()).isZero();
        assertThat(result.getSkipCounts()).hasSize(1);
        assertThat(result.getSkipCounts()).containsKey(SkipReason.VERSION_MISMATCH);
    }

    @Test
    void bulkOperation_execFailure_recordedAsSkipped() throws Exception {
        BulkOperationRequest request = createRequest(AgentAppEventActionType.UPDATE);

        AgentAppProfile profile = createProfile();
        when(profileService.findProfileById(TENANT_ID, PROFILE_ID)).thenReturn(profile);

        AgentApplication app = createApp();
        when(applicationDao.findByApplicationProfileIdAndAgentGroupId(eq(PROFILE_ID.getId()), eq(GROUP_ID.getId()), any()))
                .thenReturn(new PageData<>(List.of(app), 1, 1, false));

        doThrow(new RuntimeException("DB error"))
                .when(tbAgentApplicationService).execActionEvent(any(), any(), any(), any(), eq(true));

        AgentBulkAction result = service.bulkOperation(TENANT_ID, GROUP_ID, PROFILE_ID, request, true, USER);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getSubmitted()).isZero();
        assertThat(result.getSkipCounts()).hasSize(1);
        assertThat(result.getSkipCounts()).containsKey(SkipReason.ERROR);
    }

    @Test
    void bulkOperation_createsBulkActionWithCorrectMetadata() {
        BulkOperationRequest request = createRequest(AgentAppEventActionType.UPDATE);

        AgentAppProfile profile = createProfile();
        when(profileService.findProfileById(TENANT_ID, PROFILE_ID)).thenReturn(profile);

        List<AgentApplication> apps = List.of(createApp());
        when(applicationDao.findByApplicationProfileIdAndAgentGroupId(eq(PROFILE_ID.getId()), eq(GROUP_ID.getId()), any()))
                .thenReturn(new PageData<>(apps, 1, apps.size(), false));

        service.bulkOperation(TENANT_ID, GROUP_ID, PROFILE_ID, request, true, USER);

        ArgumentCaptor<AgentBulkAction> captor = ArgumentCaptor.forClass(AgentBulkAction.class);
        verify(agentBulkActionService, times(2)).save(eq(TENANT_ID), captor.capture());

        AgentBulkAction firstSave = captor.getAllValues().get(0);
        assertThat(firstSave.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(firstSave.getGroupId()).isEqualTo(GROUP_ID.getId());
        assertThat(firstSave.getProfileId()).isEqualTo(PROFILE_ID.getId());
        assertThat(firstSave.getActionType()).isEqualTo(AgentAppEventActionType.UPDATE);

        AgentBulkAction finalSave = captor.getAllValues().get(1);
        assertThat(finalSave.getTotal()).isEqualTo(1);
        assertThat(finalSave.getSubmitted()).isEqualTo(1);
        assertThat(finalSave.getSkipCounts()).isNullOrEmpty();
    }

    @Test
    void bulkOperation_setsBulkActionIdOnEventRequest() throws Exception {
        BulkOperationRequest request = createRequest(AgentAppEventActionType.UPDATE);

        AgentAppProfile profile = createProfile();
        when(profileService.findProfileById(TENANT_ID, PROFILE_ID)).thenReturn(profile);

        List<AgentApplication> apps = List.of(createApp(), createApp());
        when(applicationDao.findByApplicationProfileIdAndAgentGroupId(eq(PROFILE_ID.getId()), eq(GROUP_ID.getId()), any()))
                .thenReturn(new PageData<>(apps, 1, apps.size(), false));

        AgentBulkAction result = service.bulkOperation(TENANT_ID, GROUP_ID, PROFILE_ID, request, true, USER);

        ArgumentCaptor<AgentAppEventRequest> eventCaptor = ArgumentCaptor.forClass(AgentAppEventRequest.class);
        verify(tbAgentApplicationService, times(2)).execActionEvent(any(), any(), eventCaptor.capture(), any(), eq(true));

        UUID expectedBulkActionId = result.getId().getId();
        for (AgentAppEventRequest capturedRequest : eventCaptor.getAllValues()) {
            assertThat(capturedRequest.getBulkActionId()).isEqualTo(expectedBulkActionId);
        }
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

    private AgentApplication createApp() {
        AgentApplication app = new AgentApplication();
        app.setId(new AgentApplicationId(UUID.randomUUID()));
        app.setName("app-" + app.getId().getId().toString().substring(0, 8));
        app.setTemplateId(TEMPLATE_ID);
        return app;
    }
}
