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
import {
  AgentApplication,
  AgentAppEvent,
  AgentAppEventActionType,
  agentAppEventActionTypeTranslationMap,
  AgentAppEventStatus,
  agentAppEventStatusTranslationMap,
  AgentAppStep,
  AgentAppTemplate
} from '@shared/models/agent.models';
import { DialogService } from '@core/services/dialog.service';
import { Subscription, timer } from 'rxjs';

export interface AgentAppEventProgressDialogData {
  application: AgentApplication | null;
  event: AgentAppEvent;
}

interface ProgressStepView {
  step: AgentAppStep;
  index: number;
  state: 'completed' | 'processing' | 'pending' | 'error';
}

const TERMINAL_STATUSES: ReadonlyArray<AgentAppEventStatus> = [
  AgentAppEventStatus.FINISHED,
  AgentAppEventStatus.ERROR
];

@Component({
  selector: 'tb-agent-app-event-progress-dialog',
  templateUrl: './agent-app-event-progress-dialog.component.html',
  styleUrls: ['./agent-app-event-progress-dialog.component.scss'],
  standalone: false
})
export class AgentAppEventProgressDialogComponent
  extends DialogComponent<AgentAppEventProgressDialogComponent, boolean>
  implements OnInit, OnDestroy {

  application: AgentApplication | null;
  event: AgentAppEvent;
  template: AgentAppTemplate | null = null;
  steps: ProgressStepView[] = [];
  loading = true;
  loadError = '';

  agentAppEventActionTypeTranslationMap = agentAppEventActionTypeTranslationMap;
  agentAppEventStatusTranslationMap = agentAppEventStatusTranslationMap;

  private pollSub: Subscription | null = null;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              private agentService: AgentService,
              private dialogService: DialogService,
              @Inject(MAT_DIALOG_DATA) public data: AgentAppEventProgressDialogData,
              public dialogRef: MatDialogRef<AgentAppEventProgressDialogComponent, boolean>) {
    super(store, router, dialogRef);
    this.application = data.application;
    this.event = data.event;
  }

  ngOnInit(): void {
    const templateId = this.application?.templateId?.id;
    if (!templateId) {
      // Orphan event (application removed) or missing template — render read-only
      // with whatever the event already carries. No polling.
      this.loading = false;
      return;
    }
    this.agentService.getAgentAppTemplateById(templateId).subscribe({
      next: tpl => {
        this.template = tpl;
        this.rebuildSteps();
        this.loading = false;
        this.startPollingIfNeeded();
      },
      error: () => {
        this.loadError = this.translate.instant('agent.app-event-progress-load-failed');
        this.loading = false;
      }
    });
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  get statusKey(): string {
    return this.agentAppEventStatusTranslationMap.get(this.event?.status) || this.event?.status || '';
  }

  get actionKey(): string {
    return this.agentAppEventActionTypeTranslationMap.get(this.event?.actionType) || this.event?.actionType || '';
  }

  get statusBadgeClass(): string {
    if (!this.event?.status) {
      return '';
    }
    return this.event.status.toLowerCase();
  }

  get isTerminal(): boolean {
    return TERMINAL_STATUSES.includes(this.event?.status);
  }

  get canCancel(): boolean {
    if (!this.application?.id?.id) { return false; }
    const s = this.event?.status;
    return s === AgentAppEventStatus.PENDING
        || s === AgentAppEventStatus.QUEUED
        || s === AgentAppEventStatus.PROCESSING;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  viewInEvents($event: Event): void {
    if ($event) { $event.preventDefault(); $event.stopPropagation(); }
    const agentId = (this.application?.agentId as any)?.id || (this.event?.agentId as any)?.id;
    if (!agentId) { return; }
    this.dialogRef.close(false);
    this.router.navigateByUrl(agentEntityUrl(currentAgentRouteSnapshot(this.router), agentId, 'events'));
  }

  cancelEvent($event: Event): void {
    if ($event) { $event.stopPropagation(); }
    if (!this.event?.id?.id) { return; }
    this.dialogService.confirm(
      this.translate.instant('agent.app-event-cancel-title'),
      this.translate.instant('agent.app-event-cancel-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe(res => {
      if (res) {
        this.agentService.cancelAgentAppEvent(this.application.id.id, this.event.id.id).subscribe(() => {
          this.refreshEventOnce();
        });
      }
    });
  }

  private startPollingIfNeeded(): void {
    if (this.isTerminal || this.pollSub) {
      return;
    }
    this.pollSub = timer(3000, 3000).subscribe(() => this.refreshEventOnce());
  }

  private stopPolling(): void {
    if (this.pollSub) {
      this.pollSub.unsubscribe();
      this.pollSub = null;
    }
  }

  private refreshEventOnce(): void {
    if (!this.event?.id?.id) { return; }
    this.agentService.getAgentAppEventById(this.event.id.id,
      { ignoreLoading: true, ignoreErrors: true }).subscribe(fresh => {
      if (fresh) {
        this.event = fresh;
        this.rebuildSteps();
        if (this.isTerminal) {
          this.stopPolling();
        }
      }
    });
  }

  private rebuildSteps(): void {
    if (!this.template) {
      this.steps = [];
      return;
    }
    const ordered = this.orderedStepsForAction(this.template, this.event.actionType);
    const currentId = this.event.currentStepId;
    const status = this.event.status;
    const currentIdx = currentId ? ordered.findIndex(s => s.id === currentId) : -1;
    this.steps = ordered.map((step, i) => {
      let state: ProgressStepView['state'];
      if (currentIdx < 0) {
        state = 'pending';
      } else if (i < currentIdx) {
        state = 'completed';
      } else if (i === currentIdx) {
        state = status === AgentAppEventStatus.ERROR ? 'error' : 'processing';
      } else {
        state = 'pending';
      }
      if (status === AgentAppEventStatus.FINISHED) {
        state = 'completed';
      }
      return { step, index: i + 1, state };
    });
  }

  private orderedStepsForAction(template: AgentAppTemplate, action: AgentAppEventActionType): AgentAppStep[] {
    let raw: AgentAppStep[] | undefined;
    switch (action) {
      case AgentAppEventActionType.INSTALL:
      case AgentAppEventActionType.UPDATE:
        raw = template.startSteps;
        break;
      case AgentAppEventActionType.RESTART:
        raw = template.restartSteps;
        break;
      case AgentAppEventActionType.UPGRADE:
        raw = template.upgradeSteps;
        break;
      case AgentAppEventActionType.DELETE:
        raw = template.deleteSteps;
        break;
      case AgentAppEventActionType.ROLLBACK:
        raw = template.rollbackSteps;
        break;
    }
    const filtered = (raw || []).filter(s => !s.templateOnly);
    return this.orderByNextId(filtered);
  }

  /**
   * Steps are linked via `nextStepId`. The first step is the one no other step
   * points at. Walk the chain to produce a deterministic order matching the BE.
   */
  private orderByNextId(steps: AgentAppStep[]): AgentAppStep[] {
    if (!steps.length) { return []; }
    const byId = new Map(steps.map(s => [s.id, s]));
    const referenced = new Set(steps.map(s => s.nextStepId).filter(Boolean) as string[]);
    const head = steps.find(s => !referenced.has(s.id));
    if (!head) {
      return steps; // fall back to natural order if chain is broken
    }
    const ordered: AgentAppStep[] = [];
    let cursor: AgentAppStep | undefined = head;
    const seen = new Set<string>();
    while (cursor && !seen.has(cursor.id)) {
      ordered.push(cursor);
      seen.add(cursor.id);
      cursor = cursor.nextStepId ? byId.get(cursor.nextStepId) : undefined;
    }
    return ordered;
  }
}
