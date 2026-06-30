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
package org.thingsboard.server.common.data.agent.step.state;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.id.AgentAppEventId;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AgentAppStepStateMergeTest {

    @Test
    void overlayValueWinsWhenPresent() {
        ComposeStepState template = compose(true, false);
        ComposeStepState overlay = compose(false, true);
        assertThat(template.getCommandMetadata(overlay)).containsEntry("pullImages", "false");
    }

    @Test
    void fallsBackToTemplateWhenNoOverlay() {
        ComposeStepState template = compose(true, false);
        assertThat(template.getCommandMetadata(null)).containsEntry("pullImages", "true");
    }

    @Test
    void fallsBackToTemplateWhenOverlayOmitsFieldValue() {
        ComposeStepState template = compose(true, true);
        ComposeStepState overlay = new ComposeStepState();
        overlay.setPullImages(new StepField<>(null, true)); // present but no value
        assertThat(template.getCommandMetadata(overlay)).containsEntry("pullImages", "true");
    }

    @Test
    void rollbackTakesServerInjectedValueOverNullTemplate() {
        RollBackStepState template = new RollBackStepState();
        template.setFailedEventId(new StepField<>(null, false)); // template placeholder, userChoice:false
        AgentAppEventId injected = new AgentAppEventId(UUID.randomUUID());
        RollBackStepState overlay = new RollBackStepState(injected);

        assertThat(template.getCommandMetadata(overlay))
                .containsEntry("failedCommandId", injected.getId().toString());
        assertThat(template.getCommandMetadata(null)).isEmpty();
    }

    @Test
    void backupVolumesMergePerField() {
        BackupVolumesStepState template = new BackupVolumesStepState();
        template.setBackupVolumes(new StepField<>(List.of(), true));
        BackupVolumesStepState overlay = new BackupVolumesStepState();
        overlay.setBackupVolumes(new StepField<>(List.of("a", "b"), true));

        assertThat(template.getCommandMetadata(overlay)).containsEntry("backupVolumes", "a,b");
        assertThat(template.getCommandMetadata(null)).containsEntry("backupVolumes", "");
    }

    private static ComposeStepState compose(boolean value, boolean userChoice) {
        ComposeStepState state = new ComposeStepState();
        state.setPullImages(new StepField<>(value, userChoice));
        return state;
    }
}
