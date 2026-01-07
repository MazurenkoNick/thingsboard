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

import { Component, DestroyRef, forwardRef, Input, OnInit, ViewEncapsulation } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Insets } from '@shared/models/report.models';

@Component({
  selector: 'tb-report-insets',
  templateUrl: './report-insets.component.html',
  styleUrls: ['./report-insets.component.scss'],
  encapsulation: ViewEncapsulation.None,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ReportInsetsComponent),
      multi: true
    }
  ]
})
export class ReportInsetsComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  private modelValue: Insets;

  private propagateChange = null;

  public insetsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.insetsFormGroup = this.fb.group(
      {
        left: [null, [Validators.min(0)]],
        right: [null, [Validators.min(0)]],
        top: [null, [Validators.min(0)]],
        bottom: [null, [Validators.min(0)]]
      }
    )
    this.insetsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.insetsFormGroup.disable({emitEvent: false});
    } else {
      this.insetsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Insets): void {
    this.modelValue = value;
    this.insetsFormGroup.patchValue(
      {
        left: value?.left,
        right: value?.right,
        top: value?.top,
        bottom: value?.bottom
      }, {emitEvent: false}
    );
  }

  private updateModel() {
    this.modelValue = this.insetsFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }
}
