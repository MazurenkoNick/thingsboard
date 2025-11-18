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

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import {
  ComplexOperation,
  complexOperationTranslationMap,
  EntityKeyValueType,
  FilterPredicateType
} from '@shared/models/query/query.models';
import { AlarmRuleFilterPredicate, ComplexAlarmRuleFilterPredicate } from "@shared/models/alarm-rule.models";
import { CalculatedFieldArgument } from "@shared/models/calculated-field.models";

export interface AlarmRuleComplexFilterPredicateDialogData {
  complexPredicate: ComplexAlarmRuleFilterPredicate;
  isAdd: boolean;
  valueType: EntityKeyValueType;
  arguments: Record<string, CalculatedFieldArgument>;
  readonly: boolean;
  argumentInUse: string;
}

@Component({
  selector: 'tb-alarm-rule-complex-filter-predicate-dialog',
  templateUrl: './alarm-rule-complex-filter-predicate-dialog.component.html',
  providers: [],
  styleUrls: []
})

export class AlarmRuleComplexFilterPredicateDialogComponent extends
  DialogComponent<AlarmRuleComplexFilterPredicateDialogComponent, ComplexAlarmRuleFilterPredicate> {

  complexFilterFormGroup = this.fb.group(
    {
      operation: [ComplexOperation.AND, [Validators.required]],
      predicates: this.fb.control<AlarmRuleFilterPredicate[] | null>(null, Validators.required)
    }
  );

  complexOperations = Object.keys(ComplexOperation);
  complexOperationEnum = ComplexOperation;
  complexOperationTranslations = complexOperationTranslationMap;

  isAdd: boolean;

  arguments = this.data.arguments;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AlarmRuleComplexFilterPredicateDialogData,
              public dialogRef: MatDialogRef<AlarmRuleComplexFilterPredicateDialogComponent, ComplexAlarmRuleFilterPredicate>,
              private fb: FormBuilder) {
    super(store, router, dialogRef);

    this.isAdd = this.data.isAdd;

    this.complexFilterFormGroup.patchValue(this.data.complexPredicate, {emitEvent: false});
    if (this.data.readonly) {
      this.complexFilterFormGroup.disable({emitEvent: false});
    }
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    const predicate = this.complexFilterFormGroup.value as ComplexAlarmRuleFilterPredicate;
    predicate.type = FilterPredicateType.COMPLEX;
    this.dialogRef.close(predicate);
  }
}
