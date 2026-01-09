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

import { booleanAttribute, Component, forwardRef, Input } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { EntityId } from '@shared/models/id/entity-id';
import { Observable, of } from 'rxjs';
import {
  CalculatedFieldOutput,
  CalculatedFieldRelatedAggregationConfiguration,
  CalculatedFieldType,
  defaultCalculatedFieldOutput,
  getCalculatedFieldArgumentsEditorCompleter,
  getCalculatedFieldArgumentsHighlights,
  notEmptyObjectValidator,
  OutputType,
  PropagationDirectionTranslations
} from '@shared/models/calculated-field.models';
import { map } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ScriptLanguage } from '@app/shared/models/rule-node.models';
import { EntitySearchDirection } from '@shared/models/relation.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-related-entities-aggregation-component',
  templateUrl: './related-entities-aggregation-component.component.html',
  styleUrl: './related-entities-aggregation-component.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RelatedEntitiesAggregationComponentComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => RelatedEntitiesAggregationComponentComponent),
      multi: true
    }
  ],
})
export class RelatedEntitiesAggregationComponentComponent implements ControlValueAccessor, Validator {

  @Input({required: true})
  entityId: EntityId;

  @Input({required: true})
  tenantId: string;

  @Input({required: true})
  entityName: string;

  @Input({required: true})
  testScript: (expression?: string) => Observable<string>;

  @Input({transform: booleanAttribute}) isEditValue = true;

  @Input({ transform: booleanAttribute })
  readonly: boolean;

  readonly ScriptLanguage = ScriptLanguage;
  readonly CalculatedFieldType = CalculatedFieldType;
  readonly OutputType = OutputType;
  readonly Directions = Object.values(EntitySearchDirection) as Array<EntitySearchDirection>;
  readonly PropagationDirectionTranslations = PropagationDirectionTranslations;
  readonly minAllowedDeduplicationIntervalInSecForCF = getCurrentAuthState(this.store).minAllowedDeduplicationIntervalInSecForCF;

  relatedAggregationConfiguration = this.fb.group({
    relation: this.fb.group({
      direction: [EntitySearchDirection.FROM, Validators.required],
      relationType: ['Contains', Validators.required],
    }),
    arguments: this.fb.control({}, notEmptyObjectValidator()),
    metrics: this.fb.control({}, notEmptyObjectValidator()),
    deduplicationIntervalInSec: [this.minAllowedDeduplicationIntervalInSecForCF],
    output: this.fb.control<CalculatedFieldOutput>(defaultCalculatedFieldOutput),
    useLatestTs: [false]
  });

  arguments$ = this.relatedAggregationConfiguration.get('arguments').valueChanges.pipe(
    map(argumentsObj => Object.keys(argumentsObj))
  );

  argumentsEditorCompleter$ = this.relatedAggregationConfiguration.get('arguments').valueChanges.pipe(
    map(argumentsObj => getCalculatedFieldArgumentsEditorCompleter(argumentsObj ?? {}))
  );

  argumentsHighlightRules$ = this.relatedAggregationConfiguration.get('arguments').valueChanges.pipe(
    map(argumentsObj => getCalculatedFieldArgumentsHighlights(argumentsObj))
  );

  private readonly minAllowedScheduledUpdateIntervalInSecForCF = getCurrentAuthState(this.store).minAllowedScheduledUpdateIntervalInSecForCF;
  private propagateChange: (config: CalculatedFieldRelatedAggregationConfiguration) => void = () => { };

  constructor(private fb: FormBuilder,
              private store: Store<AppState>) {

    this.relatedAggregationConfiguration.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((value: CalculatedFieldRelatedAggregationConfiguration) => {
      this.updatedModel(value);
    })
  }

  validate(): ValidationErrors | null {
    return this.relatedAggregationConfiguration.valid || this.relatedAggregationConfiguration.disabled ? null : {invalidPropagateConfig: false};
  }

  writeValue(value: CalculatedFieldRelatedAggregationConfiguration): void {
    this.relatedAggregationConfiguration.patchValue(value, {emitEvent: false});
    setTimeout(() => {
      this.relatedAggregationConfiguration.get('arguments').updateValueAndValidity({onlySelf: true});
    });
  }

  registerOnChange(fn: (config: CalculatedFieldRelatedAggregationConfiguration) => void): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_: any): void { }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.relatedAggregationConfiguration.disable({emitEvent: false});
    } else {
      this.relatedAggregationConfiguration.enable({emitEvent: false});
    }
  }

  fetchOptions(searchText: string): Observable<Array<string>> {
    const search = searchText ? searchText?.toLowerCase() : '';
    return of(['Contains', 'Manages']).pipe(map(name => name?.filter(option => option.toLowerCase().includes(search))));
  }

  private updatedModel(value: CalculatedFieldRelatedAggregationConfiguration): void {
    value.type = CalculatedFieldType.RELATED_ENTITIES_AGGREGATION;
    value.scheduledUpdateInterval = this.minAllowedScheduledUpdateIntervalInSecForCF;
    this.propagateChange(value);
  }
}
