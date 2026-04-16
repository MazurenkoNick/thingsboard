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
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig,
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { Observable } from 'rxjs';
import { select, Store } from '@ngrx/store';
import { selectAuthUser } from '@core/auth/auth.selectors';
import { map, mergeMap, take } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';
import { MatDialog } from '@angular/material/dialog';
import { AgentGroupInfo, AgentProvisionType, agentProvisionTypeTranslationMap } from '@shared/models/agent.models';
import { AgentService } from '@core/http/agent.service';
import { AgentGroupComponent } from '@home/pages/agent/agent-group.component';
import { AgentGroupTabsComponent } from '@home/pages/agent/agent-group-tabs.component';
import {
  AgentGroupCreatedDialogComponent,
  AgentGroupCreatedDialogData
} from '@home/pages/agent/agent-group-created-dialog.component';

@Injectable()
export class AgentGroupsTableConfigResolver {

  private readonly config: EntityTableConfig<AgentGroupInfo> = new EntityTableConfig<AgentGroupInfo>();

  constructor(private store: Store<AppState>,
              private agentService: AgentService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private dialog: MatDialog) {

    this.config.entityType = EntityType.AGENT_GROUP;
    this.config.entityComponent = AgentGroupComponent;
    this.config.entityTabsComponent = AgentGroupTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.AGENT_GROUP);
    this.config.entityResources = entityTypeResources.get(EntityType.AGENT_GROUP);

    this.config.deleteEntityTitle = group => this.translate.instant('agent.delete-group-title', {groupName: group.name});
    this.config.deleteEntityContent = () => this.translate.instant('agent.delete-group-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('agent.delete-groups-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('agent.delete-groups-text');

    this.config.loadEntity = id => this.agentService.getAgentGroupInfoById(id.id);
    this.config.saveEntity = group => {
      return this.agentService.saveAgentGroup(group).pipe(
        mergeMap((saved) => this.agentService.getAgentGroupInfoById(saved.id.id))
      );
    };
    this.config.onEntityAction = action => this.onGroupAction(action);
    this.config.entityAdded = (group) => {
      if (group.provisionType === AgentProvisionType.ALLOW_CREATE_NEW_AGENTS) {
        this.openGroupCreatedInstructions(group);
      }
    };
  }

  private openGroupCreatedInstructions(group: AgentGroupInfo) {
    this.dialog.open<AgentGroupCreatedDialogComponent, AgentGroupCreatedDialogData>(
      AgentGroupCreatedDialogComponent, {
        disableClose: false,
        panelClass: ['tb-dialog'],
        data: { group }
      });
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<AgentGroupInfo>> {
    return this.store.pipe(select(selectAuthUser), take(1)).pipe(
      map((authUser) => {
        this.config.tableTitle = this.translate.instant('agent.agent-groups');
        this.config.columns = this.configureColumns();
        this.config.entitiesFetchFunction = pageLink =>
          this.agentService.getTenantAgentGroupInfos(pageLink);
        this.config.deleteEntity = id => this.agentService.deleteAgentGroup(id.id);
        this.config.addEnabled = authUser.authority === Authority.TENANT_ADMIN;
        this.config.entitiesDeleteEnabled = authUser.authority === Authority.TENANT_ADMIN;
        this.config.deleteEnabled = () => authUser.authority === Authority.TENANT_ADMIN;
        this.config.detailsReadonly = () => authUser.authority !== Authority.TENANT_ADMIN;
        return this.config;
      })
    );
  }

  configureColumns(): Array<EntityTableColumn<AgentGroupInfo>> {
    return [
      new DateEntityTableColumn<AgentGroupInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<AgentGroupInfo>('name', 'agent.group-name', '40%'),
      new EntityTableColumn<AgentGroupInfo>('provisionType', 'agent.provisioning-strategy', '40%',
        entity => this.provisionTypeLabel(entity.provisionType), () => ({}), false),
    ];
  }

  private provisionTypeLabel(type: AgentProvisionType | undefined): string {
    const key = agentProvisionTypeTranslationMap.get(type ?? AgentProvisionType.DISABLED);
    return key ? this.translate.instant(key) : '';
  }

  onGroupAction(action: EntityAction<AgentGroupInfo>): boolean {
    return false;
  }
}
