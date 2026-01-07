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

import { Component, DestroyRef, Inject, ViewChild, ViewEncapsulation } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import { CalculatedField, CalculatedFieldArgument, CalculatedFieldType } from '@shared/models/calculated-field.models';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { EntityId } from '@shared/models/id/entity-id';
import { AdditionalDebugActionConfig } from '@home/components/entity/debug/entity-debug-settings.model';
import { COMMA, ENTER, SEMICOLON } from "@angular/cdk/keycodes";
import {
  AlarmRule,
  AlarmRuleConditionType,
  alarmRuleEntityTypeList,
  AlarmRuleExpressionType,
  AlarmRuleTestScriptFn
} from "@shared/models/alarm-rule.models";
import { deepTrim } from "@core/utils";
import { combineLatest, Observable } from "rxjs";
import { debounceTime, startWith } from "rxjs/operators";
import { Operation } from "@shared/models/security.models";
import { UserPermissionsService } from "@core/http/user-permissions.service";
import { RelationTypes } from "@shared/models/relation.models";
import { StringItemsOption } from "@shared/components/string-items-list.component";
import { BaseData } from "@shared/models/base-data";
import { CalculatedFieldFormService } from '@core/services/calculated-field-form.service';
import { EntitySelectComponent } from '@shared/components/entity/entity-select.component';
import { TenantId } from '@shared/models/id/tenant-id';

export interface AlarmRuleDialogData {
  value?: CalculatedField;
  buttonTitle: string;
  entityId: EntityId;
  tenantId: string;
  entityName?: string;
  ownerId: EntityId;
  additionalDebugActionConfig: AdditionalDebugActionConfig<(calculatedField: CalculatedField) => void>;
  isDirty?: boolean;
  readonly: boolean;
  getTestScriptDialogFn: AlarmRuleTestScriptFn,
}

