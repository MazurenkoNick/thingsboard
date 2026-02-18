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
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.step.InfoStep;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppTemplateDao;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    void testValidateDataImpl_genericWithTemplateId_thenException() {
        AgentApplication app = createValidApplication();
        app.setAppType(AgentApplicationType.GENERIC);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).containsIgnoringCase("must not have a template");
    }

    @Test
    void testValidateDataImpl_nonGenericWithoutTemplateId_thenException() {
        AgentApplication app = createValidApplication();
        app.setAppType(AgentApplicationType.EDGE);
        app.setTemplateId(null);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).containsIgnoringCase("assigned to template");
    }

    @Test
    void testValidateDataImpl_genericWithoutTemplateId_thenOK() {
        AgentApplication app = createValidApplication();
        app.setAppType(AgentApplicationType.GENERIC);
        app.setTemplateId(null);

        assertDoesNotThrow(() -> validator.validateDataImpl(tenantId, app));
    }

    @Test
    void testValidateDataImpl_emptyInstallSteps_thenException() {
        AgentApplication app = new AgentApplication();
        app.setAgentId(agentId);
        app.setAppType(AgentApplicationType.EDGE);
        app.setTemplateId(templateId);
        app.setStartSteps(Collections.emptyList());

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).containsIgnoringCase("install steps");
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

    // ==================== Step validation tests ====================

    @Test
    void testValidateDataImpl_validStepsWithNextId_thenOK() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        InfoStep step1 = new InfoStep(id1, id2, "Step 1", false);
        InfoStep step2 = new InfoStep(id2, null, "Step 2", false);

        AgentApplication app = createValidApplication();
        app.setStartSteps(new ArrayList<>(List.of(step1, step2)));

        assertDoesNotThrow(() -> validator.validateDataImpl(tenantId, app));
    }

    @Test
    void testValidateDataImpl_installStepWithNullId_thenException() {
        InfoStep stepWithNullId = new InfoStep();
        stepWithNullId.setTitle("Step without ID");

        AgentApplication app = createValidApplication();
        app.setStartSteps(new ArrayList<>(List.of(stepWithNullId)));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).contains("Invalid install steps");
        assertThat(exception.getMessage()).contains("null id");
    }

    @Test
    void testValidateDataImpl_installStepsWithCircularReference_thenException() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        InfoStep step1 = new InfoStep(id1, id2, "Step 1", false);
        InfoStep step2 = new InfoStep(id2, id1, "Step 2", false); // circular

        AgentApplication app = createValidApplication();
        app.setStartSteps(new ArrayList<>(List.of(step1, step2)));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).contains("Invalid install steps");
    }

    @Test
    void testValidateDataImpl_installStepsWithOrphanedStep_thenException() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        InfoStep step1 = new InfoStep(id1, id2, "Step 1", false);
        InfoStep step2 = new InfoStep(id2, null, "Step 2", false);
        InfoStep step3 = new InfoStep(id3, null, "Orphan", false); // orphaned

        AgentApplication app = createValidApplication();
        app.setStartSteps(new ArrayList<>(List.of(step1, step2, step3)));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).contains("Invalid install steps");
    }

    @Test
    void testValidateDataImpl_upgradeStepWithNullId_thenException() {
        InfoStep stepWithNullId = new InfoStep();
        stepWithNullId.setTitle("Upgrade step without ID");

        AgentApplication app = createValidApplication();
        app.setUpgradeSteps(new ArrayList<>(List.of(stepWithNullId)));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).contains("Invalid upgrade steps");
        assertThat(exception.getMessage()).contains("null id");
    }

    @Test
    void testValidateDataImpl_validUpgradeSteps_thenOK() {
        UUID id1 = UUID.randomUUID();

        InfoStep upgradeStep = new InfoStep(id1, null, "Upgrade Step", false);

        AgentApplication app = createValidApplication();
        app.setUpgradeSteps(new ArrayList<>(List.of(upgradeStep)));

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

    // ==================== Helper methods ====================

    private AgentApplication createValidApplication() {
        UUID stepId = UUID.randomUUID();
        InfoStep step = new InfoStep(stepId, null, "Test Step", false);

        AgentApplication app = new AgentApplication();
        app.setAgentId(agentId);
        app.setAppType(AgentApplicationType.EDGE);
        app.setTemplateId(templateId);
        app.setStartSteps(new ArrayList<>(List.of(step)));
        return app;
    }
}
