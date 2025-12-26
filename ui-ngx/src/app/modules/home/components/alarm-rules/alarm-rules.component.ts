///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import { ChangeDetectorRef, Component, DestroyRef, inject, Inject, Input } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '@home/components/entity/entity.component';
import { FormBuilder, FormGroup } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import {
  CalculatedFieldArgument,
  CalculatedFieldConfiguration,
  CalculatedFieldInfo,
  calculatedFieldsEntityTypeList,
  CalculatedFieldType
} from '@shared/models/calculated-field.models';
import { EntityId } from '@shared/models/id/entity-id';
import { BaseData } from '@shared/models/base-data';
import { Observable } from 'rxjs';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import {
  CalculatedFieldsTableConfig,
  CalculatedFieldsTableEntity
} from '@home/components/calculated-fields/calculated-fields-table-config';
import { TenantId } from '@shared/models/id/tenant-id';
import { StringItemsOption } from '@shared/components/string-items-list.component';
import { RelationTypes } from '@shared/models/relation.models';
import { AlarmRule, AlarmRuleConditionType, AlarmRuleExpressionType } from '@shared/models/alarm-rule.models';
import { CalculatedFieldFormService } from '@core/services/calculated-field-form.service';

@Component({
  selector: 'tb-alarm-rules',
  templateUrl: './alarm-rules.component.html',
  styleUrls: []
})
export class AlarmRulesComponent extends EntityComponent<CalculatedFieldsTableEntity> {

  @Input()
  standalone = false;

  @Input()
  entityName: string;

  readonly ownerId = new TenantId(getCurrentAuthUser(this.store).tenantId);
  readonly tenantId = getCurrentAuthUser(this.store).tenantId;
  readonly EntityType = EntityType;
  readonly calculatedFieldsEntityTypeList = calculatedFieldsEntityTypeList;
  readonly CalculatedFieldType = CalculatedFieldType;

  private cfFormService = inject(CalculatedFieldFormService);
  private destroyRef = inject(DestroyRef);

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: CalculatedFieldInfo,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: CalculatedFieldsTableConfig,
              protected fb: FormBuilder,
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

  additionalDebugActionConfig = {
    ...this.entitiesTableConfig.additionalDebugActionConfig,
    action: () => this.entitiesTableConfig.additionalDebugActionConfig.action(
      { id: this.entity.id, ...this.entityFormValue() }, false,
      (expression) => {
        if (expression) {
          this.entityForm.get('configuration').setValue({...this.entityFormValue().configuration, expression});
          this.entityForm.get('configuration').markAsDirty();
        }
      }),
  };

  get entityId(): EntityId {
    return this.entityForm.get('entityId').value;
  }

  get entitiesTableConfig(): CalculatedFieldsTableConfig {
    return this.entitiesTableConfigValue;
  }

  changeEntity(entity: BaseData<EntityId>): void {
    this.entityName = entity?.name;
  }

  buildForm(_entity?: CalculatedFieldInfo): FormGroup {
    return inject(CalculatedFieldFormService).buildAlarmRuleForm();
  }

  updateForm(entity: CalculatedFieldInfo) {
    const { configuration = {} as CalculatedFieldConfiguration, type = CalculatedFieldType.ALARM, debugSettings = { failuresEnabled: true, allEnabled: true }, entityId = this.entityId, ...value } = entity ?? {};
    setTimeout(() => {
      this.entityForm.patchValue({ configuration, debugSettings, entityId, ...value }, {emitEvent: false});
    });
    if (!entityId) {
      this.entityForm.get('configuration').disable({emitEvent: false});
    }
  }

  onTestScript(expression?: string): Observable<string> {
    return this.cfFormService.testScript(
      this.entity?.id?.id,
      this.entityFormValue(),
      this.entitiesTableConfig.getTestScriptDialog.bind(this.entitiesTableConfig),
      this.destroyRef,
      expression
    );
  }

  updateFormState() {
    if (this.entityForm) {
      if (this.isEditValue) {
        this.entityForm.enable({emitEvent: false});
        this.entityForm.get('entityId').disable({emitEvent: false});
      } else {
        this.entityForm.disable({emitEvent: false});
      }
    }
  }

  get arguments(): Record<string, CalculatedFieldArgument> {
    return this.entityForm.get('configuration.arguments').value;
  }

  get predefinedTypeValues(): StringItemsOption[] {
    return RelationTypes.map(type => ({
      name: type,
      value: type
    }));
  }

  get configFormGroup(): FormGroup {
    return this.entityForm.get('configuration') as FormGroup;
  }

  public removeClearAlarmRule() {
    this.configFormGroup.patchValue({clearRule: null});
    this.entityForm.markAsDirty();
  }

  public addClearAlarmRule() {
    const clearAlarmRule: AlarmRule = {
      condition: {
        type: AlarmRuleConditionType.SIMPLE,
        expression: {
          type: AlarmRuleExpressionType.SIMPLE
        }
      }
    };
    this.configFormGroup.patchValue({clearRule: clearAlarmRule});
  }

}
