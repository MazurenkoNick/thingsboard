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
import { AbstractReportComponentPreview } from '@home/pages/report/components/report-component.component';
import { PageBreakPreviewComponent } from '@home/pages/report/components/page-break-preview.component';
import { EmptyReportConfigComponent } from '@home/pages/report/components/empty-report-config.component';
import { EntityTablePreviewComponent } from '@home/pages/report/components/entity-table-preview.component';
import { EntityTableConfigComponent } from '@home/pages/report/components/entity-table-config.component';
import {
  EntityAliasSelectCallbacks
} from '@home/components/widget/lib/settings/common/alias/entity-alias-select.component.models';
import {
  FilterSelectCallbacks
} from '@home/components/widget/lib/settings/common/filter/filter-select.component.models';
import { SubReportPreviewComponent } from '@home/pages/report/components/sub-report-preview.component';
import { SubReportConfigComponent } from '@home/pages/report/components/sub-report-config.component';
import { ImagePreviewComponent } from '@home/pages/report/components/image-preview.component';
import { ImageConfigComponent } from '@home/pages/report/components/image-config.component';

import keyImageTemplate from './key-image-svg.raw';
import { insertVariable, stringToBase64 } from '@core/utils';
import { DataKey } from '@shared/models/widget.models';

export interface ReportComponentTypeData<C extends ReportComponentConfig = ReportComponentConfig> {
  title: string;
  previewImage: string;
  previewComponent: Type<AbstractReportComponentPreview<C>>;
  configComponent: Type<AbstractReportComponentConfig<C>>;
  editable: boolean;
  pageBreak?: boolean;
}

export const reportComponentTypeMap = new Map<ReportComponentType, ReportComponentTypeData>(
  [
    [
      ReportComponentType.HEADING,
      {
        title: 'report-template.component.heading.type',
        previewImage: '/assets/report/components/heading.svg',
        previewComponent: HeadingPreviewComponent,
        configComponent: HeadingConfigComponent,
        editable: true
      }
    ],
    [
      ReportComponentType.RICH_TEXT,
      {
        title: 'report-template.component.rich-text.type',
        previewImage: '/assets/report/components/rich-text.svg',
        previewComponent: RichTextPreviewComponent,
        configComponent: RichTextConfigComponent,
        editable: true
      }
    ],
    [
      ReportComponentType.ENTITY_TABLE,
      {
        title: 'report-template.component.entity-table.type',
        previewImage: '/assets/report/components/entity-table.svg',
        previewComponent: EntityTablePreviewComponent,
        configComponent: EntityTableConfigComponent,
        editable: true
      }
    ],
    [
      ReportComponentType.IMAGE,
      {
        title: 'report-template.component.image.type',
        previewImage: '/assets/report/components/image.svg',
        previewComponent: ImagePreviewComponent,
        configComponent: ImageConfigComponent,
        editable: true
      }
    ],
    [
      ReportComponentType.SUB_REPORT,
      {
        title: 'report-template.component.sub-report.type',
        previewImage: '/assets/report/components/subreport.svg',
        previewComponent: SubReportPreviewComponent,
        configComponent: SubReportConfigComponent,
        editable: true
      }
    ],
    [
      ReportComponentType.PAGE_BREAK,
      {
        title: 'report-template.component.page-break.type',
        previewImage: '/assets/report/components/page-break.svg',
        previewComponent: PageBreakPreviewComponent,
        configComponent: EmptyReportConfigComponent,
        editable: false,
        pageBreak: true
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
  aliasAndFilterCallbacks: EntityAliasSelectCallbacks & FilterSelectCallbacks;
}

export const assignReportComponent = (reportComponent: ReportComponentConfig, sourceReportComponent: ReportComponentConfig): void => {
  Object.assign(reportComponent, sourceReportComponent);
  for(const key in reportComponent){
    if(!(key in sourceReportComponent))
      delete reportComponent[key];
  }
}

export const pointsToPixels = (points: number): number => points * 1.3333343412075;

export type ReportVariableType = 'entityKey' | 'pageVariable';

export interface ReportVariable {
  type: ReportVariableType;
  name: string;
  dataKey?: DataKey;
}

export const pageVariables: ReportVariable[] = [
  {
    type: 'pageVariable',
    name: 'pageNumber'
  },
  {
    type: 'pageVariable',
    name: 'totalPages'
  },
];

export const keyImage = (key: string): string => {
  const result = insertVariable(keyImageTemplate, 'key', `\${${key}}`);
  const encodedSvg = stringToBase64(result);
  return `data:image/svg+xml;base64,${encodedSvg}`;
}

const variablePattern = /^\${([^}]*)}$/;

export const isKeyVariable = (test: string): boolean => {
  return variablePattern.test(test);
}

export const extractKeyFromVariable = (variable: string): string => {
  const match = variablePattern.exec(variable);
  if (match !== null) {
    return match[1];
  } else {
    return '';
  }
}
