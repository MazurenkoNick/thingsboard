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

import { ReportComponentConfig, ReportComponentType } from '@shared/models/report-component.models';
import { Type } from '@angular/core';
import { HeadingPreviewComponent } from '@home/pages/report/components/heading-preview.component';
import { RichTextPreviewComponent } from '@home/pages/report/components/rich-text-preview.component';
import { AbstractReportComponentConfig } from '@home/pages/report/components/report-component-config.component';
import { HeadingConfigComponent } from '@home/pages/report/components/heading-config.component';
import { RichTextConfigComponent } from '@home/pages/report/components/rich-text-config.component';
import { IAliasController } from '@core/api/widget-api.models';
import { EntityService } from '@core/http/entity.service';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';

export interface ReportComponentPreview<C extends ReportComponentConfig = ReportComponentConfig> {
  reportComponent: C;
}

export interface ReportComponentTypeData<C extends ReportComponentConfig = ReportComponentConfig> {
  title: string;
  previewImage: string;
  previewComponent: Type<ReportComponentPreview<C>>;
  configComponent: Type<AbstractReportComponentConfig<C>>;
}

export const reportComponentTypeMap = new Map<ReportComponentType, ReportComponentTypeData>(
  [
    [
      ReportComponentType.HEADING,
      {
        title: 'report-template.component.heading.type',
        previewImage: '/assets/report/components/heading.svg',
        previewComponent: HeadingPreviewComponent,
        configComponent: HeadingConfigComponent
      }
    ],
    [
      ReportComponentType.RICH_TEXT,
      {
        title: 'report-template.component.rich-text.type',
        previewImage: '/assets/report/components/rich-text.svg',
        previewComponent: RichTextPreviewComponent,
        configComponent: RichTextConfigComponent
      }
    ]
  ]
);

export const reportComponentTypes = Array.from(reportComponentTypeMap.keys());

export interface ReportComponentContext {
  translate: TranslateService,
  utils: UtilsService,
  entityService: EntityService;
  aliasController: IAliasController;
}
