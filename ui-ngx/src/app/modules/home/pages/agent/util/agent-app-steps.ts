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

export function findBackupVolumeStep(template: AgentAppTemplate | null | undefined): AgentAppStep | null {
  return visibleSteps(template?.upgradeSteps).find(s => s.type === AgentAppStepType.BACKUP_VOLUME) || null;
}

export function findUpgradePullImagesStep(template: AgentAppTemplate | null | undefined): AgentAppStep | null {
  return visibleSteps(template?.upgradeSteps).find(s =>
    (s.type === AgentAppStepType.COMPOSE_MIGRATION || s.type === AgentAppStepType.COMPOSE)
    && stateHas(s, 'pullImages')
  ) || null;
}

export function findStartPullImagesStep(template: AgentAppTemplate | null | undefined): AgentAppStep | null {
  return visibleSteps(template?.startSteps).find(s =>
    s.type === AgentAppStepType.COMPOSE && stateHas(s, 'pullImages')
  ) || null;
}

export function findComposeDownStep(template: AgentAppTemplate | null | undefined): AgentAppStep | null {
  return visibleSteps(template?.deleteSteps).find(s => s.type === AgentAppStepType.COMPOSE_DOWN) || null;
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
