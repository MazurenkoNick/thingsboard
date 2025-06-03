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
import { FormGroup } from '@angular/forms';
import {
  DataKey,
  Datasource,
  EntityTableReportComponentConfig,
  TableReportColumnSettingsForm,
  WidgetConfigMode
} from '@app/shared/public-api';
import { AbstractReportComponentConfig } from '@home/pages/report/components/report-component-config.component';

@Component({
  selector: 'tb-entity-table-config',
  templateUrl: './entity-table-config.component.html',
  styleUrls: ['./report-component-config.scss'],
  encapsulation: ViewEncapsulation.None
})
export class EntityTableConfigComponent extends AbstractReportComponentConfig<EntityTableReportComponentConfig> {

  settingsTab: 'data' | 'layout' = 'data';

  basicMode = WidgetConfigMode.basic;

  TableReportColumnSettingsForm = TableReportColumnSettingsForm;

  protected buildForm(reportComponentConfig: EntityTableReportComponentConfig): FormGroup {
    return this.fb.group({
      dataSources: [reportComponentConfig.dataSources, []],
      columns: [this.getColumns(reportComponentConfig.dataSources), []],
    });
  }

  protected prepareOutputConfig(config: any): any {
    this.setColumns(config.columns, config.dataSources);
    delete config.columns;
    return config;
  }

  private getColumns(datasources?: Datasource[]): DataKey[] {
    if (datasources && datasources.length) {
      return datasources[0].dataKeys || [];
    }
    return [];
  }

  private setColumns(columns: DataKey[], datasources?: Datasource[]) {
    if (datasources && datasources.length) {
      datasources[0].dataKeys = columns;
    }
  }

}
