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

import { Injectable } from '@angular/core';
import { Router, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import {
  EntityGroupStateConfigFactory,
  EntityGroupStateInfo,
  GroupEntityTableConfig
} from '@home/models/group/group-entities-table-config.models';
import { EntityGroupParams } from '@shared/models/entity-group.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { GroupConfigTableConfigService } from '@home/components/group/group-config-table-config.service';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { AgentService } from '@core/http/agent.service';
import { AgentInfo } from '@shared/models/agent.models';
import { AgentComponent } from '@home/pages/agent/agent.component';

@Injectable()
export class AgentGroupConfigFactory implements EntityGroupStateConfigFactory<AgentInfo> {

  constructor(private groupConfigTableConfigService: GroupConfigTableConfigService<AgentInfo>,
              private translate: TranslateService,
              private homeDialogs: HomeDialogsService,
              private agentService: AgentService,
              private router: Router) {
  }

  createConfig(params: EntityGroupParams, entityGroup: EntityGroupStateInfo<AgentInfo>): Observable<GroupEntityTableConfig<AgentInfo>> {
    const config = new GroupEntityTableConfig<AgentInfo>(entityGroup, params);

    config.entityComponent = AgentComponent;
    config.entityTitle = (agent) => agent ? agent.name : '';

    config.deleteEntityTitle = agent => this.translate.instant('agent.delete-agent-title', { agentName: agent.name });
    config.deleteEntityContent = () => this.translate.instant('agent.delete-agent-text');
    config.deleteEntitiesTitle = count => this.translate.instant('agent.delete-agents-title', { count });
    config.deleteEntitiesContent = () => this.translate.instant('agent.delete-agents-text');

    config.loadEntity = id => this.agentService.getAgentInfoById(id.id);
    config.saveEntity = agent => this.agentService.saveAgent(agent).pipe(
      mergeMap(saved => this.agentService.getAgentInfoById(saved.id.id))
    );
    config.deleteEntity = id => this.agentService.deleteAgent(id.id);

    config.onEntityAction = action => this.onAgentAction(action, config);

    return this.groupConfigTableConfigService.prepareConfiguration(params, config);
  }

  private openAgent(_event: Event, agent: AgentInfo, config: GroupEntityTableConfig<AgentInfo>, params: EntityGroupParams) {
    if (_event) {
      _event.stopPropagation();
    }
    if (params.hierarchyView) {
      // Hierarchy view is reserved for devices/assets; agents don't surface in
      // the customer hierarchy today, so fall through to regular navigation.
      const url: UrlTree = this.router.createUrlTree([agent.id.id], { relativeTo: config.getActivatedRoute() });
      this.router.navigateByUrl(url);
    } else {
      const url = this.router.createUrlTree([agent.id.id], { relativeTo: config.getActivatedRoute() });
      this.router.navigateByUrl(url);
    }
  }

  private manageOwnerAndGroups(event: Event, agent: AgentInfo, config: GroupEntityTableConfig<AgentInfo>) {
    this.homeDialogs.manageOwnerAndGroups(event, agent).subscribe(res => {
      if (res) {
        config.updateData();
      }
    });
  }

  private onAgentAction(action: EntityAction<AgentInfo>, config: GroupEntityTableConfig<AgentInfo>): boolean {
    switch (action.action) {
      case 'open':
        this.openAgent(action.event, action.entity, config, config.groupParams);
        return true;
      case 'manageOwnerAndGroups':
        this.manageOwnerAndGroups(action.event, action.entity, config);
        return true;
    }
    return false;
  }
}