@Component({
  selector: 'tb-alarm-rule-dialog',
  templateUrl: './alarm-rule-dialog.component.html',
  styleUrls: ['./alarm-rule-dialog.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class AlarmRuleDialogComponent extends DialogComponent<AlarmRuleDialogComponent, CalculatedField> {

  @ViewChild('entitySelect') entitySelect!: EntitySelectComponent;

  fieldFormGroup: FormGroup ;

  additionalDebugActionConfig = this.data.value?.id ? {
    ...this.data.additionalDebugActionConfig,
    action: () => this.data.additionalDebugActionConfig.action({ id: this.data.value.id, ...this.fromGroupValue }),
  } : null;

  alarmRuleEntityTypeList = alarmRuleEntityTypeList.filter(entityType =>
    this.userPermissionsService.hasGenericPermissionByEntityGroupType(Operation.WRITE_CALCULATED_FIELD, entityType));

  readonly EntityType = EntityType;
  readonly entityTypeTranslations = entityTypeTranslations;
  readonly CalculatedFieldType = CalculatedFieldType;
  readonly ScriptLanguage = ScriptLanguage;

  separatorKeysCodes = [ENTER, COMMA, SEMICOLON];

  entityName = this.data.entityName;
  ownerId = this.data.ownerId;
  defaultEntityType: EntityType;

  disabledClearRuleButton = false;
  disabledArguments = false;
  isLoading = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AlarmRuleDialogData,
              protected dialogRef: MatDialogRef<AlarmRuleDialogComponent, CalculatedField>,
              private calculatedFieldsService: CalculatedFieldsService,
              private destroyRef: DestroyRef,
              private cfFormService: CalculatedFieldFormService,
              private userPermissionsService: UserPermissionsService) {
    super(store, router, dialogRef);
    this.fieldFormGroup = this.cfFormService.buildAlarmRuleForm();
    this.applyDialogData();
    this.updateRulesValidators();

    if (this.data.isDirty) {
      this.fieldFormGroup.markAsDirty();
    }

    if (this.data.readonly) {
      this.fieldFormGroup.disable();
      this.disabledArguments = true;
    }

    this.fieldFormGroup.get('configuration.arguments').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateRulesValidators();
    });

    if (!this.data.entityId) {
      combineLatest([
        this.fieldFormGroup.get('entityId')!.valueChanges.pipe(startWith(this.fieldFormGroup.get('entityId')!.value)),
        this.fieldFormGroup.get('name')!.valueChanges.pipe(startWith(this.fieldFormGroup.get('name')!.value))
      ]).pipe(
        debounceTime(50),
        takeUntilDestroyed()
      ).subscribe(([entityId, name]) => {
        this.disabledArguments = !entityId || !name?.length;
        const argsControl = this.fieldFormGroup.get('configuration.arguments')!;
        if (this.disabledArguments) {
          argsControl.disable({ emitEvent: false });
        } else {
          argsControl.enable({ emitEvent: false });
        }
      });
      if (this.alarmRuleEntityTypeList.includes(EntityType.DEVICE_PROFILE)) {
        this.defaultEntityType = EntityType.DEVICE_PROFILE;
      } else if (this.alarmRuleEntityTypeList.length === 1) {
        this.defaultEntityType = this.alarmRuleEntityTypeList[0];
      }
    }
  }

  get configFormGroup(): FormGroup {
    return this.fieldFormGroup.get('configuration') as FormGroup;
  }

  get arguments(): Record<string, CalculatedFieldArgument> {
    return this.fieldFormGroup.get('configuration.arguments').value;
  }

  public removeClearAlarmRule() {
    this.configFormGroup.patchValue({clearRule: null});
    this.fieldFormGroup.markAsDirty();
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

  get fromGroupValue(): CalculatedField {
    return deepTrim(this.fieldFormGroup.value as CalculatedField);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    if (this.fieldFormGroup.valid && Object.keys(this.arguments ?? {}).length > 0) {
      this.isLoading = true;
      const alarmRule = { entityId: this.data.entityId, ...(this.data.value ?? {}),  ...this.fromGroupValue};
      alarmRule.configuration.type = CalculatedFieldType.ALARM;

      this.calculatedFieldsService.saveCalculatedField(alarmRule)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: calculatedField => this.dialogRef.close(calculatedField),
          error: () => this.isLoading = false
        });
    } else {
      this.fieldFormGroup.get('name').markAsTouched();
      this.entitySelect.entityAutocompleteMarkAsTouched();
    }
  }

  private applyDialogData(): void {
    const { configuration = {}, type = CalculatedFieldType.ALARM, debugSettings = { failuresEnabled: true, allEnabled: true }, entityId = this.data.entityId, ...value } = this.data.value ?? {};
    this.fieldFormGroup.patchValue({ configuration, type, debugSettings, entityId, ...value }, {emitEvent: false});
  }

  onTestScript(expression: string): Observable<string> {
    return this.cfFormService.testScript(
      this.data.value?.id?.id,
      this.fromGroupValue,
      this.data.getTestScriptDialogFn,
      this.destroyRef,
      expression
    );
  }

  private updateRulesValidators(): void {
    if (Object.keys(this.arguments ?? {}).length > 0) {
      this.fieldFormGroup.get('configuration.createRules').enable({emitEvent: false});
      this.fieldFormGroup.get('configuration.clearRule').enable({emitEvent: false});
      this.fieldFormGroup.get('configuration.propagate').enable({emitEvent: false});
      this.fieldFormGroup.get('configuration.propagateToOwner').enable({emitEvent: false});
      this.fieldFormGroup.get('configuration.propagateToTenant').enable({emitEvent: false});
      this.fieldFormGroup.get('configuration.propagateToOwnerHierarchy').enable({emitEvent: false});
      this.fieldFormGroup.get('configuration.propagateRelationTypes').enable({emitEvent: false});
      this.disabledClearRuleButton = false;
    } else {
      this.fieldFormGroup.get('configuration.createRules').disable({emitEvent: false});
      this.fieldFormGroup.get('configuration.clearRule').disable({emitEvent: false});
      this.fieldFormGroup.get('configuration.propagate').disable({emitEvent: false});
      this.fieldFormGroup.get('configuration.propagateToOwner').disable({emitEvent: false});
      this.fieldFormGroup.get('configuration.propagateToTenant').disable({emitEvent: false});
      this.fieldFormGroup.get('configuration.propagateToOwnerHierarchy').disable({emitEvent: false});
      this.fieldFormGroup.get('configuration.propagateRelationTypes').disable({emitEvent: false});
      this.disabledClearRuleButton = true;
    }
  }
  get predefinedTypeValues(): StringItemsOption[] {
    return RelationTypes.map(type => ({
      name: type,
      value: type
    }));
  }

  changeEntity(entity: BaseData<EntityId>): void {
    this.entityName = entity.name;
    this.ownerId = entity.ownerId ?? new TenantId(this.data.tenantId);
  }
}
