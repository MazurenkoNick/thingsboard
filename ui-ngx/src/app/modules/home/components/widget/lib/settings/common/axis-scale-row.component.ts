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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor, NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder, UntypedFormControl,
  UntypedFormGroup, ValidationErrors, Validator,
  Validators,
} from '@angular/forms';
import {
  ValueSourceConfig,
  ValueSourceType,
  ValueSourceTypes,
  ValueSourceTypeTranslation
} from '@shared/models/widget-settings.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DataKey, Datasource, DatasourceType, } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { IAliasController } from '@core/api/widget-api.models';
import { DataKeysCallbacks } from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import { merge } from 'rxjs';

@Component({
  selector: 'tb-axis-scale-row',
  templateUrl: './axis-scale-row.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AxisScaleRowComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => AxisScaleRowComponent),
      multi: true
    },
  ]
})
export class AxisScaleRowComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  isPanelView = false;

  @Input()
  aliasController: IAliasController;

  @Input()
  callbacks: DataKeysCallbacks;

  @Input()
  datasource: Datasource;

  @Input()
  labelKey: string;

  ValueSourceType = ValueSourceType;

  DataKeyType = DataKeyType;

  DatasourceType = DatasourceType;

  ValueSourceTypeTranslation = ValueSourceTypeTranslation;

  ValueSourceTypes = ValueSourceTypes;

  limitForm: UntypedFormGroup;

  latestKeyFormControl: UntypedFormControl;

  entityKeyFormControl: UntypedFormControl;

  private propagateChanges: (value: any) => void = () => {};

  private modelValue: ValueSourceConfig | null = null;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.limitForm = this.fb.group({
      type: [ValueSourceType.constant],
      value: [null],
      entityAlias: [null, [Validators.required]]
    });
    this.latestKeyFormControl = this.fb.control(null, [Validators.required]);
    this.entityKeyFormControl = this.fb.control(null, [Validators.required]);
    merge(
      this.latestKeyFormControl.valueChanges,
      this.entityKeyFormControl.valueChanges,
      this.limitForm.valueChanges
    ).pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.updateValidators();
        this.updateModel();
      });
  }

  writeValue(value: ValueSourceConfig) {
    this.modelValue = value;
    this.limitForm.patchValue(
      {
        type: value.type || ValueSourceType.constant,
        value: value.value,
        entityAlias: value.entityAlias,
      }, {emitEvent: false}
    );
    if (value.type === ValueSourceType.latestKey) {
      this.latestKeyFormControl.patchValue({
        type: value.latestKeyType,
        name: value.latestKey
      }, {emitEvent: false});
    } else if (value.type === ValueSourceType.entity) {
      this.entityKeyFormControl.patchValue({
        type: value.entityKeyType,
        name: value.entityKey
      }, {emitEvent: false});
    }

    this.updateValidators();
    this.limitForm.markAllAsTouched();
  }

  registerOnChange(fn: any) {
    this.propagateChanges = fn;
  }

  registerOnTouched(fn: any) {
  }

  validate(): ValidationErrors | null {
    const type = this.limitForm.get('type')?.value;
    const errors: any = {};

    if (this.limitForm.invalid) {
      errors.form = false;
    }

    if (type === ValueSourceType.latestKey) {
      if (!this.latestKeyFormControl.value || this.latestKeyFormControl.invalid) {
        errors.latestKey = false;
      }
    } else if (type === ValueSourceType.entity) {
      if (!this.limitForm.get('entityAlias')?.value) {
        errors.entityAlias = false;
      }
      if (!this.entityKeyFormControl.value || this.entityKeyFormControl.invalid) {
        errors.entityKey = false;
      }
    }

    return Object.keys(errors).length ? { axisLimitForm: errors } : null;
  }

  private updateValidators() {
    const type = this.limitForm.get('type')?.value;
    const entityAliasCtr = this.limitForm.get('entityAlias');

    const isLatestKey = type === ValueSourceType.latestKey;
    const isEntity = type === ValueSourceType.entity;

    isLatestKey ? this.latestKeyFormControl.enable({ emitEvent: false })
      : this.latestKeyFormControl.disable({ emitEvent: false });

    isEntity ? this.entityKeyFormControl.enable({ emitEvent: false })
      : this.entityKeyFormControl.disable({ emitEvent: false });

    isEntity ? entityAliasCtr.enable({ emitEvent: false })
      : entityAliasCtr.disable({ emitEvent: false });

    this.latestKeyFormControl.updateValueAndValidity({ emitEvent: false });
    this.entityKeyFormControl.updateValueAndValidity({ emitEvent: false });
    entityAliasCtr.updateValueAndValidity({ emitEvent: false });
  }

  private updateModel() {
    const value = this.limitForm.value;
    const type = value.type;
    let updates: Partial<ValueSourceConfig> = { type };
    if (type === ValueSourceType.latestKey) {
      const latestKey: DataKey = this.latestKeyFormControl.value;
      updates.latestKey = latestKey?.name;
      updates.latestKeyType = latestKey?.type as any;
    } else if (type === ValueSourceType.entity) {
      const entityKey: DataKey = this.entityKeyFormControl.value;
      updates.entityKey = entityKey?.name;
      updates.entityKeyType = entityKey?.type as any;
      updates.entityAlias = value?.entityAlias;
    } else {
      updates.value = value?.value;
    }
    this.modelValue = updates as ValueSourceConfig;
    this.propagateChanges(this.modelValue);
  }
}
