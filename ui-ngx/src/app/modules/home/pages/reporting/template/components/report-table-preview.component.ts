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

import { Directive } from '@angular/core';
import { AbstractReportComponentPreview } from '@home/pages/reporting/template/components/report-component.component';
import { TableReportColumnSettings, TableReportComponentConfig } from '@shared/models/report-component.models';
import { ComponentStyle, Font, textStyle } from '@shared/models/widget-settings.models';
import { deepClone } from '@core/utils';
import { DataKey } from '@shared/models/widget.models';
import { Direction } from '@shared/models/page/sort-order';

@Directive()
export abstract class AbstractReportTablePreviewComponent<C extends TableReportComponentConfig> extends AbstractReportComponentPreview<C> {

  showTableHeading: boolean;
  headingText: string;
  headingHeight: string;
  headingStyle: ComponentStyle;

  onComponentUpdated() {
    if (this.reportComponent.showTableHeading && this.reportComponent.tableHeading) {
        this.showTableHeading = true;
        const tableHeading = this.reportComponent.tableHeading;
        if (tableHeading.text && tableHeading.text.trim().length) {
          this.headingText = tableHeading.text;
        } else {
          this.headingText = '&nbsp;';
        }
        if (!this.isPlainFormat) {
          const font: Font = deepClone(tableHeading.font || {size: 20, sizeUnit: 'pt'} as Font);
          if (!font.size) {
            font.size = 20;
          }
          if (font.sizeUnit !== 'pt') {
            font.sizeUnit = 'pt';
          }
          this.headingStyle = textStyle(font);
          if (!this.headingStyle.fontWeight) {
            this.headingStyle.fontWeight = 'normal';
          }
          this.headingStyle.color = tableHeading.color || '#000';
          if (tableHeading.textAlignment) {
            this.headingStyle.textAlign = tableHeading.textAlignment;
          }
          if (tableHeading.verticalAlignment) {
            this.headingStyle.verticalAlign = tableHeading.verticalAlignment;
          }
          if (tableHeading.height) {
            this.headingHeight = tableHeading.height + 'pt';
          } else {
            this.headingHeight = '100%';
          }
        }
    } else {
      this.showTableHeading = false;
    }
  }


  headerStyle(column: DataKey): ComponentStyle {
    return this.styleFromColumnSettings(column, true);
  }

  cellStyle(column: DataKey): ComponentStyle {
    return this.styleFromColumnSettings(column);
  }

  columnWidth(column: DataKey): string {
    if (!this.isPlainFormat && column?.settings) {
      const columnSettings: TableReportColumnSettings =  column.settings;
      if (columnSettings?.columnWidth) {
        return columnSettings?.columnWidth;
      }
    }
    return null;
  }

  hasSortOrder(column: DataKey): boolean {
    return this.reportComponent.tableSortOrder?.column === column.label;
  }

  ascSortOrder(): boolean {
    return this.reportComponent.tableSortOrder?.direction !== Direction.DESC;
  }

  cellContent(column: DataKey): string {
    return '${' + column.label + '}';
  }

  protected styleFromColumnSettings(column: DataKey, header = false): ComponentStyle {
    let style: ComponentStyle = {};
    if (!this.isPlainFormat && column?.settings) {
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
    return style;
  }
}
