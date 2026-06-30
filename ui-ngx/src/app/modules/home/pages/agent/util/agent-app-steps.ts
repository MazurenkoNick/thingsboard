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

import {
  AgentAppEventActionType,
  AgentAppStep,
  AgentAppStepState,
  AgentAppStepType,
  AgentAppTemplate
} from '@shared/models/agent.models';

type HasCompose = { config?: any };

// A step field is user-facing only when it is present AND declares userChoice === true.
// userChoice === false fields (e.g. ROLLBACK failedEventId) are applied silently and never rendered.
function isUserChoice(step: AgentAppStep, key: string): boolean {
  const field = (step?.state as any)?.[key];
  return !!field && field.userChoice === true;
}

function visibleSteps(steps: AgentAppStep[] | undefined): AgentAppStep[] {
  return (steps || []).filter(s => !s.templateOnly);
}

export function findComposeDownStep(template: AgentAppTemplate | null | undefined): AgentAppStep | null {
  return visibleSteps(template?.deleteSteps).find(s =>
    s.type === AgentAppStepType.COMPOSE_DOWN && isUserChoice(s, 'removeVolumes')
  ) || null;
}

export function readInitialPullImages(step: AgentAppStep | null | undefined): boolean {
  return !!(step?.state as any)?.pullImages?.value;
}

export function extractComposeVolumeKeys(source: HasCompose | null | undefined): string[] {
  const compose = source?.config?.compose;
  if (!compose || !compose.volumes || typeof compose.volumes !== 'object') {
    return [];
  }
  return Object.keys(compose.volumes);
}

export function buildBackupVolumeInput(step: AgentAppStep, selectedKeys: string[]): AgentAppStepState {
  return {
    backupVolumes: { value: selectedKeys, userChoice: true },
    type: AgentAppStepType.BACKUP_VOLUME
  } as AgentAppStepState;
}

export function buildPullImagesInput(step: AgentAppStep, pullImages: boolean): AgentAppStepState {
  return {
    pullImages: { value: pullImages, userChoice: true },
    type: step.type
  } as AgentAppStepState;
}

export function buildComposeDownInput(step: AgentAppStep, removeVolumes: boolean): AgentAppStepState {
  return {
    removeVolumes: { value: removeVolumes, userChoice: true },
    type: AgentAppStepType.COMPOSE_DOWN
  } as AgentAppStepState;
}

/**
 * Kinds of user-facing step inputs the FE knows how to render + collect.
 * The classifier maps an AgentAppStep → kind (or null if no user input).
 * Rendering and payload-building stay per-kind; what varies per template
 * is which steps of which kinds show up in which list — classification
 * is structural (step type + state shape), not positional.
 */
export type StepInputKind = 'backupVolume' | 'pullImages' | 'composeDown';

export interface ClassifiedStep {
  kind: StepInputKind;
  step: AgentAppStep;
}

// A step is classifiable as a user-facing input only when its state declares
// the field with userChoice === true. A COMPOSE_DOWN without such a field,
// for example, means the server runs compose-down with the template value — no prompt needed.
export function classifyStep(step: AgentAppStep): StepInputKind | null {
  switch (step.type) {
    case AgentAppStepType.BACKUP_VOLUME:
      return isUserChoice(step, 'backupVolumes') ? 'backupVolume' : null;
    case AgentAppStepType.COMPOSE_DOWN:
      return isUserChoice(step, 'removeVolumes') ? 'composeDown' : null;
    case AgentAppStepType.COMPOSE:
    case AgentAppStepType.COMPOSE_MIGRATION:
      return isUserChoice(step, 'pullImages') ? 'pullImages' : null;
    default:
      return null;
  }
}

/**
 * The template step-list the BE consults for a given action. UPDATE reuses
 * startSteps (mirroring the single-app wizard's update mode). RESTART and
 * ROLLBACK have no user-input steps today.
 */
export function stepsForAction(
  template: AgentAppTemplate | null | undefined,
  action: AgentAppEventActionType
): AgentAppStep[] {
  if (!template) { return []; }
  switch (action) {
    case AgentAppEventActionType.INSTALL:
    case AgentAppEventActionType.UPDATE:
      return template.startSteps || [];
    case AgentAppEventActionType.UPGRADE:
      return template.upgradeSteps || [];
    case AgentAppEventActionType.DELETE:
      return template.deleteSteps || [];
    default:
      return [];
  }
}

export function classifyStepsForAction(
  template: AgentAppTemplate | null | undefined,
  action: AgentAppEventActionType
): ClassifiedStep[] {
  const out: ClassifiedStep[] = [];
  for (const step of visibleSteps(stepsForAction(template, action))) {
    const kind = classifyStep(step);
    if (kind) {
      out.push({ kind, step });
    }
  }
  return out;
}

export function actionUsesTemplate(action: AgentAppEventActionType): boolean {
  switch (action) {
    case AgentAppEventActionType.INSTALL:
    case AgentAppEventActionType.UPDATE:
    case AgentAppEventActionType.UPGRADE:
    case AgentAppEventActionType.DELETE:
      return true;
    default:
      return false;
  }
}
