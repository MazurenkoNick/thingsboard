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
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { Store } from '@ngrx/store';
import {
  AgentAppTemplate,
  agentApplicationTypeTranslationMap
} from '@shared/models/agent.models';
import { AgentService } from '@core/http/agent.service';
import { AgentTemplateComponent } from '@home/pages/agent/agent-template.component';
import { AgentTemplateTabsComponent } from '@home/pages/agent/agent-template-tabs.component';

@Injectable()
export class AgentTemplatesTableConfigResolver {

  private readonly config: EntityTableConfig<AgentAppTemplate> = new EntityTableConfig<AgentAppTemplate>();

  constructor(private store: Store<AppState>,
              private agentService: AgentService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router) {

    this.config.entityType = EntityType.AGENT_APP_TEMPLATE;
    this.config.entityComponent = AgentTemplateComponent;
    this.config.entityTabsComponent = AgentTemplateTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.AGENT_APP_TEMPLATE);
    this.config.entityResources = entityTypeResources.get(EntityType.AGENT_APP_TEMPLATE);

    this.config.loadEntity = id => this.agentService.getAgentAppTemplateById(id.id);
    this.config.onEntityAction = () => false;
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<AgentAppTemplate>> {
    return of(null).pipe(
      map(() => {
        this.config.tableTitle = this.translate.instant('agent.templates');
        this.config.columns = this.configureColumns();
        this.config.entitiesFetchFunction = pageLink =>
          this.agentService.getAgentAppTemplates(pageLink);
        this.config.addEnabled = false;
        this.config.entitiesDeleteEnabled = false;
        this.config.deleteEnabled = () => false;
        this.config.detailsReadonly = () => true;
        return this.config;
      })
    );
  }

  configureColumns(): Array<EntityTableColumn<AgentAppTemplate>> {
    return [
      new DateEntityTableColumn<AgentAppTemplate>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<AgentAppTemplate>('appType', 'agent.app-type', '25%',
        entity => this.translate.instant(agentApplicationTypeTranslationMap.get(entity.appType))),
      new EntityTableColumn<AgentAppTemplate>('currentVersion', 'agent.current-version', '25%'),
      new EntityTableColumn<AgentAppTemplate>('nextVersion', 'agent.next-version', '25%',
        entity => entity.nextVersion || '-'),
    ];
  }
}
