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

import { Injectable, NgZone } from '@angular/core';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import {
  CellActionDescriptor,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig,
  GroupActionDescriptor,
  HeaderActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { AddEntityDialogData, EntityAction } from '@home/models/entity/entity-component.models';
import { forkJoin, Observable, of } from 'rxjs';
import { select, Store } from '@ngrx/store';
import { selectAuthUser } from '@core/auth/auth.selectors';
import { map, mergeMap, take, tap } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';
import { CustomerService } from '@core/http/customer.service';
import { Customer } from '@shared/models/customer.model';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import {
  AssignToCustomerDialogComponent,
  AssignToCustomerDialogData
} from '@modules/home/dialogs/assign-to-customer-dialog.component';
import {
  AddEntitiesToCustomerDialogComponent,
  AddEntitiesToCustomerDialogData
} from '@modules/home/dialogs/add-entities-to-customer-dialog.component';
import { AgentInfo } from '@shared/models/agent.models';
import { AgentId } from '@shared/models/id/agent-id';
import { AgentService } from '@core/http/agent.service';
import { AgentComponent } from '@home/pages/agent/agent.component';
import { AgentTabsComponent } from '@home/pages/agent/agent-tabs.component';
import {
  AgentInstallInstructionsDialogComponent,
  AgentInstallInstructionsDialogData
} from '@home/pages/agent/agent-install-instructions-dialog.component';
import {
  AddEntityDialogComponent
} from '@home/components/entity/add-entity-dialog.component';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import {
  AttributeScope,
  TelemetrySubscriber
} from '@shared/models/telemetry/telemetry.models';

@Injectable()
export class AgentsTableConfigResolver {

  private readonly config: EntityTableConfig<AgentInfo> = new EntityTableConfig<AgentInfo>();
  private customerId: string;
  private activeSubs = new Map<string, TelemetrySubscriber>();
  private activeStates = new Map<string, boolean>();

  constructor(private store: Store<AppState>,
              private agentService: AgentService,
              private customerService: CustomerService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private dialog: MatDialog,
              private telemetryWsService: TelemetryWebsocketService,
              private zone: NgZone) {

    this.config.entityType = EntityType.AGENT;
    this.config.entityComponent = AgentComponent;
    this.config.entityTabsComponent = AgentTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.AGENT);
    this.config.entityResources = entityTypeResources.get(EntityType.AGENT);

    this.config.deleteEntityTitle = agent => this.translate.instant('agent.delete-agent-title', {agentName: agent.name});
    this.config.deleteEntityContent = () => this.translate.instant('agent.delete-agent-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('agent.delete-agents-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('agent.delete-agents-text');

    this.config.loadEntity = id => this.agentService.getAgentInfoById(id.id);
    this.config.saveEntity = agent => {
      return this.agentService.saveAgent(agent).pipe(
        mergeMap((savedAgent) => this.agentService.getAgentInfoById(savedAgent.id.id))
      );
    };
    this.config.onEntityAction = action => this.onAgentAction(action, this.config);
    this.config.detailsReadonly = () => this.config.componentsData.agentScope === 'customer_user';
    this.config.addEntity = () => { this.addAgent(); return of(null); };
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<AgentInfo>> {
    const routeParams = route.params;
    this.config.componentsData = {
      agentScope: route.data.agentsType
    };
    this.customerId = routeParams.customerId;
    return this.store.pipe(select(selectAuthUser), take(1)).pipe(
      tap((authUser) => {
        if (authUser.authority === Authority.CUSTOMER_USER) {
          this.config.componentsData.agentScope = 'customer_user';
          this.customerId = authUser.customerId;
        }
      }),
      mergeMap(() =>
        this.customerId ? this.customerService.getCustomer(this.customerId) : of(null as Customer)
      ),
      map((parentCustomer) => {
        if (parentCustomer) {
          this.config.tableTitle = parentCustomer.title + ': ' + this.translate.instant('agent.agents');
        } else {
          this.config.tableTitle = this.translate.instant('agent.agents');
        }
        this.config.columns = this.configureColumns(this.config.componentsData.agentScope);
        this.configureEntityFunctions(this.config.componentsData.agentScope);
        this.config.cellActionDescriptors = this.configureCellActions(this.config.componentsData.agentScope);
        this.config.groupActionDescriptors = this.configureGroupActions(this.config.componentsData.agentScope);
        this.config.addActionDescriptors = this.configureAddActions();
        this.config.addEnabled = this.config.componentsData.agentScope !== 'customer_user';
        this.config.entitiesDeleteEnabled = this.config.componentsData.agentScope === 'tenant';
        this.config.deleteEnabled = () => this.config.componentsData.agentScope === 'tenant';
        return this.config;
      })
    );
  }

  configureColumns(agentScope: string): Array<EntityTableColumn<AgentInfo>> {
    const columns: Array<EntityTableColumn<AgentInfo>> = [
      new DateEntityTableColumn<AgentInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<AgentInfo>('name', 'agent.name', '25%'),
      new EntityTableColumn<AgentInfo>('groupName', 'agent.agent-group', '20%',
        entity => this.groupCell(entity), () => ({}), false)
    ];
    if (agentScope === 'tenant' || agentScope === 'customer') {
      columns.push(
        new EntityTableColumn<AgentInfo>('customerTitle', 'customer.customer', '20%'),
      );
    }
    columns.push(
      new EntityTableColumn<AgentInfo>('active', 'agent.status', '140px',
        entity => this.agentStatus(entity), entity => this.agentStatusStyle(entity), false)
    );
    return columns;
  }

  private groupCell(agent: AgentInfo): string {
    if (!agent.groupName || !agent.agentGroupId?.id) {
      return `<span style="color:rgba(0,0,0,0.38);font-size:12px;">—</span>`;
    }
    const href = `/edgeManagement/agentProfiles/${agent.agentGroupId.id}`;
    const safeName = String(agent.groupName).replace(/</g, '&lt;').replace(/>/g, '&gt;');
    return `<a href="${href}" onclick="event.stopPropagation();" style="color:#305680;font-weight:500;text-decoration:none;">${safeName}</a>`;
  }

  private agentStatus(agent: AgentInfo): string {
    const isOnline = !!agent.active;
    const dotColor = isOnline ? '#4caf50' : 'rgba(0,0,0,0.38)';
    const color = isOnline ? '#4caf50' : 'rgba(0,0,0,0.38)';
    const label = this.translate.instant(isOnline ? 'agent.online' : 'agent.offline');
    return `<span class="tb-agent-status-cell" data-agent-id="${agent.id.id}"
      style="display:inline-flex; align-items:center; white-space:nowrap; font-size:13px; font-weight:500; color:${color};">
      <span class="dot" style="display:inline-block; width:8px; height:8px; border-radius:50%; margin-right:6px; background:${dotColor};"></span>
      <span class="label">${label}</span>
    </span>`;
  }

  private agentStatusStyle(agent: AgentInfo): object {
    return {
      fontSize: '13px',
      fontWeight: '500'
    };
  }

  private updateAgentStatusDom(agentId: string, active: boolean) {
    const nodes = document.querySelectorAll(
      `.tb-agent-status-cell[data-agent-id="${agentId}"]`);
    if (!nodes || nodes.length === 0) {
      return;
    }
    const color = active ? '#4caf50' : 'rgba(0,0,0,0.38)';
    const label = this.translate.instant(active ? 'agent.online' : 'agent.offline');
    nodes.forEach(node => {
      const el = node as HTMLElement;
      el.style.color = color;
      const dot = el.querySelector('.dot') as HTMLElement | null;
      if (dot) {
        dot.style.background = color;
      }
      const lbl = el.querySelector('.label');
      if (lbl) {
        lbl.textContent = label;
      }
    });
  }

  private subscribeAgentActive(agent: AgentInfo) {
    const id = agent.id.id;
    // Seed the fresh AgentInfo with the last known state so its initial
    // render picks the right colour even before the WS replays.
    const cached = this.activeStates.get(id);
    if (cached !== undefined) {
      agent.active = cached;
    }
    if (this.activeSubs.has(id)) {
      return;
    }
    const subscriber = TelemetrySubscriber.createEntityAttributesSubscription(
      this.telemetryWsService,
      agent.id,
      AttributeScope.SERVER_SCOPE,
      this.zone,
      ['active']
    );
    subscriber.data$.subscribe(update => {
      if (!update || !update.data) {
        return;
      }
      const activeEntries = update.data['active'];
      if (activeEntries && activeEntries.length) {
        const rawValue = activeEntries[0][1];
        const active = rawValue === true || rawValue === 'true';
        this.activeStates.set(id, active);
        agent.active = active;
        this.zone.run(() => this.updateAgentStatusDom(id, active));
      }
    });
    subscriber.subscribe();
    this.activeSubs.set(id, subscriber);
  }

  private reconcileActiveSubscriptions(agents: AgentInfo[]) {
    const visibleIds = new Set(agents.map(a => a.id.id));
    // Unsubscribe from agents that are no longer on the page.
    this.activeSubs.forEach((sub, id) => {
      if (!visibleIds.has(id)) {
        sub.unsubscribe();
        sub.complete();
        this.activeSubs.delete(id);
        this.activeStates.delete(id);
      }
    });
    agents.forEach(a => this.subscribeAgentActive(a));
    // Defer until Angular has rendered the fresh rows, then patch the DOM
    // with the latest known state for every visible agent. This is what
    // brings the indicators back after navigating away and returning —
    // otherwise the already-open subscription only re-emits on change.
    setTimeout(() => {
      agents.forEach(a => {
        const id = a.id.id;
        const known = this.activeStates.get(id);
        if (known !== undefined) {
          this.updateAgentStatusDom(id, known);
        }
      });
    }, 0);
  }

  configureEntityFunctions(agentScope: string): void {
    if (agentScope === 'tenant') {
      this.config.entitiesFetchFunction = pageLink =>
        this.agentService.getTenantAgentInfos(pageLink).pipe(tap(page =>
          this.reconcileActiveSubscriptions(page.data)
        ));
      this.config.deleteEntity = id => this.agentService.deleteAgent(id.id);
    }
    if (agentScope === 'customer') {
      this.config.entitiesFetchFunction = pageLink =>
        this.agentService.getCustomerAgentInfos(this.customerId, pageLink).pipe(tap(page =>
          this.reconcileActiveSubscriptions(page.data)
        ));
      this.config.deleteEntity = id => this.agentService.unassignAgentFromCustomer(id.id);
    }
    if (agentScope === 'customer_user') {
      this.config.entitiesFetchFunction = pageLink =>
        this.agentService.getCustomerAgentInfos(this.customerId, pageLink).pipe(tap(page =>
          this.reconcileActiveSubscriptions(page.data)
        ));
    }
  }

  configureCellActions(agentScope: string): Array<CellActionDescriptor<AgentInfo>> {
    const actions: Array<CellActionDescriptor<AgentInfo>> = [];
    if (agentScope === 'tenant') {
      actions.push({
        name: this.translate.instant('agent.assign-to-customer'),
        icon: 'assignment_ind',
        isEnabled: (entity) => !entity.customerId || entity.customerId.id === NULL_UUID,
        onAction: ($event, entity) => this.assignToCustomer($event, [entity.id])
      });
      actions.push({
        name: this.translate.instant('agent.unassign-from-customer'),
        icon: 'assignment_return',
        isEnabled: (entity) => entity.customerId && entity.customerId.id !== NULL_UUID,
        onAction: ($event, entity) => this.unassignFromCustomer($event, entity)
      });
    }
    if (agentScope === 'customer') {
      actions.push({
        name: this.translate.instant('agent.unassign-from-customer'),
        icon: 'assignment_return',
        isEnabled: () => true,
        onAction: ($event, entity) => this.unassignFromCustomer($event, entity)
      });
    }
    return actions;
  }

  configureGroupActions(agentScope: string): Array<GroupActionDescriptor<AgentInfo>> {
    const actions: Array<GroupActionDescriptor<AgentInfo>> = [];
    if (agentScope === 'tenant') {
      actions.push({
        name: this.translate.instant('agent.assign-agents-to-customer'),
        icon: 'assignment_ind',
        isEnabled: true,
        onAction: ($event, entities) => this.assignToCustomer($event, entities.map(e => e.id))
      });
    }
    if (agentScope === 'customer') {
      actions.push({
        name: this.translate.instant('agent.unassign-agents-from-customer'),
        icon: 'assignment_return',
        isEnabled: true,
        onAction: ($event, entities) =>
          this.unassignAgentsFromCustomer($event, entities)
      });
    }
    return actions;
  }

  configureAddActions(): Array<HeaderActionDescriptor> {
    return [];
  }

  assignToCustomer($event: Event, agentIds: AgentId[]) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AssignToCustomerDialogComponent, AssignToCustomerDialogData>(
      AssignToCustomerDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          entityIds: agentIds,
          entityType: EntityType.AGENT
        } as AssignToCustomerDialogData
      }).afterClosed().subscribe((res) => {
      if (res) {
        this.config.updateData();
      }
    });
  }

  unassignFromCustomer($event: Event, agent: AgentInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('agent.unassign-agent-title', {agentName: agent.name}),
      this.translate.instant('agent.unassign-agent-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
      if (res) {
        this.agentService.unassignAgentFromCustomer(agent.id.id).subscribe(() => {
          this.config.updateData();
        });
      }
    });
  }

  unassignAgentsFromCustomer($event: Event, agents: AgentInfo[]) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('agent.unassign-agents-title', {count: agents.length}),
      this.translate.instant('agent.unassign-agents-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
      if (res) {
        const tasks = agents.map(agent => this.agentService.unassignAgentFromCustomer(agent.id.id));
        forkJoin(tasks).subscribe(() => {
          this.config.updateData();
        });
      }
    });
  }

  addAgent() {
    this.dialog.open<AddEntityDialogComponent, AddEntityDialogData<AgentInfo>, AgentInfo>(
      AddEntityDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          entitiesTableConfig: this.config
        }
      }).afterClosed().subscribe((entity) => {
        if (entity) {
          this.openInstallInstructions(null, entity, true);
        }
      });
  }

  openInstallInstructions($event: Event, agent: AgentInfo, afterAdd = false) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AgentInstallInstructionsDialogComponent, AgentInstallInstructionsDialogData>(
      AgentInstallInstructionsDialogComponent, {
        disableClose: false,
        panelClass: ['tb-dialog'],
        data: {
          agent,
          afterAdd
        }
      }).afterClosed().subscribe(() => {
        if (afterAdd) {
          this.config.updateData();
          this.config.entityAdded(agent);
        }
      });
  }

  onAgentAction(action: EntityAction<AgentInfo>, config: EntityTableConfig<AgentInfo>): boolean {
    switch (action.action) {
      case 'assignToCustomer':
        this.assignToCustomer(action.event, [action.entity.id]);
        return true;
      case 'unassignFromCustomer':
        this.unassignFromCustomer(action.event, action.entity);
        return true;
      case 'openInstallInstructions':
        this.openInstallInstructions(action.event, action.entity);
        return true;
    }
    return false;
  }
}
