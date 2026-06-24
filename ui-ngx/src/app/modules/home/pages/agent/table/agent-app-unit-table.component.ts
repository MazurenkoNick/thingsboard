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

import { AfterViewInit, Component, Input, NgZone, OnChanges, OnDestroy, SimpleChanges, ViewChild, ViewContainerRef } from '@angular/core';
import { Overlay } from '@angular/cdk/overlay';
import { Router } from '@angular/router';
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
              private zone: NgZone,
              private router: Router) {}

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
      this.zone,
      this.router
    );
  }
}
