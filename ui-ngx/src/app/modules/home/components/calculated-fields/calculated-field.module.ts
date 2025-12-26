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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import {
  CalculatedFieldDialogComponent
} from '@home/components/calculated-fields/components/dialog/calculated-field-dialog.component';
import {
  CalculatedFieldScriptTestDialogComponent
} from '@home/components/calculated-fields/components/test-dialog/calculated-field-script-test-dialog.component';
import {
  CalculatedFieldTestArgumentsComponent
} from '@home/components/calculated-fields/components/test-arguments/calculated-field-test-arguments.component';
import {
  EntityDebugSettingsButtonComponent
} from '@home/components/entity/debug/entity-debug-settings-button.component';
import {
  GeofencingConfigurationModule
} from '@home/components/calculated-fields/components/geofencing-configuration/geofencing-configuration.module';
import {
  SimpleConfigurationModule
} from '@home/components/calculated-fields/components/simple-configuration/simple-configuration.module';
import {
  PropagationConfigurationModule
} from '@home/components/calculated-fields/components/propagation-configuration/propagation-configuration.module';
import {
  RelatedEntitiesAggregationComponentModule
} from '@home/components/calculated-fields/components/related-entities-aggregation-configuration/related-entities-aggregation-component.module';
import {
  EntityAggregationComponentModule
} from '@home/components/calculated-fields/components/entity-aggregation-configuration/entity-aggregation-component.module';
import {
  CalculatedFieldsHeaderComponent
} from '@home/components/calculated-fields/table-header/calculated-fields-header.component';
import {
  CalculatedFieldsFilterConfigComponent
} from '@home/components/calculated-fields/table-header/calculated-fields-filter-config.component';
import { CalculatedFieldComponent } from '@home/components/calculated-fields/calculated-field.component';
import {
  CalculatedFieldReprocessingPanelComponent
} from '@home/components/calculated-fields/components/reprocessing/calculated-field-reprocessing-panel.component';

@NgModule({
  declarations: [
    CalculatedFieldDialogComponent,
    CalculatedFieldScriptTestDialogComponent,
    CalculatedFieldTestArgumentsComponent,
    CalculatedFieldsHeaderComponent,
    CalculatedFieldsFilterConfigComponent,
    CalculatedFieldComponent,
    CalculatedFieldReprocessingPanelComponent,
  ],
  imports: [
    CommonModule,
    SharedModule,
    GeofencingConfigurationModule,
    EntityDebugSettingsButtonComponent,
    SimpleConfigurationModule,
    PropagationConfigurationModule,
    RelatedEntitiesAggregationComponentModule,
    EntityAggregationComponentModule,
  ],
  exports: [
    CalculatedFieldDialogComponent,
    CalculatedFieldScriptTestDialogComponent,
    CalculatedFieldComponent,
  ]
})
export class CalculatedFieldsModule {}
