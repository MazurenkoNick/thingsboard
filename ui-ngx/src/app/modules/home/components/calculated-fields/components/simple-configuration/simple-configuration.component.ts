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

import { booleanAttribute, Component, forwardRef, Input, OnChanges, SimpleChanges } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { oneSpaceInsideRegex } from '@shared/models/regex.constants';
import {
  calculatedFieldDefaultScript,
  CalculatedFieldScriptConfiguration,
  CalculatedFieldSimpleConfiguration,
  CalculatedFieldSimpleOutput,
  CalculatedFieldType,
  defaultSimpleCalculatedFieldOutput,
  getCalculatedFieldArgumentsEditorCompleter,
  getCalculatedFieldArgumentsHighlights,
  OutputType
} from '@shared/models/calculated-field.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { deepClone } from '@core/utils';
import { EntityId } from '@shared/models/id/entity-id';
import { Observable } from 'rxjs';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { map } from 'rxjs/operators';

type SimpeConfiguration = CalculatedFieldSimpleConfiguration | CalculatedFieldScriptConfiguration;

@Component({
  selector: 'tb-simple-configuration',
  templateUrl: './simple-configuration.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SimpleConfigurationComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => SimpleConfigurationComponent),
      multi: true
    }
  ],
})
export class SimpleConfigurationComponent implements ControlValueAccessor, Validator, OnChanges {

  @Input()
  isScript: boolean;

  @Input({required: true})
  entityId: EntityId;

  @Input({required: true})
  tenantId: string;

  @Input({required: true})
  entityName: string;

  @Input({required: true})
  ownerId: EntityId;

  @Input({required: true})
  testScript: () => Observable<string>;

  @Input({ transform: booleanAttribute })
  readonly: boolean;

  simpleConfiguration = this.fb.group({
    arguments: this.fb.control({}),
    expressionSIMPLE: ['', [Validators.required, Validators.pattern(oneSpaceInsideRegex), Validators.maxLength(255)]],
    expressionSCRIPT: [calculatedFieldDefaultScript],
    output: this.fb.control<CalculatedFieldSimpleOutput>(defaultSimpleCalculatedFieldOutput),
    useLatestTs: [false]
  });

  readonly ScriptLanguage = ScriptLanguage;
  readonly OutputType = OutputType;

  functionArgs$ = this.simpleConfiguration.get('arguments').valueChanges.pipe(
    map(argumentsObj => ['ctx', ...Object.keys(argumentsObj)])
  );

  argumentsEditorCompleter$ = this.simpleConfiguration.get('arguments').valueChanges.pipe(
    map(argumentsObj => getCalculatedFieldArgumentsEditorCompleter(argumentsObj ?? {}))
  );

  argumentsHighlightRules$ = this.simpleConfiguration.get('arguments').valueChanges.pipe(
    map(argumentsObj => getCalculatedFieldArgumentsHighlights(argumentsObj))
  );

  private propagateChange: (config: SimpeConfiguration) => void = () => { };

  constructor(private fb: FormBuilder) {
    this.simpleConfiguration.get('output').valueChanges.pipe(
      takeUntilDestroyed(),
    ).subscribe(() => {
      this.toggleScopeByOutputType();
    });

    this.simpleConfiguration.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((value) => {
      const { expressionSIMPLE, expressionSCRIPT, ...config } = value;
      const cfConfig = config as SimpeConfiguration;
      cfConfig.expression = this.isScript ? expressionSCRIPT : expressionSIMPLE;
      this.updatedModel(cfConfig);
    })
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (change.currentValue !== change.previousValue) {
        if (propName === 'isScript') {
          this.updatedFormWithScript();
          if (!change.firstChange) {
            this.simpleConfiguration.updateValueAndValidity();
          }
        }
      }
    }
  }

  validate(): ValidationErrors | null {
    return this.simpleConfiguration.valid || this.simpleConfiguration.disabled ? null : {invalidSimpleConfig: false};
  }

  writeValue(value: SimpeConfiguration): void {
    const formValue: any = deepClone(value);
    if (this.isScript) {
      formValue.expressionSCRIPT = formValue.expression ?? calculatedFieldDefaultScript;
    } else {
      formValue.expressionSIMPLE = formValue.expression;
    }
    this.simpleConfiguration.patchValue(formValue, {emitEvent: false});
    this.updatedFormWithScript();
    setTimeout(() => {
      this.simpleConfiguration.get('arguments').updateValueAndValidity({onlySelf: true});
    });
  }

  registerOnChange(fn: (config: SimpeConfiguration) => void): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.simpleConfiguration.disable({emitEvent: false});
    } else {
      this.simpleConfiguration.enable({emitEvent: false});
      this.updatedFormWithScript();
    }
  }

  onTestScript() {
    this.testScript().subscribe((expression) => {
      this.simpleConfiguration.get('expressionSCRIPT').setValue(expression);
      this.simpleConfiguration.get('expressionSCRIPT').markAsDirty();
    })
  }

  private updatedModel(value: SimpeConfiguration): void {
    value.type = this.isScript ? CalculatedFieldType.SCRIPT : CalculatedFieldType.SIMPLE;
    this.propagateChange(value);
  }

  private updatedFormWithScript() {
    if (this.isScript) {
      this.simpleConfiguration.get('expressionSIMPLE').disable({emitEvent: false});
      this.simpleConfiguration.get('expressionSCRIPT').enable({emitEvent: false});
    } else {
      this.simpleConfiguration.get('expressionSIMPLE').enable({emitEvent: false});
      this.simpleConfiguration.get('expressionSCRIPT').disable({emitEvent: false});
    }
    this.toggleScopeByOutputType();
  }

  private toggleScopeByOutputType(): void {
    if (this.isScript || this.simpleConfiguration.get('output').value.type === OutputType.Attribute) {
      this.simpleConfiguration.get('useLatestTs').disable({emitEvent: false});
    } else {
      this.simpleConfiguration.get('useLatestTs').enable({emitEvent: false});
    }
  }
}
