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

import { Component, DestroyRef, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  pageOrientations,
  pageOrientationTranslationMap,
  pageSizes,
  paperSizeDisplayMap,
  ReportTemplateSettings,
  TbReportFormat
} from '@shared/models/report.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { deepTrim } from '@core/utils';

@Component({
    selector: 'tb-report-template-settings',
    templateUrl: './report-template-settings.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ReportTemplateSettingsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class ReportTemplateSettingsComponent implements OnInit, OnChanges, ControlValueAccessor {

  pageSizes = pageSizes;
  paperSizeDisplayMap = paperSizeDisplayMap;

  pageOrientations = pageOrientations;
  pageOrientationTranslationMap = pageOrientationTranslationMap;

  TbReportFormat = TbReportFormat;

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  subReport = false;

  @Input()
  format: TbReportFormat = TbReportFormat.PDF;

  private modelValue: ReportTemplateSettings;

  private propagateChange = null;

  public settingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.settingsFormGroup = this.fb.group({
      name: [null, [Validators.required, Validators.maxLength(255)]],
      namePattern: [null, [Validators.required]],
      timeDataPattern: [null, []],
      description: [null, []],
      pageSize: [null, []],
      pageOrientation: [null, []],
      pageMargins: [null, []],
      pageBackground: [null, []]
    });
    this.settingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    this.updateValidators();
  }

  ngOnChanges(changes: SimpleChanges) {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['subReport', 'format'].includes(propName)) {
          this.updateValidators();
        }
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.settingsFormGroup.disable({emitEvent: false});
    } else {
      this.settingsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: ReportTemplateSettings): void {
    this.modelValue = value;
    this.settingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
  }

  private updateValidators() {
    if (this.subReport) {
      this.settingsFormGroup.get('namePattern').disable({emitEvent: false});
      this.settingsFormGroup.get('timeDataPattern').disable({emitEvent: false});
    } else {
      this.settingsFormGroup.get('namePattern').enable({emitEvent: false});
      this.settingsFormGroup.get('timeDataPattern').enable({emitEvent: false});
    }
    if (this.subReport || this.format !== TbReportFormat.PDF) {
      this.settingsFormGroup.get('pageSize').disable({emitEvent: false});
      this.settingsFormGroup.get('pageOrientation').disable({emitEvent: false});
      this.settingsFormGroup.get('pageMargins').disable({emitEvent: false});
      this.settingsFormGroup.get('pageBackground').disable({emitEvent: false});
    } else {
      this.settingsFormGroup.get('pageSize').enable({emitEvent: false});
      this.settingsFormGroup.get('pageOrientation').enable({emitEvent: false});
      this.settingsFormGroup.get('pageMargins').enable({emitEvent: false});
      this.settingsFormGroup.get('pageBackground').enable({emitEvent: false});
    }
  }

  private updateModel() {
    this.modelValue = this.settingsFormGroup.value;
    this.propagateChange(deepTrim(this.modelValue));
  }
}
