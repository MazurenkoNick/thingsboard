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

import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '@home/components/entity/entity.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import {
  AgentGroupInfo,
  AgentProvisionType,
  agentProvisionTypeDescriptionMap,
  agentProvisionTypeTranslationMap
} from '@shared/models/agent.models';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';

@Component({
  selector: 'tb-agent-group',
  templateUrl: './agent-group.component.html',
  styleUrls: ['./agent-group.component.scss']
})
export class AgentGroupComponent extends EntityComponent<AgentGroupInfo> {

  entityType = EntityType;
  agentProvisionTypes = Object.values(AgentProvisionType);
  agentProvisionTypeTranslationMap = agentProvisionTypeTranslationMap;
  agentProvisionTypeDescriptionMap = agentProvisionTypeDescriptionMap;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: AgentGroupInfo,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<AgentGroupInfo>,
              public fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: AgentGroupInfo): UntypedFormGroup {
    return this.fb.group({
      name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
      description: [entity ? entity.description : ''],
      provisionType: [entity?.provisionType || AgentProvisionType.DISABLED],
    });
  }

  updateForm(entity: AgentGroupInfo) {
    this.entityForm.patchValue({
      name: entity.name,
      description: entity.description,
      provisionType: entity.provisionType || AgentProvisionType.DISABLED,
    });
  }

  onGroupIdCopied() {
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant('agent.group-id-copied-message'),
      type: 'success',
      duration: 750,
      verticalPosition: 'bottom',
      horizontalPosition: 'right'
    }));
  }
}
