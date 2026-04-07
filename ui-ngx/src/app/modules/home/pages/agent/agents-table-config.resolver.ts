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

import { Injectable } from '@angular/core';
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
import { EntityAction } from '@home/models/entity/entity-component.models';
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
import { AgentService } from '@core/http/agent.service';
import { AgentComponent } from '@home/pages/agent/agent.component';
import { AgentTabsComponent } from '@home/pages/agent/agent-tabs.component';

@Injectable()
export class AgentsTableConfigResolver {

  private readonly config: EntityTableConfig<AgentInfo> = new EntityTableConfig<AgentInfo>();
  private customerId: string;

  constructor(private store: Store<AppState>,
              private agentService: AgentService,
              private customerService: CustomerService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private dialog: MatDialog) {

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
      new EntityTableColumn<AgentInfo>('name', 'agent.name', '33%'),
    ];
    if (agentScope === 'tenant') {
      columns.push(
        new EntityTableColumn<AgentInfo>('customerTitle', 'customer.customer', '33%'),
      );
    }
    return columns;
  }

  configureEntityFunctions(agentScope: string): void {
    if (agentScope === 'tenant') {
      this.config.entitiesFetchFunction = pageLink =>
        this.agentService.getTenantAgentInfos(pageLink);
      this.config.deleteEntity = id => this.agentService.deleteAgent(id.id);
    }
    if (agentScope === 'customer') {
      this.config.entitiesFetchFunction = pageLink =>
        this.agentService.getCustomerAgentInfos(this.customerId, pageLink);
      this.config.deleteEntity = id => this.agentService.unassignAgentFromCustomer(id.id);
    }
    if (agentScope === 'customer_user') {
      this.config.entitiesFetchFunction = pageLink =>
        this.agentService.getCustomerAgentInfos(this.customerId, pageLink);
    }
  }

  configureCellActions(agentScope: string): Array<CellActionDescriptor<AgentInfo>> {
    const actions: Array<CellActionDescriptor<AgentInfo>> = [];
    if (agentScope === 'tenant') {
      actions.push({
        name: this.translate.instant('agent.assign-to-customer'),
        icon: 'assignment_ind',
        isEnabled: (entity) => !entity.customerId || entity.customerId.id === NULL_UUID,
        onAction: ($event, entity) => this.assignToCustomer($event, [entity.id.id])
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
        onAction: ($event, entities) => this.assignToCustomer($event, entities.map(e => e.id.id))
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

  assignToCustomer($event: Event, agentIds: string[]) {
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

  onAgentAction(action: EntityAction<AgentInfo>, config: EntityTableConfig<AgentInfo>): boolean {
    switch (action.action) {
      case 'assignToCustomer':
        this.assignToCustomer(action.event, [action.entity.id.id]);
        return true;
      case 'unassignFromCustomer':
        this.unassignFromCustomer(action.event, action.entity);
        return true;
    }
    return false;
  }
}
