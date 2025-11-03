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

import { Component, forwardRef, Input } from '@angular/core';
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
import {
  AggInterval,
  AggIntervalType,
  AggIntervalTypeTranslations,
  CalculatedFieldEntityAggregationConfiguration,
  CalculatedFieldOutput,
  CalculatedFieldType,
  notEmptyObjectValidator,
  OutputType
} from '@shared/models/calculated-field.models';
import { map } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HOUR, MINUTE, SECOND } from '@shared/models/time/time.models';
import { isDefinedAndNotNull } from '@core/utils';

interface CalculatedFieldEntityAggregationConfigurationValue extends CalculatedFieldEntityAggregationConfiguration {
  interval: AggInterval & {allowOffsetSec?: boolean};
  allowWatermark: boolean;
}

@Component({
  selector: 'tb-entity-aggregation-component',
  templateUrl: './entity-aggregation-component.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EntityAggregationComponentComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => EntityAggregationComponentComponent),
      multi: true
    }
  ],
})
export class EntityAggregationComponentComponent implements ControlValueAccessor, Validator {

  @Input({required: true})
  entityId: EntityId;

  @Input({required: true})
  tenantId: string;

  @Input({required: true})
  entityName: string;


  entityAggregationConfiguration = this.fb.group({
    arguments: this.fb.control({}, notEmptyObjectValidator()),
    metrics: this.fb.control({}, notEmptyObjectValidator()),
    interval: this.fb.group({
      type: [AggIntervalType.HOUR],
      tz: ['', Validators.required],
      durationSec: [HOUR/SECOND, Validators.required],
      allowOffsetSec: [false],
      offsetSec: [MINUTE/SECOND, Validators.required],
    }),
    allowWatermark: [false],
    watermark: this.fb.group({
      duration: [HOUR/SECOND, Validators.required],
      checkInterval: [10 * MINUTE / SECOND, Validators.required],
    }),
    output: this.fb.control<CalculatedFieldOutput>({
      type: OutputType.Timeseries,
    }),
  });

  arguments$ = this.entityAggregationConfiguration.get('arguments').valueChanges.pipe(
    map(argumentsObj => Object.keys(argumentsObj))
  );

  AggIntervalType = AggIntervalType;
  AggIntervalTypes = Object.values(AggIntervalType) as AggIntervalType[];
  AggIntervalTypeTranslations = AggIntervalTypeTranslations;

  private propagateChange: (config: CalculatedFieldEntityAggregationConfiguration) => void = () => { };

  constructor(private fb: FormBuilder) {

    this.entityAggregationConfiguration.get('interval.type').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((type: AggIntervalType) => {
      this.checkAggIntervalType(type);
    });

    this.entityAggregationConfiguration.get('interval.allowOffsetSec').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((allow: boolean) => {
      this.checkIntervalDuration(allow);
    });

    this.entityAggregationConfiguration.get('allowWatermark').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((allow: boolean) => {
      this.checkWatermark(allow);
    });

    this.entityAggregationConfiguration.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((value: CalculatedFieldEntityAggregationConfigurationValue) => {
      this.updatedModel(value);
    });
  }

  validate(): ValidationErrors | null {
    return this.entityAggregationConfiguration.valid || this.entityAggregationConfiguration.disabled ? null : {invalidPropagateConfig: false};
  }

  writeValue(value: CalculatedFieldEntityAggregationConfiguration): void {
    const data: CalculatedFieldEntityAggregationConfigurationValue = {
      ...value,
      allowWatermark: isDefinedAndNotNull(value.watermark),
      interval: {...value.interval, allowOffsetSec: isDefinedAndNotNull(value?.interval?.offsetSec)}
    }
    this.entityAggregationConfiguration.patchValue(data, {emitEvent: false});
    this.checkAggIntervalType(this.entityAggregationConfiguration.get('interval.type').value);
    this.checkIntervalDuration(this.entityAggregationConfiguration.get('interval.allowOffsetSec').value);
    this.checkWatermark(this.entityAggregationConfiguration.get('allowWatermark').value);
    setTimeout(() => {
      this.entityAggregationConfiguration.get('arguments').updateValueAndValidity({onlySelf: true});
    });
  }

  registerOnChange(fn: (config: CalculatedFieldEntityAggregationConfiguration) => void): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_: any): void { }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.entityAggregationConfiguration.disable({emitEvent: false});
    } else {
      this.entityAggregationConfiguration.enable({emitEvent: false});
      this.checkAggIntervalType(this.entityAggregationConfiguration.get('interval.type').value);
      this.checkIntervalDuration(this.entityAggregationConfiguration.get('interval.allowOffsetSec').value);
      this.checkWatermark(this.entityAggregationConfiguration.get('allowWatermark').value);
    }
  }

  private updatedModel(value: CalculatedFieldEntityAggregationConfigurationValue): void {
    value.type = CalculatedFieldType.ENTITY_AGGREGATION;
    if (!value.interval.allowOffsetSec) {
      delete value.interval.offsetSec;
    }
    delete value.interval.offsetSec;
    if (!value.allowWatermark) {
      delete value.watermark;
    }
    delete value.allowWatermark;
    this.propagateChange(value);
  }

  private checkAggIntervalType(type: AggIntervalType) {
    if (type === AggIntervalType.CUSTOM) {
      this.entityAggregationConfiguration.get('interval.durationSec').enable({emitEvent: false});
    } else {
      this.entityAggregationConfiguration.get('interval.durationSec').disable({emitEvent: false});
    }
  }

  private checkIntervalDuration(allow: boolean) {
    if (allow) {
      this.entityAggregationConfiguration.get('interval.offsetSec').enable({emitEvent: false});
    } else {
      this.entityAggregationConfiguration.get('interval.offsetSec').disable({emitEvent: false});
    }
  }

  private checkWatermark(allow: boolean) {
    if (allow) {
      this.entityAggregationConfiguration.get('watermark').enable({emitEvent: false});
    } else {
      this.entityAggregationConfiguration.get('watermark').disable({emitEvent: false});
    }
  }
}
