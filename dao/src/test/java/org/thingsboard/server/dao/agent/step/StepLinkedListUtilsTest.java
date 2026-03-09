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
package org.thingsboard.server.dao.agent.step;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.dao.agent.StepLinkedListUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StepLinkedListUtilsTest {

    // ==================== findFirstStep() tests ====================

    @Test
    void findFirstStep_shouldReturnNull_whenListIsEmpty() {
        assertNull(StepLinkedListUtils.findFirstStep(Collections.emptyList()));
        assertNull(StepLinkedListUtils.findFirstStep(null));
    }

    @Test
    void findFirstStep_shouldReturnOnlyStep_whenSingleStep() {
        UUID stepId = UUID.randomUUID();
        ComposeStartStep step = createStep(stepId, null, "Only Step");

        AgentAppStep firstStep = StepLinkedListUtils.findFirstStep(List.of(step));

        assertNotNull(firstStep);
        assertEquals(stepId, firstStep.getId());
    }

    @Test
    void findFirstStep_shouldFindFirstStep_inChain() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        // Chain: 1 -> 2 -> 3
        ComposeStartStep step1 = createStep(id1, id2, "First");
        ComposeStartStep step2 = createStep(id2, id3, "Second");
        ComposeStartStep step3 = createStep(id3, null, "Third");

        // Provide in random order
        AgentAppStep firstStep = StepLinkedListUtils.findFirstStep(List.of(step3, step1, step2));

        assertNotNull(firstStep);
        assertEquals(id1, firstStep.getId());
    }

    @Test
    void findFirstStep_shouldThrow_whenCircularReference() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        // Circular: 1 -> 2 -> 1
        ComposeStartStep step1 = createStep(id1, id2, "First");
        ComposeStartStep step2 = createStep(id2, id1, "Second");

        assertThrows(IllegalStateException.class,
                () -> StepLinkedListUtils.findFirstStep(List.of(step1, step2)));
    }

    @Test
    void findFirstStep_shouldThrow_whenMultipleFirstSteps() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        // Two separate chains: 1 -> 3, 2 -> (nothing)
        ComposeStartStep step1 = createStep(id1, id3, "First Chain Start");
        ComposeStartStep step2 = createStep(id2, null, "Second Chain Start");
        ComposeStartStep step3 = createStep(id3, null, "First Chain End");

        assertThrows(IllegalStateException.class,
                () -> StepLinkedListUtils.findFirstStep(List.of(step1, step2, step3)));
    }

    // ==================== toOrderedList() tests ====================

    @Test
    void toOrderedList_shouldReturnEmptyList_whenInputEmpty() {
        assertEquals(List.of(), StepLinkedListUtils.toOrderedList(Collections.emptyList()));
        assertEquals(List.of(), StepLinkedListUtils.toOrderedList(null));
    }

    @Test
    void toOrderedList_shouldReturnSingleStep() {
        UUID stepId = UUID.randomUUID();
        ComposeStartStep step = createStep(stepId, null, "Only Step");

        List<AgentAppStep> result = StepLinkedListUtils.toOrderedList(List.of(step));

        assertEquals(1, result.size());
        assertEquals(stepId, result.get(0).getId());
    }

    @Test
    void toOrderedList_shouldOrderStepsCorrectly() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        // Chain: 1 -> 2 -> 3
        ComposeStartStep step1 = createStep(id1, id2, "First");
        ComposeStartStep step2 = createStep(id2, id3, "Second");
        ComposeStartStep step3 = createStep(id3, null, "Third");

        // Provide in reverse order
        List<AgentAppStep> result = StepLinkedListUtils.toOrderedList(List.of(step3, step2, step1));

        assertEquals(3, result.size());
        assertEquals(id1, result.get(0).getId());
        assertEquals(id2, result.get(1).getId());
        assertEquals(id3, result.get(2).getId());
    }

    @Test
    void toOrderedList_shouldThrow_whenBrokenChain() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID nonExistentId = UUID.randomUUID();

        // Chain: 1 -> nonExistent (broken)
        ComposeStartStep step1 = createStep(id1, nonExistentId, "First");
        ComposeStartStep step2 = createStep(id2, null, "Orphan");

        assertThrows(IllegalStateException.class,
                () -> StepLinkedListUtils.toOrderedList(List.of(step1, step2)));
    }

    @Test
    void toOrderedList_shouldThrow_whenOrphanedSteps() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        // Chain: 1 -> 2, but 3 is orphaned (not referenced)
        ComposeStartStep step1 = createStep(id1, id2, "First");
        ComposeStartStep step2 = createStep(id2, null, "Second");
        ComposeStartStep step3 = createStep(id3, null, "Orphan");

        assertThrows(IllegalStateException.class,
                () -> StepLinkedListUtils.toOrderedList(List.of(step1, step2, step3)));
    }

    @Test
    void toOrderedList_shouldThrow_whenCircularReference() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        // Circular: 1 -> 2 -> 3 -> 1
        ComposeStartStep step1 = createStep(id1, id2, "First");
        ComposeStartStep step2 = createStep(id2, id3, "Second");
        ComposeStartStep step3 = createStep(id3, id1, "Third");

        assertThrows(IllegalStateException.class,
                () -> StepLinkedListUtils.toOrderedList(List.of(step1, step2, step3)));
    }

    // ==================== validate() tests ====================

    @Test
    void validate_shouldPassForEmptyList() {
        assertDoesNotThrow(() -> StepLinkedListUtils.validate(Collections.emptyList()));
        assertDoesNotThrow(() -> StepLinkedListUtils.validate(null));
    }

    @Test
    void validate_shouldPassForValidChain() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        ComposeStartStep step1 = createStep(id1, id2, "First");
        ComposeStartStep step2 = createStep(id2, null, "Second");

        assertDoesNotThrow(() -> StepLinkedListUtils.validate(List.of(step1, step2)));
    }

    @Test
    void validate_shouldThrow_whenStepHasNullId() {
        ComposeStartStep step = new ComposeStartStep();
        step.setTitle("No ID");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> StepLinkedListUtils.validate(List.of(step)));

        assertTrue(ex.getMessage().contains("null id"));
    }

    @Test
    void validate_shouldThrow_whenNextIdReferencesNonExistent() {
        UUID id1 = UUID.randomUUID();
        UUID nonExistent = UUID.randomUUID();

        ComposeStartStep step = createStep(id1, nonExistent, "Broken");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> StepLinkedListUtils.validate(List.of(step)));

        assertTrue(ex.getMessage().contains("non-existent nextId"));
    }

    // ==================== getNextStep() tests ====================

    @Test
    void getNextStep_shouldReturnEmpty_whenCurrentStepIdIsNull() {
        UUID id1 = UUID.randomUUID();
        ComposeStartStep step = createStep(id1, null, "Step");
        assertTrue(StepLinkedListUtils.getNextStep(null, List.of(step)).isEmpty());
    }

    @Test
    void getNextStep_shouldReturnEmpty_whenStepsIsEmpty() {
        assertTrue(StepLinkedListUtils.getNextStep(UUID.randomUUID(), Collections.emptyList()).isEmpty());
        assertTrue(StepLinkedListUtils.getNextStep(UUID.randomUUID(), null).isEmpty());
    }

    @Test
    void getNextStep_shouldReturnEmpty_whenCurrentStepIsLast() {
        UUID id1 = UUID.randomUUID();
        ComposeStartStep step = createStep(id1, null, "Last");
        assertTrue(StepLinkedListUtils.getNextStep(id1, List.of(step)).isEmpty());
    }

    @Test
    void getNextStep_shouldReturnNextStep_inChain() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        ComposeStartStep step1 = createStep(id1, id2, "First");
        ComposeStartStep step2 = createStep(id2, id3, "Second");
        ComposeStartStep step3 = createStep(id3, null, "Third");

        List<AgentAppStep> steps = List.of(step1, step2, step3);

        AgentAppStep next = StepLinkedListUtils.getNextStep(id1, steps).orElse(null);
        assertNotNull(next);
        assertEquals(id2, next.getId());

        next = StepLinkedListUtils.getNextStep(id2, steps).orElse(null);
        assertNotNull(next);
        assertEquals(id3, next.getId());

        assertTrue(StepLinkedListUtils.getNextStep(id3, steps).isEmpty());
    }

    @Test
    void getNextStep_shouldReturnEmpty_whenCurrentStepIdNotFound() {
        UUID id1 = UUID.randomUUID();
        ComposeStartStep step = createStep(id1, null, "Step");
        assertTrue(StepLinkedListUtils.getNextStep(UUID.randomUUID(), List.of(step)).isEmpty());
    }

    // ==================== Helper methods ====================

    private ComposeStartStep createStep(UUID id, UUID nextId, String title) {
        ComposeStartStep step = new ComposeStartStep();
        step.setId(id);
        step.setNextId(nextId);
        step.setTitle(title);
        return step;
    }
}
