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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { agentEntityUrl, currentAgentRouteSnapshot } from '@home/pages/agent/util/agent-route-params';
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
  AgentProfileInfo,
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

export interface AgentProfileBulkActionDialogData {
  agentProfile: AgentProfileInfo;
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
  selector: 'tb-agent-profile-bulk-action-dialog',
  templateUrl: './agent-profile-bulk-action-dialog.component.html',
  styleUrls: ['./agent-profile-bulk-action-dialog.component.scss'],
  standalone: false
})
export class AgentProfileBulkActionDialogComponent
  extends DialogComponent<AgentProfileBulkActionDialogComponent, AgentBulkAction>
  implements OnInit, OnDestroy {

  private readonly destroy$ = new Subject<void>();

  agentProfile: AgentProfileInfo;
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
              @Inject(MAT_DIALOG_DATA) public data: AgentProfileBulkActionDialogData,
              public dialogRef: MatDialogRef<AgentProfileBulkActionDialogComponent, AgentBulkAction>) {
    super(store, router, dialogRef);
    this.agentProfile = data.agentProfile;
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
      switchMap(() => this.agentService.previewBulkOperation(this.agentProfile.id.id, this.profile.id.id, request)),
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
          [agentEntityUrl(currentAgentRouteSnapshot(this.router), s.agentId.id, 'applications', s.applicationId.id)]);
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
    this.agentService.bulkOperation(this.agentProfile.id.id, this.profile.id.id, request)
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
