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
import {
  TableReportColumnSettings,
  TimeseriesTableReportComponentConfig
} from '@shared/models/report-component.models';
import { AbstractReportComponentPreview } from '@home/pages/report/components/report-component.component';
import { DataKey, Datasource } from '@shared/models/widget.models';
import { ComponentStyle, textStyle } from '@shared/models/widget-settings.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';

@Component({
  selector: 'tb-timeseries-table-preview',
  templateUrl: './report-table-preview.component.html',
  styleUrls: ['./report-table-preview.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class TimeseriesTablePreviewComponent extends AbstractReportComponentPreview<TimeseriesTableReportComponentConfig> {

  columns: DataKey[] = [];

  onComponentUpdated() {
    this.columns = [];
    if (this.reportComponent.showTimestamp) {
      this.columns.push(
        {
          name: 'ts',
          label: this.reportComponent.timestampLabel || 'Timestamp',
          type: DataKeyType.timeseries,
          settings: this.reportComponent.timestampColumnSettings
        }
      );
    }
    const datasources: Datasource[] = this.reportComponent.dataSources;
    if (datasources && datasources.length) {
      const datasource = datasources[0];
      this.columns.push(...(datasource.dataKeys || []));
      this.columns.push(...(datasource.latestDataKeys || []));
    }
  }

  headerStyle(column: DataKey): ComponentStyle {
    return this.styleFromColumnSettings(column, true);
  }

  cellStyle(column: DataKey): ComponentStyle {
    return this.styleFromColumnSettings(column);
  }

  columnWidth(column: DataKey): string {
    if (column?.settings) {
      const columnSettings: TableReportColumnSettings =  column.settings;
      if (columnSettings?.columnWidth) {
        return columnSettings?.columnWidth;
      }
    }
    return null;
  }

  private styleFromColumnSettings(column: DataKey, header = false): ComponentStyle {
    let style: ComponentStyle = {};
    if (column?.settings) {
      const columnSettings: TableReportColumnSettings =  column.settings;
      if (columnSettings) {
        const cellSettings = header ? columnSettings.header : columnSettings.cell;
        if (cellSettings) {
          if (cellSettings.font && cellSettings.font.sizeUnit !== 'pt') {
            cellSettings.font.sizeUnit = 'pt';
          }
          style = textStyle(cellSettings.font);
          style.textAlign = cellSettings.textAlignment;
          style.verticalAlign = cellSettings.verticalAlignment;
          style.color = cellSettings.color;
          style.backgroundColor = cellSettings.backgroundColor;
        }
      }
    }
    if (!header) {
      if ('ts' === column.name) {
        style.fontSize = style.fontSize || '9pt';
      }
    }
    return style;
  }

}
