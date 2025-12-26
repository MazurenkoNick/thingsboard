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

import { DestroyRef, inject, Injectable } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { pairwise, switchMap } from 'rxjs/operators';
import { Observable } from 'rxjs';
import {
  CalculatedField,
  CalculatedFieldConfiguration,
  CalculatedFieldEventArguments,
  CalculatedFieldType,
  OutputStrategyType
} from '@shared/models/calculated-field.models';
import { oneSpaceInsideRegex } from '@shared/models/regex.constants';
import { isDefined } from '@core/utils';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { CalculatedFieldsTableEntity } from '@home/components/calculated-fields/calculated-fields-table-config';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityType } from '@shared/models/entity-type.models';

@Injectable({ providedIn: 'root' })
export class CalculatedFieldFormService {
  private fb = inject(FormBuilder);
  private calculatedFieldsService = inject(CalculatedFieldsService);

  buildForm(): FormGroup {
    return this.fb.group({
      name: ['', [Validators.required, Validators.pattern(oneSpaceInsideRegex), Validators.maxLength(255)]],
      entityId: [{type: EntityType.DEVICE_PROFILE, id: null}, Validators.required],
      type: [CalculatedFieldType.SIMPLE],
      debugSettings: [],
      configuration: this.fb.control<CalculatedFieldConfiguration>({} as CalculatedFieldConfiguration),
    });
  }

  setupTypeChange(form: FormGroup, destroyRef: DestroyRef, isEditActive?: () => boolean): void {
    form.get('type').valueChanges.pipe(
      pairwise(),
      takeUntilDestroyed(destroyRef)
    ).subscribe(([prevType, nextType]) => {
      const shouldCheck = isEditActive ? isEditActive() : true;
      if (shouldCheck) {
        if (![CalculatedFieldType.SIMPLE, CalculatedFieldType.SCRIPT].includes(prevType) ||
          ![CalculatedFieldType.SIMPLE, CalculatedFieldType.SCRIPT].includes(nextType)) {
          form.get('configuration').setValue({} as CalculatedFieldConfiguration, { emitEvent: false });
        }
      }
    });
  }

  prepareConfig(configuration: CalculatedFieldConfiguration): CalculatedFieldConfiguration {
    const config = configuration || ({} as CalculatedFieldConfiguration);
    if (config.type !== CalculatedFieldType.ALARM) {
      if (isDefined(config?.output) && !config?.output?.strategy) {
        config.output.strategy = { type: OutputStrategyType.RULE_CHAIN };
      }
    }
    return config;
  }

  testScript(
    calculatedFieldId: string,
    formValue: CalculatedField,
    testDialogFn: (calculatedField: CalculatedFieldsTableEntity, argumentsObj?: CalculatedFieldEventArguments, openCalculatedFieldEdit?: boolean, expression?: string) => Observable<string>,
    destroyRef: DestroyRef,
    expression?: string,
  ): Observable<string> {
    if (calculatedFieldId) {
      return this.calculatedFieldsService.getLatestCalculatedFieldDebugEvent(calculatedFieldId, {ignoreLoading: true})
        .pipe(
          switchMap(event => {
            const args = event?.arguments ? JSON.parse(event.arguments) : null;
            return testDialogFn(formValue, args, false, expression);
          }),
          takeUntilDestroyed(destroyRef)
        );
    }
    return testDialogFn(formValue, null, false, expression);
  }
}