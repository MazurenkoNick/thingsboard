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
import { Observable, of } from 'rxjs';
import { mergeMap } from 'rxjs/operators';
import { AgentService } from '@core/http/agent.service';
import { EntityId, entityIdEquals } from '@shared/models/id/entity-id';
import {
  AgentAppEvent,
  AgentAppEventActionType,
  AgentAppInstallResponse
} from '@shared/models/agent.models';

export interface UpdateSubmitContext {
  existingApplicationId: string;
  application: any;
  stepInputs: { [stepId: string]: any };
  relatedEntityId: EntityId | null;
  initialRelatedEntityId: EntityId | null;
  isProfileManagedUpdate: boolean;
  skipProfileRefetch: boolean;
}

// Owns the create/install rxjs pipelines and the related-entity assignment that
// follows a successful install/update. The flow components keep the UI state
// (the submitting flag, stepper advance, installed-app capture).
@Injectable()

export class AgentAppWizardSubmitService {

  constructor(private agentService: AgentService) {}

  upgrade(existingApplicationId: string, application: any,
          stepInputs: { [stepId: string]: any }): Observable<AgentAppEvent> {
    return this.agentService.createAgentAppEvent(existingApplicationId, {
      actionType: AgentAppEventActionType.UPGRADE,
      application,
      stepInputs
    });
  }

  update(ctx: UpdateSubmitContext): Observable<AgentAppEvent> {
    return this.assign(ctx.existingApplicationId, ctx.initialRelatedEntityId, ctx.relatedEntityId).pipe(
      mergeMap(() => this.agentService.createAgentAppEvent(ctx.existingApplicationId, {
        actionType: AgentAppEventActionType.UPDATE,
        application: ctx.application,
        stepInputs: ctx.stepInputs,
        ...(ctx.isProfileManagedUpdate ? { skipProfileRefetch: ctx.skipProfileRefetch } : {})
      }))
    );
  }

  install(application: any, stepInputs: { [stepId: string]: any },
          relatedEntityId: EntityId | null): Observable<AgentAppInstallResponse> {
    return this.agentService.installAgentApp({
      actionType: AgentAppEventActionType.INSTALL,
      application,
      stepInputs,
      ...(relatedEntityId ? { relatedEntityId } : {})
    });
  }

  private assign(appId: string | null, prev: EntityId | null,
                 current: EntityId | null): Observable<any> {
    if (!appId || entityIdEquals(prev, current)) {
      return of(null);
    }
    if (current) {
      return this.agentService.assignRelatedEntity(appId, current);
    }
    return this.agentService.unassignRelatedEntity(appId);
  }
}
