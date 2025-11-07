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


import { CustomTimeSchedulerItem } from "@shared/models/device.models";
import { DashboardId } from "@shared/models/id/dashboard-id";
import { TimeUnit } from "@shared/models/time/time.models";
import {
  BooleanOperation,
  ComplexOperation,
  EntityKeyValueType,
  FilterPredicateType,
  NumericOperation,
  StringOperation
} from "@shared/models/query/query.models";

export enum AlarmRuleScheduleType {
  ANY_TIME = 'ANY_TIME',
  SPECIFIC_TIME = 'SPECIFIC_TIME',
  CUSTOM = 'CUSTOM'
}

export const AlarmRuleScheduleTypeTranslationMap = new Map<AlarmRuleScheduleType, string>(
  [
    [AlarmRuleScheduleType.ANY_TIME, 'alarm-rule.schedule.any-time'],
    [AlarmRuleScheduleType.SPECIFIC_TIME, 'alarm-rule.schedule.specific-time'],
    [AlarmRuleScheduleType.CUSTOM, 'alarm-rule.schedule.custom']
  ]
);

export enum AlarmRuleConditionType {
  SIMPLE = 'SIMPLE',
  DURATION = 'DURATION',
  REPEATING = 'REPEATING'
}

export const AlarmRuleConditionTypeTranslationMap = new Map<AlarmRuleConditionType, string>(
  [
    [AlarmRuleConditionType.SIMPLE, 'alarm-rule.conditions.simple'],
    [AlarmRuleConditionType.DURATION, 'alarm-rule.conditions.duration'],
    [AlarmRuleConditionType.REPEATING, 'alarm-rule.conditions.repeating']
  ]
);

export enum AlarmRuleExpressionType {
  SIMPLE = 'SIMPLE',
  TBEL = 'TBEL',
}

export const FilterPredicateTypeTranslationMap = new Map<FilterPredicateType, string>(
  [
    [FilterPredicateType.STRING, 'alarm-rule.filter-predicate-type.string'],
    [FilterPredicateType.NUMERIC, 'alarm-rule.filter-predicate-type.numeric'],
    [FilterPredicateType.BOOLEAN, 'alarm-rule.filter-predicate-type.boolean'],
    [FilterPredicateType.COMPLEX, 'alarm-rule.filter-predicate-type.complex']
  ]
);

export interface AlarmRule {
  condition: AlarmRuleCondition;
  alarmDetails?: string;
  dashboardId?: DashboardId;
}

export interface AlarmRuleCondition {
  type: AlarmRuleConditionType;
  expression: AlarmRuleExpression;
  schedule?: AlarmRuleSchedule;
  unit?: TimeUnit;
  value?: AlarmRuleValue<number>;
  count?: AlarmRuleValue<number>;
}

export interface AlarmRuleExpression {
  type: AlarmRuleExpressionType;
  expression?: string;
  filters?: Array<AlarmRuleFilter>;
  operation?: ComplexOperation;
}

export interface AlarmRuleSchedule {
  staticValue?: {
    type?: AlarmRuleScheduleType;
    timezone?: string;
    daysOfWeek?: number[];
    startsOn?: number;
    endsOn?: number;
    items?: CustomTimeSchedulerItem[];
  };
  dynamicValueArgument?: string;
}

export interface AlarmRuleFilter {
  argument: string;
  valueType: EntityKeyValueType;
  operation: ComplexOperation;
  predicates: AlarmRuleFilterPredicate[];
}

export interface AlarmRulePredicateInfo {
  keyFilterPredicate: AlarmRuleFilterPredicate;
}

export type AlarmRuleFilterPredicate = StringAlarmRuleFilterPredicate |
  NumericAlarmRuleFilterPredicate |
  BooleanAlarmRuleFilterPredicate |
  ComplexAlarmRuleFilterPredicate;

export interface AlarmRuleValue<T> {
  dynamicValueArgument?: string;
  staticValue?: T
}

export interface StringAlarmRuleFilterPredicate {
  type: FilterPredicateType.STRING;
  operation: StringOperation;
  value: AlarmRuleValue<string>;
  ignoreCase: boolean;
}

export interface NumericAlarmRuleFilterPredicate {
  type: FilterPredicateType.NUMERIC;
  operation: NumericOperation;
  value: AlarmRuleValue<number>;
}

export interface BooleanAlarmRuleFilterPredicate {
  type: FilterPredicateType.BOOLEAN;
  operation: BooleanOperation;
  value: AlarmRuleValue<boolean>;
}

export interface BaseComplexFilterPredicate<T extends AlarmRuleFilterPredicate> {
  type: FilterPredicateType.COMPLEX;
  operation: ComplexOperation;
  predicates: Array<T>;
}

export type ComplexAlarmRuleFilterPredicate = BaseComplexFilterPredicate<AlarmRuleFilterPredicate>;
