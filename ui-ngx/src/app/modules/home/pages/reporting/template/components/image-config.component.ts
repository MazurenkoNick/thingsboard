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
import { merge } from 'rxjs';
import {
  imageAlignments,
  imageAlignmentTranslations,
  ImageReportComponentConfig,
  imageSourceType,
  imageWidthTypes,
  imageWidthTypeTranslations
} from '@shared/models/report-component.models';
import { WidgetConfigMode } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { getDataKey, updateDataKeys } from '@shared/models/widget-settings.models';

@Component({
  selector: 'tb-image-config',
  templateUrl: './image-config.component.html',
  styleUrls: ['./report-component-config.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ImageConfigComponent extends AbstractReportComponentConfig<ImageReportComponentConfig> {

  imageWidthTypes = imageWidthTypes;
  imageWidthTypeTranslations = imageWidthTypeTranslations;

  imageAlignments = imageAlignments;
  imageAlignmentTranslations = imageAlignmentTranslations;

  basicMode = WidgetConfigMode.basic;

  DataKeyType = DataKeyType;

  settingsTab: 'image' | 'layout' = 'image';

  private initialImageUrl: string;
  private imageWidth: number;

  protected buildForm(reportComponentConfig: ImageReportComponentConfig): FormGroup {
    this.initialImageUrl = reportComponentConfig.imageUrl;
    const form = this.fb.group({
      sourceType: [reportComponentConfig.sourceType || 'image', []],
      imageUrl: [reportComponentConfig.imageUrl, []],
      dataSources: [reportComponentConfig.dataSources, []],
      entityKey: [getDataKey(reportComponentConfig.dataSources), []],
      widthType: [reportComponentConfig.widthType || 'fitWidth', []],
      customWidth: [reportComponentConfig.customWidth || 100, [Validators.min(1)]],
      alignment: [reportComponentConfig.alignment || 'center', []]
    });
    merge(form.get('sourceType').valueChanges, form.get('widthType').valueChanges).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateCustomWidth();
    });
    return form;
  }

  protected prepareOutputConfig(config: any): any {
    updateDataKeys(config.dataSources, [config.entityKey]);
    delete config.entityKey;
    return config;
  }

  private updateCustomWidth() {
    const sourceType: imageSourceType = this.reportConfigForm.get('sourceType').value;
    if (!this.reportConfigForm.get('customWidth').touched) {
      const size = sourceType === 'entityKey' ? 200 : (this.imageWidth || 100);
      this.reportConfigForm.get('customWidth').patchValue(size);
    }
  }

  imageSizeUpdated(size: {width: number, height: number}): void {
    this.imageWidth = size.width;
    if (!this.reportConfigForm.get('customWidth').touched &&
         this.reportConfigForm.get('imageUrl').value !== this.initialImageUrl) {
        this.initialImageUrl = null;
        this.reportConfigForm.get('customWidth').patchValue(size.width);
    }
  }
}
