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
  calculatedFieldDefaultScript,
  CalculatedFieldOutput,
  CalculatedFieldPropagationConfiguration,
  CalculatedFieldType,
  defaultCalculatedFieldOutput,
  getCalculatedFieldArgumentsEditorCompleter,
  getCalculatedFieldArgumentsHighlights,
  notEmptyObjectValidator,
  OutputType,
  PropagationDirectionTranslations,
  PropagationWithExpression
} from '@shared/models/calculated-field.models';
import { map } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ScriptLanguage } from '@app/shared/models/rule-node.models';
import { EntitySearchDirection } from '@shared/models/relation.models';
import {Store} from "@ngrx/store";
import {AppState} from "@core/core.state";
import {getCurrentAuthState} from "@core/auth/auth.selectors";

@Component({
  selector: 'tb-propagation-configuration',
  templateUrl: './propagation-configuration.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PropagationConfigurationComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => PropagationConfigurationComponent),
      multi: true
    }
  ],
})
export class PropagationConfigurationComponent implements ControlValueAccessor, Validator {

  @Input({required: true})
  entityId: EntityId;

  @Input({required: true})
  tenantId: string;

  @Input({required: true})
  entityName: string;

  @Input({required: true})
  ownerId: EntityId;

  @Input({ transform: booleanAttribute })
  readonly: boolean;

  @Input({required: true})
  testScript: () => Observable<string>;

  @Input({transform: booleanAttribute}) isEditValue = true;

  readonly maxRelatedEntitiesToReturnPerCfArgument = getCurrentAuthState(this.store).maxRelatedEntitiesToReturnPerCfArgument;

  propagateConfiguration = this.fb.group({
    arguments: this.fb.control({}, notEmptyObjectValidator()),
    applyExpressionToResolvedArguments: [false],
    relation: this.fb.group({
      direction: [EntitySearchDirection.TO, Validators.required],
      relationType: ['Contains', Validators.required],
    }),
    expression: [calculatedFieldDefaultScript],
    output: this.fb.control<CalculatedFieldOutput>(defaultCalculatedFieldOutput),
  });

  disabled = false;

  readonly ScriptLanguage = ScriptLanguage;
  readonly CalculatedFieldType = CalculatedFieldType;
  readonly OutputType = OutputType;
  readonly Directions = Object.values(EntitySearchDirection) as Array<EntitySearchDirection>;
  readonly PropagationDirectionTranslations = PropagationDirectionTranslations;

  functionArgs$ = this.propagateConfiguration.get('arguments').valueChanges.pipe(
    map(argumentsObj => ['ctx', ...Object.keys(argumentsObj)])
  );

  argumentsEditorCompleter$ = this.propagateConfiguration.get('arguments').valueChanges.pipe(
    map(argumentsObj => getCalculatedFieldArgumentsEditorCompleter(argumentsObj ?? {}))
  );

  argumentsHighlightRules$ = this.propagateConfiguration.get('arguments').valueChanges.pipe(
    map(argumentsObj => getCalculatedFieldArgumentsHighlights(argumentsObj))
  );

  private propagateChange: (config: CalculatedFieldPropagationConfiguration) => void = () => { };

  constructor(private fb: FormBuilder,
              private store: Store<AppState>) {
    this.propagateConfiguration.get('applyExpressionToResolvedArguments').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(() => {
      this.updatedFormWithScript();
    })

    this.propagateConfiguration.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((value: CalculatedFieldPropagationConfiguration) => {
      this.updatedModel(value);
    })
  }

  validate(): ValidationErrors | null {
    return this.propagateConfiguration.valid || this.propagateConfiguration.disabled ? null : {invalidPropagateConfig: false};
  }

  writeValue(value: PropagationWithExpression): void {
    value.expression = value.expression ?? calculatedFieldDefaultScript;
    this.propagateConfiguration.patchValue(value, {emitEvent: false});
    if (!this.disabled) {
      this.updatedFormWithScript();
    }
    setTimeout(() => {
      this.propagateConfiguration.get('arguments').updateValueAndValidity({onlySelf: true});
    });
  }

  registerOnChange(fn: (config: CalculatedFieldPropagationConfiguration) => void): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_: any): void { }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.propagateConfiguration.disable({emitEvent: false});
    } else {
      this.propagateConfiguration.enable({emitEvent: false});
      this.updatedFormWithScript();
    }
  }

  onTestScript() {
    this.testScript().subscribe((expression) => {
      this.propagateConfiguration.get('expression').setValue(expression);
      this.propagateConfiguration.get('expression').markAsDirty();
    })
  }

  fetchOptions(searchText: string): Observable<Array<string>> {
    const search = searchText ? searchText?.toLowerCase() : '';
    return of(['Contains', 'Manages']).pipe(map(name => name?.filter(option => option.toLowerCase().includes(search))));
  }

  private updatedModel(value: CalculatedFieldPropagationConfiguration): void {
    value.type = CalculatedFieldType.PROPAGATION;
    this.propagateChange(value);
  }

  private updatedFormWithScript() {
    if (this.propagateConfiguration.get('applyExpressionToResolvedArguments').value) {
      this.propagateConfiguration.get('expression').enable({emitEvent: false});
    } else {
      this.propagateConfiguration.get('expression').disable({emitEvent: false});
    }
  }
}
