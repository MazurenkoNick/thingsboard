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

import { Datasource } from '@shared/models/widget.models';
import { alignment, alignmentTranslations, Font } from '@shared/models/widget-settings.models';
import { Insets } from '@shared/models/report.models';
import { ReportTemplateId } from '@shared/models/id/report-template-id';
import { FormProperty, FormPropertyType } from '@shared/models/dynamic-form.models';
import { DashboardReportConfig } from '@shared/models/dashboard-report.models';
import { Timewindow } from '@shared/models/time/time.models';
import { Direction } from '@shared/models/page/sort-order';

export enum ReportComponentType {
  HEADING = 'HEADING',
  RICH_TEXT = 'RICH_TEXT',
  ENTITY_TABLE = 'ENTITY_TABLE',
  TIME_SERIES_TABLE = 'TIME_SERIES_TABLE',
  ALARM_TABLE = 'ALARM_TABLE',
  DASHBOARD = 'DASHBOARD',
  IMAGE = 'IMAGE',
  SUB_REPORT = 'SUB_REPORT',
  DIVIDER = 'DIVIDER',
  PAGE_BREAK = 'PAGE_BREAK'
}

export interface ReportComponentConfig {
  type: ReportComponentType;
}

export interface DataReportComponentConfig extends ReportComponentConfig {
  dataSources?: Datasource[];
}

export interface LayoutReportComponentConfig extends ReportComponentConfig {
  background?: string;
  margins?: Insets;
  paddings?: Insets;
  borderWidth?: number;
  borderRadius?: number;
  borderColor?: string;
}

export const isLayoutReportComponentConfig = (obj: any): obj is LayoutReportComponentConfig => {
  return typeof obj === 'object' && obj !== null && 'background' in obj && 'margins' in obj && 'paddings' in obj;
};

export interface ReportComponentLayoutSettings {
  background?: string;
  margins?: Insets;
  paddings?: Insets;
  borderWidth?: number;
  borderRadius?: number;
  borderColor?: string;
}

export const toReportComponentLayoutSettings = (config: LayoutReportComponentConfig): ReportComponentLayoutSettings => {
  return {
    background: config.background,
    margins: config.margins,
    paddings: config.paddings,
    borderWidth: config.borderWidth,
    borderRadius: config.borderRadius,
    borderColor: config.borderColor
  };
}

export const updateFromReportComponentLayoutSettings =
  (config: LayoutReportComponentConfig, settings: ReportComponentLayoutSettings): void => {
    config.background = settings.background;
    config.margins = settings.margins;
    config.paddings = settings.paddings;
    config.borderWidth = settings.borderWidth;
    config.borderRadius = settings.borderRadius;
    config.borderColor = settings.borderColor;
}

export interface DataWithLayoutReportComponentConfig extends DataReportComponentConfig, LayoutReportComponentConfig {}

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

export interface HeadingReportComponentConfig extends DataWithLayoutReportComponentConfig {
  value: string;
  font?: Font;
  color?: string;
  textAlignment?: alignment;
  verticalAlignment?: alignment;
  height?: number;
  type:  ReportComponentType.HEADING;
}

export interface RichTextReportComponentConfig extends DataWithLayoutReportComponentConfig {
  value: string;
  type: ReportComponentType.RICH_TEXT;
}

export interface Heading {
  text: string;
  font?: Font;
  color?: string;
  textAlignment?: alignment;
  verticalAlignment?: alignment;
  height?: number;
}

export interface TableSortOrder {
  column: string;
  direction: Direction;
}

export interface TableReportComponentConfig extends DataReportComponentConfig {
  showTableHeading: boolean;
  tableHeading: Heading;
  tableSortOrder: TableSortOrder;
}

export interface TableWithLayoutReportComponentConfig extends TableReportComponentConfig, LayoutReportComponentConfig {}

export interface EntityTableReportComponentConfig extends TableWithLayoutReportComponentConfig {
  type: ReportComponentType.ENTITY_TABLE;
}

export interface AlarmTableReportComponentConfig extends TableWithLayoutReportComponentConfig {
  alarmSource: Datasource;
  timewindow: Timewindow;
  type: ReportComponentType.ALARM_TABLE;
}

export interface TimeseriesTableReportComponentConfig extends TableWithLayoutReportComponentConfig {
  timewindow: Timewindow;
  showTimestamp: boolean;
  timestampLabel: string;
  timestampPattern: string;
  timestampColumnSettings?: TableReportColumnSettings;
  type: ReportComponentType.TIME_SERIES_TABLE;
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

export interface BaseImageReportComponentConfig extends DataWithLayoutReportComponentConfig {
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

export interface SubReportReportComponentConfig extends DataReportComponentConfig {
  templateId: ReportTemplateId;
  avoidPageBreakInside: boolean;
  type: ReportComponentType.SUB_REPORT;
}

export enum BorderLength {
  LONG = 'LONG',
  SHORT = 'SHORT'
}

export const borderLengths = Object.keys(BorderLength) as BorderLength[];

export const borderLengthTranslations = new Map<BorderLength, string>(
  [
    [BorderLength.LONG, 'report-template.component.divider.divider-type-long'],
    [BorderLength.SHORT, 'report-template.component.divider.divider-type-short']
  ]
);

export enum BorderType {
  solid = 'solid',
  dashed = 'dashed',
  dotted = 'dotted'
}

export const borderTypes = Object.keys(BorderType) as BorderType[];

export const borderTypeTranslations = new Map<BorderType, string>(
  [
    [BorderType.solid, 'report-template.component.divider.line-type-solid'],
    [BorderType.dashed, 'report-template.component.divider.line-type-dashed'],
    [BorderType.dotted, 'report-template.component.divider.line-type-dotted']
  ]
);

export interface DividerReportComponentConfig extends LayoutReportComponentConfig {
  length: BorderLength;
  borderType: BorderType;
  widthPx: number;
  color: string;
  type: ReportComponentType.DIVIDER;
}

export interface PageBreakReportComponentConfig extends ReportComponentConfig {
  type: ReportComponentType.PAGE_BREAK;
}
