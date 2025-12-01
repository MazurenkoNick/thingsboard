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

import { Component, DestroyRef, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { EntityKeyValueType } from '@shared/models/query/query.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CalculatedFieldArgument } from "@shared/models/calculated-field.models";
import { AlarmRuleValue } from "@shared/models/alarm-rule.models";
import { FormControlsFrom } from "@shared/models/tenant.model";
import { isDefinedAndNotNull } from "@core/utils";

@Component({
  selector: 'tb-alarm-rule-filter-predicate-value',
  templateUrl: './alarm-rule-filter-predicate-value.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AlarmRuleFilterPredicateValueComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => AlarmRuleFilterPredicateValueComponent),
      multi: true
    }
  ]
})
export class AlarmRuleFilterPredicateValueComponent implements ControlValueAccessor, Validator, OnInit, OnChanges {

  @Input()
  arguments: Record<string, CalculatedFieldArgument>;

  @Input()
  valueType: EntityKeyValueType;

  @Input()
  argumentInUse: string;

  valueTypeEnum = EntityKeyValueType;

  filterPredicateValueFormGroup: FormGroup<FormControlsFrom<AlarmRuleValue<string | number | boolean>>>;

  dynamicModeControl = this.fb.control(false);

  argumentsList: Array<string>;

  private propagateChange= (v: any) => { };

  constructor(private fb: FormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.argumentsList = this.arguments ? Object.keys(this.arguments): [];
    let defaultValue: string | number | boolean;
    let defaultValueValidators: ValidatorFn[];
    switch (this.valueType) {
      case EntityKeyValueType.STRING:
        defaultValue = '';
        defaultValueValidators = [];
        break;
      case EntityKeyValueType.NUMERIC:
        defaultValue = 0;
        defaultValueValidators = [Validators.required];
        break;
      case EntityKeyValueType.BOOLEAN:
        defaultValue = false;
        defaultValueValidators = [];
        break;
      case EntityKeyValueType.DATE_TIME:
        defaultValue = Date.now();
        defaultValueValidators = [Validators.required];
        break;
    }
    this.filterPredicateValueFormGroup = this.fb.group({
      staticValue: [defaultValue, defaultValueValidators],
      dynamicValueArgument: ['', Validators.required]
    });
    this.filterPredicateValueFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    this.dynamicModeControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => this.updateValueModeValidators(value));
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.argumentInUse) {
      const argumentInUseChanges = changes.argumentInUse;
      if (!argumentInUseChanges.firstChange && argumentInUseChanges.currentValue !== argumentInUseChanges.previousValue) {
        if (this.dynamicModeControl.value) {
          if (this.argumentInUse === this.filterPredicateValueFormGroup.get('dynamicValueArgument').value) {
            this.filterPredicateValueFormGroup.get('dynamicValueArgument').setErrors({argumentInUse: true});
            this.filterPredicateValueFormGroup.updateValueAndValidity();
          }
        }
      }
    }
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.filterPredicateValueFormGroup.disable({emitEvent: false});
      this.dynamicModeControl.disable({emitEvent: false});
    } else {
      this.filterPredicateValueFormGroup.enable({emitEvent: false});
      this.dynamicModeControl.enable({emitEvent: false});
      this.updateValueModeValidators(this.dynamicModeControl.value);
    }
  }

  private updateValueModeValidators(isDynamicMode: boolean): void {
    if (isDynamicMode) {
      this.filterPredicateValueFormGroup.get('staticValue').disable({emitEvent: false});
      this.filterPredicateValueFormGroup.get('dynamicValueArgument').enable();
      setTimeout(()=> {
        if (this.filterPredicateValueFormGroup.get('dynamicValueArgument').value && this.argumentInUse === this.filterPredicateValueFormGroup.get('dynamicValueArgument').value) {
          this.filterPredicateValueFormGroup.get('dynamicValueArgument').setErrors({argumentInUse: true});
          this.filterPredicateValueFormGroup.updateValueAndValidity();
        }
      }, 0);
    } else {
      this.filterPredicateValueFormGroup.get('dynamicValueArgument').disable({emitEvent: false});
      this.filterPredicateValueFormGroup.get('staticValue').enable();
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  validate(): ValidationErrors | null {
    return this.filterPredicateValueFormGroup.valid ? null : {
      filterPredicateValue: {valid: false}
    };
  }

  writeValue(predicateValue: AlarmRuleValue<string | number | boolean>): void {
    if (isDefinedAndNotNull(predicateValue.dynamicValueArgument)) {
      const availableArgument = this.argumentsList.filter(arg => arg !== this.argumentInUse);
      if (!availableArgument.includes(predicateValue.dynamicValueArgument)) {
        predicateValue.dynamicValueArgument = '';
      }
      this.dynamicModeControl.patchValue(true, {emitEvent: false});
    }
    this.filterPredicateValueFormGroup.patchValue(predicateValue, {emitEvent: false});
  }

  private updateModel() {
    this.propagateChange(this.filterPredicateValueFormGroup.value);
  }
}
