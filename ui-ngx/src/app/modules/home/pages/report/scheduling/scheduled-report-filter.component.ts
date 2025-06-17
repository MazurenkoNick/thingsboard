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
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { coerceBoolean } from '@shared/decorators/coercion';
import { Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';
import { deepClone } from '@core/utils';
import { fromEvent, Subscription } from 'rxjs';
import { POSITION_MAP } from '@shared/models/overlay.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReportTemplateType, ScheduledReportFilter, scheduledReportFiltersEquals } from '@shared/models/report.models';
import { EntityType } from '@shared/models/entity-type.models';

export const SCHEDULED_REPORT_FILTER_DATA = new InjectionToken<any>('ScheduledReportFilterData');

export interface ScheduledReportFilterData {
  panelMode: boolean;
  scheduledReportFilter: ScheduledReportFilter;
  initialScheduledReportFilter?: ScheduledReportFilter;
}

// @dynamic
@Component({
  selector: 'tb-scheduled-report-filter',
  templateUrl: './scheduled-report-filter.component.html',
  styleUrls: ['./scheduled-report-filter.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ScheduledReportFilterComponent),
      multi: true
    }
  ]
})
export class ScheduledReportFilterComponent implements OnInit, ControlValueAccessor {

  @ViewChild('scheduledReportFilterPanel')
  scheduledReportFilterPanel: TemplateRef<any>;

  @Input() disabled: boolean;

  @coerceBoolean()
  @Input()
  buttonMode = true;

  @Input()
  initialScheduledReportFilter: ScheduledReportFilter;

  ReportTemplateType = ReportTemplateType;

  EntityType = EntityType;

  panelMode = false;

  scheduledReportFilterForm: UntypedFormGroup;

  scheduledReportOverlayRef: OverlayRef;

  panelResult: ScheduledReportFilter = null;

  private scheduledReportFilter: ScheduledReportFilter;
  private resizeWindows: Subscription;

  private propagateChange = (_: any) => {};

  constructor(@Optional() @Inject(SCHEDULED_REPORT_FILTER_DATA)
              private data: ScheduledReportFilterData | undefined,
              @Optional()
              private overlayRef: OverlayRef,
              private fb: UntypedFormBuilder,
              private overlay: Overlay,
              private nativeElement: ElementRef,
              private viewContainerRef: ViewContainerRef,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    if (this.data) {
      this.panelMode = this.data.panelMode;
      this.scheduledReportFilter = this.data.scheduledReportFilter;
      this.initialScheduledReportFilter = this.data.initialScheduledReportFilter;
      if (this.panelMode && !this.initialScheduledReportFilter) {
        this.initialScheduledReportFilter = deepClone(this.scheduledReportFilter);
      }
    }
    this.scheduledReportFilterForm = this.fb.group({
      reportTemplateId: [null, []],
      userId: [null, []]
    });
    this.scheduledReportFilterForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        if (!this.buttonMode) {
          this.scheduledReportFilterUpdated(this.scheduledReportFilterForm.value);
        }
      }
    );
    if (this.panelMode) {
      this.updateScheduledReportFilterForm(this.scheduledReportFilter);
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.scheduledReportFilterForm.disable({emitEvent: false});
    } else {
      this.scheduledReportFilterForm.enable({emitEvent: false});
    }
  }

  writeValue(scheduledReportFilter?: ScheduledReportFilter): void {
    this.scheduledReportFilter = scheduledReportFilter;
    if (!this.initialScheduledReportFilter && scheduledReportFilter) {
      this.initialScheduledReportFilter = deepClone(scheduledReportFilter);
    }
    this.updateScheduledReportFilterForm(scheduledReportFilter);
  }

  toggleScheduledReportFilterPanel($event: Event) {
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

    this.scheduledReportOverlayRef = this.overlay.create(config);
    this.scheduledReportOverlayRef.backdropClick().subscribe(() => {
      this.scheduledReportOverlayRef.dispose();
    });
    this.scheduledReportOverlayRef.attach(new TemplatePortal(this.scheduledReportFilterPanel,
      this.viewContainerRef));
    this.resizeWindows = fromEvent(window, 'resize').subscribe(() => {
      this.scheduledReportOverlayRef.updatePosition();
    });
  }

  cancel() {
    this.updateScheduledReportFilterForm(this.scheduledReportFilter);
    this.scheduledReportFilterForm.markAsPristine();
    if (this.overlayRef) {
      this.overlayRef.dispose();
    } else {
      this.resizeWindows.unsubscribe();
      this.scheduledReportOverlayRef.dispose();
    }
  }

  update() {
    this.scheduledReportFilterUpdated(this.scheduledReportFilterForm.value);
    this.scheduledReportFilterForm.markAsPristine();
    if (this.panelMode) {
      this.panelResult = this.scheduledReportFilter;
    }
    if (this.overlayRef) {
      this.overlayRef.dispose();
    } else {
      this.resizeWindows.unsubscribe();
      this.scheduledReportOverlayRef.dispose();
    }
  }

  reset() {
    if (this.initialScheduledReportFilter) {
      if (this.buttonMode || this.panelMode) {
        const scheduledReportFilter = this.scheduledReportFilterFromFormValue(this.scheduledReportFilterForm.value);
        if (!scheduledReportFiltersEquals(scheduledReportFilter, this.initialScheduledReportFilter)) {
          this.updateScheduledReportFilterForm(this.initialScheduledReportFilter);
          this.scheduledReportFilterForm.markAsDirty();
        }
      } else {
        if (!scheduledReportFiltersEquals(this.scheduledReportFilter, this.initialScheduledReportFilter)) {
          this.scheduledReportFilter = this.initialScheduledReportFilter;
          this.updateScheduledReportFilterForm(this.scheduledReportFilter);
          this.propagateChange(this.scheduledReportFilter);
        }
      }
    }
  }

  private updateScheduledReportFilterForm(scheduledReportFilter?: ScheduledReportFilter) {
    this.scheduledReportFilterForm.patchValue({
      reportTemplateId: scheduledReportFilter?.reportTemplateId,
      userId: scheduledReportFilter?.userId
    }, {emitEvent: false});
  }

  private scheduledReportFilterUpdated(formValue: any) {
    this.scheduledReportFilter = this.scheduledReportFilterFromFormValue(formValue);
    this.propagateChange(this.scheduledReportFilter);
  }

  private scheduledReportFilterFromFormValue(formValue: any): ScheduledReportFilter {
    return {
      reportTemplateId: formValue.reportTemplateId,
      userId: formValue.userId
    };
  }
}
