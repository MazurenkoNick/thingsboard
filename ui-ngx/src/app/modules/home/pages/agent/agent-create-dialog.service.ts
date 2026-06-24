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
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';
import { AgentService } from '@core/http/agent.service';
import { AgentInfo } from '@shared/models/agent.models';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { AddEntityDialogComponent } from '@home/components/entity/add-entity-dialog.component';
import { AddEntityDialogData } from '@home/models/entity/entity-component.models';
import { AgentComponent } from '@home/pages/agent/agent.component';

@Injectable({ providedIn: 'root' })
export class AgentCreateDialogService {

  constructor(private dialog: MatDialog,
              private agentService: AgentService) {}

  create(): Observable<AgentInfo> {
    const config = new EntityTableConfig<AgentInfo>();
    config.entityType = EntityType.AGENT;
    config.entityComponent = AgentComponent;
    config.entityTranslations = entityTypeTranslations.get(EntityType.AGENT);
    config.entityResources = entityTypeResources.get(EntityType.AGENT);
    config.entityTitle = (agent) => agent ? agent.name : '';
    config.componentsData = { agentScope: 'tenant', customerId: null };
    config.saveEntity = (agent) => this.agentService.saveAgent(agent).pipe(
      mergeMap((saved) => this.agentService.getAgentInfoById(saved.id.id))
    );
    return this.dialog.open<AddEntityDialogComponent, AddEntityDialogData<AgentInfo>, AgentInfo>(
      AddEntityDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { entitiesTableConfig: config }
      }
    ).afterClosed();
  }
}
