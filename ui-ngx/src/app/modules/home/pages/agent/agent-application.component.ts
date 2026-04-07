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
import { AgentApplicationInfo } from '@shared/models/agent.models';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';

@Component({
  selector: 'tb-agent-application',
  templateUrl: './agent-application.component.html',
  styleUrls: []
})
export class AgentApplicationComponent extends EntityComponent<AgentApplicationInfo> {

  entityType = EntityType;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: AgentApplicationInfo,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<AgentApplicationInfo>,
              public fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  hideDelete() {
    return true;
  }

  buildForm(entity: AgentApplicationInfo): UntypedFormGroup {
    return this.fb.group({
      name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
      appType: this.fb.control({ value: entity?.appType || '', disabled: true }),
      currentVersion: this.fb.control({ value: entity?.currentVersion || '', disabled: true }),
      composeYaml: [this.dumpCompose(entity)]
    });
  }

  updateForm(entity: AgentApplicationInfo) {
    this.entityForm.patchValue({
      name: entity.name,
      appType: entity.appType,
      currentVersion: entity.currentVersion || '',
      composeYaml: this.dumpCompose(entity)
    });
  }

  prepareFormValue(formValue: any): any {
    const prepared = super.prepareFormValue(formValue);
    // Re-attach the parsed compose to the existing app config so the BE
    // round-trip preserves the rest of the AgentApplication body.
    const compose = (this.entity?.config as any)?.compose;
    prepared.config = {
      ...((this.entity?.config as any) || { type: 'DOCKER_COMPOSE' }),
      compose
    };
    delete prepared.composeYaml;
    delete prepared.appType;
    delete prepared.currentVersion;
    return prepared;
  }

  private dumpCompose(entity: AgentApplicationInfo): string {
    const compose: any = entity?.config && (entity.config as any).compose;
    if (!compose) {
      return '';
    }
    return this.dumpYaml(compose, 0).trimEnd() + '\n';
  }

  private dumpYaml(value: any, indent: number): string {
    const pad = '  '.repeat(indent);
    if (value === null || value === undefined) return `${pad}null\n`;
    if (Array.isArray(value)) {
      if (value.length === 0) return `${pad}[]\n`;
      let out = '';
      for (const item of value) {
        if (item !== null && typeof item === 'object') {
          const lines = this.dumpYaml(item, indent + 1).split('\n');
          let firstReplaced = false;
          for (const line of lines) {
            if (!line.trim()) continue;
            if (!firstReplaced) {
              out += `${pad}- ${line.trimStart()}\n`;
              firstReplaced = true;
            } else {
              out += `${line}\n`;
            }
          }
        } else {
          out += `${pad}- ${this.scalarYaml(item)}\n`;
        }
      }
      return out;
    }
    if (typeof value === 'object') {
      const keys = Object.keys(value);
      if (keys.length === 0) return `${pad}{}\n`;
      let out = '';
      for (const key of keys) {
        const v = value[key];
        if (v === null || v === undefined) {
          out += `${pad}${key}:\n`;
        } else if (typeof v === 'object') {
          if (Array.isArray(v) && v.length === 0) {
            out += `${pad}${key}: []\n`;
          } else if (!Array.isArray(v) && Object.keys(v).length === 0) {
            out += `${pad}${key}:\n`;
          } else {
            out += `${pad}${key}:\n`;
            out += this.dumpYaml(v, indent + 1);
          }
        } else {
          out += `${pad}${key}: ${this.scalarYaml(v)}\n`;
        }
      }
      return out;
    }
    return `${pad}${this.scalarYaml(value)}\n`;
  }

  private scalarYaml(value: any): string {
    if (typeof value === 'string') {
      const needsQuote = /^(true|false|null|yes|no|on|off|\d|-)/i.test(value)
        || value.includes(':') || value.includes('#') || value.includes('\n');
      return needsQuote ? `"${value.replace(/"/g, '\\"')}"` : value;
    }
    return String(value);
  }
}
