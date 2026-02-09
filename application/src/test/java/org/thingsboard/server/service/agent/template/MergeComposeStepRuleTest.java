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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.ComposeStep;
import org.thingsboard.server.common.data.agent.step.ComposeTypeChoiceStep;
import org.thingsboard.server.common.data.agent.step.InfoStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.template.TemplateMergeCtx;
import org.thingsboard.server.service.agent.template.merge.MergeComposeStepRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MergeComposeStepRuleTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MergeComposeStepRule rule;

    @BeforeEach
    void setUp() {
        rule = new MergeComposeStepRule();
    }

    // ==================== supports() tests ====================

    @Test
    void supports_shouldReturnFalse_whenCtxIsNull() {
        AgentApplication app = new AgentApplication();
        AgentAppTemplate template = new AgentAppTemplate();

        assertFalse(rule.supports(app, template, null));
    }

    @Test
    void supports_shouldReturnFalse_whenSelectedComposeTypeIsNull() {
        TemplateMergeCtx ctx = createCtx(null);

        assertFalse(rule.supports(new AgentApplication(), new AgentAppTemplate(), ctx));
    }

    @Test
    void supports_shouldReturnFalse_whenSelectedComposeTypeIsEmpty() {
        TemplateMergeCtx ctx = createCtx("");

        assertFalse(rule.supports(new AgentApplication(), new AgentAppTemplate(), ctx));
    }

    @Test
    void supports_shouldReturnTrue_whenSelectedComposeTypeIsPresent() {
        TemplateMergeCtx ctx = createCtx("monolith");

        assertTrue(rule.supports(new AgentApplication(), new AgentAppTemplate(), ctx));
    }

    // ==================== apply() - skip scenarios ====================

    @Test
    void apply_shouldDoNothing_whenTemplateHasNoComposeTypeChoiceStep() {
        ComposeStep composeStep = createComposeStep(MAPPER.createObjectNode().put("service", "value"));

        AgentApplication app = createApp(List.of(composeStep));
        AgentAppTemplate template = new AgentAppTemplate();
        template.setInstallSteps(List.of(createInfoStep()));
        TemplateMergeCtx ctx = createCtx("monolith");

        rule.apply(app, template, ctx);

        assertEquals("value", composeStep.getCompose().get("service").asText());
    }

    @Test
    void apply_shouldDoNothing_whenAppHasNoComposeStep() {
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of(
                "monolith", MAPPER.createObjectNode().put("tb", "val")
        ));

        AgentApplication app = createApp(List.of(createInfoStep()));
        AgentAppTemplate template = new AgentAppTemplate();
        template.setInstallSteps(List.of(choiceStep));
        TemplateMergeCtx ctx = createCtx("monolith");

        rule.apply(app, template, ctx);

        // no exception, app steps unchanged
        assertEquals(1, app.getInstallSteps().size());
    }

    @Test
    void apply_shouldDoNothing_whenTemplateInstallStepsAreNull() {
        ComposeStep composeStep = createComposeStep(MAPPER.createObjectNode().put("key", "val"));

        AgentApplication app = createApp(List.of(composeStep));
        AgentAppTemplate template = new AgentAppTemplate();
        template.setInstallSteps(null);
        TemplateMergeCtx ctx = createCtx("monolith");

        rule.apply(app, template, ctx);

        assertEquals("val", composeStep.getCompose().get("key").asText());
    }

    // ==================== apply() - null/empty appCompose ====================

    @Test
    void apply_shouldSetComposeFromTemplate_whenAppComposeIsNull() {
        ObjectNode templateCompose = MAPPER.createObjectNode()
                .put("tb-core", "image:core")
                .put("tb-rule", "image:rule");

        ComposeStep composeStep = createComposeStep(null);
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createApp(List.of(composeStep));
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        TemplateMergeCtx ctx = createCtx("monolith");

        rule.apply(app, template, ctx);

        assertNotNull(composeStep.getCompose());
        assertEquals("image:core", composeStep.getCompose().get("tb-core").asText());
        assertEquals("image:rule", composeStep.getCompose().get("tb-rule").asText());
    }

    @Test
    void apply_shouldSetComposeFromTemplate_whenAppComposeIsNullNode() {
        ObjectNode templateCompose = MAPPER.createObjectNode().put("svc", "img");
        ComposeStep composeStep = createComposeStep(MAPPER.nullNode());
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createApp(List.of(composeStep));
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        TemplateMergeCtx ctx = createCtx("monolith");

        rule.apply(app, template, ctx);

        assertEquals("img", composeStep.getCompose().get("svc").asText());
    }

    // ==================== apply() - deep merge: add keys ====================

    @Test
    void apply_shouldAddTopLevelKeysFromTemplate() {
        ObjectNode appCompose = MAPPER.createObjectNode().put("existing", "value");
        ObjectNode templateCompose = MAPPER.createObjectNode()
                .put("existing", "template-value")
                .put("newKey", "new-value");

        ComposeStep composeStep = createComposeStep(appCompose);
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createApp(List.of(composeStep));
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        TemplateMergeCtx ctx = createCtx("monolith");

        rule.apply(app, template, ctx);

        JsonNode result = composeStep.getCompose();
        assertEquals("value", result.get("existing").asText());
        assertEquals("new-value", result.get("newKey").asText());
    }

    // ==================== apply() - deep merge: remove keys ====================

    @Test
    void apply_shouldRemoveTopLevelKeysNotInTemplate() {
        ObjectNode appCompose = MAPPER.createObjectNode()
                .put("keep", "kept-value")
                .set("obsolete", MAPPER.createObjectNode()
                        .put("k1", "v1"));
        ObjectNode templateCompose = MAPPER.createObjectNode().put("keep", "template-value");

        ComposeStep composeStep = createComposeStep(appCompose);
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createApp(List.of(composeStep));
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        TemplateMergeCtx ctx = createCtx("monolith");

        rule.apply(app, template, ctx);

        JsonNode result = composeStep.getCompose();
        assertEquals("kept-value", result.get("keep").asText());
        assertNull(result.get("obsolete"));
    }

    // ==================== apply() - deep merge: preserve values ====================

    @Test
    void apply_shouldPreserveExistingLeafValues() {
        ObjectNode appCompose = MAPPER.createObjectNode()
                .put("port", 9090)
                .put("host", "custom-host");
        ObjectNode templateCompose = MAPPER.createObjectNode()
                .put("port", 8080)
                .put("host", "default-host");

        ComposeStep composeStep = createComposeStep(appCompose);
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createApp(List.of(composeStep));
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        TemplateMergeCtx ctx = createCtx("monolith");

        rule.apply(app, template, ctx);

        JsonNode result = composeStep.getCompose();
        assertEquals(9090, result.get("port").asInt());
        assertEquals("custom-host", result.get("host").asText());
    }

    // ==================== apply() - deep merge: nested objects ====================

    @Test
    void apply_shouldRecurseIntoNestedObjects() {
        ObjectNode appNested = MAPPER.createObjectNode().put("existingProp", "custom");
        ObjectNode appCompose = MAPPER.createObjectNode();
        appCompose.set("service", appNested);

        ObjectNode templateNested = MAPPER.createObjectNode()
                .put("existingProp", "default")
                .put("newProp", "added");
        ObjectNode templateCompose = MAPPER.createObjectNode();
        templateCompose.set("service", templateNested);

        ComposeStep composeStep = createComposeStep(appCompose);
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createApp(List.of(composeStep));
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        TemplateMergeCtx ctx = createCtx("monolith");

        rule.apply(app, template, ctx);

        JsonNode resultService = composeStep.getCompose().get("service");
        assertEquals("custom", resultService.get("existingProp").asText());
        assertEquals("added", resultService.get("newProp").asText());
    }

    @Test
    void apply_shouldRemoveNestedKeysNotInTemplate() {
        ObjectNode appNested = MAPPER.createObjectNode()
                .put("keep", "val")
                .put("remove", "gone");
        ObjectNode appCompose = MAPPER.createObjectNode();
        appCompose.set("service", appNested);

        ObjectNode templateNested = MAPPER.createObjectNode().put("keep", "default");
        ObjectNode templateCompose = MAPPER.createObjectNode();
        templateCompose.set("service", templateNested);

        ComposeStep composeStep = createComposeStep(appCompose);
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createApp(List.of(composeStep));
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        TemplateMergeCtx ctx = createCtx("monolith");

        rule.apply(app, template, ctx);

        JsonNode resultService = composeStep.getCompose().get("service");
        assertEquals("val", resultService.get("keep").asText());
        assertNull(resultService.get("remove"));
    }

    @Test
    void apply_shouldRecurseDeeplyIntoMultipleLevels() {
        // app: { a: { b: { existing: "custom", obsolete: "drop" } } }
        ObjectNode appLevel3 = MAPPER.createObjectNode()
                .put("existing", "custom")
                .put("obsolete", "drop");
        ObjectNode appLevel2 = MAPPER.createObjectNode();
        appLevel2.set("b", appLevel3);
        ObjectNode appCompose = MAPPER.createObjectNode();
        appCompose.set("a", appLevel2);

        // template: { a: { b: { existing: "default", added: "new" } } }
        ObjectNode tplLevel3 = MAPPER.createObjectNode()
                .put("existing", "default")
                .put("added", "new");
        ObjectNode tplLevel2 = MAPPER.createObjectNode();
        tplLevel2.set("b", tplLevel3);
        ObjectNode templateCompose = MAPPER.createObjectNode();
        templateCompose.set("a", tplLevel2);

        ComposeStep composeStep = createComposeStep(appCompose);
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createApp(List.of(composeStep));
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        TemplateMergeCtx ctx = createCtx("monolith");

        rule.apply(app, template, ctx);

        JsonNode result = composeStep.getCompose().get("a").get("b");
        assertEquals("custom", result.get("existing").asText());
        assertEquals("new", result.get("added").asText());
        assertNull(result.get("obsolete"));
    }

    // ==================== apply() - deep merge: type mismatches ====================

    @Test
    void apply_shouldPreserveAppValue_whenTypesDoNotMatch() {
        // app has a leaf, template has an object at the same key
        ObjectNode appCompose = MAPPER.createObjectNode().put("config", "flat-string");
        ObjectNode templateNested = MAPPER.createObjectNode().put("nested", "value");
        ObjectNode templateCompose = MAPPER.createObjectNode();
        templateCompose.set("config", templateNested);

        ComposeStep composeStep = createComposeStep(appCompose);
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createApp(List.of(composeStep));
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        TemplateMergeCtx ctx = createCtx("monolith");

        rule.apply(app, template, ctx);

        assertEquals("flat-string", composeStep.getCompose().get("config").asText());
    }

    @Test
    void apply_shouldPreserveAppObject_whenTemplateHasLeaf() {
        // app has an object, template has a leaf at the same key
        ObjectNode appNested = MAPPER.createObjectNode().put("inner", "val");
        ObjectNode appCompose = MAPPER.createObjectNode();
        appCompose.set("config", appNested);

        ObjectNode templateCompose = MAPPER.createObjectNode().put("config", "flat");

        ComposeStep composeStep = createComposeStep(appCompose);
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createApp(List.of(composeStep));
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        TemplateMergeCtx ctx = createCtx("monolith");

        rule.apply(app, template, ctx);

        assertTrue(composeStep.getCompose().get("config").isObject());
        assertEquals("val", composeStep.getCompose().get("config").get("inner").asText());
    }

    // ==================== apply() - deep merge: deep copy isolation ====================

    @Test
    void apply_shouldDeepCopyAddedKeys_soTemplateIsNotMutated() {
        ObjectNode templateNested = MAPPER.createObjectNode().put("prop", "original");
        ObjectNode templateCompose = MAPPER.createObjectNode();
        templateCompose.set("newService", templateNested);

        ObjectNode appCompose = MAPPER.createObjectNode();
        ComposeStep composeStep = createComposeStep(appCompose);
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createApp(List.of(composeStep));
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        TemplateMergeCtx ctx = createCtx("monolith");

        rule.apply(app, template, ctx);

        // Mutate the result
        ((ObjectNode) composeStep.getCompose().get("newService")).put("prop", "mutated");

        // Template should be unaffected
        assertEquals("original", templateNested.get("prop").asText());
    }

    @Test
    void apply_shouldDeepCopyFullTemplate_whenAppComposeIsNull() {
        ObjectNode templateCompose = MAPPER.createObjectNode().put("key", "original");

        ComposeStep composeStep = createComposeStep(null);
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createApp(List.of(composeStep));
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        TemplateMergeCtx ctx = createCtx("monolith");

        rule.apply(app, template, ctx);

        ((ObjectNode) composeStep.getCompose()).put("key", "mutated");
        assertEquals("original", templateCompose.get("key").asText());
    }

    // ==================== apply() - error scenarios ====================

    @Test
    void apply_shouldThrow_whenSelectedComposeTypeNotInTemplate() {
        ObjectNode templateCompose = MAPPER.createObjectNode().put("svc", "val");
        ComposeStep composeStep = createComposeStep(MAPPER.createObjectNode());
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createApp(List.of(composeStep));
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        TemplateMergeCtx ctx = createCtx("nonexistent-type");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> rule.apply(app, template, ctx));

        assertTrue(ex.getMessage().contains("nonexistent-type"));
    }

    // ==================== apply() - combined add and remove ====================

    @Test
    void apply_shouldAddAndRemoveKeysInSingleMerge() {
        ObjectNode appCompose = MAPPER.createObjectNode()
                .put("keep", "app-val")
                .put("remove1", "gone1")
                .put("remove2", "gone2");
        ObjectNode templateCompose = MAPPER.createObjectNode()
                .put("keep", "tpl-val")
                .put("add1", "new1")
                .put("add2", "new2");

        ComposeStep composeStep = createComposeStep(appCompose);
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createApp(List.of(composeStep));
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        TemplateMergeCtx ctx = createCtx("monolith");

        rule.apply(app, template, ctx);

        JsonNode result = composeStep.getCompose();
        assertEquals(3, result.size());
        assertEquals("app-val", result.get("keep").asText());
        assertEquals("new1", result.get("add1").asText());
        assertEquals("new2", result.get("add2").asText());
        assertNull(result.get("remove1"));
        assertNull(result.get("remove2"));
    }

    // ==================== apply() - compose type selection ====================

    @Test
    void apply_shouldSelectCorrectComposeTypeFromChoiceStep() {
        ObjectNode monolithCompose = MAPPER.createObjectNode().put("all-in-one", "img");
        ObjectNode microservicesCompose = MAPPER.createObjectNode()
                .put("core", "core-img")
                .put("rule", "rule-img");

        ComposeStep composeStep = createComposeStep(null);
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of(
                "monolith", monolithCompose,
                "microservices", microservicesCompose
        ));

        AgentApplication app = createApp(List.of(composeStep));
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        TemplateMergeCtx ctx = createCtx("microservices");

        rule.apply(app, template, ctx);

        JsonNode result = composeStep.getCompose();
        assertEquals("core-img", result.get("core").asText());
        assertEquals("rule-img", result.get("rule").asText());
        assertNull(result.get("all-in-one"));
    }

    // ==================== Helper methods ====================

    private TemplateMergeCtx createCtx(String selectedComposeType) {
        return TemplateMergeCtx.builder().selectedComposeType(selectedComposeType).build();
    }

    private ComposeStep createComposeStep(JsonNode compose) {
        ComposeStep step = new ComposeStep();
        step.setId(UUID.randomUUID());
        step.setCompose(compose);
        return step;
    }

    private ComposeTypeChoiceStep createChoiceStep(Map<String, ? extends JsonNode> templates) {
        ComposeTypeChoiceStep step = new ComposeTypeChoiceStep(UUID.randomUUID(), null, "Choose type");
        step.setComposeTemplates(Map.copyOf(templates));
        return step;
    }

    private InfoStep createInfoStep() {
        InfoStep step = new InfoStep();
        step.setId(UUID.randomUUID());
        step.setTitle("Info");
        return step;
    }

    private AgentApplication createApp(List<AgentAppStep> installSteps) {
        AgentApplication app = new AgentApplication();
        app.setInstallSteps(new ArrayList<>(installSteps));
        return app;
    }

    private AgentAppTemplate createTemplate(List<AgentAppStep> installSteps) {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setInstallSteps(new ArrayList<>(installSteps));
        return template;
    }
}
