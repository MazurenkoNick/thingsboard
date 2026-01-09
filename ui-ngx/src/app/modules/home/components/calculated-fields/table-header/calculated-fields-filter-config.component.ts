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

import {
  Component,
  DestroyRef,
  ElementRef,
  forwardRef,
  Inject,
  InjectionToken,
  Input,
  OnInit,
  Optional,
  TemplateRef,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { coerceBoolean } from '@shared/decorators/coercion';
import { Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';
import { deepClone, isArraysEqualIgnoreUndefined, isDefinedAndNotNull, isEmpty, isUndefinedOrNull } from '@core/utils';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { fromEvent, Subscription } from 'rxjs';
import { POSITION_MAP } from '@shared/models/overlay.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  calculatedFieldsEntityTypeList,
  CalculatedFieldsQuery,
  calculatedFieldTypes,
  CalculatedFieldTypeTranslations
} from '@shared/models/calculated-field.models';
import { StringItemsOption } from '@shared/components/string-items-list.component';
import { TranslateService } from '@ngx-translate/core';
import { Operation } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';

export const CALCULATED_FIELDS_CONFIG_DATA = new InjectionToken<any>('CalculatedFieldsFilterConfigData');

export interface CalculatedFieldsFilterConfigData {
  panelMode: boolean;
  userMode: boolean;
  filterConfig: CalculatedFieldsQuery;
  initialFilterConfig?: CalculatedFieldsQuery;
}

@Component({
  selector: 'tb-calculated-fields-filter-config',
  templateUrl: './calculated-fields-filter-config.component.html',
  styleUrls: ['./calculated-fields-filter-config.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CalculatedFieldsFilterConfigComponent),
      multi: true
    }
  ]
})
export class CalculatedFieldsFilterConfigComponent implements OnInit, ControlValueAccessor {

  @ViewChild('calculatedFieldsFilterPanel')
  calculatedFieldsFilterPanel: TemplateRef<any>;

  @Input()
  disabled: boolean;

  @coerceBoolean()
  @Input()
  buttonMode = true;

  @Input()
  initialCfFilterConfig: CalculatedFieldsQuery = {
    types: [],
    entityType: null,
    entities: []
  };

  panelMode = false;

  buttonDisplayValue = this.translate.instant('calculated-fields.calculated-field-filter-title');

  cfFilterForm: FormGroup;

  panelResult: CalculatedFieldsQuery = null;

  entityType = EntityType;

  listEntityTypes = calculatedFieldsEntityTypeList.filter(entityType =>
    this.userPermissionsService.hasGenericPermissionByEntityGroupType(Operation.READ_CALCULATED_FIELD, entityType));
  entityTypeTranslations = entityTypeTranslations;

  readonly types: StringItemsOption[] = calculatedFieldTypes.map(item => ({
    name: this.translate.instant(CalculatedFieldTypeTranslations.get(item).name),
    value: item
  }));

  private cfFilterOverlayRef: OverlayRef;
  private cfFilterConfig: CalculatedFieldsQuery;
  private resizeWindows: Subscription;

  private propagateChange = (_: any) => {};

  constructor(@Optional() @Inject(CALCULATED_FIELDS_CONFIG_DATA)
              private data: CalculatedFieldsFilterConfigData | undefined,
              @Optional() private overlayRef: OverlayRef,
              private fb: FormBuilder,
              private overlay: Overlay,
              private nativeElement: ElementRef,
              private viewContainerRef: ViewContainerRef,
              private destroyRef: DestroyRef,
              private translate: TranslateService,
              private userPermissionsService: UserPermissionsService) {
  }

