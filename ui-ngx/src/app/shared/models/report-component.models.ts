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
import { alignment, alignmentTranslations, Font } from '@shared/models/widget-settings.models';
import { Insets } from '@shared/models/report.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { ReportTemplateId } from '@shared/models/id/report-template-id';
import { FormProperty, FormPropertyType } from '@shared/models/dynamic-form.models';
import { DashboardReportConfig } from '@shared/models/dashboard-report.models';
import { DAY, historyInterval, Timewindow } from '@shared/models/time/time.models';

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

export enum ReportDataKeySettingsType {
  DEFAULT = 'DEFAULT',
  COLUMN = 'COLUMN'
}

export interface ReportDataKeySettings {
  type: ReportDataKeySettingsType;
}

export interface TableReportCellSettings {
  font?: Font;
  color?: string;
  backgroundColor?: string;
  textAlignment?: alignment;
  verticalAlignment?: alignment;
}

export interface TableReportColumnSettings extends ReportDataKeySettings {
  type: ReportDataKeySettingsType.COLUMN;
  columnWidth?: string;
  header?: TableReportCellSettings;
  cell?: TableReportCellSettings;
}

const tableReportCellSettings = (header = false): FormProperty[] => ([
  {
    id: 'font',
    type: FormPropertyType.font,
    name: '{i18n:report-template.text-style}',
    forceSizeUnit: 'pt',
    allowedFontWeights: ['normal', 'bold'],
    allowedFontStyles: ['normal', 'italic'],
    default: {
      family: 'Roboto',
      size: 12,
      sizeUnit: 'pt',
      weight: header ? 'bold' : 'normal',
      style: 'normal'
    } as Font
  },
  {
    id: 'color',
    type: FormPropertyType.color,
    name: '{i18n:report-template.text-style}',
    default: 'rgba(0,0,0,0.87)'
  },
  {
    id: 'backgroundColor',
    type: FormPropertyType.color,
    name: '{i18n:report-template.background-color}',
    default: null
  },
  {
    id: 'textAlignment',
    type: FormPropertyType.select,
    name: '{i18n:report-template.alignment}',
    subLabel: '{i18n:report-template.horizontal}',
    fieldClass: 'standard-width',
    items: [
      {
        label: `{i18n:${alignmentTranslations.get('left')}}`,
        value: 'left'
      },
      {
        label: `{i18n:${alignmentTranslations.get('center')}}`,
        value: 'center'
      },
      {
        label: `{i18n:${alignmentTranslations.get('right')}}`,
        value: 'right'
      },
      {
        label: `{i18n:${alignmentTranslations.get('justify')}}`,
        value: 'justify'
      }
    ],
    default: header ? 'center' : 'left'
  },
  {
    id: 'verticalAlignment',
    type: FormPropertyType.select,
    name: '{i18n:report-template.alignment}',
    subLabel: '{i18n:report-template.vertical}',
    fieldClass: 'standard-width',
    items: [
      {
        label: `{i18n:${alignmentTranslations.get('top')}}`,
        value: 'top'
      },
      {
        label: `{i18n:${alignmentTranslations.get('middle')}}`,
        value: 'middle'
      },
      {
        label: `{i18n:${alignmentTranslations.get('bottom')}}`,
        value: 'bottom'
      }
    ],
    default: 'middle'
  }
]);

export const TableReportColumnSettingsForm: FormProperty[] = [
  {
    id: 'type',
    type: FormPropertyType.text,
    name: '',
    rowClass: '!hidden',
    default: ReportDataKeySettingsType.COLUMN
  },
  {
    id: 'columnWidth',
    type: FormPropertyType.cssSize,
    name: '{i18n:report-template.component.table.column-width}',
    allowedCssUnits: ['px', 'em', '%', 'pt', 'pc', 'in', 'cm', 'mm'],
    default: null
  },
  {
    id: 'header',
    type: FormPropertyType.fieldset,
    name: '{i18n:report-template.component.table.header}',
    properties: tableReportCellSettings(true),
    default: null
  },
  {
    id: 'cell',
    type: FormPropertyType.fieldset,
    name: '{i18n:report-template.component.table.cell}',
    properties: tableReportCellSettings(),
    default: null
  }
];

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

