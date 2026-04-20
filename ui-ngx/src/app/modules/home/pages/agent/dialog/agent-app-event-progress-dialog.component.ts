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
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { Subscription, timer } from 'rxjs';

export interface AgentAppEventProgressDialogData {
  application: AgentApplication;
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
  styleUrls: ['./agent-app-event-progress-dialog.component.scss']
})
export class AgentAppEventProgressDialogComponent
  extends DialogComponent<AgentAppEventProgressDialogComponent, boolean>
  implements OnInit, OnDestroy {

  application: AgentApplication;
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
      this.loadError = this.translate.instant('agent.app-event-progress-no-template');
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
    const agentId = (this.application?.agentId as any)?.id;
    if (!agentId) { return; }
    this.dialogRef.close(false);
    this.router.navigateByUrl(`/edgeManagement/agents/${agentId}/events`);
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
    if (!this.application?.id?.id || !this.event?.id?.id) { return; }
    // Re-fetch the latest snapshot of the event from the server. We do not have
    // a single-event GET, so we use the events list with a small page and pick
    // the matching id by client-side filter.
    const pageLink = new PageLink(50, 0, null, { property: 'createdTime', direction: Direction.DESC });
    this.agentService.getAgentAppEvents(this.application.id.id, pageLink, undefined, undefined,
      { ignoreLoading: true, ignoreErrors: true }).subscribe(page => {
      const fresh = page?.data?.find(e => e.id.id === this.event.id.id);
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
