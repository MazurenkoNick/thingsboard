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

import { Component, DestroyRef, Inject, ViewEncapsulation } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import {
  CalculatedField,
  CalculatedFieldConfiguration,
  CalculatedFieldTestScriptFn,
  CalculatedFieldType,
  CalculatedFieldTypeTranslations,
  OutputStrategyType
} from '@shared/models/calculated-field.models';
import { oneSpaceInsideRegex } from '@shared/models/regex.constants';
import { EntityType } from '@shared/models/entity-type.models';
import { pairwise, switchMap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { Observable } from 'rxjs';
import { EntityId } from '@shared/models/id/entity-id';
import { AdditionalDebugActionConfig } from '@home/components/entity/debug/entity-debug-settings.model';
import { deepTrim, isDefined } from '@core/utils';

export interface CalculatedFieldDialogData {
  value?: CalculatedField;
  buttonTitle: string;
  entityId: EntityId;
  tenantId: string;
  entityName?: string;
  ownerId: EntityId;
  additionalDebugActionConfig: AdditionalDebugActionConfig<(calculatedField: CalculatedField) => void>;
  getTestScriptDialogFn: CalculatedFieldTestScriptFn;
  isDirty?: boolean;
  readonly: boolean;
}

@Component({
  selector: 'tb-calculated-field-dialog',
  templateUrl: './calculated-field-dialog.component.html',
  styleUrls: ['./calculated-field-dialog.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class CalculatedFieldDialogComponent extends DialogComponent<CalculatedFieldDialogComponent, CalculatedField> {

  fieldFormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.pattern(oneSpaceInsideRegex), Validators.maxLength(255)]],
    type: [CalculatedFieldType.SIMPLE],
    debugSettings: [],
    configuration: this.fb.control<CalculatedFieldConfiguration>({} as CalculatedFieldConfiguration),
  });

  additionalDebugActionConfig = this.data.value?.id ? {
    ...this.data.additionalDebugActionConfig,
    action: () => this.data.additionalDebugActionConfig.action({ id: this.data.value.id, ...this.fromGroupValue }),
  } : null;

  readonly EntityType = EntityType;
  readonly CalculatedFieldType = CalculatedFieldType;
  readonly fieldTypes = Object.values(CalculatedFieldType).filter(type => type !== CalculatedFieldType.ALARM) as CalculatedFieldType[];
  readonly CalculatedFieldTypeTranslations = CalculatedFieldTypeTranslations;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: CalculatedFieldDialogData,
              protected dialogRef: MatDialogRef<CalculatedFieldDialogComponent, CalculatedField>,
              private calculatedFieldsService: CalculatedFieldsService,
              private destroyRef: DestroyRef,
              private fb: FormBuilder) {
    super(store, router, dialogRef);
    this.observeIsLoading();
    this.observeType();
    this.applyDialogData();

    if (this.data.readonly) {
      this.fieldFormGroup.disable();
    }
  }

  get fromGroupValue(): CalculatedField {
    return deepTrim(this.fieldFormGroup.value as CalculatedField);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    if (this.fieldFormGroup.valid) {
      this.calculatedFieldsService.saveCalculatedField({ entityId: this.data.entityId, ...(this.data.value ?? {}),  ...this.fromGroupValue})
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(calculatedField => this.dialogRef.close(calculatedField));
    }
  }

  onTestScript(expression?: string): Observable<string> {
    const calculatedFieldId = this.data.value?.id?.id;
    if (calculatedFieldId) {
      return this.calculatedFieldsService.getLatestCalculatedFieldDebugEvent(calculatedFieldId, {ignoreLoading: true})
        .pipe(
          switchMap(event => {
            const args = event?.arguments ? JSON.parse(event.arguments) : null;
            return this.data.getTestScriptDialogFn(this.fromGroupValue, args, false, expression);
          }),
          takeUntilDestroyed(this.destroyRef)
        )
    }
    return this.data.getTestScriptDialogFn(this.fromGroupValue, null, false, expression);
  }

  private applyDialogData(): void {
    const { configuration = {} as CalculatedFieldConfiguration, type = CalculatedFieldType.SIMPLE, debugSettings = { failuresEnabled: true, allEnabled: true }, ...value } = this.data.value ?? {};
    if (configuration.type !== CalculatedFieldType.ALARM) {
      if (isDefined(configuration?.output) && !configuration?.output?.strategy) {
          configuration.output.strategy = {type: OutputStrategyType.RULE_CHAIN};
      }
    }
    this.fieldFormGroup.patchValue({ configuration, type, debugSettings, ...value }, {emitEvent: false});
    setTimeout(() => this.fieldFormGroup.get('type').updateValueAndValidity({onlySelf: true}));
  }

  private observeIsLoading(): void {
    this.isLoading$.pipe(takeUntilDestroyed()).subscribe(loading => {
      if (loading) {
        this.fieldFormGroup.disable({emitEvent: false});
      } else if (!this.data.readonly) {
        this.fieldFormGroup.enable({emitEvent: false});
        if (this.data.isDirty) {
          this.fieldFormGroup.markAsDirty();
        }
      }
    });
  }

  private observeType(): void {
    this.fieldFormGroup.get('type').valueChanges.pipe(
      pairwise(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(([prevType, nextType]) => {
      if (![CalculatedFieldType.SIMPLE, CalculatedFieldType.SCRIPT].includes(prevType) ||
          ![CalculatedFieldType.SIMPLE, CalculatedFieldType.SCRIPT].includes(nextType)) {
        this.fieldFormGroup.get('configuration').setValue(({} as CalculatedFieldConfiguration), {emitEvent: false});
      }
    });
  }
}
