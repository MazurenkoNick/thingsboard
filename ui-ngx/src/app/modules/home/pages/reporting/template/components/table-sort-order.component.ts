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

import { Component, DestroyRef, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TableSortOrder } from '@shared/models/report-component.models';
import { Direction } from '@shared/models/page/sort-order';

@Component({
  selector: 'tb-table-sort-order',
  templateUrl: './table-sort-order.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TableSortOrderComponent),
      multi: true
    }
  ]
})
export class TableSortOrderComponent implements OnInit, ControlValueAccessor, OnChanges {

  Direction = Direction;

  @Input()
  set columnNameChange(args: [string, string]) {
    if (this.tableSortOrderFormGroup && Array.isArray(args) && args.length === 2) {
      const columnIndex = this.columns.indexOf(args[0]);
      let updatedFormValue = false;
      if(columnIndex !== -1) {
        this.columns[columnIndex] = args[1];
        updatedFormValue = true;
      } else if (this.columns.indexOf(args[1]) !== -1) {
        updatedFormValue = true;
      }
      if (updatedFormValue && this.tableSortOrderFormGroup.get('column').value === args[0]) {
        this.tableSortOrderFormGroup.get('column').setValue(args[1]);
      }
    }
  }

  @Input()
  columns: string[] = [];

  @Input()
  disabled: boolean;

  private modelValue: TableSortOrder;

  private propagateChange = null;

  public tableSortOrderFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.tableSortOrderFormGroup = this.fb.group(
      {
        column: [null],
        direction: [Direction.ASC]
      }
    )
    this.tableSortOrderFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  ngOnChanges(changes: SimpleChanges) {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'columns') {
          this.checkColumn();
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
      this.tableSortOrderFormGroup.disable({emitEvent: false});
    } else {
      this.tableSortOrderFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: TableSortOrder): void {
    this.modelValue = value;
    this.tableSortOrderFormGroup.patchValue(
      this.modelValue, {emitEvent: false}
    );
    if (!this.tableSortOrderFormGroup.get('direction').value) {
      this.tableSortOrderFormGroup.get('direction').patchValue(Direction.ASC, {emitEvent: false});
    }
    this.checkColumn();
  }

  private updateModel() {
    this.modelValue = this.tableSortOrderFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }

  private checkColumn() {
    const column: string = this.tableSortOrderFormGroup.get('column').value;
    if (column) {
      if (!this.columns.includes(column)) {
        this.tableSortOrderFormGroup.get('column').patchValue(null);
      }
    }
  }
}
