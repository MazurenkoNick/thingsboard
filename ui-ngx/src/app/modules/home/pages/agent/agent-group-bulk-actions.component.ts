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

import {
  AfterViewInit,
  Component,
  Input,
  OnChanges,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { Router } from '@angular/router';
import { AgentService } from '@core/http/agent.service';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { AgentGroupInfo } from '@shared/models/agent.models';
import { AgentGroupBulkActionsTableConfig } from '@home/pages/agent/table/agent-group-bulk-actions-table-config';

@Component({
  selector: 'tb-agent-group-bulk-actions',
  template: '<tb-entities-table [entitiesTableConfig]="tableConfig"></tb-entities-table>',
  styles: [':host { display: block; height: 100%; }'],
  standalone: false
})
export class AgentGroupBulkActionsComponent implements AfterViewInit, OnChanges {

  @Input() group: AgentGroupInfo;
  @Input() active: boolean;

  @ViewChild(EntitiesTableComponent, { static: true }) entitiesTable: EntitiesTableComponent;

  tableConfig: AgentGroupBulkActionsTableConfig;

  constructor(private agentService: AgentService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router) {}

  ngAfterViewInit(): void {
    this.rebuild();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.group && !changes.group.firstChange) {
      this.rebuild();
    }
    if (changes.active && this.active && !changes.active.firstChange && this.tableConfig) {
      this.tableConfig.updateData();
    }
  }

  private rebuild(): void {
    if (!this.group) { return; }
    this.tableConfig = new AgentGroupBulkActionsTableConfig(
      this.group,
      this.agentService,
      this.translate,
      this.datePipe,
      this.router
    );
  }
}
