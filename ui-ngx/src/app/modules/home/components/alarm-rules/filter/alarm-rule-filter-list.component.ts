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

import { booleanAttribute, Component, DestroyRef, forwardRef, Input } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Observable } from 'rxjs';
import {
  ComplexOperation,
  complexOperationTranslationMap,
  EntityKeyValueType
} from '@shared/models/query/query.models';
import { MatDialog } from '@angular/material/dialog';
import { deepClone } from '@core/utils';
import {
  AlarmRuleFilterDialogComponent,
  AlarmRuleFilterDialogData
} from "@home/components/alarm-rules/filter/alarm-rule-filter-dialog.component";
import {
  AlarmRuleFilter,
  areFilterAndPredicateArgumentsValid,
  FilterPredicateTypeTranslationMap
} from "@shared/models/alarm-rule.models";
import { CalculatedFieldArgument } from "@shared/models/calculated-field.models";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";

@Component({
  selector: 'tb-alarm-rule-filter-list',
  templateUrl: './alarm-rule-filter-list.component.html',
  styleUrls: ['./alarm-rule-filter-list.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AlarmRuleFilterListComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => AlarmRuleFilterListComponent),
      multi: true
    }
  ]
})
export class AlarmRuleFilterListComponent implements ControlValueAccessor, Validator {

  @Input({ transform: booleanAttribute })
  readonly: boolean;

  @Input()
  arguments: Record<string, CalculatedFieldArgument>;

  @Input()
  operation: ComplexOperation = ComplexOperation.AND;

  filterListFormGroup = this.fb.group({
    filters: this.fb.array([])
  });

  disabled = false;

  areFilterAndPredicateArgumentsValid = areFilterAndPredicateArgumentsValid;

  complexOperationTranslationMap = complexOperationTranslationMap;
  FilterPredicateTypeTranslationMap = FilterPredicateTypeTranslationMap

  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder,
              private dialog: MatDialog,
              private destroyRef: DestroyRef) {
    this.filterListFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateModel());
  }


  get filtersFormArray(): FormArray {
    return this.filterListFormGroup.get('filters') as FormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  validate(): ValidationErrors | null {
    return this.filterListFormGroup.valid && this.filterListFormGroup.get('filters').value?.length ? null : {
      filterList: {valid: false}
    };
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.filterListFormGroup.disable({emitEvent: false});
    } else {
      this.filterListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(filters: Array<AlarmRuleFilter>): void {
    const keyFilterControls: Array<AbstractControl> = [];
    if (filters) {
      for (const filter of filters) {
        keyFilterControls.push(this.fb.control(filter, [Validators.required]));
      }
    }
    this.filtersFormArray.clear();
    keyFilterControls.forEach(c => this.filtersFormArray.push(c));
  }

  public removeFilter(index: number) {
    (this.filterListFormGroup.get('filters') as FormArray).removeAt(index);
  }

  public addFilter() {
    const filtersFormArray = this.filterListFormGroup.get('filters') as FormArray;
    this.openFilterDialog(null).subscribe(result => {
      if (result) {
        filtersFormArray.push(this.fb.control(result, [Validators.required]));
      }
    });
  }

  public editFilter(index: number) {
    const filter: AlarmRuleFilter =
      (this.filterListFormGroup.get('filters') as FormArray).at(index).value;
    this.openFilterDialog(filter).subscribe(result => {
      if (result) {
        (this.filterListFormGroup.get('filters') as FormArray).at(index).patchValue(result);
      }
    });
  }

  private openFilterDialog(filter?: AlarmRuleFilter): Observable<AlarmRuleFilter> {
    const isAdd = !filter;
    if (isAdd) {
      filter = {
        argument: null,
        valueType: EntityKeyValueType.STRING,
        operation: ComplexOperation.AND,
        predicates: []
      };
    }
    return this.dialog.open<AlarmRuleFilterDialogComponent, AlarmRuleFilterDialogData,
      AlarmRuleFilter>(AlarmRuleFilterDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        filter: filter ? deepClone(filter) : null,
        isAdd,
        arguments: this.arguments,
        usedArguments: this.getUsedArguments,
        readonly: this.readonly,
      }
    }).afterClosed();
  }

  get getUsedArguments(): Array<string> {
    const filters = this.filterListFormGroup.get('filters').value as Array<AlarmRuleFilter>;
    return filters.length ? filters.map((filter: AlarmRuleFilter) => filter.argument) : [];
  }

  private updateModel() {
    const filters = this.filterListFormGroup.value.filters as Array<AlarmRuleFilter>;
    this.propagateChange(filters);
  }
}
