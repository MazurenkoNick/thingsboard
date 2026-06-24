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
    void testValidateUpdate_nonExistentTemplate_thenReturnNull() {
        // templates arrive from the system template repository with their id already set, so validateUpdate runs
        // even on first import; a missing prior entity is a valid create, not an error.
        willReturn(null).given(agentAppTemplateDao).findById(eq(tenantId), eq(templateId.getId()));

        AgentAppTemplate template = createValidTemplate();
        template.setId(templateId);

        AgentAppTemplate result = validator.validateUpdate(tenantId, template);
        assertThat(result).isNull();
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
        return template;
    }
}
