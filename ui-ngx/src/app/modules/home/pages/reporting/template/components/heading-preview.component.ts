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

import { Component, ViewEncapsulation } from '@angular/core';
import { HeadingReportComponentConfig } from '@shared/models/report-component.models';
import { ComponentStyle, Font, textStyle } from '@shared/models/widget-settings.models';
import { deepClone } from '@core/utils';
import { AbstractReportComponentPreview } from '@home/pages/reporting/template/components/report-component.component';

@Component({
  selector: 'tb-report-heading-preview',
  templateUrl: './heading-preview.component.html',
  styleUrls: [],
  encapsulation: ViewEncapsulation.None
})
export class HeadingPreviewComponent extends AbstractReportComponentPreview<HeadingReportComponentConfig> {

  headingStyle: ComponentStyle;

  height: string;

  text: string;

  onComponentUpdated() {
    if (this.reportComponent.value && this.reportComponent.value.trim().length) {
      this.text = this.reportComponent.value;
    } else {
      this.text = '&nbsp;';
    }
    const font: Font = deepClone(this.reportComponent.font || { size: 10, sizeUnit: 'pt' } as Font);
    if (!font.size) {
      font.size = 10;
    }
    if (font.sizeUnit !== 'pt') {
      font.sizeUnit = 'pt';
    }
    this.headingStyle = textStyle(font);
    this.headingStyle.color = this.reportComponent.color || '#000';
    if (this.reportComponent.textAlignment) {
      this.headingStyle.textAlign = this.reportComponent.textAlignment;
    }
    if (this.reportComponent.verticalAlignment) {
      this.headingStyle.verticalAlign = this.reportComponent.verticalAlignment;
    }
    if (this.reportComponent.height) {
      this.height = this.reportComponent.height + 'pt';
    } else {
      this.height = '100%';
    }
  }

}
