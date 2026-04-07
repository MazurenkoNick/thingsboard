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
import { AgentInfo } from '@shared/models/agent.models';
import { TranslateService } from '@ngx-translate/core';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { generateSecret, guid } from '@core/utils';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';

@Component({
  selector: 'tb-agent',
  templateUrl: './agent.component.html',
  styleUrls: []
})
export class AgentComponent extends EntityComponent<AgentInfo> {

  entityType = EntityType;
  agentScope: 'tenant' | 'customer' | 'customer_user';

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: AgentInfo,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<AgentInfo>,
              public fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  ngOnInit() {
    this.agentScope = this.entitiesTableConfig.componentsData.agentScope;
    super.ngOnInit();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  isAssignedToCustomer(entity: AgentInfo): boolean {
    return entity && entity.customerId && entity.customerId.id !== NULL_UUID;
  }

  buildForm(entity: AgentInfo): UntypedFormGroup {
    const form = this.fb.group({
      name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
      routingKey: this.fb.control({value: entity ? entity.routingKey : null, disabled: true}),
      secret: this.fb.control({value: entity ? entity.secret : null, disabled: true}),
      description: [entity ? entity.description : '']
    });
    this.generateRoutingKeyAndSecret(entity, form);
    return form;
  }

  updateForm(entity: AgentInfo) {
    this.entityForm.patchValue({
      name: entity.name,
      routingKey: entity.routingKey,
      secret: entity.secret,
      description: entity.description
    });
    this.generateRoutingKeyAndSecret(entity, this.entityForm);
  }

  updateFormState() {
    super.updateFormState();
    this.entityForm.get('routingKey').disable({emitEvent: false});
    this.entityForm.get('secret').disable({emitEvent: false});
  }

  onAgentInfoCopied(type: string) {
    const messageMap: Record<string, string> = {
      id: 'agent.id-copied-message',
      key: 'agent.routing-key-copied-message',
      secret: 'agent.secret-copied-message'
    };
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant(messageMap[type]),
      type: 'success',
      duration: 750,
      verticalPosition: 'bottom',
      horizontalPosition: 'right'
    }));
  }

  private generateRoutingKeyAndSecret(entity: AgentInfo, form: UntypedFormGroup) {
    if (entity && !entity.id) {
      form.get('routingKey').patchValue(guid(), {emitEvent: false});
      form.get('secret').patchValue(generateSecret(20), {emitEvent: false});
    }
  }
}
