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

import { Component, DestroyRef, Inject, ViewEncapsulation } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import {
  CalculatedField,
  CalculatedFieldConfiguration,
  calculatedFieldsEntityTypeList,
  CalculatedFieldTestScriptFn,
  CalculatedFieldType,
  calculatedFieldTypes,
  CalculatedFieldTypeTranslations
} from '@shared/models/calculated-field.models';
import { EntityType } from '@shared/models/entity-type.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { Observable } from 'rxjs';
import { EntityId } from '@shared/models/id/entity-id';
import { AdditionalDebugActionConfig } from '@home/components/entity/debug/entity-debug-settings.model';
import { deepTrim } from '@core/utils';
import { BaseData } from '@shared/models/base-data';
import { CalculatedFieldFormService } from '@core/services/calculated-field-form.service';
import { FormGroup } from '@angular/forms';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation } from '@shared/models/security.models';
import { TenantId } from '@shared/models/id/tenant-id';

export interface CalculatedFieldDialogData {
  value?: CalculatedField;
  buttonTitle: string;
  entityId: EntityId;
  tenantId: string;
  entityName?: string;
  ownerId: EntityId;
  additionalDebugActionConfig: AdditionalDebugActionConfig<(calculatedField: CalculatedField) => void>;
  getTestScriptDialogFn: CalculatedFieldTestScriptFn;
  isDirty?: boolean;
  disabledSelectType?: boolean;
  readonly: boolean;
}

@Component({
  selector: 'tb-calculated-field-dialog',
  templateUrl: './calculated-field-dialog.component.html',
  styleUrls: ['./calculated-field-dialog.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class CalculatedFieldDialogComponent extends DialogComponent<CalculatedFieldDialogComponent, CalculatedField> {

  fieldFormGroup: FormGroup;

  additionalDebugActionConfig = this.data.value?.id ? {
    ...this.data.additionalDebugActionConfig,
    action: () => this.data.additionalDebugActionConfig.action({ id: this.data.value.id, ...this.fromGroupValue }),
  } : null;

  entityName = this.data.entityName;
  ownerId = this.data.ownerId;
  defaultEntityType: EntityType;

  disabledConfiguration = false;
  isLoading = false;

  readonly EntityType = EntityType;
  readonly calculatedFieldsEntityTypeList = calculatedFieldsEntityTypeList.filter(entityType =>
    this.userPermissionsService.hasGenericPermissionByEntityGroupType(Operation.WRITE_CALCULATED_FIELD, entityType));
  readonly CalculatedFieldType = CalculatedFieldType;
  readonly fieldTypes = calculatedFieldTypes;
  readonly CalculatedFieldTypeTranslations = CalculatedFieldTypeTranslations;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: CalculatedFieldDialogData,
              protected dialogRef: MatDialogRef<CalculatedFieldDialogComponent, CalculatedField>,
              private calculatedFieldsService: CalculatedFieldsService,
              private destroyRef: DestroyRef,
              private cfFormService: CalculatedFieldFormService,
              private userPermissionsService: UserPermissionsService) {
    super(store, router, dialogRef);
    this.fieldFormGroup = this.cfFormService.buildForm();
    this.cfFormService.setupTypeChange(this.fieldFormGroup, this.destroyRef);
    this.applyDialogData();

    if (this.data.isDirty) {
      this.fieldFormGroup.markAsDirty();
    }

    if (!this.data.entityId) {
      this.fieldFormGroup.get('entityId').valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe((entityId) => {
        this.disabledConfiguration = !entityId;
        if (this.disabledConfiguration) {
          this.fieldFormGroup.get('configuration').disable({emitEvent: false});
        } else {
          this.fieldFormGroup.get('configuration').enable({emitEvent: false});
        }
      });
      if (this.calculatedFieldsEntityTypeList.includes(EntityType.DEVICE_PROFILE)) {
        this.defaultEntityType = EntityType.DEVICE_PROFILE;
      } else if (this.calculatedFieldsEntityTypeList.length === 1) {
        this.defaultEntityType = this.calculatedFieldsEntityTypeList[0];
      }
    }

    if (this.data.disabledSelectType) {
      this.fieldFormGroup.get('type').disable({emitEvent: false});
    }

    if (this.data.readonly) {
      this.fieldFormGroup.disable();
      this.disabledConfiguration = true;
    }
  }

  get fromGroupValue(): CalculatedField {
    return deepTrim(this.fieldFormGroup.getRawValue() as CalculatedField);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    if (this.fieldFormGroup.valid) {
      this.isLoading = true;
      this.calculatedFieldsService.saveCalculatedField({ entityId: this.data.entityId, ...(this.data.value ?? {}),  ...this.fromGroupValue})
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: calculatedField => this.dialogRef.close(calculatedField),
          error: () => this.isLoading = false
        });
    } else {
      this.fieldFormGroup.get('name').markAsTouched();
    }
  }

  onTestScript(expression?: string): Observable<string> {
    return this.cfFormService.testScript(
      this.data.value?.id?.id,
      this.fromGroupValue,
      this.data.getTestScriptDialogFn,
      this.destroyRef,
      expression
    );
  }

  changeEntity(entity: BaseData<EntityId>): void {
    this.entityName = entity.name;
    this.ownerId = entity.ownerId ?? new TenantId(this.data.tenantId);
  }

  get entityId(): EntityId {
    return this.data.entityId || this.fieldFormGroup.get('entityId').value;
  }

  private applyDialogData(): void {
    const { configuration = {} as CalculatedFieldConfiguration, type = CalculatedFieldType.SIMPLE, debugSettings = { failuresEnabled: true, allEnabled: true }, entityId = this.data.entityId, ...value } = this.data.value ?? {};
    const preparedConfig = this.cfFormService.prepareConfig(configuration);
    this.fieldFormGroup.patchValue({ configuration: preparedConfig, type, debugSettings, entityId, ...value }, {emitEvent: false});
    setTimeout(() => this.fieldFormGroup.get('type').updateValueAndValidity({onlySelf: true}));
    if (!this.data.entityId) {
      this.fieldFormGroup.get('configuration').disable({emitEvent: false});
      this.disabledConfiguration = true;
    }
  }
}
