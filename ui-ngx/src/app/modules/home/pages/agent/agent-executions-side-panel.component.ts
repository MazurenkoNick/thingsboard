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
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { Subject, Subscription, timer } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { AgentService } from '@core/http/agent.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import {
  AgentAppEventInfo,
  AgentAppEventStatus,
  AgentAppProfile,
  AgentBulkAction,
  AgentBulkActionStatus,
  AgentProfileInfo
} from '@shared/models/agent.models';
import {
  AgentAppEventProgressDialogComponent,
  AgentAppEventProgressDialogData
} from '@home/pages/agent/dialog/agent-app-event-progress-dialog.component';

const NON_TERMINAL_BULK: ReadonlyArray<AgentBulkActionStatus> = [
  AgentBulkActionStatus.QUEUED,
  AgentBulkActionStatus.IN_PROGRESS
];

type StatusFilter = 'ALL' | 'ERROR' | 'RUNNING';

interface StatusCounts {
  total: number;
  done: number;
  error: number;
  running: number;
  pending: number;
}

@Component({
  selector: 'tb-agent-executions-side-panel',
  templateUrl: './agent-executions-side-panel.component.html',
  styleUrls: ['./agent-executions-side-panel.component.scss'],
  standalone: false
})
export class AgentExecutionsSidePanelComponent implements OnChanges, OnDestroy {

  @Input() agentProfile: AgentProfileInfo;
  @Input() appProfile: AgentAppProfile;
  @Input() bulkAction: AgentBulkAction;

  @Output() closePanel = new EventEmitter<void>();
  @Output() openFullDetails = new EventEmitter<void>();

  events: AgentAppEventInfo[] = [];
  counts: StatusCounts = { total: 0, done: 0, error: 0, running: 0, pending: 0 };
  filter: StatusFilter = 'ALL';
  loading = false;

  pageIndex = 0;
  pageSize = 10;
  readonly pageSizeOptions = [10, 25, 50, 100];

  private readonly destroy$ = new Subject<void>();
  private pollSub: Subscription | null = null;

