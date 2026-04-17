///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import {
  AgentAppEventActionType,
  AgentAppStep,
  AgentAppStepState,
  AgentAppStepType,
  AgentAppTemplate
} from '@shared/models/agent.models';

type HasCompose = { config?: any };

function stateHas(step: AgentAppStep, key: string): boolean {
  const state = step.state as any;
  return !!state && key in state;
}

function visibleSteps(steps: AgentAppStep[] | undefined): AgentAppStep[] {
  return (steps || []).filter(s => !s.templateOnly);
}

export function findComposeDownStep(template: AgentAppTemplate | null | undefined): AgentAppStep | null {
  return visibleSteps(template?.deleteSteps).find(s =>
    s.type === AgentAppStepType.COMPOSE_DOWN && stateHas(s, 'removeVolumes')
  ) || null;
}

export function readInitialPullImages(step: AgentAppStep | null | undefined): boolean {
  return !!(step?.state as any)?.pullImages;
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
    backupVolumes: selectedKeys,
    type: AgentAppStepType.BACKUP_VOLUME
  } as AgentAppStepState;
}

export function buildPullImagesInput(step: AgentAppStep, pullImages: boolean): AgentAppStepState {
  return {
    pullImages,
    type: step.type
  } as AgentAppStepState;
}

export function buildComposeDownInput(step: AgentAppStep, removeVolumes: boolean): AgentAppStepState {
  return {
    removeVolumes,
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
// the key the input writes. A COMPOSE_DOWN with state=null, for example,
// means the server will run compose-down with defaults — no prompt needed.
export function classifyStep(step: AgentAppStep): StepInputKind | null {
  switch (step.type) {
    case AgentAppStepType.BACKUP_VOLUME:
      return stateHas(step, 'backupVolumes') ? 'backupVolume' : null;
    case AgentAppStepType.COMPOSE_DOWN:
      return stateHas(step, 'removeVolumes') ? 'composeDown' : null;
    case AgentAppStepType.COMPOSE:
    case AgentAppStepType.COMPOSE_MIGRATION:
      return stateHas(step, 'pullImages') ? 'pullImages' : null;
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
