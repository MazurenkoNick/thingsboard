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

import { Datasource, DatasourceType } from '@shared/models/widget.models';
import { deepClone } from '@core/utils';
import { alignment, Font } from '@shared/models/widget-settings.models';
import { Insets } from '@shared/models/report.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { ReportTemplateId } from '@shared/models/id/report-template-id';

export enum ReportComponentType {
  HEADING = 'HEADING',
  RICH_TEXT = 'RICH_TEXT',
  ENTITY_TABLE = 'ENTITY_TABLE',
  TIME_SERIES_TABLE = 'TIME_SERIES_TABLE',
  ALARM_TABLE = 'ALARM_TABLE',
  DASHBOARD = 'DASHBOARD',
  IMAGE = 'IMAGE',
  SUB_REPORT = 'SUB_REPORT',
  PAGE_BREAK = 'PAGE_BREAK'
}

export interface ReportComponentConfig {
  background?: string;
  margins?: Insets;
  paddings?: Insets;
  dataSources?: Datasource[];
  type: ReportComponentType;
}

export interface TableReportComponentConfig extends ReportComponentConfig {
  type: ReportComponentType;
}

export interface HeadingReportComponentConfig extends ReportComponentConfig {
  value: string;
  font?: Font;
  color?: string;
  textAlignment?: alignment;
  verticalAlignment?: alignment;
  height?: number;
  type:  ReportComponentType.HEADING;
}

export interface RichTextReportComponentConfig extends ReportComponentConfig {
  value: string;
  type: ReportComponentType.RICH_TEXT;
}

export interface EntityTableReportComponentConfig extends TableReportComponentConfig {
  type: ReportComponentType.ENTITY_TABLE;
}

export interface SubReportReportComponentConfig extends ReportComponentConfig {
  templateId: ReportTemplateId;
  type: ReportComponentType.SUB_REPORT;
}

export interface PageBreakReportComponentConfig extends ReportComponentConfig {
  type: ReportComponentType.PAGE_BREAK;
}

export type ReportComponentConfigs =
  HeadingReportComponentConfig |
  RichTextReportComponentConfig |
  EntityTableReportComponentConfig |
  SubReportReportComponentConfig |
  PageBreakReportComponentConfig;

export const reportComponentTypeDefaultConfigMap = new Map<ReportComponentType, ReportComponentConfigs>(
  [
    [
      ReportComponentType.HEADING,
      {
        type: ReportComponentType.HEADING,
        value: 'Heading text',
        font: {
          size: 40,
          sizeUnit: 'pt',
          weight: 'normal',
          style: 'normal',
          family: 'Roboto'
        } as Font,
        color: '#000',
        textAlignment: 'center',
        verticalAlignment: 'middle',
        height: undefined,
        dataSources: []
      }
    ],
    [
      ReportComponentType.RICH_TEXT,
      {
        type: ReportComponentType.RICH_TEXT,
        value: '<p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec libero orci, faucibus in iaculis quis, vestibulum sit amet ligula. Nulla facilisi. Ut ut iaculis tortor.</p>',
        dataSources: []
      }
    ],
    [
      ReportComponentType.ENTITY_TABLE,
      {
        type: ReportComponentType.ENTITY_TABLE,
        dataSources: [
          {
            type: DatasourceType.entity,
            dataKeys: [
              {
                name: 'name',
                type: DataKeyType.entityField,
                label: 'Name'
              }
            ]
          }
        ]
      }
    ],
    [
      ReportComponentType.SUB_REPORT,
      {
        type: ReportComponentType.SUB_REPORT,
        dataSources: [
          {
            type: DatasourceType.entity,
            dataKeys: []
          }
        ],
        templateId: null
      }
    ],
    [
      ReportComponentType.PAGE_BREAK,
      {
        type: ReportComponentType.PAGE_BREAK
      }
    ]
  ]
);

export const defaultReportComponentConfig = (type: ReportComponentType): ReportComponentConfig => {
  const config = reportComponentTypeDefaultConfigMap.get(type);
  if (config) {
    return { type, ...deepClone(config)};
  }
}