  constructor(private agentService: AgentService,
              private datePipe: DatePipe,
              private dialog: MatDialog) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.bulkAction) {
      this.stopPolling();
      this.pageIndex = 0;
      if (this.bulkAction) {
        this.refresh();
        if (NON_TERMINAL_BULK.includes(this.bulkAction.status)) {
          this.startPolling();
        }
      }
    }
  }

  ngOnDestroy(): void {
    this.stopPolling();
    this.destroy$.next();
    this.destroy$.complete();
  }

  get filteredEvents(): AgentAppEventInfo[] {
    switch (this.filter) {
      case 'ERROR':
        return this.events.filter(e =>
          e.status === AgentAppEventStatus.ERROR ||
          e.status === AgentAppEventStatus.START_FAILED);
      case 'RUNNING':
        return this.events.filter(e => e.status === AgentAppEventStatus.PROCESSING);
      default:
        return this.events;
    }
  }

  get pagedEvents(): AgentAppEventInfo[] {
    const start = this.pageIndex * this.pageSize;
    return this.filteredEvents.slice(start, start + this.pageSize);
  }

  onPageChange(e: { pageIndex: number; pageSize: number }): void {
    this.pageIndex = e.pageIndex;
    this.pageSize = e.pageSize;
  }

  get successPct(): number {
    return this.counts.total === 0
      ? 0
      : Math.round((this.counts.done / this.counts.total) * 100);
  }

  get donePct(): number {
    return this.pctOf(this.counts.done);
  }
  get errorPct(): number {
    return this.pctOf(this.counts.error);
  }
  get runningPct(): number {
    return this.pctOf(this.counts.running);
  }

  setFilter(f: StatusFilter): void {
    this.filter = f;
    this.pageIndex = 0;
  }

  close(): void {
    this.closePanel.emit();
  }

  openDetails(): void {
    this.openFullDetails.emit();
  }

  refreshNow(): void {
    this.refresh();
  }

  isEventClickable(event: AgentAppEventInfo): boolean {
    return !!event.applicationId
      && (event.status === AgentAppEventStatus.PENDING
        || event.status === AgentAppEventStatus.QUEUED
        || event.status === AgentAppEventStatus.PROCESSING);
  }

  openEventProgress(event: AgentAppEventInfo): void {
    if (!this.isEventClickable(event)) {
      return;
    }
    this.agentService.getAgentApplicationById(event.applicationId.id, { ignoreErrors: true } as any).subscribe({
      next: application => this.openProgressDialog(application, event),
      error: () => this.openProgressDialog(null, event)
    });
  }

  private openProgressDialog(application: any, event: AgentAppEventInfo): void {
    this.dialog.open<AgentAppEventProgressDialogComponent, AgentAppEventProgressDialogData, boolean>(
      AgentAppEventProgressDialogComponent, {
        disableClose: false,
        panelClass: ['tb-dialog'],
        data: { application, event }
      }
    ).afterClosed().subscribe(() => this.refresh());
  }

  formatTime(ts: number): string {
    if (!ts) { return ''; }
    return this.datePipe.transform(ts, 'HH:mm:ss') || '';
  }

  statusLabel(status: AgentAppEventStatus): string {
    switch (status) {
      case AgentAppEventStatus.FINISHED: return 'Done';
      case AgentAppEventStatus.ERROR: return 'Error';
      case AgentAppEventStatus.START_FAILED: return 'Start failed';
      case AgentAppEventStatus.PROCESSING: return 'Running';
      case AgentAppEventStatus.QUEUED: return 'Queued';
      case AgentAppEventStatus.PENDING: return 'Pending';
      default: return String(status);
    }
  }

  statusColor(status: AgentAppEventStatus): string {
    switch (status) {
      case AgentAppEventStatus.FINISHED: return '#2e7d32';
      case AgentAppEventStatus.ERROR:
      case AgentAppEventStatus.START_FAILED: return '#c62828';
      case AgentAppEventStatus.PROCESSING: return '#1565c0';
      case AgentAppEventStatus.QUEUED:
      case AgentAppEventStatus.PENDING:
      default: return '#616161';
    }
  }

  private pctOf(n: number): number {
    return this.counts.total === 0 ? 0 : (n / this.counts.total) * 100;
  }

  private clampPageIndex(): void {
    const total = this.filteredEvents.length;
    const maxIndex = Math.max(0, Math.ceil(total / this.pageSize) - 1);
    if (this.pageIndex > maxIndex) {
      this.pageIndex = maxIndex;
    }
  }

  private refresh(): void {
    if (!this.bulkAction) { return; }
    this.loading = true;
    const pageLink = new PageLink(100, 0, null, { property: 'updatedTime', direction: Direction.DESC });
    this.agentService.getAgentBulkActionEvents(this.bulkAction.id.id, pageLink).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: page => {
        this.events = page.data;
        this.counts = this.computeCounts(this.events, this.bulkAction.total);
        this.clampPageIndex();
        this.loading = false;
        if (this.allEventsTerminal()) {
          this.stopPolling();
        }
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  private allEventsTerminal(): boolean {
    // Nothing dispatched yet — keep polling until events start arriving.
    if (!this.events.length || this.counts.pending > 0 || this.counts.running > 0) {
      return false;
    }
    return this.events.every(e =>
      e.status === AgentAppEventStatus.FINISHED ||
      e.status === AgentAppEventStatus.ERROR ||
      e.status === AgentAppEventStatus.START_FAILED);
  }

  private computeCounts(events: AgentAppEventInfo[], total: number): StatusCounts {
    const done = events.filter(e => e.status === AgentAppEventStatus.FINISHED).length;
    const error = events.filter(e =>
      e.status === AgentAppEventStatus.ERROR ||
      e.status === AgentAppEventStatus.START_FAILED).length;
    const running = events.filter(e => e.status === AgentAppEventStatus.PROCESSING).length;
    const submitted = done + error + running +
      events.filter(e =>
        e.status === AgentAppEventStatus.QUEUED ||
        e.status === AgentAppEventStatus.PENDING).length;
    const pending = Math.max(0, (total || submitted) - submitted);
    return {
      total: total || submitted,
      done,
      error,
      running,
      pending
    };
  }

  private startPolling(): void {
    this.pollSub = timer(3000, 3000).pipe(takeUntil(this.destroy$)).subscribe(() => this.refresh());
  }

  private stopPolling(): void {
    this.pollSub?.unsubscribe();
    this.pollSub = null;
  }
}
