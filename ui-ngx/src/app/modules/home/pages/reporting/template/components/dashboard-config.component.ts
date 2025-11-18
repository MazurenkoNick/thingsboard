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

import { Component, ViewEncapsulation } from '@angular/core';
import { FormGroup, Validators } from '@angular/forms';
import {
  AbstractReportComponentConfig
} from '@home/pages/reporting/template/components/report-component-config.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  DashboardReportComponentConfig,
  imageAlignments,
  imageAlignmentTranslations,
  imageWidthTypes,
  imageWidthTypeTranslations
} from '@shared/models/report-component.models';
import { WidgetConfigMode } from '@shared/models/widget.models';

@Component({
  selector: 'tb-dashboard-config',
  templateUrl: './dashboard-config.component.html',
  styleUrls: ['./report-component-config.scss'],
  encapsulation: ViewEncapsulation.None
})
export class DashboardConfigComponent extends AbstractReportComponentConfig<DashboardReportComponentConfig> {

  imageWidthTypes = imageWidthTypes;
  imageWidthTypeTranslations = imageWidthTypeTranslations;

  imageAlignments = imageAlignments;
  imageAlignmentTranslations = imageAlignmentTranslations;

  basicMode = WidgetConfigMode.basic;

  settingsTab: 'dashboard' | 'layout' = 'dashboard';

  protected buildForm(reportComponentConfig: DashboardReportComponentConfig): FormGroup {
    const form = this.fb.group({
      dataSources: [reportComponentConfig.dataSources, []],
      config: [reportComponentConfig.config, []],
      widthType: [reportComponentConfig.widthType || 'fitWidth', []],
      customWidth: [reportComponentConfig.customWidth || 100, [Validators.min(1)]],
      alignment: [reportComponentConfig.alignment || 'center', []]
    });
    form.get('widthType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateCustomWidth();
    });
    return form;
  }

  private updateCustomWidth() {
    if (!this.reportConfigForm.get('customWidth').touched) {
      const size = 200;
      this.reportConfigForm.get('customWidth').patchValue(size);
    }
  }
}
