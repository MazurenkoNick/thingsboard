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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormControl,
  Validator
} from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { isDefinedAndNotNull } from '@core/utils';
import { DashboardId } from '@shared/models/id/dashboard-id';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AlarmRule, AlarmRuleCondition } from "@shared/models/alarm-rule.models";
import { CalculatedField, CalculatedFieldArgument } from "@shared/models/calculated-field.models";
import {
  AlarmRuleDetailsDialogComponent,
  AlarmRuleDetailsDialogData
} from "@home/components/alarm-rules/alarm-rule-details-dialog.component";
import { coerceBoolean } from "@shared/decorators/coercion";

@Component({
  selector: 'tb-cf-alarm-rule',
  templateUrl: './cf-alarm-rule.component.html',
  styleUrls: ['./cf-alarm-rule.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CfAlarmRuleComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CfAlarmRuleComponent),
      multi: true,
    }
  ]
})
export class CfAlarmRuleComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  @coerceBoolean()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  arguments: Record<string, CalculatedFieldArgument>;

  @Input()
  @coerceBoolean()
  isClearCondition = false;

  @Input()
  value: CalculatedField;

  private modelValue: AlarmRule;

  alarmRuleFormGroup = this.fb.group({
    condition: this.fb.control<AlarmRuleCondition | null>(null),
    alarmDetails: [null],
    dashboardId: [null]
  });

  private propagateChange = (v: any) => { };

  constructor(private dialog: MatDialog,
              private fb: FormBuilder,
              private destroyRef: DestroyRef) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.alarmRuleFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.alarmRuleFormGroup.disable({emitEvent: false});
    } else {
      this.alarmRuleFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: AlarmRule): void {
    if (value) {
      this.modelValue = value;
      const model = this.modelValue ? {
        ...this.modelValue,
        dashboardId: this.modelValue.dashboardId?.id
      } : null;
      this.alarmRuleFormGroup.patchValue(model, {emitEvent: false});
    }
  }

  public openEditDetailsDialog($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AlarmRuleDetailsDialogComponent, AlarmRuleDetailsDialogData,
          string>(AlarmRuleDetailsDialogComponent, {
          disableClose: true,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: {
            alarmDetails: this.alarmRuleFormGroup.get('alarmDetails').value,
            readonly: this.disabled
          }
        }).afterClosed().subscribe((alarmDetails) => {
          if (isDefinedAndNotNull(alarmDetails)) {
            this.alarmRuleFormGroup.patchValue({alarmDetails});
          }
    });
  }

  public validate(c: UntypedFormControl) {
    return (!this.required && !this.modelValue || this.alarmRuleFormGroup.valid) ? null : {
      alarmRule: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value = this.alarmRuleFormGroup.value;
    this.modelValue = {...value, dashboardId: value.dashboardId ? new DashboardId(value.dashboardId) : null} as AlarmRule;
    this.propagateChange(this.modelValue);
  }
}
