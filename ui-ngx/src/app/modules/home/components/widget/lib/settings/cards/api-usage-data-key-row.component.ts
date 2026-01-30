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

import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  EventEmitter,
  forwardRef,
  Input,
  OnInit,
  Output,
  ViewEncapsulation
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DataKey, DatasourceType, widgetType } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import {
  ApiUsageDataKeysSettings,
  ApiUsageSettingsContext
} from "@home/components/widget/lib/settings/cards/api-usage-settings.component.models";
import { Observable, of } from "rxjs";

@Component({
    selector: 'tb-api-usage-data-key-row',
    templateUrl: './api-usage-data-key-row.component.html',
    styleUrls: ['./api-usage-data-key-row.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ApiUsageDataKeyRowComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ApiUsageDataKeyRowComponent implements ControlValueAccessor, OnInit {

  DatasourceType = DatasourceType;
  DataKeyType = DataKeyType;

  widgetType = widgetType;

  @Input()
  disabled: boolean;

  @Input()
  dsEntityAliasId: string;

  @Input()
  context: ApiUsageSettingsContext;

  @Output()
  dataKeyRemoved = new EventEmitter();

  dataKeyFormGroup: UntypedFormGroup;

  modelValue: ApiUsageDataKeysSettings;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.dataKeyFormGroup = this.fb.group({
      label: [null, [Validators.required]],
      state: [null, []],
      status: [null, [Validators.required]],
      maxLimit: [null, [Validators.required]],
      current: [null, [Validators.required]]
    });
    this.dataKeyFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.dataKeyFormGroup.disable({emitEvent: false});
    } else {
      this.dataKeyFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: ApiUsageDataKeysSettings): void {
    this.modelValue = value;
    this.dataKeyFormGroup.patchValue(
      {
        label: value?.label,
        state: value?.state,
        status: value?.status,
        maxLimit: value?.maxLimit,
        current: value?.current
      }, {emitEvent: false}
    );
    this.updateValidators();
    this.cd.markForCheck();
  }

  editKey(keyType: 'status' | 'maxLimit' | 'current') {
    const targetDataKey: DataKey = this.dataKeyFormGroup.get(keyType).value;
    this.context.editKey(targetDataKey, this.dsEntityAliasId).subscribe(
      (updatedDataKey) => {
        if (updatedDataKey) {
          this.dataKeyFormGroup.get(keyType).patchValue(updatedDataKey);
        }
      }
    );
  }

  private updateValidators() {
  }

  private updateModel() {
    this.modelValue = {...this.modelValue, ...this.dataKeyFormGroup.value};
    this.propagateChange(this.modelValue);
  }

  fetchDashboardStates(searchText?: string): Observable<Array<string>> {
    return of(this.context.callbacks.fetchDashboardStates(searchText));
  }
}
