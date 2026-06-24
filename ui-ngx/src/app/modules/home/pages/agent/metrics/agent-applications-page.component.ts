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
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  NgZone,
  OnDestroy,
  OnInit,
  ViewChild
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MatDrawer } from '@angular/material/sidenav';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { AgentService } from '@core/http/agent.service';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { AgentId } from '@shared/models/id/agent-id';
import { AgentApplicationId } from '@shared/models/id/agent-application-id';
import { AgentMetricsSubscription } from '@home/pages/agent/util/agent-metrics-subscription';
import { MetricsSnapshot } from '@home/pages/agent/util/agent-metrics';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { MetricsEntityRef } from './agent-multi-entity-metrics-panel.component';

@Component({
  selector: 'tb-agent-applications-page',
  templateUrl: './agent-applications-page.component.html',
  styleUrls: ['./agent-applications-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class AgentApplicationsPageComponent implements OnInit, OnDestroy {

  @ViewChild('chartsDrawer') chartsDrawer!: MatDrawer;

  entitiesTableConfig: EntityTableConfig<any> | null = null;
  agentEntityId: AgentId | null = null;
  snapshot: MetricsSnapshot = AgentMetricsSubscription.empty();
  chartsOpen = false;
  entities: MetricsEntityRef[] = [];

  private destroy$ = new Subject<void>();
  private subscription: AgentMetricsSubscription;
  private loadedAppsForAgentId: string | null = null;

  constructor(private route: ActivatedRoute,
              private telemetryWsService: TelemetryWebsocketService,
              private zone: NgZone,
              private cdr: ChangeDetectorRef,
              private agentService: AgentService) {
    this.subscription = new AgentMetricsSubscription(telemetryWsService, zone);
  }

  ngOnInit(): void {
    this.route.data.pipe(takeUntil(this.destroy$)).subscribe(data => {
      const config = data.entitiesTableConfig as EntityTableConfig<any> | undefined;
      if (!config) {
        return;
      }
      this.entitiesTableConfig = config;
      const componentsData = (config.componentsData as { agentId?: string }) || {};
      const newAgentId = componentsData.agentId ? new AgentId(componentsData.agentId) : null;
      const changed = newAgentId?.id !== this.agentEntityId?.id;
      this.agentEntityId = newAgentId;
      this.cdr.markForCheck();
      if (changed) {
        this.subscription.tearDown();
        this.entities = [];
        this.loadedAppsForAgentId = null;
        if (this.agentEntityId) {
          this.subscription.snapshot$.pipe(takeUntil(this.destroy$)).subscribe(snap => {
            this.snapshot = snap;
            this.cdr.markForCheck();
          });
          this.subscription.subscribe(this.agentEntityId, this.agentEntityId);
          this.fetchApplications(this.agentEntityId.id);
        }
      }
    });
  }

  private fetchApplications(agentId: string): void {
    if (this.loadedAppsForAgentId === agentId) { return; }
    this.loadedAppsForAgentId = agentId;
    const pageLink = new PageLink(200, 0, null,
      { property: 'name', direction: Direction.ASC });
    this.agentService.getAgentApplicationsByAgentId(agentId, pageLink,
      { ignoreLoading: true, ignoreErrors: true } as any)
      .pipe(takeUntil(this.destroy$))
      .subscribe(page => {
        this.entities = page.data.map(app => ({
          entityId: new AgentApplicationId(app.id.id),
          label: app.name
        }));
        this.cdr.markForCheck();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.subscription.tearDown();
  }

  toggleCharts(): void {
    this.chartsOpen = !this.chartsOpen;
    this.cdr.markForCheck();
  }

  closeCharts(): void {
    if (this.chartsOpen) {
      this.chartsOpen = false;
      this.cdr.markForCheck();
    }
  }
}
