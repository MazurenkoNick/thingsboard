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

import { Component, DestroyRef, inject, ViewEncapsulation } from '@angular/core';
import { EntityTableReportComponentConfig } from '@shared/models/report-component.models';
import { DataKey, Datasource } from '@shared/models/widget.models';
import {
  AbstractReportTablePreviewComponent
} from '@home/pages/reporting/template/components/report-table-preview.component';
import { DatePipe } from '@angular/common';
import { ReportTemplatePageComponent } from '@home/pages/reporting/template/report-template-page.component';
import { ComponentStyle, dateFormatPreview } from '@shared/models/widget-settings.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-entity-table-preview',
  templateUrl: './report-table-preview.component.html',
  styleUrls: ['./report-table-preview.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class EntityTablePreviewComponent extends AbstractReportTablePreviewComponent<EntityTableReportComponentConfig> {

  private date = inject(DatePipe);
  private destroyRef = inject(DestroyRef);
  private templatePage = inject(ReportTemplatePageComponent);
  private timestampPreview = dateFormatPreview(this.date, this.templatePage.timeDataPattern);

  ngOnInit() {
    super.ngOnInit();
    this.templatePage.timeDataPattern$.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.timestampPreview = dateFormatPreview(this.date, this.templatePage.timeDataPattern);;
    });
  }

  cellContent(column: DataKey): string {
    if (column.name === 'createdTime' && !column.usePostProcessing) {
      return this.timestampPreview;
    } else {
      return super.cellContent(column);
    }
  }

  protected styleFromColumnSettings(column: DataKey, header = false): ComponentStyle {
    const style = super.styleFromColumnSettings(column, header);
    if (!this.isPlainFormat && !header) {
      if (column.name === 'createdTime') {
        style.fontSize = style.fontSize || '9pt';
      }
    }
    return style;
  }

  get columns(): DataKey[] {
    const datasources: Datasource[] = this.reportComponent.dataSources;
    if (datasources && datasources.length) {
      return datasources[0].dataKeys || [];
    }
    return [];
  }

}
