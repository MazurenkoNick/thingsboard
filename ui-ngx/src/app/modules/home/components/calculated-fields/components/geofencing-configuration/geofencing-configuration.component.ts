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
import {
  ArgumentEntityType,
  CalculatedFieldGeofencing,
  CalculatedFieldGeofencingConfiguration,
  CalculatedFieldOutput,
  CalculatedFieldType,
  defaultCalculatedFieldOutput,
  getCalculatedFieldCurrentEntityFilter,
  notEmptyObjectValidator
} from '@shared/models/calculated-field.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityFilter } from '@shared/models/query/query.models';
import { EntityId } from '@shared/models/id/entity-id';

@Component({
  selector: 'tb-geofencing-configuration',
  templateUrl: './geofencing-configuration.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GeofencingConfigurationComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => GeofencingConfigurationComponent),
      multi: true
    }
  ],
})
export class GeofencingConfigurationComponent implements ControlValueAccessor, Validator, OnChanges {

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

  readonly minAllowedScheduledUpdateIntervalInSecForCF = getCurrentAuthState(this.store).minAllowedScheduledUpdateIntervalInSecForCF;
  readonly DataKeyType = DataKeyType;

  geofencingConfiguration = this.fb.group({
    entityCoordinates: this.fb.group({
      latitudeKeyName: [null, [Validators.required]],
      longitudeKeyName: [null, [Validators.required]],
    }),
    zoneGroups: this.fb.control<Record<string, CalculatedFieldGeofencing>>({}, notEmptyObjectValidator()),
    scheduledUpdateEnabled: [true],
    scheduledUpdateInterval: [this.minAllowedScheduledUpdateIntervalInSecForCF],
    output: this.fb.control<CalculatedFieldOutput>(defaultCalculatedFieldOutput)
  });

  currentEntityFilter: EntityFilter;
  isRelatedEntity: boolean;

  private propagateChange: (config: CalculatedFieldGeofencingConfiguration) => void = () => { };

  constructor(private fb: FormBuilder,
              private store: Store<AppState>) {

    this.geofencingConfiguration.get('zoneGroups').valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((zoneGroups: Record<string, CalculatedFieldGeofencing>) =>
        this.checkRelatedEntity(zoneGroups)
      );

    this.geofencingConfiguration.get('scheduledUpdateEnabled').valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value: boolean) =>
        this.checkScheduledUpdateEnabled(value)
      );

    this.geofencingConfiguration.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(() => {
      this.updatedModel(this.geofencingConfiguration.getRawValue() as any);
    })
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.entityName || changes.entityId) {
      const entityNameChanges = changes.entityName;
      const entityIdChanges = changes.entityId;
      if ((entityNameChanges?.currentValue !== entityNameChanges?.previousValue) || (entityIdChanges?.currentValue !== entityIdChanges?.previousValue)) {
        this.currentEntityFilter = getCalculatedFieldCurrentEntityFilter(this.entityName, this.entityId);
      }
    }
  }

  validate(): ValidationErrors | null {
    return this.geofencingConfiguration.valid || this.geofencingConfiguration.disabled ? null : { geofencingConfigError: false };
  }

  writeValue(config: CalculatedFieldGeofencingConfiguration): void {
    this.geofencingConfiguration.patchValue(config, {emitEvent: false});
    this.checkRelatedEntity(this.geofencingConfiguration.get('zoneGroups').value);
    if (this.geofencingConfiguration.enabled) {
      this.checkScheduledUpdateEnabled(this.geofencingConfiguration.get('scheduledUpdateEnabled').value);
    }
  }

  registerOnChange(fn: (config: CalculatedFieldGeofencingConfiguration) => void): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_: any): void { }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.geofencingConfiguration.disable({emitEvent: false});
    } else {
      this.geofencingConfiguration.enable({emitEvent: false});
      this.checkScheduledUpdateEnabled(this.geofencingConfiguration.get('scheduledUpdateEnabled').value);
    }
  }

  private updatedModel(value: CalculatedFieldGeofencingConfiguration) {
    value.type = CalculatedFieldType.GEOFENCING;
    this.propagateChange(value)
  }

  private checkScheduledUpdateEnabled(value: boolean) {
    if (value) {
      this.geofencingConfiguration.get('scheduledUpdateInterval').enable({emitEvent: false});
    } else {
      this.geofencingConfiguration.get('scheduledUpdateInterval').disable({emitEvent: false});
    }
  }

  private checkRelatedEntity(zoneGroups: Record<string, CalculatedFieldGeofencing>) {
    this.isRelatedEntity = Object.values(zoneGroups).some(zone => zone.refDynamicSourceConfiguration?.type === ArgumentEntityType.RelationQuery);
  }
}
