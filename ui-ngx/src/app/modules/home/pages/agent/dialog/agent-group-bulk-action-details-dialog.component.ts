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
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { AgentService } from '@core/http/agent.service';
import {
  AgentAppEvent,
  AgentAppEventInfo,
  AgentAppEventStatus,
  agentAppEventStatusTranslationMap,
  AgentBulkAction,
  AgentBulkActionStatus,
  SkipReason
} from '@shared/models/agent.models';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { PageData } from '@shared/models/page/page-data';
import { Subscription, timer } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import {
  AgentAppEventProgressDialogComponent,
  AgentAppEventProgressDialogData
} from '@home/pages/agent/dialog/agent-app-event-progress-dialog.component';

export interface AgentGroupBulkActionDetailsDialogData {
  bulkAction: AgentBulkAction;
}

const NON_TERMINAL: ReadonlyArray<AgentBulkActionStatus> = [
  AgentBulkActionStatus.QUEUED,
  AgentBulkActionStatus.IN_PROGRESS
];

@Component({
  selector: 'tb-agent-group-bulk-action-details-dialog',
  templateUrl: './agent-group-bulk-action-details-dialog.component.html',
  styleUrls: ['./agent-group-bulk-action-details-dialog.component.scss']
})
export class AgentGroupBulkActionDetailsDialogComponent
  extends DialogComponent<AgentGroupBulkActionDetailsDialogComponent, boolean>
  implements OnInit, OnDestroy {

  bulkAction: AgentBulkAction;
  events: AgentAppEventInfo[] = [];
  loading = false;
  statusFilter: AgentAppEventStatus | null = null;
  skipReasons: Array<{ reason: SkipReason; count: number }> = [];

  readonly AgentBulkActionStatus = AgentBulkActionStatus;
  readonly AgentAppEventStatus = AgentAppEventStatus;
  readonly SkipReason = SkipReason;
  readonly statusOptions: Array<AgentAppEventStatus | null> = [
    null,
    AgentAppEventStatus.PENDING,
    AgentAppEventStatus.QUEUED,
    AgentAppEventStatus.PROCESSING,
    AgentAppEventStatus.FINISHED,
    AgentAppEventStatus.ERROR
  ];
  readonly eventStatusTranslationMap = agentAppEventStatusTranslationMap;

  private pollSub: Subscription | null = null;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              private agentService: AgentService,
              private dialog: MatDialog,
              @Inject(MAT_DIALOG_DATA) public data: AgentGroupBulkActionDetailsDialogData,
              public dialogRef: MatDialogRef<AgentGroupBulkActionDetailsDialogComponent, boolean>) {
    super(store, router, dialogRef);
    this.bulkAction = data.bulkAction;
    this.rebuildSkipReasons();
  }

  ngOnInit() {
    this.reloadEvents();
    if (NON_TERMINAL.includes(this.bulkAction.status)) {
      this.startPolling();
    }
  }

  ngOnDestroy() {
    this.pollSub?.unsubscribe();
  }

  onStatusFilterChange(value: AgentAppEventStatus | null) {
    this.statusFilter = value;
    this.reloadEvents();
  }

  close() {
    this.dialogRef.close(false);
  }

  statusColor(status: AgentAppEventStatus): string {
    switch (status) {
      case AgentAppEventStatus.FINISHED:   return '#2e7d32';
      case AgentAppEventStatus.ERROR:      return '#c62828';
      case AgentAppEventStatus.PROCESSING: return '#1565c0';
      case AgentAppEventStatus.QUEUED:     return '#616161';
      case AgentAppEventStatus.PENDING:    return '#616161';
      default: return '#616161';
    }
  }

  bulkStatusColor(): string {
    switch (this.bulkAction.status) {
      case AgentBulkActionStatus.STARTED:       return '#2e7d32';
      case AgentBulkActionStatus.START_FAILED:  return '#c62828';
      case AgentBulkActionStatus.IN_PROGRESS:   return '#1565c0';
      default: return '#616161';
    }
  }

  get skippedTotal(): number {
    return Math.max(0, (this.bulkAction.total || 0) - (this.bulkAction.submitted || 0));
  }

  openEventProgress(event: AgentAppEventInfo) {
    this.agentService.getAgentApplicationById(event.applicationId.id).subscribe((app) => {
      this.dialog.open<AgentAppEventProgressDialogComponent, AgentAppEventProgressDialogData, boolean>(
        AgentAppEventProgressDialogComponent, {
          disableClose: false,
          panelClass: ['tb-dialog'],
          data: { application: app as any, event: event as AgentAppEvent }
        }
      );
    });
  }

  trackByEventId(_: number, event: AgentAppEventInfo): string {
    return event.id?.id || '';
  }

  private reloadEvents() {
    this.loading = true;
    const pageLink = new PageLink(100, 0, null, {
      property: 'updatedTime',
      direction: Direction.DESC
    });
    this.agentService.getAgentBulkActionEvents(
      this.bulkAction.id.id,
      pageLink,
      this.statusFilter || undefined
    ).subscribe({
      next: (page: PageData<AgentAppEvent>) => {
        this.events = (page.data || []) as AgentAppEventInfo[];
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  private reloadBulkAction() {
    this.agentService.getAgentBulkAction(this.bulkAction.id.id).subscribe({
      next: (action) => {
        this.bulkAction = action;
        this.rebuildSkipReasons();
        if (!NON_TERMINAL.includes(action.status)) {
          this.pollSub?.unsubscribe();
          this.pollSub = null;
        }
      }
    });
  }

  private startPolling() {
    this.pollSub = timer(3000, 3000).pipe(
      switchMap(() => {
        this.reloadEvents();
        return this.agentService.getAgentBulkAction(this.bulkAction.id.id);
      })
    ).subscribe({
      next: (action) => {
        this.bulkAction = action;
        this.rebuildSkipReasons();
        if (!NON_TERMINAL.includes(action.status)) {
          this.pollSub?.unsubscribe();
          this.pollSub = null;
        }
      }
    });
  }

  private rebuildSkipReasons() {
    const counts = this.bulkAction.skipCounts || {};
    this.skipReasons = Object.keys(counts)
      .map((key) => ({ reason: key as SkipReason, count: counts[key as SkipReason] || 0 }))
      .filter(e => e.count > 0);
  }
}
