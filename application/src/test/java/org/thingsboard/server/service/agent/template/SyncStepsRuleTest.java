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
package org.thingsboard.server.service.agent.template;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.InfoStep;
import org.thingsboard.server.common.data.agent.template.TemplateMergeCtx;
import org.thingsboard.server.service.agent.template.merge.SyncStepsRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SyncStepsRuleTest {

    private SyncStepsRule syncStep;
    private TemplateMergeCtx ctx;

    @BeforeEach
    void setUp() {
        syncStep = new SyncStepsRule();
        ctx = TemplateMergeCtx.builder().build();
    }

    // ==================== supports() tests ====================

    @Test
    void supports_shouldReturnTrue_whenApplicationHasInstallSteps() {
        AgentApplication app = createApplication(List.of(createStep("step1")), null);
        AgentAppTemplate template = createTemplate(null, null);

        assertTrue(syncStep.supports(app, template, ctx));
    }

    @Test
    void supports_shouldReturnTrue_whenTemplateHasInstallSteps() {
        AgentApplication app = createApplication(null, null);
        AgentAppTemplate template = createTemplate(List.of(createStep("step1")), null);

        assertTrue(syncStep.supports(app, template, ctx));
    }

    @Test
    void supports_shouldReturnTrue_whenApplicationHasUpdateSteps() {
        AgentApplication app = createApplication(null, List.of(createStep("step1")));
        AgentAppTemplate template = createTemplate(null, null);

        assertTrue(syncStep.supports(app, template, ctx));
    }

    @Test
    void supports_shouldReturnTrue_whenTemplateHasUpgradeSteps() {
        AgentApplication app = createApplication(null, null);
        AgentAppTemplate template = createTemplate(null, List.of(createStep("step1")));

        assertTrue(syncStep.supports(app, template, ctx));
    }

    @Test
    void supports_shouldReturnFalse_whenBothHaveNoSteps() {
        AgentApplication app = createApplication(null, null);
        AgentAppTemplate template = createTemplate(null, null);

        assertFalse(syncStep.supports(app, template, ctx));
    }

    @Test
    void supports_shouldReturnFalse_whenBothHaveEmptySteps() {
        AgentApplication app = createApplication(Collections.emptyList(), Collections.emptyList());
        AgentAppTemplate template = createTemplate(Collections.emptyList(), Collections.emptyList());

        assertFalse(syncStep.supports(app, template, ctx));
    }

    // ==================== apply() - basic scenarios ====================

    @Test
    void apply_shouldClearSteps_whenTemplateHasNoSteps() {
        AgentApplication app = createApplication(
                new ArrayList<>(List.of(createStep("implStep"))),
                new ArrayList<>(List.of(createStep("updateStep")))
        );
        AgentAppTemplate template = createTemplate(null, null);

        syncStep.apply(app, template, ctx);

        assertTrue(app.getInstallSteps().isEmpty());
        assertTrue(app.getUpdateSteps().isEmpty());
    }

    @Test
    void apply_shouldUseTemplateSteps_whenApplicationHasNoSteps() {
        AgentAppStep templateInstallStep = createStep("templateInstall");
        AgentAppStep templateUpgradeStep = createStep("templateUpgrade");

        AgentApplication app = createApplication(null, null);
        AgentAppTemplate template = createTemplate(
                List.of(templateInstallStep),
                List.of(templateUpgradeStep)
        );

        syncStep.apply(app, template, ctx);

        assertEquals(1, app.getInstallSteps().size());
        assertEquals(templateInstallStep.getId(), app.getInstallSteps().get(0).getId());
        assertEquals(1, app.getUpdateSteps().size());
        assertEquals(templateUpgradeStep.getId(), app.getUpdateSteps().get(0).getId());
    }

    @Test
    void apply_shouldUseTemplateSteps_whenApplicationHasEmptySteps() {
        AgentAppStep templateInstallStep = createStep("templateInstall");

        AgentApplication app = createApplication(Collections.emptyList(), null);
        AgentAppTemplate template = createTemplate(List.of(templateInstallStep), null);

        syncStep.apply(app, template, ctx);

        assertEquals(1, app.getInstallSteps().size());
        assertEquals(templateInstallStep.getId(), app.getInstallSteps().get(0).getId());
    }

    // ==================== apply() - merge scenarios ====================

    @Test
    void apply_shouldPreserveExistingStep_whenIdMatches() {
        UUID sharedId = UUID.randomUUID();

        InfoStep implStep = createStepWithId(sharedId, null, "Implementation Step");
        implStep.setMessage("Custom implementation message");

        InfoStep templateStep = createStepWithId(sharedId, null, "Template Step");
        templateStep.setMessage("Default template message");

        AgentApplication app = createApplication(new ArrayList<>(List.of(implStep)), null);
        AgentAppTemplate template = createTemplate(List.of(templateStep), null);

        syncStep.apply(app, template, ctx);

        assertEquals(1, app.getInstallSteps().size());
        InfoStep resultStep = (InfoStep) app.getInstallSteps().get(0);
        assertEquals(sharedId, resultStep.getId());
        assertEquals("Implementation Step", resultStep.getTitle());
        assertEquals("Custom implementation message", resultStep.getMessage());
    }

    @Test
    void apply_shouldUseTemplateStep_whenNoMatchingIdInImplementation() {
        UUID templateStepId = UUID.randomUUID();
        UUID implStepId = UUID.randomUUID();

        AgentAppStep implStep = createStepWithId(implStepId, null, "Old Implementation Step");
        AgentAppStep templateStep = createStepWithId(templateStepId, null, "New Template Step");

        AgentApplication app = createApplication(new ArrayList<>(List.of(implStep)), null);
        AgentAppTemplate template = createTemplate(List.of(templateStep), null);

        syncStep.apply(app, template, ctx);

        assertEquals(1, app.getInstallSteps().size());
        assertEquals(templateStepId, app.getInstallSteps().get(0).getId());
        assertEquals("New Template Step", app.getInstallSteps().get(0).getTitle());
    }

    @Test
    void apply_shouldPreserveOrderFromTemplate() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        // Implementation has steps (order doesn't matter, matched by ID)
        AgentAppStep implStep2 = createStepWithId(id2, null, "Impl Step 2");
        AgentAppStep implStep1 = createStepWithId(id1, null, "Impl Step 1");

        // Template defines the linked list order: 1 -> 2 -> 3
        AgentAppStep templateStep1 = createStepWithId(id1, id2, "Template Step 1");
        AgentAppStep templateStep2 = createStepWithId(id2, id3, "Template Step 2");
        AgentAppStep templateStep3 = createStepWithId(id3, null, "Template Step 3");

        AgentApplication app = createApplication(new ArrayList<>(List.of(implStep2, implStep1)), null);
        AgentAppTemplate template = createTemplate(List.of(templateStep1, templateStep2, templateStep3), null);

        syncStep.apply(app, template, ctx);

        assertEquals(3, app.getInstallSteps().size());
        // Order should match template's linked list
        assertEquals(id1, app.getInstallSteps().get(0).getId());
        assertEquals(id2, app.getInstallSteps().get(1).getId());
        assertEquals(id3, app.getInstallSteps().get(2).getId());
        // Existing impl steps should preserve their titles
        assertEquals("Impl Step 1", app.getInstallSteps().get(0).getTitle());
        assertEquals("Impl Step 2", app.getInstallSteps().get(1).getTitle());
        // New step from template
        assertEquals("Template Step 3", app.getInstallSteps().get(2).getTitle());
    }

    @Test
    void apply_shouldRemoveStepsNotInTemplate() {
        UUID keepId = UUID.randomUUID();
        UUID removeId = UUID.randomUUID();

        AgentAppStep implStepToKeep = createStepWithId(keepId, null, "Keep This");
        AgentAppStep implStepToRemove = createStepWithId(removeId, null, "Remove This");

        AgentAppStep templateStep = createStepWithId(keepId, null, "Template Step");

        AgentApplication app = createApplication(new ArrayList<>(List.of(implStepToKeep, implStepToRemove)), null);
        AgentAppTemplate template = createTemplate(List.of(templateStep), null);

        syncStep.apply(app, template, ctx);

        assertEquals(1, app.getInstallSteps().size());
        assertEquals(keepId, app.getInstallSteps().get(0).getId());
    }

    // ==================== apply() - update steps ====================

    @Test
    void apply_shouldSyncUpdateStepsSeparately() {
        UUID installId = UUID.randomUUID();
        UUID updateId = UUID.randomUUID();

        AgentAppStep implInstallStep = createStepWithId(installId, null, "Impl Install");
        AgentAppStep implUpdateStep = createStepWithId(updateId, null, "Impl Update");

        AgentAppStep templateInstallStep = createStepWithId(installId, null, "Template Install");
        AgentAppStep templateUpgradeStep = createStepWithId(updateId, null, "Template Upgrade");

        AgentApplication app = createApplication(
                new ArrayList<>(List.of(implInstallStep)),
                new ArrayList<>(List.of(implUpdateStep))
        );
        AgentAppTemplate template = createTemplate(
                List.of(templateInstallStep),
                List.of(templateUpgradeStep)
        );

        syncStep.apply(app, template, ctx);

        assertEquals(1, app.getInstallSteps().size());
        assertEquals("Impl Install", app.getInstallSteps().get(0).getTitle());

        assertEquals(1, app.getUpdateSteps().size());
        assertEquals("Impl Update", app.getUpdateSteps().get(0).getTitle());
    }

    // ==================== apply() - nextId handling ====================

    @Test
    void apply_shouldUpdateNextIdFromTemplate() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        // Impl steps have no nextId set
        InfoStep implStep1 = createStepWithId(id1, null, "Impl Step 1");
        implStep1.setMessage("Custom message 1");
        InfoStep implStep2 = createStepWithId(id2, null, "Impl Step 2");
        implStep2.setMessage("Custom message 2");

        // Template defines the linked list order: 1 -> 2 -> null
        InfoStep templateStep1 = createStepWithId(id1, id2, "Template Step 1");
        InfoStep templateStep2 = createStepWithId(id2, null, "Template Step 2");

        AgentApplication app = createApplication(new ArrayList<>(List.of(implStep1, implStep2)), null);
        AgentAppTemplate template = createTemplate(List.of(templateStep1, templateStep2), null);

        syncStep.apply(app, template, ctx);

        // Verify nextId is updated from template
        assertEquals(id2, app.getInstallSteps().get(0).getNextId());
        assertNull(app.getInstallSteps().get(1).getNextId());
        // Verify impl content is preserved
        assertEquals("Custom message 1", ((InfoStep) app.getInstallSteps().get(0)).getMessage());
        assertEquals("Custom message 2", ((InfoStep) app.getInstallSteps().get(1)).getMessage());
    }

    @Test
    void apply_shouldInsertNewStepBetweenExisting() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID(); // New step
        UUID id3 = UUID.randomUUID();

        // Impl has steps 1 and 3
        InfoStep implStep1 = createStepWithId(id1, id3, "Impl Step 1");
        InfoStep implStep3 = createStepWithId(id3, null, "Impl Step 3");

        // Template inserts step 2 between 1 and 3: 1 -> 2 -> 3
        InfoStep templateStep1 = createStepWithId(id1, id2, "Template Step 1");
        InfoStep templateStep2 = createStepWithId(id2, id3, "New Template Step 2");
        InfoStep templateStep3 = createStepWithId(id3, null, "Template Step 3");

        AgentApplication app = createApplication(new ArrayList<>(List.of(implStep1, implStep3)), null);
        AgentAppTemplate template = createTemplate(List.of(templateStep1, templateStep2, templateStep3), null);

        syncStep.apply(app, template, ctx);

        assertEquals(3, app.getInstallSteps().size());
        // Order follows template
        assertEquals(id1, app.getInstallSteps().get(0).getId());
        assertEquals(id2, app.getInstallSteps().get(1).getId());
        assertEquals(id3, app.getInstallSteps().get(2).getId());
        // Impl steps preserve their titles, new step uses template title
        assertEquals("Impl Step 1", app.getInstallSteps().get(0).getTitle());
        assertEquals("New Template Step 2", app.getInstallSteps().get(1).getTitle());
        assertEquals("Impl Step 3", app.getInstallSteps().get(2).getTitle());
    }

    @Test
    void apply_shouldReorderStepsBasedOnTemplateNextId() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        // Impl has order: 1 -> 2 -> 3
        InfoStep implStep1 = createStepWithId(id1, id2, "Impl Step 1");
        InfoStep implStep2 = createStepWithId(id2, id3, "Impl Step 2");
        InfoStep implStep3 = createStepWithId(id3, null, "Impl Step 3");

        // Template reorders to: 3 -> 1 -> 2
        InfoStep templateStep3 = createStepWithId(id3, id1, "Template Step 3");
        InfoStep templateStep1 = createStepWithId(id1, id2, "Template Step 1");
        InfoStep templateStep2 = createStepWithId(id2, null, "Template Step 2");

        AgentApplication app = createApplication(
                new ArrayList<>(List.of(implStep1, implStep2, implStep3)), null);
        AgentAppTemplate template = createTemplate(
                List.of(templateStep3, templateStep1, templateStep2), null);

        syncStep.apply(app, template, ctx);

        // Order should follow template's linked list: 3 -> 1 -> 2
        assertEquals(3, app.getInstallSteps().size());
        assertEquals(id3, app.getInstallSteps().get(0).getId());
        assertEquals(id1, app.getInstallSteps().get(1).getId());
        assertEquals(id2, app.getInstallSteps().get(2).getId());
        // Impl titles preserved
        assertEquals("Impl Step 3", app.getInstallSteps().get(0).getTitle());
        assertEquals("Impl Step 1", app.getInstallSteps().get(1).getTitle());
        assertEquals("Impl Step 2", app.getInstallSteps().get(2).getTitle());
    }

    // ==================== apply() - error scenarios ====================

    @Test
    void apply_shouldThrowException_whenStepHasNullId() {
        InfoStep stepWithNullId = new InfoStep();
        stepWithNullId.setTitle("Step without ID");

        AgentApplication app = createApplication(new ArrayList<>(List.of(stepWithNullId)), null);
        AgentAppTemplate template = createTemplate(List.of(createStep("template")), null);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> syncStep.apply(app, template, ctx));

        assertTrue(exception.getMessage().contains("null id"));
    }

    @Test
    void apply_shouldThrowException_whenTemplateStepHasNullId() {
        InfoStep templateStepWithNullId = new InfoStep();
        templateStepWithNullId.setTitle("Template step without ID");

        AgentApplication app = createApplication(new ArrayList<>(List.of(createStep("impl"))), null);
        AgentAppTemplate template = createTemplate(List.of(templateStepWithNullId), null);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> syncStep.apply(app, template, ctx));

        assertTrue(exception.getMessage().contains("Orphaned steps detected"));
    }

    // ==================== Helper methods ====================

    private AgentApplication createApplication(List<AgentAppStep> installSteps, List<AgentAppStep> updateSteps) {
        AgentApplication app = new AgentApplication();
        app.setInstallSteps(installSteps);
        app.setUpdateSteps(updateSteps);
        return app;
    }

    private AgentAppTemplate createTemplate(List<AgentAppStep> installSteps, List<AgentAppStep> upgradeSteps) {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setInstallSteps(installSteps);
        template.setUpgradeSteps(upgradeSteps);
        return template;
    }

    private InfoStep createStep(String title) {
        UUID id = UUID.randomUUID();
        return createStepWithId(id, null, title);
    }

    private InfoStep createStepWithId(UUID id, String title) {
        return createStepWithId(id, null, title);
    }

    private InfoStep createStepWithId(UUID id, UUID nextId, String title) {
        return new InfoStep(id, nextId, title, false);
    }
}
