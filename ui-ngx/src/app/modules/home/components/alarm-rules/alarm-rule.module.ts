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
import { AlarmRuleDialogComponent } from "@home/components/alarm-rules/alarm-rule-dialog.component";
import { CreateCfAlarmRulesComponent } from "@home/components/alarm-rules/create-cf-alarm-rules.component";
import { CfAlarmRuleComponent } from "@home/components/alarm-rules/cf-alarm-rule.component";
import { CfAlarmRuleConditionComponent } from "@home/components/alarm-rules/cf-alarm-rule-condition.component";
import {
  CfAlarmRuleConditionDialogComponent
} from "@home/components/alarm-rules/cf-alarm-rule-condition-dialog.component";
import { CfAlarmScheduleComponent } from "@home/components/alarm-rules/cf-alarm-schedule.component";
import { CfAlarmScheduleDialogComponent } from "@home/components/alarm-rules/cf-alarm-schedule-dialog.component";
import {
  EntityDebugSettingsButtonComponent
} from "@home/components/entity/debug/entity-debug-settings-button.component";
import { AlarmRuleFilterTextComponent } from "@home/components/alarm-rules/filter/alarm-rule-filter-text.component";
import {
  CalculatedFieldArgumentsTableModule
} from "@home/components/calculated-fields/components/calculated-field-arguments/calculated-field-arguments-table.module";
import {
  AlarmRuleFilterPredicateListComponent
} from "@home/components/alarm-rules/filter/alarm-rule-filter-predicate-list.component";
import {
  AlarmRuleFilterPredicateComponent
} from "@home/components/alarm-rules/filter/alarm-rule-filter-predicate.component";
import {
  AlarmRuleFilterPredicateValueComponent
} from "@home/components/alarm-rules/filter/alarm-rule-filter-predicate-value.component";
import {
  AlarmRuleComplexFilterPredicateDialogComponent
} from "@home/components/alarm-rules/filter/alarm-rule-complex-filter-predicate-dialog.component";
import { AlarmRuleFilterListComponent } from "@home/components/alarm-rules/filter/alarm-rule-filter-list.component";
import { AlarmRuleFilterDialogComponent } from "@home/components/alarm-rules/filter/alarm-rule-filter-dialog.component";
import { AlarmRuleDetailsDialogComponent } from "@home/components/alarm-rules/alarm-rule-details-dialog.component";
import { AlarmRuleFilterConfigComponent } from "@home/components/alarm-rules/alarm-rule-filter-config.component";
import { AlarmRuleTableHeaderComponent } from "@home/components/alarm-rules/alarm-rule-table-header.component";
import {
  AlarmRuleFilterPredicateNoDataValueComponent
} from "@home/components/alarm-rules/filter/alarm-rule-filter-predicate-no-data-value.component";
import { AlarmRulesComponent } from '@home/components/alarm-rules/alarm-rules.component';

@NgModule({
  declarations: [
    AlarmRuleDialogComponent,
    CreateCfAlarmRulesComponent,
    CfAlarmRuleComponent,
    CfAlarmRuleConditionComponent,
    CfAlarmRuleConditionDialogComponent,
    CfAlarmScheduleComponent,
    CfAlarmScheduleDialogComponent,
    AlarmRuleFilterTextComponent,
    AlarmRuleFilterListComponent,
    AlarmRuleFilterDialogComponent,
    AlarmRuleFilterPredicateListComponent,
    AlarmRuleFilterPredicateComponent,
    AlarmRuleFilterPredicateValueComponent,
    AlarmRuleComplexFilterPredicateDialogComponent,
    AlarmRuleDetailsDialogComponent,
    AlarmRuleFilterConfigComponent,
    AlarmRuleTableHeaderComponent,
    AlarmRuleFilterPredicateNoDataValueComponent,
    AlarmRulesComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    EntityDebugSettingsButtonComponent,
    CalculatedFieldArgumentsTableModule
  ],
  exports: [
    AlarmRuleDialogComponent,
    AlarmRulesComponent
  ]
})
export class AlarmRuleModule { }
