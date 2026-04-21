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

import { AfterViewInit, Component, Input, OnChanges, OnInit, SimpleChanges, ViewChild, ViewContainerRef } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Overlay } from '@angular/cdk/overlay';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';

import { AgentService } from '@core/http/agent.service';
import { DialogService } from '@core/services/dialog.service';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { AgentBulkActionEventsTableConfig } from './agent-bulk-action-events-table-config';

@Component({
  selector: 'tb-agent-bulk-action-event-table',
  template: '<tb-entities-table *ngIf="tableConfig" [entitiesTableConfig]="tableConfig"></tb-entities-table>',
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
export class AgentBulkActionEventTableComponent implements OnInit, AfterViewInit, OnChanges {

  @Input() bulkActionId: string;

  @ViewChild(EntitiesTableComponent, { static: true }) entitiesTable: EntitiesTableComponent;

  tableConfig: AgentBulkActionEventsTableConfig;

  constructor(private agentService: AgentService,
              private dialogService: DialogService,
              private dialog: MatDialog,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef) {}

  ngOnInit(): void {
    this.rebuild();
  }

  ngAfterViewInit(): void {
    // no-op; tableConfig is built in ngOnInit so the first change-detection
    // pass already has a non-undefined value to bind.
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.bulkActionId && !changes.bulkActionId.firstChange) {
      this.rebuild();
    }
  }

  refresh(): void {
    if (this.tableConfig) {
      this.tableConfig.updateData();
    }
  }

  private rebuild(): void {
    if (!this.bulkActionId) { return; }
    this.tableConfig = new AgentBulkActionEventsTableConfig(
      this.bulkActionId,
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
