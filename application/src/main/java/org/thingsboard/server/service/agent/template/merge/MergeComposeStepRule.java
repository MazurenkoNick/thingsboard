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
package org.thingsboard.server.service.agent.template.merge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.AgentAppStepType;
import org.thingsboard.server.common.data.agent.step.ComposeStep;
import org.thingsboard.server.common.data.agent.step.ComposeTypeChoiceStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.template.TemplateMergeCtx;
import org.thingsboard.server.dao.agent.StepLinkedListUtils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

@Component
@Slf4j
public class MergeComposeStepRule implements AppTemplateMergeRule {

    @Override
    public boolean supports(AgentApplication agentApp, AgentAppTemplate template, TemplateMergeCtx ctx) {
        return hasSelectedComposeType(ctx) && agentApp.getConfig() instanceof DockerComposeConfig;
    }

    private boolean hasSelectedComposeType(TemplateMergeCtx ctx) {
        return ctx != null && ctx.hasSelectedComposeType();
    }

    @Override
    public void apply(AgentApplication agentApp, AgentAppTemplate template, TemplateMergeCtx ctx) {
        Optional<ComposeTypeChoiceStep> choiceStep = StepLinkedListUtils.getByType(
                AgentAppStepType.COMPOSE_TYPE_CHOICE, ComposeTypeChoiceStep.class, template.getInstallSteps()
        );
        Optional<ComposeStep> composeStep = StepLinkedListUtils.getByType(
                AgentAppStepType.COMPOSE, ComposeStep.class, agentApp.getInstallSteps()
        );
        if (choiceStep.isEmpty() || composeStep.isEmpty()) {
            log.trace("Compose Choice Step from template or Compose Step from app couldn't be found: choice: {} compose: {}", choiceStep, composeStep);
            return;
        }
        mergeComposeByType(ctx, choiceStep.get(), agentApp);
    }

    private void mergeComposeByType(TemplateMergeCtx ctx, ComposeTypeChoiceStep choiceStep, AgentApplication agentApp) {
        String selectedComposeType = ctx.getSelectedComposeType();
        JsonNode composeTemplate = getComposeTemplateByType(choiceStep, selectedComposeType);
        AgentAppConfig appConfig = agentApp.getConfig();

        if (!(appConfig instanceof DockerComposeConfig appDockerConfig)) {
            throw new IllegalStateException("Can't set compose to the non-compose configuration " + appConfig);
        }
        JsonNode appCompose = appDockerConfig.getCompose();

        if (appCompose == null || appCompose.isNull() || !appCompose.isObject()) {
            appDockerConfig.setCompose(composeTemplate.deepCopy());
            return;
        }
        deepMerge((ObjectNode) appCompose, composeTemplate);
    }

    /**
     * Recursively syncs the structure of {@code appNode} to match {@code templateNode}:
     * <ul>
     *   <li>Keys in template but not in app → added (deep-copied from template)</li>
     *   <li>Keys in app but not in template → removed</li>
     *   <li>Keys in both, both objects → recurse</li>
     *   <li>Keys in both, different or non-object types → app value preserved</li>
     * </ul>
     */
    private void deepMerge(ObjectNode appNode, JsonNode templateNode) {
        Set<String> templateKeys = new HashSet<>();
        templateNode.fieldNames().forEachRemaining(templateKeys::add);

        // Add missing keys from template; recurse into shared object keys
        for (String key : templateKeys) {
            JsonNode templateValue = templateNode.get(key);
            if (!appNode.has(key)) {
                appNode.set(key, templateValue.deepCopy());
            } else if (appNode.get(key).isObject() && templateValue.isObject()) {
                deepMerge((ObjectNode) appNode.get(key), templateValue);
            }
        }

        // Remove keys from app that are not in template
        Iterator<String> appKeys = appNode.fieldNames();
        while (appKeys.hasNext()) {
            if (!templateKeys.contains(appKeys.next())) {
                appKeys.remove();
            }
        }
    }

    private JsonNode getComposeTemplateByType(ComposeTypeChoiceStep choiceStep, String selectedComposeType) {
        return choiceStep.getTemplateByType(selectedComposeType)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("There's no '%s' compose type in: %s", selectedComposeType, choiceStep.getComposeTypes())
                ));
    }
}

