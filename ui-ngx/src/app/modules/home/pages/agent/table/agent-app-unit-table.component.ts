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

import { AfterViewInit, Component, Input, NgZone, OnChanges, OnDestroy, SimpleChanges, ViewChild, ViewContainerRef } from '@angular/core';
import { Overlay } from '@angular/cdk/overlay';
import { TranslateService } from '@ngx-translate/core';

import { AgentService } from '@core/http/agent.service';
import { AttributeService } from '@core/http/attribute.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { AgentApplicationInfo } from '@shared/models/agent.models';
import { AgentAppUnitTableConfig } from './agent-app-unit-table-config';

@Component({
  selector: 'tb-agent-app-unit-table',
  template: '<tb-entities-table [entitiesTableConfig]="tableConfig"></tb-entities-table>',
  styles: [':host { display: block; height: 100%; }'],
  standalone: false
})
export class AgentAppUnitTableComponent implements AfterViewInit, OnChanges, OnDestroy {

  @Input() application: AgentApplicationInfo;
  @Input() active: boolean;

  @ViewChild(EntitiesTableComponent, { static: true }) entitiesTable: EntitiesTableComponent;

  tableConfig: AgentAppUnitTableConfig;

  constructor(private agentService: AgentService,
              private attributeService: AttributeService,
              private translate: TranslateService,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef,
              private telemetryWsService: TelemetryWebsocketService,
              private zone: NgZone) {}

  ngAfterViewInit(): void {
    this.rebuild();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.application && !changes.application.firstChange) {
      this.rebuild();
    }
  }

  ngOnDestroy(): void {
    this.tableConfig?.destroySubscriptions();
  }

  private rebuild(): void {
    if (!this.application) { return; }
    this.tableConfig?.destroySubscriptions();
    this.tableConfig = new AgentAppUnitTableConfig(
      this.application,
      this.agentService,
      this.attributeService,
      this.translate,
      this.overlay,
      this.viewContainerRef,
      this.telemetryWsService,
      this.zone
    );
  }
}
