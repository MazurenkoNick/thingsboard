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
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventDao;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentAppTemplateDao;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willReturn;

@SpringBootTest(classes = AgentApplicationDataValidator.class)
class AgentApplicationDataValidatorTest {

    @MockitoBean
    AgentService agentService;
    @MockitoBean
    AgentApplicationDao agentApplicationDao;
    @MockitoBean
    AgentAppTemplateDao agentAppTemplateDao;
    @MockitoBean
    AgentAppEventDao agentAppEventDao;
    @MockitoBean
    AgentAppProfileService agentAppProfileService;
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
        willReturn(template).given(agentAppTemplateDao).findById(any(), eq(templateId.getId()));
    }

    // ==================== Basic validation tests ====================

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
        willReturn(null).given(agentAppTemplateDao).findById(any(), eq(nonExistentTemplateId.getId()));

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

    // ==================== Config validation tests ====================

    @Test
    void testValidateDataImpl_nullConfigWithProfile_thenOK() {
        // A null config is allowed when the app is managed by a profile —
        // the config will be resolved from the profile server-side.
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
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(new com.fasterxml.jackson.databind.node.TextNode("version: '3'"));
        app.setConfig(config);

        assertDoesNotThrow(() -> validator.validateDataImpl(tenantId, app));
    }

    // ==================== Update validation tests ====================

    @Test
    void testValidateUpdate_existingApplication_thenReturnOld() {
        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(oldApp).given(agentApplicationDao).findById(eq(tenantId), eq(applicationId.getId()));

        AgentApplication newApp = createValidApplication();
        newApp.setId(applicationId);

        AgentApplication result = validator.validateUpdate(tenantId, newApp);
        assertThat(result).isEqualTo(oldApp);
    }

    @Test
    void testValidateUpdate_nonExistentApplication_thenException() {
        willReturn(null).given(agentApplicationDao).findById(eq(tenantId), eq(applicationId.getId()));

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
        willReturn(oldApp).given(agentApplicationDao).findById(eq(tenantId), eq(applicationId.getId()));

        AgentApplication newApp = createValidApplication();
        newApp.setId(applicationId);
        newApp.setPendingDeletion(true);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("pending for removal");
    }

    @Test
    void testValidateUpdate_activeEvent_thenException() {
        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(oldApp).given(agentApplicationDao).findById(eq(tenantId), eq(applicationId.getId()));
        willReturn(true).given(agentAppEventDao).hasActiveEventForApplication(eq(applicationId.getId()));

        AgentApplication newApp = createValidApplication();
        newApp.setId(applicationId);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("event is being processed");
    }

    // ==================== Upgrade version validation tests ====================

    @Test
    void testValidateUpdate_upgradeVersion_noNextVersion_thenException() {
        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(oldApp).given(agentApplicationDao).findById(eq(tenantId), eq(applicationId.getId()));

        AgentAppTemplate currentTemplate = new AgentAppTemplate();
        currentTemplate.setId(templateId);
        currentTemplate.setCurrentVersion("1.0");
        currentTemplate.setNextVersion(null);
        willReturn(currentTemplate).given(agentAppTemplateDao).findById(eq(TenantId.SYS_TENANT_ID), eq(templateId.getId()));

        AgentAppTemplateId desiredTemplateId = new AgentAppTemplateId(UUID.randomUUID());
        AgentApplication newApp = createValidApplication();
        newApp.setId(applicationId);
        newApp.setDesiredTemplateId(desiredTemplateId);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("No next version");
    }

    @Test
    void testValidateUpdate_upgradeVersion_desiredTemplateNotFound_thenException() {
        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(oldApp).given(agentApplicationDao).findById(eq(tenantId), eq(applicationId.getId()));

        AgentAppTemplate currentTemplate = new AgentAppTemplate();
        currentTemplate.setId(templateId);
        currentTemplate.setCurrentVersion("1.0");
        currentTemplate.setNextVersion("2.0");
        willReturn(currentTemplate).given(agentAppTemplateDao).findById(eq(TenantId.SYS_TENANT_ID), eq(templateId.getId()));

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
    void testValidateUpdate_upgradeVersion_versionMismatch_thenException() {
        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(oldApp).given(agentApplicationDao).findById(eq(tenantId), eq(applicationId.getId()));

        AgentAppTemplate currentTemplate = new AgentAppTemplate();
        currentTemplate.setId(templateId);
        currentTemplate.setCurrentVersion("1.0");
        currentTemplate.setNextVersion("2.0");
        willReturn(currentTemplate).given(agentAppTemplateDao).findById(eq(TenantId.SYS_TENANT_ID), eq(templateId.getId()));

        AgentAppTemplateId desiredTemplateId = new AgentAppTemplateId(UUID.randomUUID());
        AgentAppTemplate desiredTemplate = new AgentAppTemplate();
        desiredTemplate.setId(desiredTemplateId);
        desiredTemplate.setCurrentVersion("3.0");
        willReturn(desiredTemplate).given(agentAppTemplateDao).findById(eq(TenantId.SYS_TENANT_ID), eq(desiredTemplateId.getId()));

        AgentApplication newApp = createValidApplication();
        newApp.setId(applicationId);
        newApp.setDesiredTemplateId(desiredTemplateId);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("does not match the next available version");
    }

    @Test
    void testValidateUpdate_upgradeVersion_valid_thenOK() {
        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        willReturn(oldApp).given(agentApplicationDao).findById(eq(tenantId), eq(applicationId.getId()));

        AgentAppTemplate currentTemplate = new AgentAppTemplate();
        currentTemplate.setId(templateId);
        currentTemplate.setCurrentVersion("1.0");
        currentTemplate.setNextVersion("2.0");
        willReturn(currentTemplate).given(agentAppTemplateDao).findById(eq(TenantId.SYS_TENANT_ID), eq(templateId.getId()));

        AgentAppTemplateId desiredTemplateId = new AgentAppTemplateId(UUID.randomUUID());
        AgentAppTemplate desiredTemplate = new AgentAppTemplate();
        desiredTemplate.setId(desiredTemplateId);
        desiredTemplate.setCurrentVersion("2.0");
        willReturn(desiredTemplate).given(agentAppTemplateDao).findById(eq(TenantId.SYS_TENANT_ID), eq(desiredTemplateId.getId()));

        AgentApplication newApp = createValidApplication();
        newApp.setId(applicationId);
        newApp.setDesiredTemplateId(desiredTemplateId);

        AgentApplication result = validator.validateUpdate(tenantId, newApp);
        assertThat(result).isEqualTo(oldApp);
    }

    // ==================== Stale-template config-guard tests ====================

    @Test
    void testValidateUpdate_staleTemplate_noProfile_thenOK() {
        AgentApplication oldApp = createValidApplication();
        oldApp.setId(applicationId);
        oldApp.setConfig(createEdgeComposeConfig("rk-1", "tb.cloud"));
        willReturn(oldApp).given(agentApplicationDao).findById(eq(tenantId), eq(applicationId.getId()));

        AgentApplication newApp = createValidApplication();
        newApp.setId(applicationId);
        newApp.setConfig(createEdgeComposeConfig("rk-2", "other.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    @Test
    void testValidateUpdate_staleTemplate_profileMissing_credOnlyChange_thenOK() {
        // When the profile lookup fails but config diff is cred-only, the
        // profile-managed guard passes and the stale-template guard also
        // returns early (can't check versions without the profile).
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        AgentApplication oldApp = createProfileManagedApplication(profileId);
        oldApp.setConfig(createEdgeComposeConfig("rk-1", "tb.cloud"));
        willReturn(oldApp).given(agentApplicationDao).findById(eq(tenantId), eq(applicationId.getId()));
        willReturn(null).given(agentAppProfileService).findProfileById(eq(tenantId), eq(profileId));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setConfig(createEdgeComposeConfig("rotated-rk", "tb.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    @Test
    void testValidateUpdate_staleTemplate_sameTemplateId_credOnlyChange_thenOK() {
        // Template IDs match so the stale-template guard skips. The profile-
        // managed guard still enforces cred-only diffs.
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        AgentApplication oldApp = createProfileManagedApplication(profileId);
        oldApp.setConfig(createEdgeComposeConfig("rk-1", "tb.cloud"));
        willReturn(oldApp).given(agentApplicationDao).findById(eq(tenantId), eq(applicationId.getId()));
        willReturn(profileWithTemplate(profileId, templateId)).given(agentAppProfileService).findProfileById(eq(tenantId), eq(profileId));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setConfig(createEdgeComposeConfig("rk-2", "tb.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    @Test
    void testValidateUpdate_staleTemplate_differentTemplateId_configUnchanged_thenOK() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        AgentAppTemplateId profileTemplateId = new AgentAppTemplateId(UUID.randomUUID());
        AgentApplication oldApp = createProfileManagedApplication(profileId);
        oldApp.setConfig(createEdgeComposeConfig("rk-1", "tb.cloud"));
        willReturn(oldApp).given(agentApplicationDao).findById(eq(tenantId), eq(applicationId.getId()));
        willReturn(profileWithTemplate(profileId, profileTemplateId)).given(agentAppProfileService).findProfileById(eq(tenantId), eq(profileId));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setConfig(createEdgeComposeConfig("rk-1", "tb.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    @Test
    void testValidateUpdate_staleTemplate_differentTemplateId_onlyCredsChanged_thenOK() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        AgentAppTemplateId profileTemplateId = new AgentAppTemplateId(UUID.randomUUID());
        AgentApplication oldApp = createProfileManagedApplication(profileId);
        oldApp.setConfig(createEdgeComposeConfig("rk-1", "tb.cloud"));
        willReturn(oldApp).given(agentApplicationDao).findById(eq(tenantId), eq(applicationId.getId()));
        willReturn(profileWithTemplate(profileId, profileTemplateId)).given(agentAppProfileService).findProfileById(eq(tenantId), eq(profileId));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setConfig(createEdgeComposeConfig("rotated-rk", "tb.cloud"));

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    @Test
    void testValidateUpdate_staleTemplate_differentTemplateId_nonCredConfigChanged_thenException() {
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        AgentAppTemplateId profileTemplateId = new AgentAppTemplateId(UUID.randomUUID());
        AgentApplication oldApp = createProfileManagedApplication(profileId);
        oldApp.setConfig(createEdgeComposeConfig("rk-1", "tb.cloud"));
        willReturn(oldApp).given(agentApplicationDao).findById(eq(tenantId), eq(applicationId.getId()));
        willReturn(profileWithTemplate(profileId, profileTemplateId)).given(agentAppProfileService).findProfileById(eq(tenantId), eq(profileId));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setConfig(createEdgeComposeConfig("rk-1", "other.cloud"));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newApp));
        assertThat(exception.getMessage()).contains("out of sync");
    }

    @Test
    void testValidateUpdate_staleTemplate_withDesiredTemplate_skipsStaleGuard() {
        // Upgrade path (desiredTemplateId != null) routes to validateUpgradeVersion
        // and must not trip the stale-template guard even if the config changed.
        AgentAppProfileId profileId = new AgentAppProfileId(UUID.randomUUID());
        AgentApplication oldApp = createProfileManagedApplication(profileId);
        oldApp.setConfig(createEdgeComposeConfig("rk-1", "tb.cloud"));
        willReturn(oldApp).given(agentApplicationDao).findById(eq(tenantId), eq(applicationId.getId()));

        AgentAppTemplate currentTemplate = new AgentAppTemplate();
        currentTemplate.setId(templateId);
        currentTemplate.setCurrentVersion("1.0");
        currentTemplate.setNextVersion("2.0");
        willReturn(currentTemplate).given(agentAppTemplateDao).findById(eq(TenantId.SYS_TENANT_ID), eq(templateId.getId()));

        AgentAppTemplateId desiredTemplateId = new AgentAppTemplateId(UUID.randomUUID());
        AgentAppTemplate desiredTemplate = new AgentAppTemplate();
        desiredTemplate.setId(desiredTemplateId);
        desiredTemplate.setCurrentVersion("2.0");
        willReturn(desiredTemplate).given(agentAppTemplateDao).findById(eq(TenantId.SYS_TENANT_ID), eq(desiredTemplateId.getId()));

        AgentApplication newApp = createProfileManagedApplication(profileId);
        newApp.setConfig(createEdgeComposeConfig("rk-1", "other.cloud"));
        newApp.setDesiredTemplateId(desiredTemplateId);

        assertDoesNotThrow(() -> validator.validateUpdate(tenantId, newApp));
    }

    // ==================== Helper methods ====================

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AgentApplication createValidApplication() {
        AgentApplication app = new AgentApplication();
        app.setAgentId(agentId);
        app.setAppType(AgentApplicationType.EDGE);
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

    private AgentAppProfile profileWithTemplate(AgentAppProfileId id, AgentAppTemplateId tmpl) {
        AgentAppProfile profile = new AgentAppProfile(id);
        profile.setTemplateId(tmpl);
        return profile;
    }

    private DockerComposeConfig createEdgeComposeConfig(String routingKey, String rpcHost) {
        ObjectNode env = MAPPER.createObjectNode();
        env.put("CLOUD_ROUTING_KEY", routingKey);
        env.put("CLOUD_ROUTING_SECRET", "secret");
        env.put("CLOUD_RPC_HOST", rpcHost);

        ObjectNode service = MAPPER.createObjectNode();
        service.put("image", "thingsboard/tb-edge:3.8.0");
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
