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

import { AfterViewInit, Component, forwardRef, Input, OnDestroy } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ReportConfig } from '@shared/models/report.models';

@Component({
  selector: 'tb-generate-report-event-config',
  templateUrl: './generate-report.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => GenerateReportComponent),
    multi: true
  },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => GenerateReportComponent),
      multi: true
    }]
})
export class GenerateReportComponent implements ControlValueAccessor, AfterViewInit, OnDestroy, Validator {

  modelValue: ReportConfig | null;

  generateReportFormGroup: UntypedFormGroup;

  private destroy$ = new Subject<void>();

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    this.generateReportFormGroup = this.fb.group({
      reportConfig: [null, [Validators.required]]
    });

    this.generateReportFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  private updateEnabledState() {
    if (this.disabled) {
      this.generateReportFormGroup.disable({emitEvent: false});
    } else {
      this.generateReportFormGroup.enable({emitEvent: false});
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngAfterViewInit(): void {
    if (!this.generateReportFormGroup.valid) {
      setTimeout(() => {
        this.updateModel();
      }, 0);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.updateEnabledState();
  }

  writeValue(value: ReportConfig | null): void {
    this.modelValue = value;
    this.generateReportFormGroup.reset({
      reportConfig: this.modelValue
    },{emitEvent: false});
    this.updateEnabledState();
  }

  validate(): ValidationErrors | null {
    if (!this.generateReportFormGroup.valid) {
      return {
        generateReportForm: {
          valid: false
        }
      };
    }

    return null;
  }

  private updateModel() {
    if (this.generateReportFormGroup.valid) {
      this.modelValue = this.generateReportFormGroup.value?.reportConfig || {};
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

}
