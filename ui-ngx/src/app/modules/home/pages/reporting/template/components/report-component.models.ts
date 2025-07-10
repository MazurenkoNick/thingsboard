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
  AlarmTableReportComponentConfig,
  BorderLength,
  BorderType,
  DashboardReportComponentConfig,
  DividerReportComponentConfig,
  EntityTableReportComponentConfig,
  HeadingReportComponentConfig,
  ImageReportComponentConfig,
  PageBreakReportComponentConfig,
  ReportComponentConfig,
  ReportComponentType,
  ReportDataKeySettingsType,
  RichTextReportComponentConfig,
  SubReportReportComponentConfig,
  TimeseriesTableReportComponentConfig
} from '@shared/models/report-component.models';
import { Type } from '@angular/core';
import { HeadingPreviewComponent } from '@home/pages/reporting/template/components/heading-preview.component';
import { RichTextPreviewComponent } from '@home/pages/reporting/template/components/rich-text-preview.component';
import {
  AbstractReportComponentConfig
} from '@home/pages/reporting/template/components/report-component-config.component';
import { HeadingConfigComponent } from '@home/pages/reporting/template/components/heading-config.component';
import { RichTextConfigComponent } from '@home/pages/reporting/template/components/rich-text-config.component';
import { IAliasController } from '@core/api/widget-api.models';
import { EntityService } from '@core/http/entity.service';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';
import { AbstractReportComponentPreview } from '@home/pages/reporting/template/components/report-component.component';
import { PageBreakPreviewComponent } from '@home/pages/reporting/template/components/page-break-preview.component';
import { EmptyReportConfigComponent } from '@home/pages/reporting/template/components/empty-report-config.component';
import { EntityTablePreviewComponent } from '@home/pages/reporting/template/components/entity-table-preview.component';
import { EntityTableConfigComponent } from '@home/pages/reporting/template/components/entity-table-config.component';
import {
  EntityAliasSelectCallbacks
} from '@home/components/widget/lib/settings/common/alias/entity-alias-select.component.models';
import {
  FilterSelectCallbacks
} from '@home/components/widget/lib/settings/common/filter/filter-select.component.models';
import { SubReportPreviewComponent } from '@home/pages/reporting/template/components/sub-report-preview.component';
import { SubReportConfigComponent } from '@home/pages/reporting/template/components/sub-report-config.component';
import { ImagePreviewComponent } from '@home/pages/reporting/template/components/image-preview.component';
import { ImageConfigComponent } from '@home/pages/reporting/template/components/image-config.component';

import keyImageTemplate from './key-image-svg.raw';
import { insertVariable, stringToBase64 } from '@core/utils';
import { DataKey, DatasourceType } from '@shared/models/widget.models';
import { DashboardPreviewComponent } from '@home/pages/reporting/template/components/dashboard-preview.component';
import { DashboardConfigComponent } from '@home/pages/reporting/template/components/dashboard-config.component';
import { AlarmTablePreviewComponent } from '@home/pages/reporting/template/components/alarm-table-preview.component';
import { AlarmTableConfigComponent } from '@home/pages/reporting/template/components/alarm-table-config.component';
import {
  TimeseriesTablePreviewComponent
} from '@home/pages/reporting/template/components/timeseries-table-preview.component';
import {
  TimeseriesTableConfigComponent
} from '@home/pages/reporting/template/components/timeseries-table-config.component';
import { TbReportFormat } from '@shared/models/report.models';
import { Font } from '@shared/models/widget-settings.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { AggregationType, DAY, historyInterval } from '@shared/models/time/time.models';
import { DividerPreviewComponent } from '@home/pages/reporting/template/components/divider-preview.component';
import { DividerConfigComponent } from '@home/pages/reporting/template/components/divider-config.component';
import { Direction } from '@shared/models/page/sort-order';

export interface ReportComponentLibraryItem<C extends ReportComponentConfig = ReportComponentConfig> {
  title: string;
  previewImage: string;
  type: ReportComponentType;
  defaultConfig: C;
}

