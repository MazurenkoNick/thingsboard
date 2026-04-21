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

import { Component, OnInit, ViewContainerRef } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DatePipe } from '@angular/common';
import { Overlay } from '@angular/cdk/overlay';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';

import { AgentService } from '@core/http/agent.service';
import { DialogService } from '@core/services/dialog.service';
import { AgentEventsTableConfig } from '@home/pages/agent/table/agent-events-table-config';
import { resolveAgentIdParam } from '@home/pages/agent/util/agent-route-params';

@Component({
  selector: 'tb-agent-events-page',
  template: `
    <tb-entities-table *ngIf="tableConfig" [entitiesTableConfig]="tableConfig"></tb-entities-table>
  `,
  styles: [`
    :host { display: block; height: 100%; }
    :host ::ng-deep mat-row:has(.tb-agent-app-event-inflight) {
      background-color: #fff8e1;
      cursor: pointer;
    }
    :host ::ng-deep mat-row:has(.tb-agent-app-event-inflight) .mat-mdc-cell {
      background-color: #fff8e1;
      cursor: pointer;
    }
  `],
  standalone: false
})
export class AgentEventsPageComponent implements OnInit {

  tableConfig: AgentEventsTableConfig;

  constructor(private route: ActivatedRoute,
              private agentService: AgentService,
              private dialogService: DialogService,
              private dialog: MatDialog,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef) {}

  ngOnInit(): void {
    const agentId = resolveAgentIdParam(this.route.snapshot);
    if (!agentId) {
      return;
    }
    this.agentService.getAgentInfoById(agentId).subscribe({
      next: agent => this.buildConfig(agentId, agent),
      error: () => this.buildConfig(agentId, null)
    });
  }

  private buildConfig(agentId: string, agent: any): void {
    this.tableConfig = new AgentEventsTableConfig(
      agentId,
      agent,
      this.agentService,
      this.dialogService,
      this.dialog,
      this.translate,
      this.datePipe,
      this.overlay,
      this.viewContainerRef
    );
  }
}
