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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.config.AgentAppArgument;
import org.thingsboard.server.common.data.agent.config.AgentAppArgumentSource;
import org.thingsboard.server.common.data.agent.config.AgentAppArgumentValueType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentAppTemplateDao;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.entity.EntityDaoService;
import org.thingsboard.server.dao.entity.EntityServiceRegistry;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = AgentApplicationDataValidator.class)
class AgentApplicationDataValidatorTest {

    @MockitoBean
    AgentService agentService;
    @MockitoBean
    AgentApplicationDao agentApplicationDao;
    @MockitoBean
    AgentAppTemplateDao agentAppTemplateDao;
    @MockitoBean
    AgentAppProfileService agentAppProfileService;
    @MockitoBean
    EntityServiceRegistry entityServiceRegistry;
    @MockitoBean(name = "agentArgDeviceDaoService")
    EntityDaoService deviceDaoService;
    @Autowired
    AgentApplicationDataValidator validator;

    TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));
    AgentId agentId = new AgentId(UUID.fromString("8ef79cdf-37a8-4119-b682-2e7ed4e018da"));
    AgentApplicationId applicationId = new AgentApplicationId(UUID.fromString("7ef79cdf-37a8-4119-b682-2e7ed4e018da"));
    AgentAppTemplateId templateId = new AgentAppTemplateId(UUID.fromString("6ef79cdf-37a8-4119-b682-2e7ed4e018da"));

    @BeforeEach
    void setUp() {
        Agent agent = new Agent();
        agent.setId(agentId);
        agent.setTenantId(tenantId);
        agent.setName("Test Agent");
        willReturn(agent).given(agentService).findAgentById(eq(tenantId), eq(agentId));

        AgentAppTemplate template = new AgentAppTemplate();
        template.setId(templateId);
        template.setCurrentVersion("1.0");
        template.setNextVersion("2.0");
        willReturn(template).given(agentAppTemplateDao).findById(eq(TenantId.SYS_TENANT_ID), eq(templateId.getId()));
    }

    // ==================== Basic data validation (validateDataImpl) ====================

    @Test
    void testValidateDataImpl_nullAgentId_thenException() {
        AgentApplication app = new AgentApplication();

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).containsIgnoringCase("assigned to agent");
    }

    @Test
    void testValidateDataImpl_nullTemplateId_thenException() {
        AgentApplication app = new AgentApplication();
        app.setAgentId(agentId);
        app.setAppType(AgentApplicationType.EDGE);
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName("test-project");

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).containsIgnoringCase("assigned to template");
    }

    @Test
    void testValidateDataImpl_nullAppType_thenException() {
        AgentApplication app = new AgentApplication();
        app.setAgentId(agentId);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).containsIgnoringCase("type must not be null");
    }

    @Test
    void testValidateDataImpl_nullOrigin_thenException() {
        AgentApplication app = createValidApplication();
        app.setOrigin(null);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).containsIgnoringCase("origin must not be null");
    }

    @Test
    void testValidateDataImpl_nullProjectName_thenException() {
        AgentApplication app = createValidApplication();
        app.setProjectName(null);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).containsIgnoringCase("project name must not be null");
    }

    @Test
    void testValidateDataImpl_nullTemplateId_forAnyType_thenException() {
        AgentApplication app = createValidApplication();
        app.setTemplateId(null);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).containsIgnoringCase("assigned to template");
    }

    @Test
    void testValidateDataImpl_nonExistentAgent_thenException() {
        AgentId nonExistentAgentId = new AgentId(UUID.randomUUID());
        willReturn(null).given(agentService).findAgentById(eq(tenantId), eq(nonExistentAgentId));

        AgentApplication app = createValidApplication();
        app.setAgentId(nonExistentAgentId);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).contains("non-existent agent");
    }

    @Test
    void testValidateDataImpl_agentFromDifferentTenant_thenException() {
        TenantId differentTenantId = TenantId.fromUUID(UUID.randomUUID());
        Agent agentFromDifferentTenant = new Agent();
        agentFromDifferentTenant.setId(agentId);
        agentFromDifferentTenant.setTenantId(differentTenantId);
        willReturn(agentFromDifferentTenant).given(agentService).findAgentById(eq(tenantId), eq(agentId));

        AgentApplication app = createValidApplication();

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).contains("different tenant");
    }

    @Test
    void testValidateDataImpl_nonExistentTemplate_thenException() {
        AgentAppTemplateId nonExistentTemplateId = new AgentAppTemplateId(UUID.randomUUID());
        willReturn(null).given(agentAppTemplateDao).findById(eq(TenantId.SYS_TENANT_ID), eq(nonExistentTemplateId.getId()));

        AgentApplication app = createValidApplication();
        app.setTemplateId(nonExistentTemplateId);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).contains("non-existent template");
    }

    @Test
    void testValidateDataImpl_nameTooLong_thenException() {
        AgentApplication app = createValidApplication();
        app.setName("a".repeat(256));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).contains("name length");
    }

    @Test
    void testValidateDataImpl_valid_thenOK() {
        AgentApplication app = createValidApplication();

        assertDoesNotThrow(() -> validator.validateDataImpl(tenantId, app));
    }

    @Test
    void testValidateDataImpl_nullConfigWithProfile_thenOK() {
        AgentApplication app = createValidApplication();
        app.setConfig(null);
        app.setApplicationProfileId(new AgentAppProfileId(UUID.randomUUID()));

        assertDoesNotThrow(() -> validator.validateDataImpl(tenantId, app));
    }

    @Test
    void testValidateDataImpl_nullConfigWithoutProfile_thenException() {
        AgentApplication app = createValidApplication();
        app.setConfig(null);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).containsIgnoringCase("config must not be null");
    }

    @Test
    void testValidateDataImpl_dockerComposeConfig_nullCompose_thenException() {
        AgentApplication app = createValidApplication();
        DockerComposeConfig config = new DockerComposeConfig();
        app.setConfig(config);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).contains("compose content");
    }

    @Test
    void testValidateDataImpl_dockerComposeConfig_valid_thenOK() {
        AgentApplication app = createValidApplication();

        assertDoesNotThrow(() -> validator.validateDataImpl(tenantId, app));
    }

    @Test
    void testValidateDataImpl_concreteEntityArgument_invokesReferenceValidator() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        willReturn(deviceDaoService).given(entityServiceRegistry).getServiceByEntityType(EntityType.DEVICE);
        willReturn(Optional.of(new Device(deviceId))).given(deviceDaoService).findEntity(eq(tenantId), any());

        AgentApplication app = createValidApplication();
        app.setConfig(configWithDeviceArgument(deviceId));

        assertDoesNotThrow(() -> validator.validateDataImpl(tenantId, app));
        verify(entityServiceRegistry).getServiceByEntityType(EntityType.DEVICE);
        verify(deviceDaoService).findEntity(eq(tenantId), eq(deviceId));
    }

    @Test
    void testValidateDataImpl_concreteEntityArgument_nonExistentEntity_thenException() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        willReturn(deviceDaoService).given(entityServiceRegistry).getServiceByEntityType(EntityType.DEVICE);
        willReturn(Optional.empty()).given(deviceDaoService).findEntity(eq(tenantId), any());

        AgentApplication app = createValidApplication();
        app.setConfig(configWithDeviceArgument(deviceId));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).containsIgnoringCase("non-existent");
    }

    // ==================== Update — preconditions ====================

    @Test
    void testValidateUpdate_nonExistentApplication_thenException() {
        willReturn(null).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        AgentApplication app = createValidApplication();
        app.setId(applicationId);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, app));
        assertThat(exception.getMessage()).contains("non existing agent application");
    }

    @Test
    void testValidateUpdate_pendingDeletion_thenException() {
        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        oldApp.setPendingDeletion(true);
        willReturn(asInfo(oldApp)).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        AgentApplication newApp = createValidApplication();
        newApp.setId(applicationId);
        newApp.setPendingDeletion(true);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("pending for removal");
    }

    // ==================== Update — same profile, no real config change (early return) ====================

    @Test
    void testValidateUpdate_sameProfile_configUnchanged_thenOK() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        stubExistingApplication(profileId, createEdgeComposeConfig("rk-1", "tb.cloud"));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setConfig(createEdgeComposeConfig("rk-1", "tb.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    @Test
    void testValidateUpdate_sameProfile_credentialsRotated_thenOK() {
        // Cred-only diffs are accepted on a profile-managed app — users can rotate
        // routing keys / tokens without going through a profile re-sync.
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        stubExistingApplication(profileId, createEdgeComposeConfig("rk-1", "tb.cloud"));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setConfig(createEdgeComposeConfig("rotated-rk", "tb.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    // ==================== Update — same profile, config refetched from profile ====================
    // Triggered when the profile's config drifts and the client pushes the  profile's new config back into the app.
    // Requires the profile's template to still be at the application's current version.

    @Test
    void testValidateUpdate_sameProfile_configReplacedWithProfileConfig_thenOK() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        stubExistingApplication(profileId, createEdgeComposeConfig("rk-1", "tb.cloud"));

        DockerComposeConfig profileConfig = createEdgeComposeConfig("rk-1", "other.cloud");
        stubProfile(profileId, templateId, profileConfig);

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setConfig(createEdgeComposeConfig("rk-1", "other.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    @Test
    void testValidateUpdate_sameProfile_configReplaced_butProfileTemplateAheadOfApp_thenException() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        AgentAppTemplateId profileTemplateId = new AgentAppTemplateId(UUID.randomUUID());
        stubExistingApplication(profileId, createEdgeComposeConfig("rk-1", "tb.cloud"));

        stubProfile(profileId, profileTemplateId, createEdgeComposeConfig("rk-1", "other.cloud"));
        stubTemplate(profileTemplateId, "5.0");

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setConfig(createEdgeComposeConfig("rk-1", "other.cloud"));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("does not match the application's current version");
    }

    @Test
    void testValidateUpdate_sameProfile_configReplaced_butDoesNotMatchProfile_thenException() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        stubExistingApplication(profileId, createEdgeComposeConfig("rk-1", "tb.cloud"));

        stubProfile(profileId, templateId, createEdgeComposeConfig("rk-1", "profile.cloud"));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setConfig(createEdgeComposeConfig("rk-1", "client-supplied.cloud"));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("does not match the profile's config");
    }

    // ==================== Update — switching to a different profile ====================

    @Test
    void testValidateUpdate_profileSwitched_configMatchesNewProfile_thenOK() {
        AgentAppProfileId oldProfileId = new AgentAppProfileId(UUID.randomUUID());
        AgentAppProfileId newProfileId = new AgentAppProfileId(UUID.randomUUID());
        DockerComposeConfig profileConfig = createEdgeComposeConfig("rk-9", "new.cloud");

        stubExistingApplication(oldProfileId, createEdgeComposeConfig("rk-1", "tb.cloud"));
        stubProfile(newProfileId, templateId, profileConfig);

        AgentApplication newApp = createProfileManagedApplication(newProfileId);
        newApp.setConfig(createEdgeComposeConfig("rk-9", "new.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    @Test
    void testValidateUpdate_profileSwitched_newProfileTemplateAheadOfApp_thenException() {
        AgentAppProfileId oldProfileId = new AgentAppProfileId(UUID.randomUUID());
        AgentAppProfileId newProfileId = new AgentAppProfileId(UUID.randomUUID());
        AgentAppTemplateId newProfileTemplateId = new AgentAppTemplateId(UUID.randomUUID());

        stubExistingApplication(oldProfileId, createEdgeComposeConfig("rk-1", "tb.cloud"));
        stubProfile(newProfileId, newProfileTemplateId, createEdgeComposeConfig("rk-9", "new.cloud"));
        stubTemplate(newProfileTemplateId, "2.0");

        AgentApplication newApp = createProfileManagedApplication(newProfileId);
        newApp.setConfig(createEdgeComposeConfig("rk-9", "new.cloud"));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("does not match the application's current version");
    }

    @Test
    void testValidateUpdate_profileSwitched_configDoesNotMatchNewProfile_thenException() {
        AgentAppProfileId oldProfileId = new AgentAppProfileId(UUID.randomUUID());
        AgentAppProfileId newProfileId = new AgentAppProfileId(UUID.randomUUID());

        stubExistingApplication(oldProfileId, createEdgeComposeConfig("rk-1", "tb.cloud"));
        stubProfile(newProfileId, templateId, createEdgeComposeConfig("rk-9", "profile.cloud"));

        AgentApplication newApp = createProfileManagedApplication(newProfileId);
        newApp.setConfig(createEdgeComposeConfig("rk-9", "wrong.cloud"));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("does not match the profile's config");
    }

    // ==================== Update — adding a profile to an unprofiled application ====================

    @Test
    void testValidateUpdate_profileAdded_configMatchesProfile_thenOK() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        DockerComposeConfig profileConfig = createEdgeComposeConfig("rk-9", "new.cloud");

        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(asInfo(oldApp)).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        stubProfile(profileId, templateId, profileConfig);

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setConfig(createEdgeComposeConfig("rk-9", "new.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    @Test
    void testValidateUpdate_profileAdded_profileTemplateAheadOfApp_thenException() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        AgentAppTemplateId profileTemplateId = new AgentAppTemplateId(UUID.randomUUID());

        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(asInfo(oldApp)).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        stubProfile(profileId, profileTemplateId, createEdgeComposeConfig("rk-9", "new.cloud"));
        stubTemplate(profileTemplateId, "2.0");

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setConfig(createEdgeComposeConfig("rk-9", "new.cloud"));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("does not match the application's current version");
    }

    @Test
    void testValidateUpdate_profileAdded_configDoesNotMatchProfile_thenException() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());

        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(asInfo(oldApp)).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        stubProfile(profileId, templateId, createEdgeComposeConfig("rk-9", "profile.cloud"));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setConfig(createEdgeComposeConfig("rk-9", "wrong.cloud"));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("does not match the profile's config");
    }

    // ==================== Update — no profile on the new state (drop or never had one) ====================

    @Test
    void testValidateUpdate_profileDropped_anyConfigAccepted_thenOK() {
        AgentAppProfileId oldProfileId = new AgentAppProfileId(UUID.randomUUID());
        stubExistingApplication(oldProfileId, createEdgeComposeConfig("rk-1", "tb.cloud"));

        AgentApplication newApp = createValidApplication();
        newApp.setId(applicationId);
        newApp.setConfig(createEdgeComposeConfig("rk-anything", "anywhere.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    @Test
    void testValidateUpdate_neverHadProfile_thenOK() {
        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        AgentApplicationInfo oldInfo = asInfo(oldApp);
        willReturn(oldInfo).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        AgentApplication newApp = createValidApplication();
        newApp.setId(applicationId);

        AgentApplication result = validator.validateUpdate(tenantId, newApp);
        assertThat(result).isEqualTo(oldInfo);
    }

    // ==================== Upgrade — without a profile (upgrade-chain check only) ====================

    @Test
    void testValidateUpdate_upgrade_noProfile_validVersionChain_thenOK() {
        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        AgentApplicationInfo oldInfo = asInfo(oldApp);
        willReturn(oldInfo).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        AgentAppTemplateId desiredTemplateId = new AgentAppTemplateId(UUID.randomUUID());
        stubTemplate(desiredTemplateId, "2.0");

        AgentApplication newApp = createValidApplication();
        newApp.setId(applicationId);
        newApp.setDesiredTemplateId(desiredTemplateId);

        AgentApplication result = validator.validateUpdate(tenantId, newApp);
        assertThat(result).isEqualTo(oldInfo);
    }

    @Test
    void testValidateUpdate_upgrade_noProfile_currentTemplateHasNoNextVersion_thenException() {
        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(asInfo(oldApp)).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        AgentAppTemplate currentTemplate = new AgentAppTemplate();
        currentTemplate.setId(templateId);
        currentTemplate.setCurrentVersion("1.0");
        currentTemplate.setNextVersion(null);
        willReturn(currentTemplate).given(agentAppTemplateDao).findById(eq(TenantId.SYS_TENANT_ID), eq(templateId.getId()));

        AgentApplication newApp = createValidApplication();
        newApp.setId(applicationId);
        newApp.setDesiredTemplateId(new AgentAppTemplateId(UUID.randomUUID()));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("No next version");
    }

    @Test
    void testValidateUpdate_upgrade_noProfile_desiredTemplateNotFound_thenException() {
        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(asInfo(oldApp)).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        AgentAppTemplateId desiredTemplateId = new AgentAppTemplateId(UUID.randomUUID());
        willReturn(null).given(agentAppTemplateDao).findById(eq(TenantId.SYS_TENANT_ID), eq(desiredTemplateId.getId()));

        AgentApplication newApp = createValidApplication();
        newApp.setId(applicationId);
        newApp.setDesiredTemplateId(desiredTemplateId);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("Desired template not found");
    }

    @Test
    void testValidateUpdate_upgrade_noProfile_desiredVersionNotEqualToNextVersion_thenException() {
        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(asInfo(oldApp)).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        AgentAppTemplateId desiredTemplateId = new AgentAppTemplateId(UUID.randomUUID());
        stubTemplate(desiredTemplateId, "3.0");

        AgentApplication newApp = createValidApplication();
        newApp.setId(applicationId);
        newApp.setDesiredTemplateId(desiredTemplateId);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("does not match the next available version");
    }

    // ==================== Upgrade — same profile ====================

    @Test
    void testValidateUpdate_upgrade_sameProfile_alignedWithProfileTemplate_thenOK() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        AgentAppTemplateId desiredTemplateId = new AgentAppTemplateId(UUID.randomUUID());
        DockerComposeConfig profileConfig = createEdgeComposeConfig("rk-1", "tb.cloud");

        stubExistingApplication(profileId, createEdgeComposeConfig("rk-1", "tb.cloud"));
        stubTemplate(desiredTemplateId, "2.0");
        stubProfile(profileId, desiredTemplateId, profileConfig);

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setDesiredTemplateId(desiredTemplateId);
        newApp.setConfig(createEdgeComposeConfig("rotated-rk", "tb.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    @Test
    void testValidateUpdate_upgrade_sameProfile_desiredTemplateNotProfileTemplate_thenException() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        AgentAppTemplateId desiredTemplateId = new AgentAppTemplateId(UUID.randomUUID());
        AgentAppTemplateId profileTemplateId = new AgentAppTemplateId(UUID.randomUUID());

        stubExistingApplication(profileId, createEdgeComposeConfig("rk-1", "tb.cloud"));
        stubTemplate(desiredTemplateId, "2.0");
        stubProfile(profileId, profileTemplateId, createEdgeComposeConfig("rk-1", "tb.cloud"));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setDesiredTemplateId(desiredTemplateId);
        newApp.setConfig(createEdgeComposeConfig("rk-1", "tb.cloud"));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("Desired template does not match the profile's template");
    }

    @Test
    void testValidateUpdate_upgrade_sameProfile_configDoesNotMatchProfile_thenException() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        AgentAppTemplateId desiredTemplateId = new AgentAppTemplateId(UUID.randomUUID());

        stubExistingApplication(profileId, createEdgeComposeConfig("rk-1", "tb.cloud"));
        stubTemplate(desiredTemplateId, "2.0");
        stubProfile(profileId, desiredTemplateId, createEdgeComposeConfig("rk-1", "profile.cloud"));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setDesiredTemplateId(desiredTemplateId);
        newApp.setConfig(createEdgeComposeConfig("rk-1", "client.cloud"));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("does not match the profile's config");
    }

    // ==================== Upgrade — switching to a different profile ====================

    @Test
    void testValidateUpdate_upgrade_profileSwitched_alignedWithNewProfileTemplate_thenOK() {
        AgentAppProfileId oldProfileId = new AgentAppProfileId(UUID.randomUUID());
        AgentAppProfileId newProfileId = new AgentAppProfileId(UUID.randomUUID());
        AgentAppTemplateId desiredTemplateId = new AgentAppTemplateId(UUID.randomUUID());
        DockerComposeConfig profileConfig = createEdgeComposeConfig("rk-9", "new.cloud");

        stubExistingApplication(oldProfileId, createEdgeComposeConfig("rk-1", "tb.cloud"));
        stubTemplate(desiredTemplateId, "2.0");
        stubProfile(newProfileId, desiredTemplateId, profileConfig);

        AgentApplication newApp = createProfileManagedApplication(newProfileId);
        newApp.setDesiredTemplateId(desiredTemplateId);
        newApp.setConfig(createEdgeComposeConfig("rk-9", "new.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    @Test
    void testValidateUpdate_upgrade_profileSwitched_desiredTemplateNotNewProfileTemplate_thenException() {
        AgentAppProfileId oldProfileId = new AgentAppProfileId(UUID.randomUUID());
        AgentAppProfileId newProfileId = new AgentAppProfileId(UUID.randomUUID());
        AgentAppTemplateId desiredTemplateId = new AgentAppTemplateId(UUID.randomUUID());
        AgentAppTemplateId newProfileTemplateId = new AgentAppTemplateId(UUID.randomUUID());

        stubExistingApplication(oldProfileId, createEdgeComposeConfig("rk-1", "tb.cloud"));
        stubTemplate(desiredTemplateId, "2.0");
        stubProfile(newProfileId, newProfileTemplateId, createEdgeComposeConfig("rk-9", "new.cloud"));

        AgentApplication newApp = createProfileManagedApplication(newProfileId);
        newApp.setDesiredTemplateId(desiredTemplateId);
        newApp.setConfig(createEdgeComposeConfig("rk-9", "new.cloud"));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("Desired template does not match the profile's template");
    }

    // ==================== Upgrade — adding a profile to an unprofiled application ====================

    @Test
    void testValidateUpdate_upgrade_profileAdded_alignedWithProfileTemplate_thenOK() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        AgentAppTemplateId desiredTemplateId = new AgentAppTemplateId(UUID.randomUUID());
        DockerComposeConfig profileConfig = createEdgeComposeConfig("rk-9", "new.cloud");

        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(asInfo(oldApp)).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        stubTemplate(desiredTemplateId, "2.0");
        stubProfile(profileId, desiredTemplateId, profileConfig);

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setDesiredTemplateId(desiredTemplateId);
        newApp.setConfig(createEdgeComposeConfig("rk-9", "new.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    @Test
    void testValidateUpdate_upgrade_profileAdded_desiredTemplateNotProfileTemplate_thenException() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        AgentAppTemplateId desiredTemplateId = new AgentAppTemplateId(UUID.randomUUID());
        AgentAppTemplateId profileTemplateId = new AgentAppTemplateId(UUID.randomUUID());

        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(asInfo(oldApp)).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));

        stubTemplate(desiredTemplateId, "2.0");
        stubProfile(profileId, profileTemplateId, createEdgeComposeConfig("rk-9", "new.cloud"));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setDesiredTemplateId(desiredTemplateId);
        newApp.setConfig(createEdgeComposeConfig("rk-9", "new.cloud"));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("Desired template does not match the profile's template");
    }

    // ==================== Helpers ====================

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AgentApplication createValidApplication() {
        AgentApplication app = new AgentApplication();
        app.setAgentId(agentId);
        app.setAppType(AgentApplicationType.EDGE);
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName("test-project");
        app.setTemplateId(templateId);
        app.setConfig(createEdgeComposeConfig("rk-valid", "tb.cloud"));
        return app;
    }

    private AgentApplication createProfileManagedApplication(AgentAppProfileId profileId) {
        AgentApplication app = createValidApplication();
        app.setId(applicationId);
        app.setApplicationProfileId(profileId);
        return app;
    }

    private void stubExistingApplication(AgentAppProfileId profileId, DockerComposeConfig config) {
        AgentApplication oldApp = createProfileManagedApplication(profileId);
        oldApp.setConfig(config);
        willReturn(asInfo(oldApp)).given(agentApplicationDao).findInfoById(eq(tenantId), eq(applicationId.getId()));
    }

    private void stubProfile(AgentAppProfileId id, AgentAppTemplateId tmpl, DockerComposeConfig config) {
        AgentAppProfile profile = new AgentAppProfile(id);
        profile.setTemplateId(tmpl);
        profile.setConfig(config);
        willReturn(profile).given(agentAppProfileService).findProfileById(eq(tenantId), eq(id));
    }

    private void stubTemplate(AgentAppTemplateId id, String currentVersion) {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setId(id);
        template.setCurrentVersion(currentVersion);
        willReturn(template).given(agentAppTemplateDao).findById(eq(TenantId.SYS_TENANT_ID), eq(id.getId()));
    }

    private AgentApplicationInfo asInfo(AgentApplication app) {
        return new AgentApplicationInfo(app, "1.0", "2.0");
    }

    private DockerComposeConfig configWithDeviceArgument(DeviceId deviceId) {
        DockerComposeConfig config = createEdgeComposeConfig("rk-ref", "tb.cloud");
        AgentAppArgument argument = new AgentAppArgument();
        argument.setName("dev");
        argument.setSourceType(AgentAppArgumentSource.DEVICE);
        argument.setSourceEntityId(deviceId);
        argument.setValueType(AgentAppArgumentValueType.ATTRIBUTE);
        argument.setScope(AttributeScope.SERVER_SCOPE);
        argument.setKey("k");
        config.setArguments(List.of(argument));
        return config;
    }

    private DockerComposeConfig createEdgeComposeConfig(String routingKey, String rpcHost) {
        ObjectNode env = MAPPER.createObjectNode();
        env.put("CLOUD_ROUTING_KEY", routingKey);
        env.put("CLOUD_ROUTING_SECRET", "secret");
        env.put("CLOUD_RPC_HOST", rpcHost);

        ObjectNode service = MAPPER.createObjectNode();
        service.put("image", "thingsboard/tb-edge-pe:3.8.0");
        service.set("environment", env);

        ObjectNode services = MAPPER.createObjectNode();
        services.set("mytbedge", service);

        ObjectNode compose = MAPPER.createObjectNode();
        compose.set("services", services);

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);
        return config;
    }
}
