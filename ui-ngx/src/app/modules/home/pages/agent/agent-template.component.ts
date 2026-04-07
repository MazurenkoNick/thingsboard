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
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { AgentAppTemplate } from '@shared/models/agent.models';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';

@Component({
  selector: 'tb-agent-template',
  templateUrl: './agent-template.component.html',
  styleUrls: []
})
export class AgentTemplateComponent extends EntityComponent<AgentAppTemplate> {

  entityType = EntityType;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: AgentAppTemplate,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<AgentAppTemplate>,
              public fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  buildForm(entity: AgentAppTemplate): UntypedFormGroup {
    return this.fb.group({
      appType: this.fb.control({value: entity ? entity.appType : null, disabled: true}),
      configType: this.fb.control({value: entity?.config?.type || 'DOCKER_COMPOSE', disabled: true}),
      currentVersion: this.fb.control({value: entity ? entity.currentVersion : null, disabled: true}),
      previousVersion: this.fb.control({value: entity?.previousVersion || '—', disabled: true}),
      nextVersion: this.fb.control({value: entity?.nextVersion || '—', disabled: true}),
    });
  }

  updateForm(entity: AgentAppTemplate) {
    this.entityForm.patchValue({
      appType: entity.appType,
      configType: entity.config?.type || 'DOCKER_COMPOSE',
      currentVersion: entity.currentVersion,
      previousVersion: entity.previousVersion || '—',
      nextVersion: entity.nextVersion || '—',
    });
  }
}
