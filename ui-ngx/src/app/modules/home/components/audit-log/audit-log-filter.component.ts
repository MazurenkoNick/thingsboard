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

import { Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';
import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  ElementRef,
  forwardRef,
  Input,
  OnInit,
  TemplateRef,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { isDefined, isDefinedAndNotNull } from '@app/core/utils';
import { StringItemsOption } from '@app/shared/components/string-items-list.component';
import {
  ActionType,
  actionTypeTranslations,
  AuditLogFilter,
  auditLogFilterEquals,
  POSITION_MAP,
} from '@app/shared/public-api';
import { TranslateService } from '@ngx-translate/core';

// @dynamic
@Component({
  selector: 'tb-audit-log-filter',
  templateUrl: './audit-log-filter.component.html',
  styleUrls: ['./audit-log-filter.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AuditLogFilterComponent),
      multi: true
    }
  ]
})
export class AuditLogFilterComponent implements OnInit, ControlValueAccessor {

  @ViewChild('auditLogFilterPanel')
  auditLogFilterPanel: TemplateRef<any>;

  @Input() disabled: boolean;

  ActionType = ActionType;

  actionTypes = Object.values(ActionType);

  actionTypeTranslations = actionTypeTranslations;

  buttonDisplayValue = this.translate.instant('audit-log.audit-log-filter-title');

  auditLogFilterForm: FormGroup;

  auditLogOverlayRef: OverlayRef;

  initialAuditLogFilter: AuditLogFilter;

  private auditLogFilter: AuditLogFilter;

  private propagateChange = (_: any) => {};

  constructor(private fb: FormBuilder,
              private translate: TranslateService,
              private overlay: Overlay,
              private nativeElement: ElementRef,
              private viewContainerRef: ViewContainerRef,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {}

  ngOnInit(): void {
    this.auditLogFilterForm = this.fb.group({
      actionTypes: [[]]
    });
    this.auditLogFilterForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        this.updateValidators();
      }
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.auditLogFilterForm.disable({emitEvent: false});
    } else {
      this.auditLogFilterForm.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(auditLogFilter?: AuditLogFilter): void {
    this.auditLogFilter = auditLogFilter;
    if(!this.initialAuditLogFilter && isDefined(this.auditLogFilter)) {
      this.initialAuditLogFilter = this.auditLogFilterForm.getRawValue();
    }
    this.updateButtonDisplayValue();
    this.updateAuditLogFilterForm(auditLogFilter);
  }

  get predefinedTypeValues (): StringItemsOption[] {
    return [...this.actionTypes].map(type => ({
      name: this.translate.instant(this.actionTypeTranslations.get(type)),
      value: type
    }));
  }

  private updateValidators() {
  }

  toggleAuditLogFilterPanel($event: Event) {
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

    this.auditLogOverlayRef = this.overlay.create(config);
    this.auditLogOverlayRef.backdropClick().subscribe(() => {
      this.auditLogOverlayRef.dispose();
    });
    this.auditLogOverlayRef.attach(new TemplatePortal(this.auditLogFilterPanel,
      this.viewContainerRef));
  }

  cancel() {
    this.updateAuditLogFilterForm(this.auditLogFilter);
    this.auditLogOverlayRef.dispose();
  }

  update() {
    this.auditLogFilterUpdated(this.auditLogFilterForm.value);
    this.auditLogOverlayRef.dispose();
  }

  reset() {
    if (this.initialAuditLogFilter) {
      if (!auditLogFilterEquals(this.auditLogFilterForm.value, this.initialAuditLogFilter)) {
        this.updateAuditLogFilterForm(this.initialAuditLogFilter);
        this.auditLogFilterForm.markAsDirty();
      }
    }
  }

  private updateAuditLogFilterForm(auditLogFilter?: AuditLogFilter) {
    this.auditLogFilterForm.patchValue({
      actionTypes: auditLogFilter?.actionTypes,
    }, {emitEvent: false});
    this.updateValidators();
  }

  private auditLogFilterUpdated(auditLogFilter: AuditLogFilter) {
    this.auditLogFilter = auditLogFilter;
    this.updateButtonDisplayValue();
    this.propagateChange(this.auditLogFilter);
  }

  private updateButtonDisplayValue() {
    const filterTextParts: string[] = [];
    if (isDefinedAndNotNull(this.auditLogFilter?.actionTypes)) {
      const actionTypes = this.auditLogFilter.actionTypes;
      actionTypes.forEach(actionType => filterTextParts.push(this.translate.instant(this.actionTypeTranslations.get(actionType as ActionType))))
    }
    if (!filterTextParts.length) {
      this.buttonDisplayValue = this.translate.instant('audit-log.audit-log-filter-title');
    } else {
      this.buttonDisplayValue = this.translate.instant('audit-log.filter-title') + `: ${filterTextParts.join(', ')}`;
    }
    this.cd.detectChanges();
  }
}