  ngOnInit(): void {
    if (this.data) {
      this.panelMode = this.data.panelMode;
      this.cfFilterConfig = this.data.filterConfig;
      this.initialCfFilterConfig = this.data.initialFilterConfig;
      if (this.panelMode && !this.initialCfFilterConfig) {
        this.initialCfFilterConfig = deepClone(this.cfFilterConfig);
      }
    }
    this.cfFilterForm = this.fb.group({
      types: [null, []],
      entityType: [null, []],
      entities: [null, []]
    });
    this.cfFilterForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        if (!this.buttonMode) {
          this.cfConfigUpdated(this.cfFilterForm.value);
        }
      }
    );
    if (this.panelMode) {
      this.updateCfConfigForm(this.cfFilterConfig);
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.cfFilterForm.disable({emitEvent: false});
    } else {
      this.cfFilterForm.enable({emitEvent: false});
    }
  }

  writeValue(cfFilterConfig?: CalculatedFieldsQuery): void {
    this.cfFilterConfig = cfFilterConfig;
    if (!this.initialCfFilterConfig && cfFilterConfig) {
      this.initialCfFilterConfig = deepClone(cfFilterConfig);
    }
    this.updateButtonDisplayValue();
    this.updateCfConfigForm(cfFilterConfig);
  }

  toggleCfFilterPanel($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const config = new OverlayConfig({
      panelClass: 'tb-filter-panel',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: true,
      maxHeight: '80vh',
      height: 'min-content',
      minWidth: ''
    });
    config.hasBackdrop = true;
    config.positionStrategy = this.overlay.position()
      .flexibleConnectedTo(this.nativeElement)
      .withPositions([POSITION_MAP.bottomLeft]);

    this.cfFilterOverlayRef = this.overlay.create(config);
    this.cfFilterOverlayRef.backdropClick().subscribe(() => {
      this.cfFilterOverlayRef.dispose();
    });
    this.cfFilterOverlayRef.attach(new TemplatePortal(this.calculatedFieldsFilterPanel,
      this.viewContainerRef));
    this.resizeWindows = fromEvent(window, 'resize').subscribe(() => {
      this.cfFilterOverlayRef.updatePosition();
    });
  }

  cancel() {
    this.updateCfConfigForm(this.cfFilterConfig);
    this.cfFilterForm.markAsPristine();
    if (this.overlayRef) {
      this.overlayRef.dispose();
    } else {
      this.resizeWindows.unsubscribe();
      this.cfFilterOverlayRef.dispose();
    }
  }

  update() {
    this.cfConfigUpdated(this.cfFilterForm.value);
    this.cfFilterForm.markAsPristine();
    if (this.panelMode) {
      this.panelResult = this.cfFilterConfig;
    }
    if (this.overlayRef) {
      this.overlayRef.dispose();
    } else {
      this.resizeWindows.unsubscribe();
      this.cfFilterOverlayRef.dispose();
    }
  }

  reset() {
    const cfFilterConfig = this.cfFilterFromFormValue(this.cfFilterForm.value);
    if (!this.cfFilterConfigEquals(cfFilterConfig, this.initialCfFilterConfig)) {
      this.updateCfConfigForm(this.initialCfFilterConfig);
      this.cfFilterForm.markAsDirty();
    }
  }

  private cfFilterConfigEquals = (filter1?: CalculatedFieldsQuery, filter2?: CalculatedFieldsQuery): boolean => {
    if (filter1 === filter2) {
      return true;
    }
    if ((isUndefinedOrNull(filter1) || isEmpty(filter1)) && (isUndefinedOrNull(filter2) || isEmpty(filter2))) {
      return true;
    } else if (isDefinedAndNotNull(filter1) && isDefinedAndNotNull(filter2)) {
      if (!isArraysEqualIgnoreUndefined(filter1.types, filter2.types)) {
        return false;
      }
      if (!isArraysEqualIgnoreUndefined(filter1.entities, filter2.entities)) {
        return false;
      }
      return filter1.entityType === filter2.entityType;
    }
    return false;
  };

  private updateCfConfigForm(cfFilterConfig?: CalculatedFieldsQuery) {
    this.cfFilterForm.patchValue({
      types: cfFilterConfig?.types ?? [],
      entityType: cfFilterConfig?.entityType ?? null,
      entities: cfFilterConfig?.entities ?? [],
    }, {emitEvent: false});
  }

  private cfConfigUpdated(formValue: any) {
    this.cfFilterConfig = this.cfFilterFromFormValue(formValue);
    this.updateButtonDisplayValue();
    this.propagateChange(this.cfFilterConfig);
  }

  private updateButtonDisplayValue() {
    if (this.buttonMode) {
      const filterTextParts: string[] = [];
      if (this.cfFilterConfig?.types?.length) {
        filterTextParts.push(this.cfFilterConfig.types.map((type) => this.translate.instant(CalculatedFieldTypeTranslations.get(type).name)).join(', '));
      }
      if (this.cfFilterConfig?.entityType) {
        filterTextParts.push(this.translate.instant( entityTypeTranslations.get(this.cfFilterConfig.entityType).type));
      }
      if (!filterTextParts.length) {
        this.buttonDisplayValue = this.translate.instant('calculated-fields.calculated-field-filter-title');
      } else {
        this.buttonDisplayValue = this.translate.instant('calculated-fields.filter-title') + `: ${filterTextParts.join(', ')}`;
      }
    }
  }

  private cfFilterFromFormValue(formValue: any): CalculatedFieldsQuery {
    return {
      types: formValue?.types ?? [],
      entityType: formValue?.entityType ?? null,
      entities: formValue?.entities ?? [],
    };
  }
}
