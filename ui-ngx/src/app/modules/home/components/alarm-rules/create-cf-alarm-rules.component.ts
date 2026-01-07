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

import { Component, DestroyRef, forwardRef, Input } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { AlarmSeverity, alarmSeverityColors, alarmSeverityTranslations } from '@shared/models/alarm.models';
import { AlarmRule } from "@shared/models/alarm-rule.models";
import { CalculatedFieldArgument } from "@shared/models/calculated-field.models";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { coerceBoolean } from "@shared/decorators/coercion";
import { Observable } from "rxjs";

@Component({
  selector: 'tb-create-cf-alarm-rules',
  templateUrl: './create-cf-alarm-rules.component.html',
  styleUrls: ['./create-cf-alarm-rules.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CreateCfAlarmRulesComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CreateCfAlarmRulesComponent),
      multi: true,
    }
  ]
})
export class CreateCfAlarmRulesComponent implements ControlValueAccessor, Validator {

  @Input()
  @coerceBoolean()
  disabled: boolean;

  @Input()
  arguments: Record<string, CalculatedFieldArgument>;

  @Input({required: true})
  testScript: (expression: string) => Observable<string>;

  alarmSeverities = Object.keys(AlarmSeverity);
  alarmSeverityEnum = AlarmSeverity;
  alarmSeverityTranslationMap = alarmSeverityTranslations;

  AlarmSeverityNotificationColors = alarmSeverityColors;

  createAlarmRulesFormGroup = this.fb.group({
    createAlarmRules: this.fb.array<{severity: AlarmSeverity, alarmRule: AlarmRule}>([])
  });

  private usedSeverities: AlarmSeverity[] = [];

  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder,
              private destroyRef: DestroyRef) {
    this.createAlarmRulesFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateModel());
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  createAlarmRulesFormArray(): UntypedFormArray {
    return this.createAlarmRulesFormGroup.get('createAlarmRules') as UntypedFormArray;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.createAlarmRulesFormGroup.disable({emitEvent: false});
    } else {
      this.createAlarmRulesFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(createAlarmRules: Record<AlarmSeverity, AlarmRule>): void {
    const createAlarmRulesControls: Array<AbstractControl> = [];
    if (createAlarmRules) {
      Object.keys(createAlarmRules).forEach((severity) => {
        const createAlarmRule = createAlarmRules[severity];
        if (severity === 'empty') {
          severity = null;
        }
        createAlarmRulesControls.push(this.fb.group({
          severity: [severity, Validators.required],
          alarmRule: [{value: createAlarmRule, disabled: this.disabled}, Validators.required]
        }));
      });
    }
    const formArray = this.createAlarmRulesFormGroup.get('createAlarmRules') as FormArray;
    formArray.clear({emitEvent: false});
    createAlarmRulesControls.forEach(c => formArray.push(c, {emitEvent: false}));
    if (this.disabled) {
      this.createAlarmRulesFormGroup.disable({emitEvent: false});
    } else {
      this.createAlarmRulesFormGroup.enable({emitEvent: false});
    }
    this.updateUsedSeverities();
    if (!this.disabled && !this.createAlarmRulesFormGroup.valid) {
      this.updateModel();
    }
  }

  public removeCreateAlarmRule(index: number) {
    (this.createAlarmRulesFormGroup.get('createAlarmRules') as FormArray).removeAt(index);
  }

  public addCreateAlarmRule() {
    const createAlarmRulesArray = this.createAlarmRulesFormGroup.get('createAlarmRules') as FormArray;
    createAlarmRulesArray.push(this.fb.group({
      severity: [this.getFirstUnusedSeverity(), Validators.required],
      alarmRule: [null, Validators.required]
    }));
    this.createAlarmRulesFormGroup.updateValueAndValidity();
    if (!this.createAlarmRulesFormGroup.valid) {
      this.updateModel();
    }
  }

  private getFirstUnusedSeverity(): AlarmSeverity {
    for (const severityKey of Object.keys(AlarmSeverity)) {
      const severity = AlarmSeverity[severityKey];
      if (this.usedSeverities.indexOf(severity) === -1) {
        return severity;
      }
    }
    return null;
  }

  public validate(): ValidationErrors | null {
    return this.createAlarmRulesFormGroup.valid && this.createAlarmRulesFormArray().length > 0 ? null : {
      createAlarmRules: {
        valid: false,
      },
    };
  }

  public isDisabledSeverity(severity: AlarmSeverity, index: number): boolean {
    const usedIndex = this.usedSeverities.indexOf(severity);
    return usedIndex > -1 && usedIndex !== index;
  }

  private updateUsedSeverities() {
    this.usedSeverities = [];
    const value = this.createAlarmRulesFormGroup.get('createAlarmRules').value;
    value.forEach((rule, index) => {
      this.usedSeverities[index] = AlarmSeverity[rule.severity];
    });
  }

  private updateModel() {
    const value = this.createAlarmRulesFormGroup.get('createAlarmRules').value;
    const createAlarmRules = {} as Record<AlarmSeverity, AlarmRule>;
    value.forEach(v => createAlarmRules[v.severity] = v.alarmRule);
    this.updateUsedSeverities();
    this.propagateChange(createAlarmRules);
  }
}
