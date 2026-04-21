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

import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription, timer } from 'rxjs';
import {
  AgentBulkAction,
  AgentBulkActionStatus,
  agentBulkActionStatusTranslationMap,
  SkipReason
} from '@shared/models/agent.models';
import { AgentService } from '@core/http/agent.service';
import {
  AgentBulkActionEventTableComponent
} from '@home/pages/agent/table/agent-bulk-action-event-table.component';

const NON_TERMINAL: ReadonlyArray<AgentBulkActionStatus> = [
  AgentBulkActionStatus.QUEUED,
  AgentBulkActionStatus.IN_PROGRESS
];

@Component({
  selector: 'tb-agent-bulk-action-events-page',
  templateUrl: './agent-bulk-action-events-page.component.html',
  styleUrls: ['./agent-bulk-action-events-page.component.scss'],
  standalone: false
})
export class AgentBulkActionEventsPageComponent implements OnInit, OnDestroy {

  @ViewChild(AgentBulkActionEventTableComponent) eventsTable: AgentBulkActionEventTableComponent;

  bulkAction: AgentBulkAction | null = null;
  bulkActionId: string;
  skipReasons: Array<{ reason: SkipReason; count: number }> = [];

  readonly bulkStatusTranslationMap = agentBulkActionStatusTranslationMap;

  private pollSub: Subscription | null = null;

  constructor(private route: ActivatedRoute,
              private router: Router,
              private agentService: AgentService) {}

  ngOnInit(): void {
    this.bulkActionId = this.route.snapshot.params['bulkActionId'];
    if (!this.bulkActionId) {
      return;
    }
    this.agentService.getAgentBulkAction(this.bulkActionId).subscribe(action => {
      this.bulkAction = action;
      this.rebuildSkipReasons();
      if (NON_TERMINAL.includes(action.status)) {
        this.startPolling();
      }
    });
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
  }

  bulkStatusColor(): string {
    switch (this.bulkAction?.status) {
      case AgentBulkActionStatus.STARTED:       return '#2e7d32';
      case AgentBulkActionStatus.START_FAILED:  return '#c62828';
      case AgentBulkActionStatus.IN_PROGRESS:   return '#1565c0';
      default: return '#616161';
    }
  }

  get skippedTotal(): number {
    if (!this.bulkAction) { return 0; }
    return Math.max(0, (this.bulkAction.total || 0) - (this.bulkAction.submitted || 0));
  }

  back(): void {
    history.back();
  }

  private startPolling() {
    this.pollSub = timer(3000, 3000).subscribe(() => {
      this.eventsTable?.refresh();
      this.agentService.getAgentBulkAction(this.bulkActionId).subscribe(action => {
        this.bulkAction = action;
        this.rebuildSkipReasons();
        if (!NON_TERMINAL.includes(action.status)) {
          this.pollSub?.unsubscribe();
          this.pollSub = null;
        }
      });
    });
  }

  private rebuildSkipReasons() {
    if (!this.bulkAction) { return; }
    const counts = this.bulkAction.skipCounts || {};
    this.skipReasons = Object.keys(counts)
      .map((key) => ({ reason: key as SkipReason, count: counts[key as SkipReason] || 0 }))
      .filter(e => e.count > 0);
  }
}
