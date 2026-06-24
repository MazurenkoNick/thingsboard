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
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { EntityId } from '@shared/models/id/entity-id';
import { AgentApplication, AgentApplicationType, AgentAppEvent } from '@shared/models/agent.models';
import { AgentService } from '@core/http/agent.service';
import {
  AgentAppInstallWizardComponent,
  AgentAppInstallWizardData
} from '@home/pages/agent/wizard/agent-app-install-wizard.component';

@Injectable({ providedIn: 'root' })
export class AgentDeployDialogService {

  constructor(private dialog: MatDialog,
              private agentService: AgentService) {}

  getManagedApp(relatedEntity: EntityId): Observable<AgentApplication | null> {
    return this.agentService.getAgentApplicationByRelatedEntity(
      relatedEntity.entityType as string, relatedEntity.id,
      { ignoreErrors: true, ignoreLoading: true }).pipe(
      catchError(() => of(null))
    );
  }

  open(appType: AgentApplicationType, relatedEntity: EntityId,
       options?: { showBack?: boolean }): Observable<AgentAppEvent | null> {
    return this.dialog.open<AgentAppInstallWizardComponent, AgentAppInstallWizardData, AgentAppEvent | null>(
      AgentAppInstallWizardComponent, {
        disableClose: false,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          mode: 'install',
          selectAgent: true,
          lockedType: appType,
          lockedRelatedEntity: relatedEntity,
          navigateToAgentOnFinish: true,
          showBack: !!options?.showBack
        }
      }
    ).afterClosed();
  }
}
