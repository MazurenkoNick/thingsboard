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
import { map, take } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';
import {
  AgentAppProfile,
  agentApplicationTypeTranslationMap
} from '@shared/models/agent.models';
import { AgentService } from '@core/http/agent.service';
import { AgentAppProfileComponent } from '@home/pages/agent/agent-app-profile.component';
import { AgentAppProfileTabsComponent } from '@home/pages/agent/agent-app-profile-tabs.component';

@Injectable()
export class AgentAppProfilesTableConfigResolver {

  private readonly config: EntityTableConfig<AgentAppProfile> = new EntityTableConfig<AgentAppProfile>();

  constructor(private store: Store<AppState>,
              private agentService: AgentService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router) {

    this.config.entityType = EntityType.AGENT_APP_PROFILE;
    this.config.entityComponent = AgentAppProfileComponent;
    this.config.entityTabsComponent = AgentAppProfileTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.AGENT_APP_PROFILE);
    this.config.entityResources = entityTypeResources.get(EntityType.AGENT_APP_PROFILE);

    this.config.deleteEntityTitle = profile => this.translate.instant('agent.delete-app-profile-title', {profileName: profile.name});
    this.config.deleteEntityContent = () => this.translate.instant('agent.delete-app-profile-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('agent.delete-app-profiles-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('agent.delete-app-profiles-text');

    this.config.loadEntity = id => this.agentService.getAgentAppProfileById(id.id);
    this.config.saveEntity = profile => this.agentService.saveAgentAppProfile(profile);
    this.config.onEntityAction = action => this.onProfileAction(action);
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<AgentAppProfile>> {
    return this.store.pipe(select(selectAuthUser), take(1)).pipe(
      map((authUser) => {
        this.config.tableTitle = this.translate.instant('agent.app-profiles');
        this.config.columns = this.configureColumns();
        this.config.entitiesFetchFunction = pageLink =>
          this.agentService.getTenantAgentAppProfiles(pageLink);
        this.config.deleteEntity = id => this.agentService.deleteAgentAppProfile(id.id);
        this.config.addEnabled = authUser.authority === Authority.TENANT_ADMIN;
        this.config.entitiesDeleteEnabled = authUser.authority === Authority.TENANT_ADMIN;
        this.config.deleteEnabled = () => authUser.authority === Authority.TENANT_ADMIN;
        this.config.detailsReadonly = () => authUser.authority !== Authority.TENANT_ADMIN;
        return this.config;
      })
    );
  }

  configureColumns(): Array<EntityTableColumn<AgentAppProfile>> {
    return [
      new DateEntityTableColumn<AgentAppProfile>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<AgentAppProfile>('name', 'agent.profile-name', '33%'),
      new EntityTableColumn<AgentAppProfile>('appType', 'agent.app-type', '33%',
        entity => {
          return this.translate.instant(agentApplicationTypeTranslationMap.get(entity.appType));
        }),
    ];
  }

  onProfileAction(action: EntityAction<AgentAppProfile>): boolean {
    return false;
  }
}
