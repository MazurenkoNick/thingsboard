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

import { IAliasController } from '@core/api/widget-api.models';
import { WidgetConfigCallbacks } from '@home/components/widget/config/widget-config.component.models';
import { DataKey, Widget, widgetType } from '@shared/models/widget.models';
import { Observable } from 'rxjs';
import { BackgroundSettings, BackgroundType } from '@shared/models/widget-settings.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { materialColors } from '@shared/models/material.models';

export interface ApiUsageSettingsContext {
  aliasController: IAliasController;
  callbacks: WidgetConfigCallbacks;
  widget: Widget;
  editKey: (key: DataKey, entityAliasId: string, WidgetType?: widgetType) => Observable<DataKey>;
  generateDataKey: (key: DataKey) => DataKey;
}


export interface ApiUsageWidgetSettings {
  dsEntityAliasId: string;
  apiUsageDataKeys: ApiUsageDataKeysSettings[];
  targetDashboardState: string;
  background: BackgroundSettings;
  padding: string;
}

export interface ApiUsageDataKeysSettings {
  label: string;
  state: string;
  status: DataKey;
  maxLimit: DataKey;
  current: DataKey;
}

const generateDataKey = (label: string, status: string, maxLimit: string, current: string) => {
  return {
    label,
    state: '',
    status: {
      name: status,
      label: status,
      type: DataKeyType.timeseries,
      funcBody: undefined,
      settings: {},
      color: materialColors[0].value
    },
    maxLimit: {
      name: maxLimit,
      label: maxLimit,
      type: DataKeyType.timeseries,
      funcBody: undefined,
      settings: {},
      color: materialColors[0].value
    },
    current: {
      name: current,
      label: current,
      type: DataKeyType.timeseries,
      funcBody: undefined,
      settings: {},
      color: materialColors[0].value
    }
  }
}

export const apiUsageDefaultSettings: ApiUsageWidgetSettings = {
  dsEntityAliasId: '',
  apiUsageDataKeys: [
    generateDataKey('{i18n:api-usage.transport-messages}', 'transportApiState', 'transportMsgLimit', 'transportMsgCount'),
    generateDataKey('{i18n:api-usage.transport-data-points}', 'transportApiState', 'transportDataPointsLimit', 'transportDataPointsCount'),
    generateDataKey('{i18n:api-usage.rule-engine-executions}', 'ruleEngineApiState', 'ruleEngineExecutionLimit', 'ruleEngineExecutionCount'),
    generateDataKey('{i18n:api-usage.javascript-function-executions}', 'jsExecutionApiState', 'jsExecutionLimit', 'jsExecutionCount'),
    generateDataKey('{i18n:api-usage.tbel-function-executions}', 'tbelExecutionApiState', 'tbelExecutionLimit', 'tbelExecutionCount'),
    generateDataKey('{i18n:api-usage.data-points-storage-days}', 'dbApiState', 'storageDataPointsLimit', 'storageDataPointsCount'),
    generateDataKey('{i18n:api-usage.alarms-created}', 'alarmApiState', 'createdAlarmsLimit', 'createdAlarmsCount'),
    generateDataKey('{i18n:api-usage.reports-created}', 'reportApiState', 'generatedReportsLimit', 'generatedReportsCount'),
    generateDataKey('{i18n:api-usage.emails}', 'emailApiState', 'emailLimit', 'emailCount'),
    generateDataKey('{i18n:api-usage.sms}', 'notificationApiState', 'smsLimit', 'smsCount'),
  ],
  targetDashboardState: 'default',
  background: {
    type: BackgroundType.color,
    color: '#fff',
    overlay: {
      enabled: false,
      color: 'rgba(255,255,255,0.72)',
      blur: 3
    }
  },
  padding: '0'
};

export const getUniqueDataKeys = (data: ApiUsageDataKeysSettings[]): DataKey[] => {
  const seenNames = new Set<string>();
  return data
    .flatMap(item => [item.status, item.maxLimit, item.current])
    .filter(key => {
      if (seenNames.has(key.name)) {
        return false;
      }
      seenNames.add(key.name);
      return true;
    });
};
