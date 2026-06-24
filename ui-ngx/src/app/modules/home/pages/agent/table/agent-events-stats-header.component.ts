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

import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Observable, Subject, Subscription, timer } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { EntityTableHeaderComponent } from '@home/components/entity/entity-table-header.component';
import {
  AgentAppEvent,
  AgentAppEventInfo,
  AgentAppEventStatus
} from '@shared/models/agent.models';
import { PageData } from '@shared/models/page/page-data';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';

/**
 * Any table config that wants to render the events stats header implements this
 * by exposing a fetch returning enough events to compute aggregate counts.
 */
export interface EventsStatsFetcher {
  fetchAllEventsForStats(): Observable<PageData<AgentAppEvent | AgentAppEventInfo>>;
}

interface StatusCounts {
  total: number;
  done: number;
  error: number;
  running: number;
  pending: number;
}

type AnyEvent = AgentAppEvent | AgentAppEventInfo;

@Component({
  selector: 'tb-agent-events-stats-header',
  templateUrl: './agent-events-stats-header.component.html',
  styleUrls: ['./agent-events-stats-header.component.scss'],
  standalone: false
})
export class AgentEventsStatsHeaderComponent
  extends EntityTableHeaderComponent<AnyEvent>
  implements OnInit, OnDestroy {

  counts: StatusCounts = { total: 0, done: 0, error: 0, running: 0, pending: 0 };

  private pollSub: Subscription | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(private cd: ChangeDetectorRef) {
    super();
  }

  ngOnInit() {
    super.ngOnInit();
  }

  ngOnDestroy(): void {
    this.stopPolling();
    this.destroy$.next();
    this.destroy$.complete();
  }

  private stopPolling(): void {
    this.pollSub?.unsubscribe();
    this.pollSub = null;
  }

  get successPct(): number {
    return this.counts.total === 0
      ? 0
      : Math.round((this.counts.done / this.counts.total) * 100);
  }

  get donePct(): number { return this.pctOf(this.counts.done); }
  get errorPct(): number { return this.pctOf(this.counts.error); }
  get runningPct(): number { return this.pctOf(this.counts.running); }

  private pctOf(n: number): number {
    return this.counts.total === 0 ? 0 : (n / this.counts.total) * 100;
  }

  protected override setEntitiesTableConfig(cfg: EntityTableConfig<AnyEvent>) {
    super.setEntitiesTableConfig(cfg);
    this.stopPolling();
    const fetcher = cfg as unknown as EventsStatsFetcher;
    if (cfg && typeof fetcher.fetchAllEventsForStats === 'function') {
      this.loadCounts();
      this.pollSub = timer(3000, 3000).pipe(takeUntil(this.destroy$)).subscribe(() => this.loadCounts());
    }
  }

  private loadCounts(): void {
    const fetcher = this.entitiesTableConfig as unknown as EventsStatsFetcher;
    if (!fetcher?.fetchAllEventsForStats) { return; }
    fetcher.fetchAllEventsForStats().pipe(takeUntil(this.destroy$)).subscribe({
      next: page => {
        this.counts = this.computeCounts(page.data || []);
        this.cd.markForCheck();
        // Once every event has reached a terminal state there is nothing left
        // to refresh — stop the 3s poll (which would otherwise re-fetch up to
        // 10k events forever).
        if (this.allEventsTerminal()) {
          this.stopPolling();
        }
      },
      error: () => {}
    });
  }

  private allEventsTerminal(): boolean {
    if (this.counts.total === 0 || this.counts.pending > 0 || this.counts.running > 0) {
      return false;
    }
    return this.counts.done + this.counts.error === this.counts.total;
  }

  private computeCounts(events: AnyEvent[]): StatusCounts {
    const done = events.filter(e => e.status === AgentAppEventStatus.FINISHED).length;
    const error = events.filter(e =>
      e.status === AgentAppEventStatus.ERROR ||
      e.status === AgentAppEventStatus.START_FAILED).length;
    const running = events.filter(e => e.status === AgentAppEventStatus.PROCESSING).length;
    const pending = events.filter(e =>
      e.status === AgentAppEventStatus.QUEUED ||
      e.status === AgentAppEventStatus.PENDING).length;
    const total = done + error + running + pending;
    return { total, done, error, running, pending };
  }
}
