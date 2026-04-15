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

import { Component, Inject, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { AgentService } from '@core/http/agent.service';
import {
  AgentAppEventActionType,
  AgentAppProfile,
  AgentAppStep,
  AgentAppStepState,
  AgentAppStepType,
  AgentAppTemplate,
  AgentBulkAction,
  AgentGroupInfo,
  BulkOperationPreview,
  BulkOperationRequest,
  SkippedApp,
  SkipReason
} from '@shared/models/agent.models';

export interface AgentGroupBulkActionDialogData {
  group: AgentGroupInfo;
  profile: AgentAppProfile;
  actionType: AgentAppEventActionType;
}

interface VolumeChoice {
  key: string;
  selected: boolean;
}

@Component({
  selector: 'tb-agent-group-bulk-action-dialog',
  templateUrl: './agent-group-bulk-action-dialog.component.html',
  styleUrls: ['./agent-group-bulk-action-dialog.component.scss']
})
export class AgentGroupBulkActionDialogComponent
  extends DialogComponent<AgentGroupBulkActionDialogComponent, AgentBulkAction>
  implements OnInit {

  group: AgentGroupInfo;
  profile: AgentAppProfile;
  actionType: AgentAppEventActionType;

  loading = false;
  submitting = false;
  previewLoaded = false;
  preview: BulkOperationPreview | null = null;

  // UPGRADE step inputs
  backupVolumeStep: AgentAppStep | null = null;
  backupVolumes: VolumeChoice[] = [];
  pullImagesStep: AgentAppStep | null = null;
  pullImages = false;

  // DELETE step inputs
  composeDownStep: AgentAppStep | null = null;
  removeVolumes = false;
  profileVolumeKeys: string[] = [];

  readonly ActionType = AgentAppEventActionType;
  readonly SkipReason = SkipReason;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              private agentService: AgentService,
              @Inject(MAT_DIALOG_DATA) public data: AgentGroupBulkActionDialogData,
              public dialogRef: MatDialogRef<AgentGroupBulkActionDialogComponent, AgentBulkAction>) {
    super(store, router, dialogRef);
    this.group = data.group;
    this.profile = data.profile;
    this.actionType = data.actionType;
    this.profileVolumeKeys = this.parseVolumeKeys(this.profile);
  }

  ngOnInit() {
    this.loading = true;
    if (this.needsTemplate && this.profile?.templateId?.id) {
      this.agentService.getAgentAppTemplateById(this.profile.templateId.id).subscribe({
        next: (template) => {
          this.initStepsFromTemplate(template);
          this.loadPreview();
        },
        error: () => {
          // No template: still allow confirm with empty stepInputs.
          this.loadPreview();
        }
      });
    } else {
      this.loadPreview();
    }
  }

  get needsTemplate(): boolean {
    return this.actionType === AgentAppEventActionType.UPGRADE
      || this.actionType === AgentAppEventActionType.DELETE;
  }

  get titleKey(): string {
    switch (this.actionType) {
      case AgentAppEventActionType.RESTART: return 'agent.bulk-restart-title';
      case AgentAppEventActionType.UPDATE:  return 'agent.bulk-update-title';
      case AgentAppEventActionType.UPGRADE: return 'agent.bulk-upgrade-title';
      case AgentAppEventActionType.DELETE:  return 'agent.bulk-delete-title';
      default: return 'agent.bulk-action-title';
    }
  }

  get confirmKey(): string {
    switch (this.actionType) {
      case AgentAppEventActionType.RESTART: return 'agent.bulk-restart-cta';
      case AgentAppEventActionType.UPDATE:  return 'agent.bulk-update-cta';
      case AgentAppEventActionType.UPGRADE: return 'agent.bulk-upgrade-cta';
      case AgentAppEventActionType.DELETE:  return 'agent.bulk-delete-cta';
      default: return 'action.confirm';
    }
  }

  get confirmColor(): 'primary' | 'warn' | 'accent' {
    return this.actionType === AgentAppEventActionType.DELETE ? 'warn' :
           this.actionType === AgentAppEventActionType.UPGRADE ? 'accent' : 'primary';
  }

  get confirmDisabled(): boolean {
    return this.submitting || this.loading || !this.preview || this.preview.eligible === 0;
  }

  skippedByReason(reason: SkipReason): SkippedApp[] {
    return (this.preview?.skipped || []).filter(s => s.reason === reason);
  }

  toggleBackupVolume(v: VolumeChoice) {
    v.selected = !v.selected;
  }

  cancel() {
    this.dialogRef.close(null);
  }

  confirm() {
    if (this.confirmDisabled) {
      return;
    }
    this.submitting = true;
    const request: BulkOperationRequest = {
      actionType: this.actionType,
      stepInputs: this.buildStepInputs()
    };
    this.agentService.bulkOperation(this.group.id.id, this.profile.id.id, request).subscribe({
      next: (action) => this.dialogRef.close(action as unknown as AgentBulkAction),
      error: () => this.submitting = false
    });
  }

  private loadPreview() {
    const request: BulkOperationRequest = {
      actionType: this.actionType,
      stepInputs: {}
    };
    this.agentService.previewBulkOperation(this.group.id.id, this.profile.id.id, request).subscribe({
      next: (preview) => {
        this.preview = preview;
        this.previewLoaded = true;
        this.loading = false;
      },
      error: () => {
        this.previewLoaded = true;
        this.loading = false;
      }
    });
  }

  private initStepsFromTemplate(template: AgentAppTemplate) {
    if (this.actionType === AgentAppEventActionType.UPGRADE) {
      const upgradeSteps = (template.upgradeSteps || []).filter(s => !s.templateOnly);
      this.backupVolumeStep = upgradeSteps.find(s => s.type === AgentAppStepType.BACKUP_VOLUME) || null;
      this.pullImagesStep = upgradeSteps.find(s =>
        (s.type === AgentAppStepType.COMPOSE_MIGRATION || s.type === AgentAppStepType.COMPOSE)
        && (s.state as any) && 'pullImages' in (s.state as any)
      ) || null;
      if (this.pullImagesStep) {
        this.pullImages = !!(this.pullImagesStep.state as any)?.pullImages;
      }
      if (this.backupVolumeStep) {
        this.backupVolumes = this.profileVolumeKeys.map(k => ({ key: k, selected: false }));
      }
    } else if (this.actionType === AgentAppEventActionType.DELETE) {
      const deleteSteps = (template.deleteSteps || []).filter(s => !s.templateOnly);
      this.composeDownStep = deleteSteps.find(s => s.type === AgentAppStepType.COMPOSE_DOWN) || null;
    }
  }

  private buildStepInputs(): { [stepId: string]: AgentAppStepState } {
    const stepInputs: { [stepId: string]: AgentAppStepState } = {};
    if (this.actionType === AgentAppEventActionType.UPGRADE) {
      if (this.backupVolumeStep) {
        stepInputs[this.backupVolumeStep.id] = {
          backupVolumes: this.backupVolumes.filter(v => v.selected).map(v => v.key),
          type: AgentAppStepType.BACKUP_VOLUME
        } as AgentAppStepState;
      }
      if (this.pullImagesStep) {
        stepInputs[this.pullImagesStep.id] = {
          pullImages: this.pullImages,
          type: this.pullImagesStep.type
        } as AgentAppStepState;
      }
    } else if (this.actionType === AgentAppEventActionType.DELETE) {
      if (this.composeDownStep) {
        stepInputs[this.composeDownStep.id] = {
          removeVolumes: this.removeVolumes,
          type: AgentAppStepType.COMPOSE_DOWN
        } as AgentAppStepState;
      }
    }
    return stepInputs;
  }

  private parseVolumeKeys(profile: AgentAppProfile): string[] {
    const compose: any = profile?.config && (profile.config as any).compose;
    if (!compose || !compose.volumes || typeof compose.volumes !== 'object') {
      return [];
    }
    return Object.keys(compose.volumes);
  }
}
