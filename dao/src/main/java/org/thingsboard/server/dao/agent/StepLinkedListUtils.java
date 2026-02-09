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
package org.thingsboard.server.dao.agent;

import lombok.experimental.UtilityClass;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.AgentAppStepType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@UtilityClass
public class StepLinkedListUtils {

    /**
     * Find the first step ID (the step not referenced by any other step's nextId).
     *
     * @param steps list of steps
     * @return UUID of the first step, or null if list is empty
     * @throws IllegalStateException if no first step found or multiple first steps exist
     */
    public UUID findFirstStepId(List<AgentAppStep> steps) {
        if (CollectionUtils.isEmpty(steps)) {
            return null;
        }

        Set<UUID> allIds = steps.stream()
                .map(AgentAppStep::getId)
                .collect(Collectors.toSet());

        Set<UUID> referencedIds = steps.stream()
                .map(AgentAppStep::getNextId)
                .filter(nextId -> nextId != null)
                .collect(Collectors.toSet());

        Set<UUID> firstStepCandidates = new HashSet<>(allIds);
        firstStepCandidates.removeAll(referencedIds);

        if (firstStepCandidates.isEmpty()) {
            throw new IllegalStateException("No first step found - possible circular reference");
        }
        if (firstStepCandidates.size() > 1) {
            throw new IllegalStateException("Multiple first steps found: " + firstStepCandidates);
        }

        return firstStepCandidates.iterator().next();
    }

    /**
     * Convert an unordered list of steps to an ordered list following the nextId chain.
     *
     * @param steps list of steps with nextId references
     * @return ordered list of steps
     * @throws IllegalStateException if chain is broken or has cycles
     */
    public List<AgentAppStep> toOrderedList(List<AgentAppStep> steps) {
        if (CollectionUtils.isEmpty(steps)) {
            return List.of();
        }

        Map<UUID, AgentAppStep> stepsById = steps.stream()
                .collect(Collectors.toMap(AgentAppStep::getId, Function.identity()));

        UUID firstId = findFirstStepId(steps);
        List<AgentAppStep> ordered = new ArrayList<>(steps.size());
        Set<UUID> visited = new HashSet<>();

        UUID currentId = firstId;
        while (currentId != null) {
            if (visited.contains(currentId)) {
                throw new IllegalStateException("Circular reference detected at step: " + currentId);
            }

            AgentAppStep step = stepsById.get(currentId);
            if (step == null) {
                throw new IllegalStateException("Broken chain - step not found: " + currentId);
            }

            visited.add(currentId);
            ordered.add(step);
            currentId = step.getNextId();
        }

        if (ordered.size() != steps.size()) {
            throw new IllegalStateException("Orphaned steps detected. Expected " + steps.size() +
                    " steps but chain contains " + ordered.size());
        }

        return ordered;
    }

    /**
     * Validate the step list for cycles and orphans.
     *
     * @param steps list of steps to validate
     * @throws IllegalStateException if validation fails
     */
    public void validate(List<AgentAppStep> steps) {
        if (CollectionUtils.isEmpty(steps)) {
            return;
        }

        // Validate all steps have IDs
        for (AgentAppStep step : steps) {
            if (step.getId() == null) {
                throw new IllegalStateException("Step has null id: " + step.getTitle());
            }
        }

        // Validate nextId references exist
        Set<UUID> allIds = steps.stream()
                .map(AgentAppStep::getId)
                .collect(Collectors.toSet());

        for (AgentAppStep step : steps) {
            if (step.getNextId() != null && !allIds.contains(step.getNextId())) {
                throw new IllegalStateException("Step '" + step.getTitle() +
                        "' references non-existent nextId: " + step.getNextId());
            }
        }

        // This will throw if there are cycles or orphans
        toOrderedList(steps);
    }

    public static <T extends AgentAppStep> Optional<T> getByType(AgentAppStepType agentAppStepType,
                                                              Class<T> stepCls,
                                                              List<AgentAppStep> steps) {
        if (CollectionUtils.isEmpty(steps)) {
            return Optional.empty();
        }
        return steps.stream()
                .filter(stepCls::isInstance)
                .map(stepCls::cast)
                .filter(s -> s.getType() == agentAppStepType)
                .findFirst();
    }
}
