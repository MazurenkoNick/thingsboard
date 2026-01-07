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
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Observable, of } from 'rxjs';
import {
  ComplexOperation,
  complexOperationTranslationMap,
  EntityKeyValueType,
  entityKeyValueTypeToFilterPredicateType
} from '@shared/models/query/query.models';
import { MatDialog } from '@angular/material/dialog';
import { map } from 'rxjs/operators';
import {
  AlarmRuleComplexFilterPredicateDialogComponent,
  AlarmRuleComplexFilterPredicateDialogData
} from "@home/components/alarm-rules/filter/alarm-rule-complex-filter-predicate-dialog.component";
import {
  AlarmRuleBooleanOperation,
  AlarmRuleFilterPredicate,
  AlarmRuleFilterPredicateType,
  AlarmRuleNumericOperation,
  AlarmRulePredicateInfo,
  AlarmRuleStringOperation,
  ComplexAlarmRuleFilterPredicate
} from "@shared/models/alarm-rule.models";
import { CalculatedFieldArgument } from "@shared/models/calculated-field.models";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";

@Component({
  selector: 'tb-alarm-rule-filter-predicate-list',
  templateUrl: './alarm-rule-filter-predicate-list.component.html',
  styleUrls: ['./alarm-rule-filter-predicate-list.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AlarmRuleFilterPredicateListComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => AlarmRuleFilterPredicateListComponent),
      multi: true
    }
  ]
})
export class AlarmRuleFilterPredicateListComponent implements ControlValueAccessor, Validator {

  @Input() disabled: boolean;

  @Input() valueType: EntityKeyValueType;

  @Input() operation: ComplexOperation = ComplexOperation.AND;

  @Input() arguments: Record<string, CalculatedFieldArgument>;

  @Input() argumentInUse: string;

  filterListFormGroup = this.fb.group({
    predicates: this.fb.array([])
  });

  valueTypeEnum = EntityKeyValueType;

  complexOperationTranslations = complexOperationTranslationMap;

  private propagateChange= (v: any) => { };

  constructor(private fb: FormBuilder,
              private dialog: MatDialog,
              private destroyRef: DestroyRef) {
    this.filterListFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateModel());
  }

  get predicatesFormArray(): FormArray {
    return this.filterListFormGroup.get('predicates') as FormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.filterListFormGroup.disable({emitEvent: false});
    } else {
      this.filterListFormGroup.enable({emitEvent: false});
    }
  }

  validate(control: AbstractControl): ValidationErrors | null {
    return this.filterListFormGroup.valid ? null : {
      filterList: {valid: false}
    };
  }

  writeValue(predicates: Array<AlarmRulePredicateInfo>): void {
    const predicateControls: Array<AbstractControl> = [];
    if (predicates) {
      for (const predicate of predicates) {
        predicateControls.push(this.fb.control(predicate, [Validators.required]));
      }
    }
    this.predicatesFormArray.clear();
    predicateControls.forEach(predicate => this.predicatesFormArray.push(predicate));
  }

  public removePredicate(index: number) {
    this.predicatesFormArray.removeAt(index);
  }

  public addPredicate(complex: boolean) {
    const predicatesFormArray = this.filterListFormGroup.get('predicates') as FormArray;
    const predicate = this.createDefaultFilterPredicate(this.valueType, complex);
    let observable: Observable<AlarmRuleFilterPredicate>;
    if (complex) {
      observable = this.openComplexFilterDialog(predicate as ComplexAlarmRuleFilterPredicate);
    } else {
      observable = of(predicate);
    }
    observable.subscribe((result) => {
      if (result) {
        predicatesFormArray.push(this.fb.control(result, [Validators.required]));
      }
    });
  }

  private createDefaultFilterPredicate(valueType: EntityKeyValueType, complex: boolean): AlarmRuleFilterPredicate {
    const predicate = {
      type: complex ? AlarmRuleFilterPredicateType.COMPLEX : entityKeyValueTypeToFilterPredicateType(valueType)
    } as AlarmRuleFilterPredicate;
    switch (predicate.type) {
      case AlarmRuleFilterPredicateType.STRING:
        predicate.operation = AlarmRuleStringOperation.STARTS_WITH;
        predicate.value = {
          staticValue: ''
        };
        predicate.ignoreCase = false;
        break;
      case AlarmRuleFilterPredicateType.NUMERIC:
        predicate.operation = AlarmRuleNumericOperation.EQUAL;
        predicate.value = {
          staticValue: valueType === EntityKeyValueType.DATE_TIME ? Date.now() : 0
        };
        break;
      case AlarmRuleFilterPredicateType.BOOLEAN:
        predicate.operation = AlarmRuleBooleanOperation.EQUAL;
        predicate.value = {
          staticValue: false
        };
        break;
      case AlarmRuleFilterPredicateType.COMPLEX:
        predicate.operation = ComplexOperation.AND;
        predicate.predicates = [];
        break;
    }
    return predicate;
  }

  private openComplexFilterDialog(predicate: ComplexAlarmRuleFilterPredicate): Observable<ComplexAlarmRuleFilterPredicate> {
    return this.dialog.open<AlarmRuleComplexFilterPredicateDialogComponent, AlarmRuleComplexFilterPredicateDialogData,
      ComplexAlarmRuleFilterPredicate>(AlarmRuleComplexFilterPredicateDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        complexPredicate: predicate as ComplexAlarmRuleFilterPredicate,
        valueType: this.valueType,
        isAdd: true,
        arguments: this.arguments,
        argumentInUse: this.argumentInUse,
        readonly: this.disabled
      }
    }).afterClosed().pipe(
      map(result => result)
    );
  }

  private updateModel() {
    this.propagateChange(this.filterListFormGroup.get('predicates').value);
  }
}
