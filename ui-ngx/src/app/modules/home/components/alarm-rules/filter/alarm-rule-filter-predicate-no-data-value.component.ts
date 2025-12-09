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
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CalculatedFieldArgument } from "@shared/models/calculated-field.models";
import { TimeUnit, timeUnitTranslations } from "@home/components/rule-node/rule-node-config.models";
import { AlarmRuleFilterPredicateType, NoDataAlarmRuleFilterPredicate } from "@shared/models/alarm-rule.models";
import { isDefinedAndNotNull } from "@core/utils";

@Component({
  selector: 'tb-alarm-rule-filter-predicate-no-data-value',
  templateUrl: './alarm-rule-filter-predicate-no-data-value.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AlarmRuleFilterPredicateNoDataValueComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => AlarmRuleFilterPredicateNoDataValueComponent),
      multi: true
    }
  ]
})
export class AlarmRuleFilterPredicateNoDataValueComponent implements ControlValueAccessor, Validator, OnInit, OnChanges {

  @Input()
  arguments: Record<string, CalculatedFieldArgument>;

  @Input()
  valueType: AlarmRuleFilterPredicateType;

  @Input()
  argumentInUse: string;

  valueTypeEnum = AlarmRuleFilterPredicateType;

  filterPredicateValueNoDataFormGroup = this.fb.group({
    type: ['NO_DATA'],
    unit: [TimeUnit.MINUTES, Validators.required],
    duration: this.fb.group({
      staticValue: [null as null | number, [Validators.required, Validators.min(1)]],
      dynamicValueArgument: ['', Validators.required]
    })
  });

  timeUnits = [TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS];
  timeUnitsTranslationMap = timeUnitTranslations;


  dynamicModeControl = this.fb.control(false);

  argumentsList: Array<string>;

  private propagateChange= (v: any) => { };

  constructor(private fb: FormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.argumentsList = this.arguments ? Object.keys(this.arguments): [];
    this.filterPredicateValueNoDataFormGroup.valueChanges.pipe(
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
          if (this.argumentInUse === this.filterPredicateValueNoDataFormGroup.get('duration.dynamicValueArgument').value) {
            this.filterPredicateValueNoDataFormGroup.get('duration.dynamicValueArgument').setErrors({argumentInUse: true});
            this.filterPredicateValueNoDataFormGroup.updateValueAndValidity();
          }
        }
      }
    }
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.filterPredicateValueNoDataFormGroup.disable({emitEvent: false});
      this.dynamicModeControl.disable({emitEvent: false});
    } else {
      this.filterPredicateValueNoDataFormGroup.enable({emitEvent: false});
      this.dynamicModeControl.enable({emitEvent: false});
      this.updateValueModeValidators(this.dynamicModeControl.value);
    }
  }

  private updateValueModeValidators(isDynamicMode: boolean): void {
    if (isDynamicMode) {
      this.filterPredicateValueNoDataFormGroup.get('duration.staticValue').disable({emitEvent: false});
      this.filterPredicateValueNoDataFormGroup.get('duration.dynamicValueArgument').enable();
      setTimeout(()=> {
        if (this.filterPredicateValueNoDataFormGroup.get('duration.dynamicValueArgument').value && this.argumentInUse === this.filterPredicateValueNoDataFormGroup.get('dynamicValueArgument').value) {
          this.filterPredicateValueNoDataFormGroup.get('duration.dynamicValueArgument').setErrors({argumentInUse: true});
          this.filterPredicateValueNoDataFormGroup.updateValueAndValidity();
        }
      }, 0);
    } else {
      this.filterPredicateValueNoDataFormGroup.get('duration.dynamicValueArgument').disable({emitEvent: false});
      this.filterPredicateValueNoDataFormGroup.get('duration.staticValue').enable();
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  validate(): ValidationErrors | null {
    return this.filterPredicateValueNoDataFormGroup.valid ? null : {
      filterPredicateValue: {valid: false}
    };
  }

  writeValue(predicateValue: NoDataAlarmRuleFilterPredicate): void {
    if (isDefinedAndNotNull(predicateValue?.duration?.dynamicValueArgument)) {
      const availableArgument = this.argumentsList.filter(arg => arg !== this.argumentInUse);
      if (!availableArgument.includes(predicateValue.duration.dynamicValueArgument)) {
        predicateValue.duration.dynamicValueArgument = '';
      }
      this.dynamicModeControl.patchValue(true, {emitEvent: false});
    }
    this.filterPredicateValueNoDataFormGroup.patchValue(predicateValue, {emitEvent: false});
  }

  private updateModel() {
    this.propagateChange(this.filterPredicateValueNoDataFormGroup.value);
  }
}
