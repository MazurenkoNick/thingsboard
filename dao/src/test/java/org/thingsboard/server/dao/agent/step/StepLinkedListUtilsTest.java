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
import org.thingsboard.server.common.data.agent.step.InfoStep;
import org.thingsboard.server.dao.agent.StepLinkedListUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StepLinkedListUtilsTest {

    // ==================== findFirstStepId() tests ====================

    @Test
    void findFirstStepId_shouldReturnNull_whenListIsEmpty() {
        assertNull(StepLinkedListUtils.findFirstStepId(Collections.emptyList()));
        assertNull(StepLinkedListUtils.findFirstStepId(null));
    }

    @Test
    void findFirstStepId_shouldReturnOnlyStep_whenSingleStep() {
        UUID stepId = UUID.randomUUID();
        InfoStep step = createStep(stepId, null, "Only Step");

        UUID firstId = StepLinkedListUtils.findFirstStepId(List.of(step));

        assertEquals(stepId, firstId);
    }

    @Test
    void findFirstStepId_shouldFindFirstStep_inChain() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        // Chain: 1 -> 2 -> 3
        InfoStep step1 = createStep(id1, id2, "First");
        InfoStep step2 = createStep(id2, id3, "Second");
        InfoStep step3 = createStep(id3, null, "Third");

        // Provide in random order
        UUID firstId = StepLinkedListUtils.findFirstStepId(List.of(step3, step1, step2));

        assertEquals(id1, firstId);
    }

    @Test
    void findFirstStepId_shouldThrow_whenCircularReference() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        // Circular: 1 -> 2 -> 1
        InfoStep step1 = createStep(id1, id2, "First");
        InfoStep step2 = createStep(id2, id1, "Second");

        assertThrows(IllegalStateException.class,
                () -> StepLinkedListUtils.findFirstStepId(List.of(step1, step2)));
    }

    @Test
    void findFirstStepId_shouldThrow_whenMultipleFirstSteps() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        // Two separate chains: 1 -> 3, 2 -> (nothing)
        InfoStep step1 = createStep(id1, id3, "First Chain Start");
        InfoStep step2 = createStep(id2, null, "Second Chain Start");
        InfoStep step3 = createStep(id3, null, "First Chain End");

        assertThrows(IllegalStateException.class,
                () -> StepLinkedListUtils.findFirstStepId(List.of(step1, step2, step3)));
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
        InfoStep step = createStep(stepId, null, "Only Step");

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
        InfoStep step1 = createStep(id1, id2, "First");
        InfoStep step2 = createStep(id2, id3, "Second");
        InfoStep step3 = createStep(id3, null, "Third");

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
        InfoStep step1 = createStep(id1, nonExistentId, "First");
        InfoStep step2 = createStep(id2, null, "Orphan");

        assertThrows(IllegalStateException.class,
                () -> StepLinkedListUtils.toOrderedList(List.of(step1, step2)));
    }

    @Test
    void toOrderedList_shouldThrow_whenOrphanedSteps() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        // Chain: 1 -> 2, but 3 is orphaned (not referenced)
        InfoStep step1 = createStep(id1, id2, "First");
        InfoStep step2 = createStep(id2, null, "Second");
        InfoStep step3 = createStep(id3, null, "Orphan");

        assertThrows(IllegalStateException.class,
                () -> StepLinkedListUtils.toOrderedList(List.of(step1, step2, step3)));
    }

    @Test
    void toOrderedList_shouldThrow_whenCircularReference() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        // Circular: 1 -> 2 -> 3 -> 1
        InfoStep step1 = createStep(id1, id2, "First");
        InfoStep step2 = createStep(id2, id3, "Second");
        InfoStep step3 = createStep(id3, id1, "Third");

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

        InfoStep step1 = createStep(id1, id2, "First");
        InfoStep step2 = createStep(id2, null, "Second");

        assertDoesNotThrow(() -> StepLinkedListUtils.validate(List.of(step1, step2)));
    }

    @Test
    void validate_shouldThrow_whenStepHasNullId() {
        InfoStep step = new InfoStep();
        step.setTitle("No ID");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> StepLinkedListUtils.validate(List.of(step)));

        assertTrue(ex.getMessage().contains("null id"));
    }

    @Test
    void validate_shouldThrow_whenNextIdReferencesNonExistent() {
        UUID id1 = UUID.randomUUID();
        UUID nonExistent = UUID.randomUUID();

        InfoStep step = createStep(id1, nonExistent, "Broken");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> StepLinkedListUtils.validate(List.of(step)));

        assertTrue(ex.getMessage().contains("non-existent nextId"));
    }

    // ==================== Helper methods ====================

    private InfoStep createStep(UUID id, UUID nextId, String title) {
        InfoStep step = new InfoStep(id, nextId, title, false);
        return step;
    }
}
