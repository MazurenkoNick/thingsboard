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

import { Component, ViewEncapsulation } from '@angular/core';
import { FormGroup } from '@angular/forms';
import {
  AbstractReportComponentConfig
} from '@home/pages/reporting/template/components/report-component-config.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  EntityTableReportComponentConfig,
  ReportDataKeySettingsType,
  TableReportColumnSettingsForm,
  TimeColumnSettingsForm
} from '@shared/models/report-component.models';
import { DataKey, Datasource, WidgetConfigMode } from '@shared/models/widget.models';
import {
  DataKeySettingsFormFunction
} from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import { FormProperty } from '@shared/models/dynamic-form.models';
import { pairwise, startWith } from 'rxjs';

@Component({
    selector: 'tb-entity-table-config',
    templateUrl: './entity-table-config.component.html',
    styleUrls: ['./report-component-config.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class EntityTableConfigComponent extends AbstractReportComponentConfig<EntityTableReportComponentConfig> {

  get columnLabels(): string[] {
    const columns: DataKey[] = this.reportConfigForm.get('columns').value;
    return (columns || []).map(key => key.label);
  }

  columnNameChanged: [string, string];

  settingsTab: 'data' | 'layout' = 'data';

  basicMode = WidgetConfigMode.basic;

  dataKeySettingsFormFunction: DataKeySettingsFormFunction = this.getDataKeySettingsForm.bind(this);

  private getDataKeySettingsForm(key: DataKey): FormProperty[] {
    if (key.name === 'createdTime') {
      return TimeColumnSettingsForm;
    }
    return TableReportColumnSettingsForm;
  }

  protected buildForm(reportComponentConfig: EntityTableReportComponentConfig): FormGroup {
    const form = this.fb.group({
      showTableHeading: [reportComponentConfig.showTableHeading, []],
      tableHeading: [reportComponentConfig.tableHeading, []],
      tableSortOrder: [reportComponentConfig.tableSortOrder, []],
      dataSources: [reportComponentConfig.dataSources, []],
      columns: [this.getColumns(reportComponentConfig.dataSources), []],
    });
    form.get('showTableHeading').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(form);
    });
    form.get('columns').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef),
      startWith(this.getColumns(reportComponentConfig.dataSources)),
      pairwise()
    ).subscribe(([prev, current]) => {
      const tableSortOrder = form.get("tableSortOrder").value;
      if (tableSortOrder && tableSortOrder.column) {
        const oldColumn = prev.find(c => c.label === tableSortOrder.column);
        if (oldColumn) {
          const newColumn = current.find(c => c.name === oldColumn.name);
          if (newColumn && newColumn.label !== tableSortOrder.column) {
            this.columnNameChanged = [oldColumn.label, newColumn.label];
          }
        }
      }
    });
    this.updateValidators(form);
    return form;
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
      columns.forEach(key => {
        if (key?.settings) {
          key.settings.type = ReportDataKeySettingsType.COLUMN;
        }
      });
      datasources[0].dataKeys = columns;
    }
  }

  private updateValidators(form: FormGroup) {
    const showTableHeading: boolean = form.get('showTableHeading').value;
    if (showTableHeading) {
      form.get('tableHeading').enable({emitEvent: false});
    } else {
      form.get('tableHeading').disable({emitEvent: false});
    }
  }
}
