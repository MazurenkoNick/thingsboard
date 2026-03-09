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
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppTemplateDao;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willReturn;

@SpringBootTest(classes = AgentAppTemplateDataValidator.class)
class AgentAppTemplateDataValidatorTest {

    @MockitoBean
    AgentAppTemplateDao agentAppTemplateDao;
    @Autowired
    AgentAppTemplateDataValidator validator;

    TenantId tenantId = TenantId.SYS_TENANT_ID;
    AgentAppTemplateId templateId = new AgentAppTemplateId(UUID.fromString("6ef79cdf-37a8-4119-b682-2e7ed4e018da"));

    @BeforeEach
    void setUp() {
    }

    // ==================== Basic validation tests ====================

    @Test
    void testValidateDataImpl_nullAppType_thenException() {
        AgentAppTemplate template = createValidTemplate();
        template.setAppType(null);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, template));
        assertThat(exception.getMessage()).containsIgnoringCase("app type");
    }

    @Test
    void testValidateDataImpl_nullCurrentVersion_thenException() {
        AgentAppTemplate template = createValidTemplate();
        template.setCurrentVersion(null);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, template));
        assertThat(exception.getMessage()).containsIgnoringCase("current version");
    }

    @Test
    void testValidateDataImpl_blankCurrentVersion_thenException() {
        AgentAppTemplate template = createValidTemplate();
        template.setCurrentVersion("  ");

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, template));
        assertThat(exception.getMessage()).containsIgnoringCase("current version");
    }

    @Test
    void testValidateDataImpl_nullPreviousVersion_thenException() {
        AgentAppTemplate template = createValidTemplate();
        template.setPreviousVersion(null);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, template));
        assertThat(exception.getMessage()).containsIgnoringCase("previous version");
    }

    @Test
    void testValidateDataImpl_blankPreviousVersion_thenException() {
        AgentAppTemplate template = createValidTemplate();
        template.setPreviousVersion("  ");

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, template));
        assertThat(exception.getMessage()).containsIgnoringCase("previous version");
    }

    @Test
    void testValidateDataImpl_valid_thenOK() {
        AgentAppTemplate template = createValidTemplate();

        assertDoesNotThrow(() -> validator.validateDataImpl(tenantId, template));
    }

    // ==================== Step validation tests ====================

    @Test
    void testValidateDataImpl_validInstallStepsWithNextId_thenOK() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        ComposeStartStep step1 = createStep(id1, id2, "Step 1", false);
        ComposeStartStep step2 = createStep(id2, null, "Step 2", false);

        AgentAppTemplate template = createValidTemplate();
        template.setStartSteps(new ArrayList<>(List.of(step1, step2)));

        assertDoesNotThrow(() -> validator.validateDataImpl(tenantId, template));
    }

    @Test
    void testValidateDataImpl_installStepWithNullId_thenException() {
        ComposeStartStep stepWithNullId = new ComposeStartStep();
        stepWithNullId.setTitle("Step without ID");

        AgentAppTemplate template = createValidTemplate();
        template.setStartSteps(new ArrayList<>(List.of(stepWithNullId)));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, template));
        assertThat(exception.getMessage()).contains("Invalid install steps");
        assertThat(exception.getMessage()).contains("null id");
    }

    @Test
    void testValidateDataImpl_installStepsWithCircularReference_thenException() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        ComposeStartStep step1 = createStep(id1, id2, "Step 1", false);
        ComposeStartStep step2 = createStep(id2, id1, "Step 2", false); // circular

        AgentAppTemplate template = createValidTemplate();
        template.setStartSteps(new ArrayList<>(List.of(step1, step2)));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, template));
        assertThat(exception.getMessage()).contains("Invalid install steps");
    }

    @Test
    void testValidateDataImpl_installStepsWithOrphanedStep_thenException() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        ComposeStartStep step1 = createStep(id1, id2, "Step 1", false);
        ComposeStartStep step2 = createStep(id2, null, "Step 2", false);
        ComposeStartStep step3 = createStep(id3, null, "Orphan", false); // orphaned

        AgentAppTemplate template = createValidTemplate();
        template.setStartSteps(new ArrayList<>(List.of(step1, step2, step3)));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, template));
        assertThat(exception.getMessage()).contains("Invalid install steps");
    }

    @Test
    void testValidateDataImpl_installStepsWithInvalidNextIdReference_thenException() {
        UUID id1 = UUID.randomUUID();
        UUID nonExistentId = UUID.randomUUID();

        ComposeStartStep step1 = createStep(id1, nonExistentId, "Step 1", false);

        AgentAppTemplate template = createValidTemplate();
        template.setStartSteps(new ArrayList<>(List.of(step1)));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, template));
        assertThat(exception.getMessage()).contains("Invalid install steps");
        assertThat(exception.getMessage()).contains("non-existent nextId");
    }

    @Test
    void testValidateDataImpl_upgradeStepWithNullId_thenException() {
        ComposeStartStep stepWithNullId = new ComposeStartStep();
        stepWithNullId.setTitle("Upgrade step without ID");

        AgentAppTemplate template = createValidTemplate();
        template.setUpgradeSteps(new ArrayList<>(List.of(stepWithNullId)));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, template));
        assertThat(exception.getMessage()).contains("Invalid upgrade steps");
        assertThat(exception.getMessage()).contains("null id");
    }

    @Test
    void testValidateDataImpl_upgradeStepsWithCircularReference_thenException() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        ComposeStartStep step1 = createStep(id1, id2, "Upgrade Step 1", false);
        ComposeStartStep step2 = createStep(id2, id1, "Upgrade Step 2", false); // circular

        AgentAppTemplate template = createValidTemplate();
        template.setUpgradeSteps(new ArrayList<>(List.of(step1, step2)));

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, template));
        assertThat(exception.getMessage()).contains("Invalid upgrade steps");
    }

    @Test
    void testValidateDataImpl_validUpgradeSteps_thenOK() {
        UUID id1 = UUID.randomUUID();

        ComposeStartStep upgradeStep = createStep(id1, null, "Upgrade Step", false);

        AgentAppTemplate template = createValidTemplate();
        template.setUpgradeSteps(new ArrayList<>(List.of(upgradeStep)));

        assertDoesNotThrow(() -> validator.validateDataImpl(tenantId, template));
    }

    @Test
    void testValidateDataImpl_emptySteps_thenOK() {
        AgentAppTemplate template = createValidTemplate();
        template.setStartSteps(null);
        template.setUpgradeSteps(null);

        assertDoesNotThrow(() -> validator.validateDataImpl(tenantId, template));
    }

    // ==================== Update validation tests ====================

    @Test
    void testValidateUpdate_existingTemplate_thenReturnOld() {
        AgentAppTemplate oldTemplate = createValidTemplate();
        oldTemplate.setId(templateId);
        willReturn(oldTemplate).given(agentAppTemplateDao).findById(eq(tenantId), eq(templateId.getId()));

        AgentAppTemplate newTemplate = createValidTemplate();
        newTemplate.setId(templateId);

        AgentAppTemplate result = validator.validateUpdate(tenantId, newTemplate);
        assertThat(result).isEqualTo(oldTemplate);
    }

    @Test
    void testValidateUpdate_nonExistentTemplate_thenException() {
        willReturn(null).given(agentAppTemplateDao).findById(eq(tenantId), eq(templateId.getId()));

        AgentAppTemplate template = createValidTemplate();
        template.setId(templateId);

        DataValidationException exception = assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, template));
        assertThat(exception.getMessage()).contains("non existing agent app template");
    }

    // ==================== Helper methods ====================

    private ComposeStartStep createStep(UUID id, UUID nextId, String title, boolean templateOnly) {
        ComposeStartStep step = new ComposeStartStep();
        step.setId(id);
        step.setNextId(nextId);
        step.setTitle(title);
        step.setTemplateOnly(templateOnly);
        return step;
    }

    private AgentAppTemplate createValidTemplate() {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(AgentApplicationType.GENERIC);
        template.setCurrentVersion("1.0.0");
        template.setPreviousVersion("0.9.0");
        return template;
    }
}
