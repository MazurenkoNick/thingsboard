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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { AgentService } from '@core/http/agent.service';
import { of, Subject } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
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
  implements OnInit, OnDestroy {

  private readonly destroy$ = new Subject<void>();

  group: AgentGroupInfo;
  profile: AgentAppProfile;
  actionType: AgentAppEventActionType;

  loading = false;
  submitting = false;
  previewLoaded = false;
  preview: BulkOperationPreview | null = null;

  // Pre-computed from `preview.skippedSample` so the template doesn't re-filter
  // on every change-detection cycle. Keys are SkipReason values.
  skippedByReasonMap: Partial<Record<SkipReason, SkippedApp[]>> = {};
  skippedLinkMap = new Map<string, string[]>();
  totalSkipped = 0;

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

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    super.ngOnDestroy();
  }

  ngOnInit() {
    this.loading = true;

    // Single chain: optional template fetch → preview fetch. One takeUntil on
    // the whole pipe so cancel short-circuits both in-flight requests and no
    // late callback can push state into a closed dialog.
    const template$ = this.needsTemplate && this.profile?.templateId?.id
      ? this.agentService.getAgentAppTemplateById(this.profile.templateId.id).pipe(
          catchError(() => of(null as AgentAppTemplate))
        )
      : of(null as AgentAppTemplate);

    const request: BulkOperationRequest = { actionType: this.actionType, stepInputs: {} };
    template$.pipe(
      tap(template => { if (template) { this.initStepsFromTemplate(template); } }),
      switchMap(() => this.agentService.previewBulkOperation(this.group.id.id, this.profile.id.id, request)),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (preview) => {
        this.preview = preview;
        this.hydratePreview(preview);
        this.previewLoaded = true;
        this.loading = false;
      },
      error: () => {
        this.previewLoaded = true;
        this.loading = false;
      }
    });
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
    return this.skippedByReasonMap[reason] || [];
  }

  skippedCount(reason: SkipReason): number {
    return this.preview?.skippedCountsByReason?.[reason] ?? 0;
  }

  skippedExtraCount(reason: SkipReason): number {
    return Math.max(0, this.skippedCount(reason) - this.skippedByReason(reason).length);
  }

  appLink(s: SkippedApp): string[] | null {
    const key = s.applicationId?.id;
    return key ? (this.skippedLinkMap.get(key) ?? null) : null;
  }

  // Close the dialog first, then navigate. Using [routerLink] with an extra
  // (click)="cancel()" races the close animation against the route change —
  // the dialog fades out over the new page, looking like a stuck overlay.
  navigateToApp(link: string[], $event: Event) {
    if ($event) { $event.preventDefault(); }
    this.dialogRef.close(null);
    this.router.navigate(link);
  }

  private hydratePreview(preview: BulkOperationPreview) {
    const byReason: Partial<Record<SkipReason, SkippedApp[]>> = {};
    const linkMap = new Map<string, string[]>();
    for (const s of preview.skippedSample || []) {
      (byReason[s.reason] ||= []).push(s);
      if (s.agentId?.id && s.applicationId?.id) {
        linkMap.set(s.applicationId.id,
          ['/edgeManagement', 'agents', s.agentId.id, 'applications', s.applicationId.id]);
      }
    }
    this.skippedByReasonMap = byReason;
    this.skippedLinkMap = linkMap;
    const counts = preview.skippedCountsByReason || {};
    this.totalSkipped = Object.values(counts).reduce((sum, n) => sum + (n || 0), 0);
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
    this.agentService.bulkOperation(this.group.id.id, this.profile.id.id, request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (action) => this.dialogRef.close(action as unknown as AgentBulkAction),
        error: () => this.submitting = false
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
