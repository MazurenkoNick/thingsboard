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
  AgentAppTemplate,
  AgentBulkAction,
  AgentGroupInfo,
  BulkOperationPreview,
  BulkOperationRequest,
  SkippedApp,
  SkipReason
} from '@shared/models/agent.models';
import {
  actionUsesTemplate,
  buildBackupVolumeInput,
  buildComposeDownInput,
  buildPullImagesInput,
  classifyStepsForAction,
  ClassifiedStep,
  extractComposeVolumeKeys,
  readInitialPullImages,
  StepInputKind
} from '@home/pages/agent/util/agent-app-steps';

export interface AgentGroupBulkActionDialogData {
  group: AgentGroupInfo;
  profile: AgentAppProfile;
  actionType: AgentAppEventActionType;
}

interface VolumeChoice {
  key: string;
  selected: boolean;
}

// One rendered input block per classified step. Per-kind local state lives on
// optional fields; only the field matching `kind` is populated and read.
export interface StepBinding {
  kind: StepInputKind;
  step: AgentAppStep;
  backupVolumes?: VolumeChoice[];
  pullImages?: boolean;
  removeVolumes?: boolean;
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

  // One binding per user-input step surfaced by the template, in BE order.
  // Supports multiple steps of the same kind — each gets its own block.
  bindings: StepBinding[] = [];
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
    this.profileVolumeKeys = extractComposeVolumeKeys(this.profile);
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
    return actionUsesTemplate(this.actionType);
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

  trackBinding(_idx: number, b: StepBinding): string {
    return b.step.id;
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
    this.bindings = classifyStepsForAction(template, this.actionType)
      .map(cs => this.createBinding(cs));
  }

  private createBinding({ kind, step }: ClassifiedStep): StepBinding {
    switch (kind) {
      case 'backupVolume':
        return {
          kind, step,
          backupVolumes: this.profileVolumeKeys.map(k => ({ key: k, selected: false }))
        };
      case 'pullImages':
        return { kind, step, pullImages: readInitialPullImages(step) };
      case 'composeDown':
        return { kind, step, removeVolumes: false };
    }
  }

  private buildStepInputs(): { [stepId: string]: AgentAppStepState } {
    const stepInputs: { [stepId: string]: AgentAppStepState } = {};
    for (const b of this.bindings) {
      stepInputs[b.step.id] = this.buildBindingInput(b);
    }
    return stepInputs;
  }

  private buildBindingInput(b: StepBinding): AgentAppStepState {
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
}
