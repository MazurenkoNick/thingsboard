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
import { AgentGroupInfo, AgentProvisionType, agentProvisionTypeTranslationMap } from '@shared/models/agent.models';
import { TranslateService } from '@ngx-translate/core';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { generateSecret } from '@core/utils';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';

@Component({
  selector: 'tb-agent-group',
  templateUrl: './agent-group.component.html',
  styleUrls: []
})
export class AgentGroupComponent extends EntityComponent<AgentGroupInfo> {

  entityType = EntityType;
  agentProvisionTypes = Object.values(AgentProvisionType);
  agentProvisionTypeTranslationMap = agentProvisionTypeTranslationMap;

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

  isAssignedToCustomer(entity: AgentGroupInfo): boolean {
    return entity && entity.customerId && entity.customerId.id !== NULL_UUID;
  }

  buildForm(entity: AgentGroupInfo): UntypedFormGroup {
    const form = this.fb.group({
      name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
      description: [entity ? entity.description : ''],
      provisionType: [entity?.provisionType || AgentProvisionType.DISABLED],
      provisionKey: this.fb.control({value: entity?.provisionKey || null, disabled: true}),
      provisionSecret: this.fb.control({value: entity?.provisionSecret || null, disabled: true}),
    });
    if (entity && !entity.id) {
      this.generateProvisionCredentials(form);
    }
    return form;
  }

  updateForm(entity: AgentGroupInfo) {
    this.entityForm.patchValue({
      name: entity.name,
      description: entity.description,
      provisionType: entity.provisionType || AgentProvisionType.DISABLED,
      provisionKey: entity.provisionKey,
      provisionSecret: entity.provisionSecret,
    });
  }

  updateFormState() {
    super.updateFormState();
    this.entityForm.get('provisionKey').disable({emitEvent: false});
    this.entityForm.get('provisionSecret').disable({emitEvent: false});
  }

  onProvisionCopied(isKey: boolean) {
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant(isKey ? 'agent.provision-key-copied-message' : 'agent.provision-secret-copied-message'),
      type: 'success',
      duration: 750,
      verticalPosition: 'bottom',
      horizontalPosition: 'right'
    }));
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

  private generateProvisionCredentials(form: UntypedFormGroup) {
    form.get('provisionKey').patchValue(generateSecret(20), {emitEvent: false});
    form.get('provisionSecret').patchValue(generateSecret(20), {emitEvent: false});
  }
}
