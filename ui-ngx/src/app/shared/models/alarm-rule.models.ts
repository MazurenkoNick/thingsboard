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
import { ComplexOperation, EntityKeyValueType, FilterPredicateType } from "@shared/models/query/query.models";
import { EntityType } from "@shared/models/entity-type.models";
import { Observable } from "rxjs";
import { CalculatedField } from "@shared/models/calculated-field.models";

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
  ComplexAlarmRuleFilterPredicate |
  NoDataAlarmRuleFilterPredicate;

export interface AlarmRuleValue<T> {
  dynamicValueArgument?: string;
  staticValue?: T
}

export interface StringAlarmRuleFilterPredicate {
  type: AlarmRuleFilterPredicateType.STRING;
  operation: AlarmRuleStringOperation;
  value: AlarmRuleValue<string>;
  ignoreCase: boolean;
}

export interface NumericAlarmRuleFilterPredicate {
  type: AlarmRuleFilterPredicateType.NUMERIC;
  operation: AlarmRuleNumericOperation;
  value: AlarmRuleValue<number>;
}

export interface BooleanAlarmRuleFilterPredicate {
  type: AlarmRuleFilterPredicateType.BOOLEAN;
  operation: AlarmRuleBooleanOperation;
  value: AlarmRuleValue<boolean>;
}

export interface NoDataAlarmRuleFilterPredicate {
  type: AlarmRuleFilterPredicateType.NO_DATA;
  unit: TimeUnit,
  operation: AlarmRuleStringOperation.NO_DATA | AlarmRuleNumericOperation.NO_DATA | AlarmRuleBooleanOperation.NO_DATA;
  duration: AlarmRuleValue<number>;
}

export interface BaseComplexFilterPredicate<T extends AlarmRuleFilterPredicate> {
  type: AlarmRuleFilterPredicateType.COMPLEX;
  operation: ComplexOperation;
  predicates: Array<T>;
}

export type ComplexAlarmRuleFilterPredicate = BaseComplexFilterPredicate<AlarmRuleFilterPredicate>;

export interface AlarmRuleFilterConfig {
  name?: Array<string>;
  entityType?: EntityType;
  entities?: Array<string>;
}

export enum AlarmRuleFilterPredicateType {
  STRING = 'STRING',
  NUMERIC = 'NUMERIC',
  BOOLEAN = 'BOOLEAN',
  COMPLEX = 'COMPLEX',
  NO_DATA = 'NO_DATA'
}

export enum AlarmRuleStringOperation {
  EQUAL = 'EQUAL',
  NOT_EQUAL = 'NOT_EQUAL',
  NO_DATA = 'NO_DATA',
  STARTS_WITH = 'STARTS_WITH',
  ENDS_WITH = 'ENDS_WITH',
  CONTAINS = 'CONTAINS',
  NOT_CONTAINS = 'NOT_CONTAINS',
  IN = 'IN',
  NOT_IN = 'NOT_IN',
}

export const alarmRuleStringOperationTranslationMap = new Map<AlarmRuleStringOperation, string>(
  [
    [AlarmRuleStringOperation.EQUAL, 'filter.operation.equal'],
    [AlarmRuleStringOperation.NOT_EQUAL, 'filter.operation.not-equal'],
    [AlarmRuleStringOperation.STARTS_WITH, 'filter.operation.starts-with'],
    [AlarmRuleStringOperation.ENDS_WITH, 'filter.operation.ends-with'],
    [AlarmRuleStringOperation.CONTAINS, 'filter.operation.contains'],
    [AlarmRuleStringOperation.NOT_CONTAINS, 'filter.operation.not-contains'],
    [AlarmRuleStringOperation.IN, 'filter.operation.in'],
    [AlarmRuleStringOperation.NOT_IN, 'filter.operation.not-in'],
    [AlarmRuleStringOperation.NO_DATA, 'alarm-rule.missing-for']
  ]
);

export enum AlarmRuleNumericOperation {
  EQUAL = 'EQUAL',
  NOT_EQUAL = 'NOT_EQUAL',
  NO_DATA = 'NO_DATA',
  GREATER = 'GREATER',
  LESS = 'LESS',
  GREATER_OR_EQUAL = 'GREATER_OR_EQUAL',
  LESS_OR_EQUAL = 'LESS_OR_EQUAL'
}

export const alarmRuleNumericOperationTranslationMap = new Map<AlarmRuleNumericOperation, string>(
  [
    [AlarmRuleNumericOperation.EQUAL, 'filter.operation.equal'],
    [AlarmRuleNumericOperation.NOT_EQUAL, 'filter.operation.not-equal'],
    [AlarmRuleNumericOperation.GREATER, 'filter.operation.greater'],
    [AlarmRuleNumericOperation.LESS, 'filter.operation.less'],
    [AlarmRuleNumericOperation.GREATER_OR_EQUAL, 'filter.operation.greater-or-equal'],
    [AlarmRuleNumericOperation.LESS_OR_EQUAL, 'filter.operation.less-or-equal'],
    [AlarmRuleNumericOperation.NO_DATA, 'alarm-rule.missing-for']
  ]
);

export enum AlarmRuleBooleanOperation {
  EQUAL = 'EQUAL',
  NOT_EQUAL = 'NOT_EQUAL',
  NO_DATA = 'NO_DATA',
}

export const alarmRuleBooleanOperationTranslationMap = new Map<AlarmRuleBooleanOperation, string>(
  [
    [AlarmRuleBooleanOperation.EQUAL, 'filter.operation.equal'],
    [AlarmRuleBooleanOperation.NOT_EQUAL, 'filter.operation.not-equal'],
    [AlarmRuleBooleanOperation.NO_DATA, 'alarm-rule.missing-for']
  ]
);

export const alarmRuleDefaultScript =
  '// Sample expression for an alarm rule: triggers when temperature is above 20 degree\n' +
  'return temperature > 20;'

export type AlarmRuleTestScriptFn = (calculatedField: CalculatedField, expression: string, argumentsObj?: Record<string, unknown>, closeAllOnSave?: boolean) => Observable<string>;
