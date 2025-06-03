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
  EntityTableReportComponentConfig,
  RichTextReportComponentConfig, TableReportColumnSettings
} from '@shared/models/report-component.models';
import { AbstractReportComponentPreview } from '@home/pages/report/components/report-component.component';
import { DataKey, Datasource } from '@shared/models/widget.models';
import { ComponentStyle, textStyle } from '@shared/models/widget-settings.models';

@Component({
  selector: 'tb-entity-table-preview',
  templateUrl: './entity-table-preview.component.html',
  styleUrls: ['./entity-table-preview.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class EntityTablePreviewComponent extends AbstractReportComponentPreview<EntityTableReportComponentConfig> {

  get columns(): DataKey[] {
    const datasources: Datasource[] = this.reportComponent.dataSources;
    if (datasources && datasources.length) {
      return datasources[0].dataKeys || [];
    }
    return [];
  }

  onComponentUpdated() {
  }

  headerStyle(column: DataKey): ComponentStyle {
    return this.styleFromColumnSettings(column, true);
  }

  cellStyle(column: DataKey): ComponentStyle {
    return this.styleFromColumnSettings(column);
  }

  private styleFromColumnSettings(column: DataKey, header = false): ComponentStyle {
    let style: ComponentStyle = {};
    if (column?.settings) {
      const columnSettings: TableReportColumnSettings =  column.settings;
      if (columnSettings) {
        const cellSettings = header ? columnSettings.header : columnSettings.cell;
        if (cellSettings) {
          style = textStyle(cellSettings.font);
          style.textAlign = cellSettings.textAlignment;
          style.verticalAlign = cellSettings.verticalAlignment;
          style.color = cellSettings.color;
          style.backgroundColor = cellSettings.backgroundColor;
        }
        if (header && columnSettings.columnWidth) {
          style.width = columnSettings.columnWidth;
        }
      }
    }
    return style;
  }

}