export const reportComponentsLibrary = new Map<string, ReportComponentLibraryItem>(
  [
    [
      'heading',
      {
        title: 'report-template.component.heading.type',
        previewImage: '/assets/report/components/heading.svg',
        type: ReportComponentType.HEADING,
        defaultConfig: {
          type: ReportComponentType.HEADING,
          value: 'Heading',
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
          dataSources: [],
          margins: null,
          paddings: null,
          background: null
        } as HeadingReportComponentConfig
      }
    ],
    [
      'richText',
      {
        title: 'report-template.component.rich-text.type',
        previewImage: '/assets/report/components/rich-text.svg',
        type: ReportComponentType.RICH_TEXT,
        defaultConfig: {
          type: ReportComponentType.RICH_TEXT,
          value: '<p style="line-height: 1.5;">Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec libero orci, faucibus in iaculis quis, vestibulum sit amet ligula. Nulla facilisi. Ut ut iaculis tortor.</p>',
          dataSources: [],
          margins: null,
          paddings: null,
          background: null
        } as RichTextReportComponentConfig
      }
    ],
    [
      'textSection',
      {
        title: 'report-template.component.text-section',
        previewImage: '/assets/report/components/text-section.svg',
        type: ReportComponentType.RICH_TEXT,
        defaultConfig: {
          type: ReportComponentType.RICH_TEXT,
          value: '<p><span style="font-size: 14px; color: rgb(0, 105, 92); font-weight: 500;">SUBHEADING</span></p>\n' +
            '<p><span style="font-size: 28px; font-weight: 500;">Heading</span></p>\n' +
            '<p style="line-height: 1.5;" >Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec libero orci, faucibus in iaculis quis, vestibulum sit amet ligula. Nulla facilisi. Ut ut iaculis tortor.</p>',
          dataSources: [],
          margins: null,
          paddings: null,
          background: null
        } as RichTextReportComponentConfig
      }
    ],
    [
      'textImage',
      {
        title: 'report-template.component.text-image',
        previewImage: '/assets/report/components/text-image.svg',
        type: ReportComponentType.RICH_TEXT,
        defaultConfig: {
          type: ReportComponentType.RICH_TEXT,
          value: '<table style="border-collapse: collapse; width: 100%; border-width: 0px; border-style: none; border-spacing: 0px;" border="1"><colgroup><col style="width: 49%;"><col style="width: 2%;"><col style="width: 49%;"></colgroup>\n' +
            '<tbody>\n' +
            '<tr>\n' +
            '<td style="border-style: none; padding: 0px;">\n' +
            '<p><span style="font-size: 28px; font-weight: 500;">Heading</span></p>\n' +
            '<p style="line-height: 1.5;">Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec libero orci, faucibus in iaculis quis, vestibulum sit amet ligula. Nulla facilisi. Ut ut iaculis tortor.</p>\n' +
            '</td>\n' +
            '<td style="border-style: none; padding: 0px;">\n' +
            '<p>&nbsp;</p>\n' +
            '</td>\n' +
            '<td style="vertical-align: middle; border-style: none; padding: 0px;"><img style="display: block; margin-left: auto; margin-right: auto;" src="" width="366px" height="232px"></td>\n' +
            '</tr>\n' +
            '</tbody>\n' +
            '</table>',
          dataSources: [],
          margins: null,
          paddings: null,
          background: null
        } as RichTextReportComponentConfig
      }
    ],
    [
      'imageText',
      {
        title: 'report-template.component.image-text',
        previewImage: '/assets/report/components/image-text.svg',
        type: ReportComponentType.RICH_TEXT,
        defaultConfig: {
          type: ReportComponentType.RICH_TEXT,
          value: '<table style="border-collapse: collapse; width: 100%; border-width: 0px; border-style: none; border-spacing: 0px;" border="1"><colgroup><col style="width: 49%;"><col style="width: 2%;"><col style="width: 49%;"></colgroup>\n' +
            '<tbody>\n' +
            '<tr>\n' +
            '<td style="vertical-align: middle; border-style: none; padding: 0px;"><img style="display: block; margin-left: auto; margin-right: auto;" src="" width="366px" height="232px"></td>\n' +
            '<td style="border-style: none; padding: 0px;">\n' +
            '<p>&nbsp;</p>\n' +
            '</td>\n' +
            '<td style="border-style: none; padding: 0px;">\n' +
            '<p><span style="font-size: 28px; font-weight: 500;">Heading</span></p>\n' +
            '<p style="line-height: 1.5;">Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec libero orci, faucibus in iaculis quis, vestibulum sit amet ligula. Nulla facilisi. Ut ut iaculis tortor.</p>\n' +
            '</td>\n' +
            '</tr>\n' +
            '</tbody>\n' +
            '</table>',
          dataSources: [],
          margins: null,
          paddings: null,
          background: null
        } as RichTextReportComponentConfig
      }
    ],
    [
      'entityTable',
      {
        title: 'report-template.component.entity-table.type',
        previewImage: '/assets/report/components/entity-table.svg',
        type: ReportComponentType.ENTITY_TABLE,
        defaultConfig: {
          type: ReportComponentType.ENTITY_TABLE,
          showTableHeading: false,
          tableHeading: {
            text: "Entities",
            font: {
              size: 20,
              sizeUnit: 'pt',
              weight: 'normal',
              style: 'normal',
              family: 'Roboto'
            } as Font,
            color: '#000',
            textAlignment: 'center',
            verticalAlignment: 'middle',
            height: 40
          },
          tableSortOrder: {
            column: 'Name',
            direction: Direction.ASC
          },
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
          ],
          margins: {
            top: 20
          },
          paddings: null,
          background: null
        } as EntityTableReportComponentConfig
      }
    ],
    [
      'timeSeriesTable',
      {
        title: 'report-template.component.timeseries-table.type',
        previewImage: '/assets/report/components/timeseries-table.svg',
        type: ReportComponentType.TIME_SERIES_TABLE,
        defaultConfig: {
          type: ReportComponentType.TIME_SERIES_TABLE,
          showTableHeading: true,
          tableHeading: {
            text: '${entityName}',
            font: {
              size: 20,
              sizeUnit: 'pt',
              weight: 'normal',
              style: 'normal',
              family: 'Roboto'
            } as Font,
            color: '#000',
            textAlignment: 'center',
            verticalAlignment: 'middle',
            height: 40
          },
          tableSortOrder: {
            column: 'Timestamp',
            direction: Direction.DESC
          },
          dataSources: [
            {
              type: DatasourceType.entity,
              dataKeys: [
                {
                  name: 'temperature',
                  type: DataKeyType.timeseries,
                  label: 'Temperature',
                  units: '°C',
                  decimals: 0
                }
              ]
            }
          ],
          timewindow: {...historyInterval(DAY),
            aggregation: {
              type: AggregationType.NONE,
              limit: 200
            }
          },
          showTimestamp: true,
          timestampLabel: 'Timestamp',
          timestampPattern: 'yyyy-MM-dd HH:mm:ss',
          timestampColumnSettings: {
            type: ReportDataKeySettingsType.COLUMN
          },
          margins: {
            top: 20
          },
          paddings: null,
          background: null
        } as TimeseriesTableReportComponentConfig
      }
    ],
    [
      'alarmTable',
      {
        title: 'report-template.component.alarm-table.type',
        previewImage: '/assets/report/components/alarm-table.svg',
        type: ReportComponentType.ALARM_TABLE,
        defaultConfig: {
          type: ReportComponentType.ALARM_TABLE,
          showTableHeading: false,
          tableHeading: {
            text: "Alarms",
            font: {
              size: 20,
              sizeUnit: 'pt',
              weight: 'normal',
              style: 'normal',
              family: 'Roboto'
            } as Font,
            color: '#000',
            textAlignment: 'center',
            verticalAlignment: 'middle',
            height: 40
          },
          tableSortOrder: {
            column: 'Created time',
            direction: Direction.DESC
          },
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
          timewindow: historyInterval(DAY),
          margins: {
            top: 20
          },
          paddings: null,
          background: null
        } as AlarmTableReportComponentConfig
      }
    ],
    [
      'image',
      {
        title: 'report-template.component.image.type',
        previewImage: '/assets/report/components/image.svg',
        type: ReportComponentType.IMAGE,
        defaultConfig: {
          type: ReportComponentType.IMAGE,
          sourceType: 'image',
          imageUrl: null,
          widthType: 'fitWidth',
          alignment: 'center',
          dataSources: [],
          margins: null,
          paddings: null,
          background: null
        } as ImageReportComponentConfig
      }
    ],
    [
      'dashboard',
      {
        title: 'report-template.component.dashboard.type',
        previewImage: '/assets/report/components/dashboard.svg',
        type: ReportComponentType.DASHBOARD,
        defaultConfig: {
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
          alignment: 'center',
          margins: null,
          paddings: null,
          background: null
        } as DashboardReportComponentConfig
      }
    ],
    [
      'subReport',
      {
        title: 'report-template.component.sub-report.type',
        previewImage: '/assets/report/components/subreport.svg',
        type: ReportComponentType.SUB_REPORT,
        defaultConfig: {
          type: ReportComponentType.SUB_REPORT,
          dataSources: [
            {
              type: DatasourceType.entity,
              dataKeys: []
            }
          ],
          templateId: null,
          avoidPageBreakInside: false
        } as SubReportReportComponentConfig
      }
    ],
    [
      'logoHeading',
      {
        title: 'report-template.component.logo-heading',
        previewImage: '/assets/report/components/logo-heading.svg',
        type: ReportComponentType.RICH_TEXT,
        defaultConfig: {
          type: ReportComponentType.RICH_TEXT,
          value: '<table style="border-collapse: collapse; width: 100%; border-width: 0px; border-style: none; border-spacing: 0px;" border="1"><colgroup><col><col style="width: 100%;"></colgroup>\n' +
            '<tbody>\n' +
            '<tr>\n' +
            '<td style="border-style: none; padding: 0px;"><img style="float: left;" src="tb-image;/assets/report/components/logo-placeholder.svg" width="140px" height="24px"></td>\n' +
            '<td style="vertical-align: middle; border-style: none; padding: 0px; text-align: right; line-height: 1.2;"><strong><span style="font-size: 20px;">Heading</span></strong></td>\n' +
            '</tr>\n' +
            '</tbody>\n' +
            '</table>',
          dataSources: [],
          margins: null,
          paddings: {
            top: 9,
            bottom: 9,
            left: 6,
            right: 6
          },
          background: null
        } as RichTextReportComponentConfig
      }
    ],
    [
      'headingLogo',
      {
        title: 'report-template.component.heading-logo',
        previewImage: '/assets/report/components/heading-logo.svg',
        type: ReportComponentType.RICH_TEXT,
        defaultConfig: {
          type: ReportComponentType.RICH_TEXT,
          value: '<table style="border-collapse: collapse; width: 100%; border-width: 0px; border-style: none; border-spacing: 0px;" border="1"><colgroup><col style="width: 100%;"><col></colgroup>\n' +
            '<tbody>\n' +
            '<tr>\n' +
            '<td style="vertical-align: middle; border-style: none; padding: 0px; text-align: left; line-height: 1.2;"><strong><span style="font-size: 20px;">Heading</span></strong></td>\n' +
            '<td style="border-style: none; padding: 0px;"><img style="float: right;" src="tb-image;/assets/report/components/logo-placeholder.svg" width="140px" height="24px"></td>\n' +
            '</tr>\n' +
            '</tbody>\n' +
            '</table>',
          dataSources: [],
          margins: null,
          paddings: {
            top: 9,
            bottom: 9,
            left: 6,
            right: 6
          },
          background: null
        } as RichTextReportComponentConfig
      }
    ],
    [
      'logoText',
      {
        title: 'report-template.component.logo-text',
        previewImage: '/assets/report/components/logo-text.svg',
        type: ReportComponentType.RICH_TEXT,
        defaultConfig: {
          type: ReportComponentType.RICH_TEXT,
          value: '<table style="border-collapse: collapse; width: 100%; border-width: 0px; border-style: none; border-spacing: 0px;" border="1"><colgroup><col><col style="width: 100%;"></colgroup>\n' +
            '<tbody>\n' +
            '<tr>\n' +
            '<td style="border-style: none; padding: 0px;"><img style="float: left;" src="tb-image;/assets/report/components/logo-placeholder.svg" width="140px" height="24px"></td>\n' +
            '<td style="vertical-align: middle; border-style: none; padding: 0px; text-align: right; line-height: 1.2;"><span style="font-size: 14px; color: rgb(117, 117, 117);">2289 5th Ave New York, New York(NY), 10037</span></td>\n' +
            '</tr>\n' +
            '</tbody>\n' +
            '</table>',
          dataSources: [],
          margins: null,
          paddings: {
            top: 9,
            bottom: 9,
            left: 6,
            right: 6
          },
          background: null
        } as RichTextReportComponentConfig
      }
    ],
    [
      'textLogo',
      {
        title: 'report-template.component.text-logo',
        previewImage: '/assets/report/components/text-logo.svg',
        type: ReportComponentType.RICH_TEXT,
        defaultConfig: {
          type: ReportComponentType.RICH_TEXT,
          value: '<table style="border-collapse: collapse; width: 100%; border-width: 0px; border-style: none; border-spacing: 0px;" border="1"><colgroup><col style="width: 100%;"><col></colgroup>\n' +
            '<tbody>\n' +
            '<tr>\n' +
            '<td style="vertical-align: middle; border-style: none; padding: 0px; text-align: left; line-height: 1.2;"><span style="font-size: 14px; color: rgb(117, 117, 117);">2289 5th Ave New York, New York(NY), 10037</span></td>\n' +
            '<td style="border-style: none; padding: 0px;"><img style="float: right;" src="tb-image;/assets/report/components/logo-placeholder.svg" width="140px" height="24px"></td>\n' +
            '</tr>\n' +
            '</tbody>\n' +
            '</table>',
          dataSources: [],
          margins: null,
          paddings: {
            top: 9,
            bottom: 9,
            left: 6,
            right: 6
          },
          background: null
        } as RichTextReportComponentConfig
      }
    ],
    [
      'logoText2',
      {
        title: 'report-template.component.logo-text-2',
        previewImage: '/assets/report/components/logo-text-2.svg',
        type: ReportComponentType.RICH_TEXT,
        defaultConfig: {
          type: ReportComponentType.RICH_TEXT,
          value: '<table style="border-collapse: collapse; width: 100%; border-width: 0px; border-style: none; border-spacing: 0px;" border="1"><colgroup><col style="width: 33%;"><col style="width: 33%;"><col style="width: 33%;"></colgroup>\n' +
            '<tbody>\n' +
            '<tr>\n' +
            '<td style="vertical-align: middle; border-style: none; padding: 0px; text-align: right; line-height: 1.2;"><span style="font-size: 14px; color: rgb(117, 117, 117);">2289 5th Ave New York,<br>New York(NY), 10037</span></td>\n' +
            '<td style="border-style: none; padding: 0px;"><img style="display: block; margin-left: auto; margin-right: auto;" src="tb-image;/assets/report/components/logo-placeholder.svg" width="140px" height="23px"></td>\n' +
            '<td style="vertical-align: middle; border-style: none; padding: 0px; line-height: 1.2; text-align: left;"><span style="font-size: 14px; color: rgb(117, 117, 117);">Company name<br>+1 (727) 441-2403</span></td>\n' +
            '</tr>\n' +
            '</tbody>\n' +
            '</table>',
          dataSources: [],
          margins: null,
          paddings: {
            top: 9,
            bottom: 9,
            left: 6,
            right: 6
          },
          background: null
        } as RichTextReportComponentConfig
      }
    ],
    [
      'footer1',
      {
        title: 'report-template.component.footer-1',
        previewImage: '/assets/report/components/footer-1.svg',
        type: ReportComponentType.RICH_TEXT,
        defaultConfig: {
          type: ReportComponentType.RICH_TEXT,
          value: '<table style="border-collapse: collapse; width: 100%; border-width: 0px; height: 44px;" border="1"><colgroup><col style="width: 100%;"></colgroup>\n' +
            '<tbody>\n' +
            '<tr style="height: 24px;">\n' +
            '<td style="border-width: 0px; height: 24px; line-height: 1.5;"><span style="font-size: 16px; font-weight: 500;">Company name</span></td>\n' +
            '</tr>\n' +
            '<tr style="height: 20px;">\n' +
            '<td style="border-width: 0px; height: 20px; line-height: 1.5;"><span style="font-size: 14px; color: rgb(117, 117, 117);">2289 5th Ave New York, New York(NY), 10037</span></td>\n' +
            '</tr>\n' +
            '</tbody>\n' +
            '</table>',
          dataSources: [],
          margins: null,
          paddings: {
            left: 6,
            right: 6
          },
          background: null
        } as RichTextReportComponentConfig
      }
    ],
    [
      'footer2',
      {
        title: 'report-template.component.footer-2',
        previewImage: '/assets/report/components/footer-2.svg',
        type: ReportComponentType.RICH_TEXT,
        defaultConfig: {
          type: ReportComponentType.RICH_TEXT,
          value: '<table style="border-collapse: collapse; width: 100%; border-width: 0px;" border="1"><colgroup><col><col style="width: 100%;"></colgroup>\n' +
            '<tbody>\n' +
            '<tr>\n' +
            '<td style="border-width: 0px;"><img style="float: left;" src="tb-image;/assets/report/components/logo-placeholder.svg" width="140px" height="23px"></td>\n' +
            '<td style="border-width: 0px;">\n' +
            '<table style="border-collapse: collapse; width: 100%; border-width: 0px; height: 48px;" border="1"><colgroup><col style="width: 100%;"></colgroup>\n' +
            '<tbody>\n' +
            '<tr style="height: 24px;">\n' +
            '<td style="border-width: 0px; height: 24px; line-height: 1.5; text-align: right;"><span style="font-size: 16px; font-weight: 500;">Company name</span></td>\n' +
            '</tr>\n' +
            '<tr style="height: 24px;">\n' +
            '<td style="border-width: 0px; height: 24px; line-height: 1.5; text-align: right;"><span style="font-size: 14px; color: rgb(117, 117, 117);">2289 5th Ave New York, New York(NY), 10037</span></td>\n' +
            '</tr>\n' +
            '</tbody>\n' +
            '</table>\n' +
            '</td>\n' +
            '</tr>\n' +
            '</tbody>\n' +
            '</table>',
          dataSources: [],
          margins: null,
          paddings: {
            left: 6,
            right: 6
          },
          background: null
        } as RichTextReportComponentConfig
      }
    ],
    [
      'footer3',
      {
        title: 'report-template.component.footer-3',
        previewImage: '/assets/report/components/footer-3.svg',
        type: ReportComponentType.RICH_TEXT,
        defaultConfig: {
          type: ReportComponentType.RICH_TEXT,
          value: '<table style="border-collapse: collapse; width: 100%; border-width: 0px;" border="1"><colgroup><col style="width: 100%;"><col></colgroup>\n' +
            '<tbody>\n' +
            '<tr>\n' +
            '<td style="border-width: 0px;">\n' +
            '<table style="border-collapse: collapse; width: 100%; border-width: 0px; height: 48px;" border="1"><colgroup><col style="width: 100%;"></colgroup>\n' +
            '<tbody>\n' +
            '<tr style="height: 24px;">\n' +
            '<td style="border-width: 0px; height: 24px; line-height: 1.5; text-align: left;"><span style="font-size: 16px; font-weight: 500;">Company name</span></td>\n' +
            '</tr>\n' +
            '<tr style="height: 24px;">\n' +
            '<td style="border-width: 0px; height: 24px; line-height: 1.5; text-align: left;"><span style="font-size: 14px; color: rgb(117, 117, 117);">2289 5th Ave New York, New York(NY), 10037</span></td>\n' +
            '</tr>\n' +
            '</tbody>\n' +
            '</table>\n' +
            '</td>\n' +
            '<td style="border-width: 0px;"><img style="float: right;" src="tb-image;/assets/report/components/logo-placeholder.svg" width="140px" height="23px"></td>\n' +
            '</tr>\n' +
            '</tbody>\n' +
            '</table>',
          dataSources: [],
          margins: null,
          paddings: {
            left: 6,
            right: 6
          },
          background: null
        } as RichTextReportComponentConfig
      }
    ],
    [
      'pageNumber',
      {
        title: 'report-template.component.page-number',
        previewImage: '/assets/report/components/page-number.svg',
        type: ReportComponentType.HEADING,
        defaultConfig: {
          type: ReportComponentType.HEADING,
          value: 'Page: ${pageNumber}/${totalPages}',
          font: {
            size: 9,
            sizeUnit: 'pt',
            weight: '500',
            style: 'normal',
            family: 'Roboto'
          } as Font,
          color: '#000',
          textAlignment: 'center',
          verticalAlignment: 'middle',
          height: undefined,
          dataSources: [],
          margins: null,
          paddings: {
            top: 6,
            bottom: 6,
            left: 6,
            right: 6
          },
          background: null
        } as HeadingReportComponentConfig
      }
    ],
    [
      'createdTime',
      {
        title: 'report-template.component.created-time',
        previewImage: '/assets/report/components/created-time.svg',
        type: ReportComponentType.HEADING,
        defaultConfig: {
          type: ReportComponentType.HEADING,
          value: 'Created: ${reportCreatedTime}',
          font: {
            size: 9,
            sizeUnit: 'pt',
            weight: '500',
            style: 'normal',
            family: 'Roboto'
          } as Font,
          color: '#000',
          textAlignment: 'center',
          verticalAlignment: 'middle',
          height: undefined,
          dataSources: [],
          margins: null,
          paddings: {
            top: 6,
            bottom: 6,
            left: 6,
            right: 6
          },
          background: null
        } as HeadingReportComponentConfig
      }
    ],
    [
      'divider',
      {
        title: 'report-template.component.divider.type',
        previewImage: '/assets/report/components/divider.svg',
        type: ReportComponentType.DIVIDER,
        defaultConfig: {
          type: ReportComponentType.DIVIDER,
          length: BorderLength.LONG,
          borderType: BorderType.solid,
          widthPx: 1,
          color: '#d6d6d6',
          margins: null,
          paddings: {
            top: 15,
            bottom: 15,
            left: 8,
            right: 8
          },
          background: null
        } as DividerReportComponentConfig
      }
    ],
    [
      'pageBreak',
      {
        title: 'report-template.component.page-break.type',
        previewImage: '/assets/report/components/page-break.svg',
        type: ReportComponentType.PAGE_BREAK,
        defaultConfig: {
          type: ReportComponentType.PAGE_BREAK
        } as PageBreakReportComponentConfig
      }
    ]
  ]
);

