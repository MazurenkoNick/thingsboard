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

import { Injectable, NgZone } from '@angular/core';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { agentEntityUrl, currentAgentRouteSnapshot } from '@home/pages/agent/util/agent-route-params';
import {
  CellActionDescriptor,
  DateEntityTableColumn,
  EntityChipsEntityTableColumn,
  EntityLinkTableColumn,
  EntityColumn,
  EntityTableColumn,
  EntityTableConfig,
  GroupActionDescriptor,
  HeaderActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { AddEntityDialogData, EntityAction } from '@home/models/entity/entity-component.models';
import { Observable, of } from 'rxjs';
import { select, Store } from '@ngrx/store';
import { selectAuthUser } from '@core/auth/auth.selectors';
import { map, mergeMap, take, tap } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';
import { CustomerService } from '@core/http/customer.service';
import { Customer } from '@shared/models/customer.model';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { AgentInfo } from '@shared/models/agent.models';
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
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { resolveGroupParams } from '@shared/models/entity-group.models';
import { AllEntitiesTableConfigService } from '@home/components/entity/all-entities-table-config.service';
import { GroupEntityTabsComponent } from '@home/components/group/group-entity-tabs.component';
import { AgentAutoProvisionDialogService } from '@home/pages/edge/agent-auto-provision-dialog.service';

@Injectable()
export class AgentsTableConfigResolver {

  // Telemetry subscriptions + cached online state are kept on the resolver
  // instance (singleton). A fresh EntityTableConfig is built per resolve(),
  // so the subscriptions must outlive any particular table-config instance.
  private activeSubs = new Map<string, TelemetrySubscriber>();
  private activeStates = new Map<string, boolean>();

  constructor(private allEntitiesTableConfigService: AllEntitiesTableConfigService<AgentInfo>,
              private store: Store<AppState>,
              private agentService: AgentService,
              private customerService: CustomerService,
              private dialogService: DialogService,
              private homeDialogs: HomeDialogsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private dialog: MatDialog,
              private telemetryWsService: TelemetryWebsocketService,
              private autoProvisionDialogService: AgentAutoProvisionDialogService,
              private zone: NgZone) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<AgentInfo>> {
    const groupParams = resolveGroupParams(route);
    const config = new EntityTableConfig<AgentInfo>(groupParams);
    this.configDefaults(config, route);

    const customerId = config.customerId;
    return this.store.pipe(select(selectAuthUser), take(1)).pipe(
      tap((authUser) => {
        if (authUser.authority === Authority.CUSTOMER_USER) {
          config.componentsData.agentScope = 'customer_user';
          config.componentsData.customerId = authUser.customerId;
        } else if (customerId) {
          config.componentsData.agentScope = 'customer';
          config.componentsData.customerId = customerId;
        }
      }),
      mergeMap(() =>
        config.componentsData.customerId
          ? this.customerService.getCustomer(config.componentsData.customerId)
          : of(null as Customer)
      ),
      map((parentCustomer) => {
        if (parentCustomer) {
          config.tableTitle = parentCustomer.title + ': ' + this.translate.instant('agent.agents');
        } else {
          config.tableTitle = this.translate.instant('agent.agents');
        }
        config.columns = this.configureColumns(config);
        this.configureEntityFunctions(config);
        config.cellActionDescriptors = this.configureCellActions(config);
        config.groupActionDescriptors = this.configureGroupActions();
        config.addActionDescriptors = this.configureAddActions(config);
        config.addEnabled = config.componentsData.agentScope !== 'customer_user';
        config.entitiesDeleteEnabled = config.componentsData.agentScope === 'tenant';
        config.deleteEnabled = () => config.componentsData.agentScope === 'tenant';
        // This resolver is a singleton, so its per-agent telemetry
        // subscriptions would otherwise stay open after navigating away from
        // the table. Tear them all down when the table is destroyed.
        config.onDestroy = () => this.destroyActiveSubscriptions();
        return this.allEntitiesTableConfigService.prepareConfiguration(config);
      })
    );
  }

  configDefaults(config: EntityTableConfig<AgentInfo>, route: ActivatedRouteSnapshot) {
    config.entityType = EntityType.AGENT;
    config.entityComponent = AgentComponent;
    config.entityTabsComponent = AgentTabsComponent;
    // When inside a group-scoped leaf, the group tabs component is used by the
    // group container; for the flat /all list we keep the agent-specific tabs.
    if ((route.data as any)?.groupType && (route.data as any)?.hideTabs) {
      config.entityTabsComponent = GroupEntityTabsComponent<AgentInfo>;
    }
    config.entityTranslations = entityTypeTranslations.get(EntityType.AGENT);
    config.entityResources = entityTypeResources.get(EntityType.AGENT);

    config.entityTitle = (agent) => agent ? agent.name : '';
    config.rowPointer = true;

    config.deleteEntityTitle = agent => this.translate.instant('agent.delete-agent-title', {agentName: agent.name});
    config.deleteEntityContent = () => this.translate.instant('agent.delete-agent-text');
    config.deleteEntitiesTitle = count => this.translate.instant('agent.delete-agents-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('agent.delete-agents-text');

    config.loadEntity = id => this.agentService.getAgentInfoById(id.id);
    config.saveEntity = agent => this.agentService.saveAgent(agent).pipe(
      mergeMap((savedAgent) => this.agentService.getAgentInfoById(savedAgent.id.id))
    );
    config.onEntityAction = action => this.onAgentAction(action, config);
    config.handleRowClick = ($event, agent) => {
      this.manageApplications($event, agent);
      return true;
    };
    config.detailsReadonly = () => config.componentsData?.agentScope === 'customer_user';
    config.addEntity = () => { this.addAgent(config); return of(null); };

    // Default scope comes from route.data (e.g. 'tenant' under /all) and is
    // overridden below if the current user is a customer user, or if the
    // route belongs to a customer-scoped hierarchy view (config.customerId).
    config.componentsData = {
      agentScope: route.data?.agentsType || 'tenant',
      customerId: null as string | null
    };
  }

  configureColumns(config: EntityTableConfig<AgentInfo>): Array<EntityColumn<AgentInfo>> {
    const scope = config.componentsData.agentScope;
    const columns: Array<EntityColumn<AgentInfo>> = [
      new DateEntityTableColumn<AgentInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<AgentInfo>('name', 'agent.name', '20%', config.entityTitle),
      new EntityLinkTableColumn<AgentInfo>('agentProfileName', 'agent.agent-profile', '20%',
        entity => entity.agentProfileName || '—',
        entity => entity.agentProfileId?.id
          ? `/edgeManagement/profiles/agent/${entity.agentProfileId.id}`
          : '',
        false)
    ];
    if (scope === 'tenant' || scope === 'customer') {
      columns.push(
        new EntityTableColumn<AgentInfo>('customerTitle', 'customer.customer', '15%')
      );
    }
    columns.push(
      new EntityChipsEntityTableColumn<AgentInfo>('groups', 'entity.groups', '25%')
    );
    columns.push(
      new EntityTableColumn<AgentInfo>('active', 'agent.status', '140px',
        entity => this.agentStatus(entity), entity => this.agentStatusStyle(entity), false)
    );
    return columns;
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

  private agentStatusStyle(_agent: AgentInfo): object {
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

  private destroyActiveSubscriptions() {
    this.activeSubs.forEach(sub => {
      sub.unsubscribe();
      sub.complete();
    });
    this.activeSubs.clear();
    this.activeStates.clear();
  }

  private reconcileActiveSubscriptions(agents: AgentInfo[]) {
    const visibleIds = new Set(agents.map(a => a.id.id));
    this.activeSubs.forEach((sub, id) => {
      if (!visibleIds.has(id)) {
        sub.unsubscribe();
        sub.complete();
        this.activeSubs.delete(id);
        this.activeStates.delete(id);
      }
    });
    agents.forEach(a => this.subscribeAgentActive(a));
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

  configureEntityFunctions(config: EntityTableConfig<AgentInfo>): void {
    const scope = config.componentsData.agentScope;
    if (scope === 'tenant') {
      config.entitiesFetchFunction = pageLink =>
        this.agentService.getTenantAgentInfos(pageLink).pipe(tap(page =>
          this.reconcileActiveSubscriptions(page.data)
        ));
      config.deleteEntity = id => this.agentService.deleteAgent(id.id);
    }
    if (scope === 'customer' || scope === 'customer_user') {
      config.entitiesFetchFunction = pageLink =>
        this.agentService.getCustomerAgentInfos(config.componentsData.customerId, pageLink).pipe(tap(page =>
          this.reconcileActiveSubscriptions(page.data)
        ));
    }
  }

  configureCellActions(config: EntityTableConfig<AgentInfo>): Array<CellActionDescriptor<AgentInfo>> {
    return [
      {
        name: this.translate.instant('agent.agent-details'),
        icon: 'edit',
        isEnabled: () => true,
        onAction: ($event, agent) => config.toggleEntityDetails($event, agent)
      }
    ];
  }

  private manageApplications($event: Event, agent: AgentInfo) {
    if ($event) { $event.stopPropagation(); }
    this.router.navigateByUrl(agentEntityUrl(currentAgentRouteSnapshot(this.router), agent.id.id, 'applications'));
  }

  private openAgent($event: Event, agent: AgentInfo, config: EntityTableConfig<AgentInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([agent.id.id], {relativeTo: config.getActivatedRoute()});
    this.router.navigateByUrl(url);
  }

  configureGroupActions(): Array<GroupActionDescriptor<AgentInfo>> {
    return [];
  }

  configureAddActions(config: EntityTableConfig<AgentInfo>): Array<HeaderActionDescriptor> {
    return [
      {
        name: this.translate.instant('agent.add-agent-text'),
        icon: 'insert_drive_file',
        isEnabled: () => true,
        onAction: ($event) => config.getTable().addEntity($event)
      },
      {
        name: this.translate.instant('agent.auto-provision'),
        icon: 'auto_fix_high',
        isEnabled: () => true,
        onAction: ($event) => this.autoProvisionAgent($event, config)
      }
    ];
  }

  private autoProvisionAgent($event: Event, config: EntityTableConfig<AgentInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.autoProvisionDialogService.open().subscribe(result => {
      if (result) {
        config.updateData();
      }
    });
  }

  private addAgent(config: EntityTableConfig<AgentInfo>) {
    this.dialog.open<AddEntityDialogComponent, AddEntityDialogData<AgentInfo>, AgentInfo>(
      AddEntityDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          entitiesTableConfig: config
        }
      }).afterClosed().subscribe((entity) => {
        if (entity) {
          this.openInstallInstructions(null, entity, true, config);
        }
      });
  }

  openInstallInstructions($event: Event, agent: AgentInfo, afterAdd = false, config?: EntityTableConfig<AgentInfo>) {
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
        if (afterAdd && config) {
          config.updateData();
          config.entityAdded(agent);
        }
      });
  }

  manageOwnerAndGroups($event: Event, agent: AgentInfo, config: EntityTableConfig<AgentInfo>) {
    this.homeDialogs.manageOwnerAndGroups($event, agent).subscribe(res => {
      if (res) {
        config.updateData();
      }
    });
  }

  onAgentAction(action: EntityAction<AgentInfo>, config: EntityTableConfig<AgentInfo>): boolean {
    switch (action.action) {
      case 'open':
        this.openAgent(action.event, action.entity, config);
        return true;
      case 'openInstallInstructions':
        this.openInstallInstructions(action.event, action.entity, false, config);
        return true;
      case 'manageOwnerAndGroups':
        this.manageOwnerAndGroups(action.event, action.entity, config);
        return true;
    }
    return false;
  }
}
