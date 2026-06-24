///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
///

import { AgentApplication, AgentAppStep } from '@shared/models/agent.models';
import {
  buildBackupVolumeInput,
  buildComposeDownInput,
  buildPullImagesInput,
  ClassifiedStep,
  extractComposeVolumeKeys,
  readInitialPullImages,
  StepInputKind
} from '@home/pages/agent/util/agent-app-steps';

export interface VolumeChoice {
  key: string;
  selected: boolean;
}

export interface StepBinding {
  kind: StepInputKind;
  step: AgentAppStep;
  backupVolumes?: VolumeChoice[];
  pullImages?: boolean;
  removeVolumes?: boolean;
}

export function seedBackupVolumes(backupSource: AgentApplication | null): VolumeChoice[] {
  if (!backupSource) {
    return [];
  }
  return extractComposeVolumeKeys(backupSource).map(key => ({ key, selected: true }));
}

export function createStepBinding({ kind, step }: ClassifiedStep, backupSource: AgentApplication | null): StepBinding {
  switch (kind) {
    case 'backupVolume':
      return { kind, step, backupVolumes: seedBackupVolumes(backupSource) };
    case 'pullImages':
      return { kind, step, pullImages: readInitialPullImages(step) };
    case 'composeDown':
      return { kind, step, removeVolumes: false };
  }
}

export function buildStepPayload(b: StepBinding): any {
  switch (b.kind) {
    case 'backupVolume':
      return buildBackupVolumeInput(
        b.step,
        (b.backupVolumes || []).filter(v => v.selected).map(v => v.key)
      );
    case 'pullImages':
      return buildPullImagesInput(b.step, !!b.pullImages);
    case 'composeDown':
      return buildComposeDownInput(b.step, !!b.removeVolumes);
  }
}

export function buildStepInputs(bindings: StepBinding[]): { [stepId: string]: any } {
  const out: { [stepId: string]: any } = {};
  for (const b of bindings) {
    out[b.step.id] = buildStepPayload(b);
  }
  return out;
}
