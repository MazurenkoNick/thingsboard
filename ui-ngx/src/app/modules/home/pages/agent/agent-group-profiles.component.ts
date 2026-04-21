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
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { AgentService } from '@core/http/agent.service';
import { DialogService } from '@core/services/dialog.service';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { AgentGroupInfo } from '@shared/models/agent.models';
import { AgentGroupProfilesTableConfig } from '@home/pages/agent/table/agent-group-profiles-table-config';

@Component({
  selector: 'tb-agent-group-profiles',
  template: `
    <div class="tb-agent-group-profiles-host">
      <div class="tb-auto-install-note">
        <mat-icon>info_outline</mat-icon>
        <span translate>agent.auto-install-template-dedup-note</span>
      </div>
      <tb-entities-table [entitiesTableConfig]="tableConfig"></tb-entities-table>
    </div>
  `,
  styles: [`
    :host { display: block; height: 100%; }
    .tb-agent-group-profiles-host {
      display: flex;
      flex-direction: column;
      height: 100%;
    }
    .tb-auto-install-note {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      padding: 10px 16px;
      margin: 12px 16px 0;
      border-radius: 4px;
      background: rgba(48, 86, 128, 0.06);
      color: rgba(0, 0, 0, 0.72);
      font-size: 13px;
      line-height: 1.5;
    }
    .tb-auto-install-note mat-icon {
      flex-shrink: 0;
      font-size: 20px;
      width: 20px;
      height: 20px;
      color: #305680;
    }
    tb-entities-table { flex: 1; }
  `],
  standalone: false
})
export class AgentGroupProfilesComponent implements AfterViewInit, OnChanges {

  @Input() group: AgentGroupInfo;
  @Input() active: boolean;

  @ViewChild(EntitiesTableComponent, { static: true }) entitiesTable: EntitiesTableComponent;

  tableConfig: AgentGroupProfilesTableConfig;

  constructor(private agentService: AgentService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private dialogService: DialogService,
              private router: Router) {}

  ngAfterViewInit(): void {
    this.rebuild();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.group && !changes.group.firstChange) {
      this.rebuild();
    }
  }

  private rebuild(): void {
    if (!this.group) { return; }
    this.tableConfig = new AgentGroupProfilesTableConfig(
      this.group,
      this.agentService,
      this.translate,
      this.dialog,
      this.dialogService,
      this.router
    );
  }
}