export interface ReportComponentTypeData<C extends ReportComponentConfig = ReportComponentConfig> {
  title: string;
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
        previewComponent: HeadingPreviewComponent,
        configComponent: HeadingConfigComponent,
        editable: true
      }
    ],
    [
      ReportComponentType.RICH_TEXT,
      {
        title: 'report-template.component.rich-text.type',
        previewComponent: RichTextPreviewComponent,
        configComponent: RichTextConfigComponent,
        editable: true
      }
    ],
    [
      ReportComponentType.ENTITY_TABLE,
      {
        title: 'report-template.component.entity-table.type',
        previewComponent: EntityTablePreviewComponent,
        configComponent: EntityTableConfigComponent,
        editable: true
      }
    ],
    [
      ReportComponentType.TIME_SERIES_TABLE,
      {
        title: 'report-template.component.timeseries-table.type',
        previewComponent: TimeseriesTablePreviewComponent,
        configComponent: TimeseriesTableConfigComponent,
        editable: true
      }
    ],
    [
      ReportComponentType.ALARM_TABLE,
      {
        title: 'report-template.component.alarm-table.type',
        previewComponent: AlarmTablePreviewComponent,
        configComponent: AlarmTableConfigComponent,
        editable: true
      }
    ],
    [
      ReportComponentType.IMAGE,
      {
        title: 'report-template.component.image.type',
        previewComponent: ImagePreviewComponent,
        configComponent: ImageConfigComponent,
        editable: true
      }
    ],
    [
      ReportComponentType.DASHBOARD,
      {
        title: 'report-template.component.dashboard.type',
        previewComponent: DashboardPreviewComponent,
        configComponent: DashboardConfigComponent,
        editable: true
      }
    ],
    [
      ReportComponentType.SUB_REPORT,
      {
        title: 'report-template.component.sub-report.type',
        previewComponent: SubReportPreviewComponent,
        configComponent: SubReportConfigComponent,
        editable: true
      }
    ],
    [
      ReportComponentType.DIVIDER,
      {
        title: 'report-template.component.divider.type',
        previewComponent: DividerPreviewComponent,
        configComponent: DividerConfigComponent,
        editable: true
      }
    ],
    [
      ReportComponentType.PAGE_BREAK,
      {
        title: 'report-template.component.page-break.type',
        previewComponent: PageBreakPreviewComponent,
        configComponent: EmptyReportConfigComponent,
        editable: false,
        pageBreak: true
      }
    ]
  ]
);

export const reportComponentTypes = Array.from(reportComponentTypeMap.keys());

export const csvReportComponentTypes: ReportComponentType[] =
  [
    ReportComponentType.ENTITY_TABLE,
    ReportComponentType.TIME_SERIES_TABLE,
    ReportComponentType.ALARM_TABLE,
    ReportComponentType.SUB_REPORT
  ];

export interface ReportComponentContext {
  translate: TranslateService,
  utils: UtilsService,
  entityService: EntityService;
  aliasController: IAliasController;
  aliasAndFilterCallbacks: EntityAliasSelectCallbacks & FilterSelectCallbacks;
  format: TbReportFormat;
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
  {
    type: 'pageVariable',
    name: 'reportCreatedTime'
  }
];

export const keyImage = (key: string): string => {
  const result = insertVariable(keyImageTemplate, 'key', `\${${key}}`);
  const encodedSvg = stringToBase64(result);
  return `data:image/svg+xml;base64,${encodedSvg}`;
}

export const imagePlaceholder = '/assets/report/components/image-placeholder.svg';

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