export interface AlarmTableReportComponentConfig extends TableReportComponentConfig {
  alarmSource: Datasource;
  timewindow: Timewindow;
  type: ReportComponentType.ALARM_TABLE;
}

export const imageSourceTypes = ['image', 'entityKey'];
type imageSourceTypeTuple = typeof imageSourceTypes;
export type imageSourceType = imageSourceTypeTuple[number];

export const imageSourceTypeTranslations = new Map<imageSourceType, string>(
  [
    ['image', 'report-template.component.image.source-image'],
    ['entityKey', 'report-template.component.image.source-entity-key']
  ]
);

export const imageWidthTypes = ['fitWidth', 'original', 'custom'];
type imageWidthTypeTuple = typeof imageWidthTypes;
export type imageWidthType = imageWidthTypeTuple[number];

export const imageWidthTypeTranslations = new Map<imageWidthType, string>(
  [
    ['fitWidth', 'report-template.component.image.width-fit-width'],
    ['original', 'report-template.component.image.width-original'],
    ['custom', 'report-template.component.image.width-custom']
  ]
);

export const imageAlignments = ['left', 'center', 'right'];
type imageAlignmentTuple = typeof imageAlignments;
export type imageAlignment = imageAlignmentTuple[number];

export const imageAlignmentTranslations = new Map<imageAlignment, string>(
  [
    ['left', 'report-template.component.image.alignment-left'],
    ['center', 'report-template.component.image.alignment-center'],
    ['right', 'report-template.component.image.alignment-right']
  ]
);

export interface BaseImageReportComponentConfig extends ReportComponentConfig {
  widthType: imageWidthType;
  customWidth?: number;
  alignment: imageAlignment;
}

export interface ImageReportComponentConfig extends BaseImageReportComponentConfig {
  sourceType: imageSourceType;
  imageUrl: string;
  type: ReportComponentType.IMAGE;
}

export interface DashboardReportComponentConfig extends BaseImageReportComponentConfig {
  config: Partial<DashboardReportConfig>;
  type: ReportComponentType.DASHBOARD;
}

export interface SubReportReportComponentConfig extends ReportComponentConfig {
  templateId: ReportTemplateId;
  avoidPageBreakInside: boolean;
  type: ReportComponentType.SUB_REPORT;
}

export interface PageBreakReportComponentConfig extends ReportComponentConfig {
  type: ReportComponentType.PAGE_BREAK;
}

export type ReportComponentConfigs =
  HeadingReportComponentConfig |
  RichTextReportComponentConfig |
  EntityTableReportComponentConfig |
  AlarmTableReportComponentConfig |
  ImageReportComponentConfig |
  DashboardReportComponentConfig |
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
      ReportComponentType.ALARM_TABLE,
      {
        type: ReportComponentType.ALARM_TABLE,
        alarmSource: {
          type: DatasourceType.entity,
          alarmFilterConfig: {},
          dataKeys: [
            {
              name: 'createdTime',
              type: DataKeyType.alarm,
              label: "Created time"
            },
            {
              name: 'originator',
              type: DataKeyType.alarm,
              label: "Originator"
            },
            {
              name: 'type',
              type: DataKeyType.alarm,
              label: "Type"
            },
            {
              name: 'severity',
              type: DataKeyType.alarm,
              label: "Severity"
            },
            {
              name: 'status',
              type: DataKeyType.alarm,
              label: "Status"
            },
            {
              name: 'assignee',
              type: DataKeyType.alarm,
              label: "Assignee"
            }
          ]
        },
        timewindow: historyInterval(DAY)
      }
    ],
    [
      ReportComponentType.IMAGE,
      {
        type: ReportComponentType.IMAGE,
        sourceType: 'image',
        imageUrl: null,
        widthType: 'fitWidth',
        alignment: 'center',
        dataSources: []
      }
    ],
    [
      ReportComponentType.DASHBOARD,
      {
        type: ReportComponentType.DASHBOARD,
        dataSources: [
          {
            type: DatasourceType.entity,
            dataKeys: []
          }
        ],
        config: {
          type: 'png'
        },
        widthType: 'fitWidth',
        alignment: 'center'
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
        templateId: null,
        avoidPageBreakInside: false
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
