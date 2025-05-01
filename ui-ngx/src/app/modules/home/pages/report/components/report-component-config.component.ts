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
  ChangeDetectorRef,
  Component,
  ComponentRef,
  DestroyRef,
  Directive,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { ReportComponentConfig } from '@shared/models/report-component.models';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';
import { FormBuilder, FormGroup } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { mergeDeep } from '@core/utils';
import { reportComponentTypeMap } from '@home/pages/report/components/report-component.models';

@Component({
  selector: 'tb-report-component-config',
  templateUrl: './report-component-config.component.html',
  styleUrls: ['./report-component-config.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ReportComponentConfigComponent implements OnInit, OnChanges {

  @Input()
  reportComponent: ReportComponentConfig;

  @ViewChild('reportConfigContainer', {static: true}) reportConfigContainer: TbAnchorComponent;

  reportConfigForm: FormGroup;

  private reportConfigComponentRef: ComponentRef<AbstractReportComponentConfig>;
  private reportConfigComponent: AbstractReportComponentConfig;

  constructor(private container: ViewContainerRef,
              private cd: ChangeDetectorRef,
              private fb: FormBuilder) {}

  ngOnInit() {
    this.init();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'reportComponent') {
          this.init();
        }
      }
    }
  }

  private init() {
    this.reportConfigForm = null;
    if (this.reportConfigComponentRef) {
      this.reportConfigComponentRef.destroy();
      this.reportConfigComponentRef = null;
    }
    this.reportConfigContainer.viewContainerRef.clear();
    if (this.reportComponent) {
      const typeData = reportComponentTypeMap.get(this.reportComponent.type);
      if (typeData) {
        this.reportConfigComponentRef = this.reportConfigContainer.viewContainerRef.createComponent(typeData.configComponent);
        this.reportConfigComponent = this.reportConfigComponentRef.instance;
        this.reportConfigComponent.reportConfigUpdated.subscribe((updated) => {
          Object.assign(this.reportComponent, updated);
        });
        this.reportConfigForm = this.reportConfigComponent.setupConfig(this.reportComponent);
      }
    }
  }
}

@Directive()
export abstract class AbstractReportComponentConfig {

  @Output()
  reportConfigUpdated = new EventEmitter<ReportComponentConfig>();

  reportConfigForm: FormGroup;

  private reportComponentConfig: ReportComponentConfig;

  protected constructor(private destroyRef: DestroyRef) {}

  setupConfig(reportComponentConfig: ReportComponentConfig): FormGroup {
    this.reportComponentConfig = reportComponentConfig;
    this.reportConfigForm = this.buildForm(reportComponentConfig);
    this.reportConfigForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    return this.reportConfigForm;
  }

  private updateModel() {
    this.reportComponentConfig = mergeDeep(this.reportComponentConfig, this.reportConfigForm.getRawValue());
    this.reportConfigUpdated.emit(this.reportComponentConfig);
  }

  protected abstract buildForm(reportComponentConfig: ReportComponentConfig): FormGroup;
}
