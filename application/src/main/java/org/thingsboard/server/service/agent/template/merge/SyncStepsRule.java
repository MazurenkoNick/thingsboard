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

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.template.TemplateMergeCtx;
import org.thingsboard.server.dao.agent.StepLinkedListUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Merge rule that synchronizes agent application steps with template steps.
 * <p>
 * This rule preserves user customizations made to individual steps while adopting
 * the ordering and structure defined by the template. Steps are matched by their UUID,
 * and the template's linked-list ordering (via {@code nextId}) takes precedence.
 * <p>
 * Merge behavior:
 * <ul>
 *   <li>Steps present in both impl and template: impl content is preserved, {@code nextId} is updated from template</li>
 *   <li>Steps only in template: added as-is (new steps from template update)</li>
 *   <li>Steps only in impl: dropped (removed by template update)</li>
 *   <li>Empty template steps: results in empty list (all steps removed)</li>
 * </ul>
 *
 * @see AppTemplateMergeRule
 * @see StepLinkedListUtils
 */
@Component
public class SyncStepsRule implements AppTemplateMergeRule {

    @Override
    public boolean supports(AgentApplication agentApplication, AgentAppTemplate template, TemplateMergeCtx ctx) {
        boolean hasStartSteps = !CollectionUtils.isEmpty(agentApplication.getStartSteps())
                || !CollectionUtils.isEmpty(template.getStartSteps());
        boolean hasUpgradeSteps = !CollectionUtils.isEmpty(agentApplication.getUpgradeSteps())
                || !CollectionUtils.isEmpty(template.getUpgradeSteps());
        return hasStartSteps || hasUpgradeSteps;
    }

    @Override
    public void apply(AgentApplication agentApplication, AgentAppTemplate template, TemplateMergeCtx ctx) {
        List<AgentAppStep> syncedStartSteps = syncSteps(agentApplication.getStartSteps(), template.getStartSteps());
        List<AgentAppStep> syncedUpgradeSteps = syncSteps(agentApplication.getUpgradeSteps(), template.getUpgradeSteps());

        agentApplication.setStartSteps(syncedStartSteps);
        agentApplication.setUpgradeSteps(syncedUpgradeSteps);
    }

    private List<AgentAppStep> syncSteps(List<AgentAppStep> implSteps, List<AgentAppStep> templateSteps) {
        if (CollectionUtils.isEmpty(templateSteps)) {
            return List.of();
        }
        if (CollectionUtils.isEmpty(implSteps)) {
            return templateSteps;
        }

        Map<UUID, AgentAppStep> implStepsById = toMap(implSteps);

        List<AgentAppStep> orderedTemplateSteps = StepLinkedListUtils.toOrderedList(templateSteps);

        return orderedTemplateSteps.stream()
                .map(templateStep -> {
                    AgentAppStep existing = implStepsById.get(templateStep.getId());
                    if (existing != null) {
                        // Preserve impl step content but update nextId from template
                        existing.setNextId(templateStep.getNextId());
                        return existing;
                    }
                    return templateStep;
                })
                .toList();
    }

    private Map<UUID, AgentAppStep> toMap(List<AgentAppStep> steps) {
        return steps.stream()
                .peek(s -> {
                    if (s.getId() == null) {
                        throw new IllegalStateException("Step has null id: " + s.getTitle());
                    }
                })
                .collect(Collectors.toMap(
                        AgentAppStep::getId,
                        Function.identity(),
                        (a, b) -> b
                ));
    }
}
