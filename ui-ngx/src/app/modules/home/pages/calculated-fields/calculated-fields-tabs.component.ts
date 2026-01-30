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

import { Component } from '@angular/core';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';
import { CalculatedFieldEventBody, DebugEventType, EventType } from '@shared/models/event.models';
import type {
  CalculatedFieldsTableConfig,
  CalculatedFieldsTableEntity
} from '@home/components/calculated-fields/calculated-fields-table-config';
import { debugCfActionEnabled } from '@shared/models/calculated-field.models';

@Component({
    selector: 'tb-calculated-fields-tabs',
    templateUrl: './calculated-fields-tabs.component.html',
    styleUrls: [],
    standalone: false
})
export class CalculatedFieldsTabsComponent extends EntityTabsComponent<CalculatedFieldsTableEntity> {

  readonly DebugEventType = DebugEventType;
  readonly EventType = EventType;

  constructor() {
    super();
  }

  get debugActionDisabled(): boolean {
    return !debugCfActionEnabled(this.entity);
  };

  onDebugEventSelected(event: CalculatedFieldEventBody) {
    (this.entitiesTableConfig as CalculatedFieldsTableConfig).getTestScriptDialog(this.entity, JSON.parse(event.arguments), false)
      .subscribe((expression) => {
        (this.entitiesTableConfig as CalculatedFieldsTableConfig).getTable();
        const entityDetailsPanel = this.entitiesTableConfig.getTable().entityDetailsPanel;
        entityDetailsPanel.onToggleEditMode(true);
        entityDetailsPanel.selectedTab = 0;
        setTimeout(() => {
          entityDetailsPanel.detailsForm.get('configuration').setValue({...this.entity.configuration, expression});
          entityDetailsPanel.detailsForm.get('configuration').markAsDirty();
        });
      });
  };
}
