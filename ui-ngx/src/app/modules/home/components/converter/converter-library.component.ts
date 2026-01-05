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

import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  forwardRef,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
  ViewEncapsulation
} from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { combineLatest, merge, of, Subject, Subscription } from 'rxjs';
import { catchError, distinctUntilChanged, map, switchMap, takeUntil, tap, } from 'rxjs/operators';
import { ConverterLibraryService } from '@core/http/converter-library.service';
import { IntegrationType } from '@shared/models/integration.models';
import { Converter, ConverterLibraryInfo, ConverterType, Model, Vendor } from '@shared/models/converter.models';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-converter-library',
  templateUrl: './converter-library.component.html',
  styleUrls: ['./converter-library.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ConverterLibraryComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ConverterLibraryComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class ConverterLibraryComponent implements ControlValueAccessor, Validator, OnChanges, OnDestroy, OnInit {

  @Input() converterType = ConverterType.UPLINK;
  @Input() integrationType: IntegrationType;
  @Input() set interacted(interacted: boolean) {
    if (interacted) {
      this.libraryFormGroup.markAllAsTouched();
    }
  }

  @Output() converter = new EventEmitter<Converter>();

  libraryFormGroup: UntypedFormGroup;
  converter$: Subscription;

  private destroy$ = new Subject<void>();
  private modelValue: ConverterLibraryInfo;
  private propagateChange: (value: any) => void = () => {};

  constructor(
    private fb: FormBuilder,
    private converterLibraryService: ConverterLibraryService,
  ) {
    this.libraryFormGroup = this.fb.group({
      vendor: [null, Validators.required],
      model: [null, Validators.required],
    });

    merge(
      this.libraryFormGroup.get('model').valueChanges,
      this.libraryFormGroup.get('vendor').valueChanges.pipe(
        tap(() => this.libraryFormGroup.get('model').setValue(null, { emitEvent: false }))
      )
    ).pipe(
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        this.updateView(this.libraryFormGroup.getRawValue());
      });
  }

  ngOnInit() {
    this.converter$ = combineLatest([
      this.libraryFormGroup.get('vendor').valueChanges,
      this.libraryFormGroup.get('model').valueChanges
    ])
      .pipe(
        switchMap(([vendor, model]: [Vendor, Model]) =>
          vendor?.name && model?.name
            ? this.converterLibraryService.getConverter(this.integrationType, vendor.name, model.name, this.converterType)
            : of(null)
        ),
        catchError(() => of(null)),
        distinctUntilChanged(),
        map((converter: Converter) => {
          const defaultDebugSettings = { allEnabled: true, failuresEnabled: true };
          const defaultConverter = {
            integrationType: this.integrationType,
            converterVersion: 1,
            debugSettings: defaultDebugSettings
          } as Converter;

          if (converter) {
            return {
              ...defaultConverter,
              ...converter,
              debugSettings: defaultDebugSettings
            };
          }
          return null;
        }),
        takeUntil(this.destroy$)
    ).subscribe(value => this.converter.emit(value));
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (
      changes.integrationType
      && !changes.integrationType.firstChange
      && changes.integrationType.currentValue !== changes.integrationType.previousValue
    ) {
      this.libraryFormGroup.get('vendor').reset('');
      this.libraryFormGroup.get('model').reset('');
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.libraryFormGroup.disable({emitEvent: false});
    } else {
      this.libraryFormGroup.enable({emitEvent: false});
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_: any): void {
  }

  private updateView(value: ConverterLibraryInfo) {
    if (this.modelValue !== value && (value.model && value.vendor)) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

  writeValue(converterLibraryValue: ConverterLibraryInfo): void {
    if (isDefinedAndNotNull(converterLibraryValue)) {
      this.modelValue = converterLibraryValue;
      this.libraryFormGroup.patchValue(converterLibraryValue, {emitEvent: true});
    } else {
      this.modelValue = null;
      this.libraryFormGroup.patchValue( {vendor: '', model: ''}, {emitEvent: true})
    }
  }

  validate(): ValidationErrors | null {
    return this.libraryFormGroup.valid ? null : {
      converterFormGroup: {valid: false}
    };
  }
}
