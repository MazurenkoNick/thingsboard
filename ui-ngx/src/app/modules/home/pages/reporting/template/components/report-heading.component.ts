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
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Heading } from '@shared/models/report-component.models';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-report-heading',
    templateUrl: './report-heading.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ReportHeadingComponent),
            multi: true
        }
    ],
    standalone: false
})
export class ReportHeadingComponent implements OnInit, ControlValueAccessor {

  @Input()
  variableNames: string[] = [];

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  withLayout = true;

  private modelValue: Heading;

  private propagateChange = null;

  public headingFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.headingFormGroup = this.fb.group(
      {
        text: [null],
      }
    )
    if (this.withLayout) {
      this.headingFormGroup.addControl('font', this.fb.control(null));
      this.headingFormGroup.addControl('color', this.fb.control(null));
      this.headingFormGroup.addControl('textAlignment', this.fb.control(null));
      this.headingFormGroup.addControl('verticalAlignment', this.fb.control(null));
      this.headingFormGroup.addControl('height', this.fb.control(null));
    }
    this.headingFormGroup.valueChanges.pipe(
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
      this.headingFormGroup.disable({emitEvent: false});
    } else {
      this.headingFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Heading): void {
    this.modelValue = value;
    this.headingFormGroup.patchValue(
      value, {emitEvent: false}
    );
  }

  private updateModel() {
    this.modelValue = this.headingFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }
}
