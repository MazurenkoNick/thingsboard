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
import { ReportFilter, reportFiltersEquals, ReportTemplateType } from '@shared/models/report.models';
import { EntityType } from '@shared/models/entity-type.models';

export const REPORT_FILTER_DATA = new InjectionToken<any>('ReportFilterData');

export interface ReportFilterData {
  panelMode: boolean;
  reportFilter: ReportFilter;
  initialReportFilter?: ReportFilter;
}

// @dynamic
@Component({
  selector: 'tb-report-filter',
  templateUrl: './report-filter.component.html',
  styleUrls: ['./report-filter.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ReportFilterComponent),
      multi: true
    }
  ]
})
export class ReportFilterComponent implements OnInit, ControlValueAccessor {

  @ViewChild('reportFilterPanel')
  reportFilterPanel: TemplateRef<any>;

  @Input() disabled: boolean;

  @coerceBoolean()
  @Input()
  buttonMode = true;

  @Input()
  initialReportFilter: ReportFilter;

  ReportTemplateType = ReportTemplateType;

  EntityType = EntityType;

  panelMode = false;

  reportFilterForm: UntypedFormGroup;

  reportOverlayRef: OverlayRef;

  panelResult: ReportFilter = null;

  private reportFilter: ReportFilter;
  private resizeWindows: Subscription;

  private propagateChange = (_: any) => {};

  constructor(@Optional() @Inject(REPORT_FILTER_DATA)
              private data: ReportFilterData | undefined,
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
      this.reportFilter = this.data.reportFilter;
      this.initialReportFilter = this.data.initialReportFilter;
      if (this.panelMode && !this.initialReportFilter) {
        this.initialReportFilter = deepClone(this.reportFilter);
      }
    }
    this.reportFilterForm = this.fb.group({
      reportTemplateId: [null, []],
      userId: [null, []]
    });
    this.reportFilterForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        if (!this.buttonMode) {
          this.reportFilterUpdated(this.reportFilterForm.value);
        }
      }
    );
    if (this.panelMode) {
      this.updateReportFilterForm(this.reportFilter);
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
      this.reportFilterForm.disable({emitEvent: false});
    } else {
      this.reportFilterForm.enable({emitEvent: false});
    }
  }

  writeValue(reportFilter?: ReportFilter): void {
    this.reportFilter = reportFilter;
    if (!this.initialReportFilter && reportFilter) {
      this.initialReportFilter = deepClone(reportFilter);
    }
    this.updateReportFilterForm(reportFilter);
  }

  toggleReportFilterPanel($event: Event) {
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

    this.reportOverlayRef = this.overlay.create(config);
    this.reportOverlayRef.backdropClick().subscribe(() => {
      this.reportOverlayRef.dispose();
    });
    this.reportOverlayRef.attach(new TemplatePortal(this.reportFilterPanel,
      this.viewContainerRef));
    this.resizeWindows = fromEvent(window, 'resize').subscribe(() => {
      this.reportOverlayRef.updatePosition();
    });
  }

  cancel() {
    this.updateReportFilterForm(this.reportFilter);
    this.reportFilterForm.markAsPristine();
    if (this.overlayRef) {
      this.overlayRef.dispose();
    } else {
      this.resizeWindows.unsubscribe();
      this.reportOverlayRef.dispose();
    }
  }

  update() {
    this.reportFilterUpdated(this.reportFilterForm.value);
    this.reportFilterForm.markAsPristine();
    if (this.panelMode) {
      this.panelResult = this.reportFilter;
    }
    if (this.overlayRef) {
      this.overlayRef.dispose();
    } else {
      this.resizeWindows.unsubscribe();
      this.reportOverlayRef.dispose();
    }
  }

  reset() {
    if (this.initialReportFilter) {
      if (this.buttonMode || this.panelMode) {
        const reportFilter = this.reportFilterFromFormValue(this.reportFilterForm.value);
        if (!reportFiltersEquals(reportFilter, this.initialReportFilter)) {
          this.updateReportFilterForm(this.initialReportFilter);
          this.reportFilterForm.markAsDirty();
        }
      } else {
        if (!reportFiltersEquals(this.reportFilter, this.initialReportFilter)) {
          this.reportFilter = this.initialReportFilter;
          this.updateReportFilterForm(this.reportFilter);
          this.propagateChange(this.reportFilter);
        }
      }
    }
  }

  private updateReportFilterForm(reportFilter?: ReportFilter) {
    this.reportFilterForm.patchValue({
      reportTemplateId: reportFilter?.reportTemplateId,
      userId: reportFilter?.userId
    }, {emitEvent: false});
  }

  private reportFilterUpdated(formValue: any) {
    this.reportFilter = this.reportFilterFromFormValue(formValue);
    this.propagateChange(this.reportFilter);
  }

  private reportFilterFromFormValue(formValue: any): ReportFilter {
    return {
      reportTemplateId: formValue.reportTemplateId,
      userId: formValue.userId
    };
  }
}
