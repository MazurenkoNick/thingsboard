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

import { Component, DestroyRef, forwardRef, inject, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import {
  CalculatedFieldOutput,
  CalculatedFieldSimpleOutput,
  OutputStrategyType,
  OutputStrategyTypeTranslations,
  OutputType,
  OutputTypeTranslations
} from '@shared/models/calculated-field.models';
import { digitsRegex, oneSpaceInsideRegex } from '@shared/models/regex.constants';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
  selector: 'tb-calculate-field-output',
  templateUrl: './calculated-field-output.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CalculatedFieldOutputComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CalculatedFieldOutputComponent),
      multi: true
    }
  ],
  styleUrls: ['./calculated-field-output.component.scss'],
})
export class CalculatedFieldOutputComponent implements ControlValueAccessor, Validator, OnInit, OnChanges {

  @Input()
  @coerceBoolean()
  simpleMode = false;

  @Input()
  @coerceBoolean()
  hiddenName = false;

  @Input({required: true})
  entityId: EntityId;

  readonly outputTypes = Object.values(OutputType) as OutputType[];
  readonly OutputType = OutputType;
  readonly AttributeScope = AttributeScope;
  readonly OutputTypeTranslations = OutputTypeTranslations;
  readonly EntityType = EntityType;

  readonly OutputStrategyType  = OutputStrategyType;
  readonly OutputStrategyTypes  = Object.values(OutputStrategyType) as OutputStrategyType[];
  readonly OutputStrategyTypeTranslations  = OutputStrategyTypeTranslations;

  private fb = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);

  outputForm = this.fb.group({
    name: ['', [Validators.required, Validators.pattern(oneSpaceInsideRegex), Validators.maxLength(255)]],
    scope: [{value: AttributeScope.SERVER_SCOPE, disabled: true}],
    type: [OutputType.Timeseries],
    decimalsByDefault: [null as number, [Validators.min(0), Validators.max(15), Validators.pattern(digitsRegex)]],
    strategy: this.fb.group({
      type: [OutputStrategyType.IMMEDIATE],
      saveTimeSeries: [true],
      saveLatest: [true],
      saveAttribute: [true],
      sendWsUpdate: [true],
      processCfs: [true],
      updateAttributesOnlyOnValueChange: [true],
      ttl: [0]
    })
  });

  private propagateChange: (config: CalculatedFieldOutput | CalculatedFieldSimpleOutput) => void = () => { };

  ngOnInit() {
    this.outputForm.get('type').valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(type => {
        this.toggleScopeByOutputType(type);
        this.updatedStrategy();
      });

    this.outputForm.get('strategy.type').valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.updatedStrategy();
      });

    this.updatedFormWithMode();

    this.outputForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value: CalculatedFieldOutput | CalculatedFieldSimpleOutput) => {
      this.updatedModel(value);
    })
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (change.currentValue !== change.previousValue) {
        if (propName === 'simpleMode') {
          this.updatedFormWithMode();
          if (!change.firstChange) {
            this.outputForm.updateValueAndValidity();
          }
        }
      }
    }
  }

  validate(): ValidationErrors | null {
    return this.outputForm.valid || this.outputForm.disabled ? null : {outputConfig: false};
  }

  writeValue(value: CalculatedFieldOutput | CalculatedFieldSimpleOutput): void {
    this.outputForm.patchValue(value, {emitEvent: false});
    this.outputForm.get('type').updateValueAndValidity({onlySelf: true});
  }

  registerOnChange(fn: (config: CalculatedFieldOutput | CalculatedFieldSimpleOutput) => void): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_: any): void { }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.outputForm.disable({emitEvent: false});
    } else {
      this.outputForm.enable({emitEvent: false});
      this.updatedFormWithMode();
      this.toggleScopeByOutputType(this.outputForm.get('type').value);
      this.updatedStrategy();
    }
  }

  toggleChip(controlName: string) {
    const control = this.outputForm.get('strategy').get(controlName);
    if (control && control.enabled) {
      control.setValue(!control.value);
    }
  }

  private updatedModel(value: CalculatedFieldOutput | CalculatedFieldSimpleOutput) {
    if (this.simpleMode && 'name' in value) {
      value.name = value.name?.trim() ?? '';
    }
    this.propagateChange(value);
  }

  private toggleScopeByOutputType(type: OutputType): void {
    if (type === OutputType.Attribute) {
      this.outputForm.get('scope').enable({emitEvent: false});
    } else {
      this.outputForm.get('scope').disable({emitEvent: false});
    }
  }

  private updatedFormWithMode(): void {
    if (this.simpleMode && !this.hiddenName) {
      this.outputForm.get('name').enable({emitEvent: false});
    } else {
      this.outputForm.get('name').disable({emitEvent: false});
    }
    if (this.simpleMode) {
      this.outputForm.get('decimalsByDefault').enable({emitEvent: false});
    } else {
      this.outputForm.get('decimalsByDefault').disable({emitEvent: false});
    }
  }

  private updatedStrategy(): void {
    const strategyType = this.outputForm.get('strategy.type').value;
    this.outputForm.get('strategy').disable({emitEvent: false});
    this.outputForm.get('strategy.type').enable({emitEvent: false});

    if (strategyType === OutputStrategyType.IMMEDIATE) {
      const outputType = this.outputForm.get('type').value;
      this.outputForm.get('strategy.sendWsUpdate').enable({emitEvent: false});
      this.outputForm.get('strategy.processCfs').enable({emitEvent: false});
      if (outputType === OutputType.Attribute) {
        this.outputForm.get('strategy.saveAttribute').enable({emitEvent: false});
        this.outputForm.get('strategy.updateAttributesOnlyOnValueChange').enable({emitEvent: false});
      } else {
        this.outputForm.get('strategy.saveTimeSeries').enable({emitEvent: false});
        this.outputForm.get('strategy.saveLatest').enable({emitEvent: false});
        this.outputForm.get('strategy.ttl').enable({emitEvent: false});
      }
    }
  }
}
