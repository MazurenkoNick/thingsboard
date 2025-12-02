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

export enum SubscriptionErrorCode {
  LIMIT_REACHED = 'LIMIT_REACHED',
  FEATURE_DISABLED = 'FEATURE_DISABLED',
  UNSUPPORTED_SOLUTION_TEMPLATE_PLAN = 'UNSUPPORTED_SOLUTION_TEMPLATE_PLAN'
}

export enum SubscriptionEntry {
  DEVICE_COUNT = 'DEVICE_COUNT',
  ASSET_COUNT = 'ASSET_COUNT',
  WHITE_LABELING = 'WHITE_LABELING'
}

export interface SubscriptionErrorData {
  subscriptionErrorCode: SubscriptionErrorCode;
  subscriptionEntry: SubscriptionEntry;
  subscriptionValue: any;
  message?: string;
}

export const subscriptionErrorsMap = new Map<SubscriptionErrorCode, Map<SubscriptionEntry, string>>(
  [
    [SubscriptionErrorCode.LIMIT_REACHED, new Map<SubscriptionEntry, string>(
      [
        [SubscriptionEntry.DEVICE_COUNT, 'subscription-error.limit-reached.device-count'],
        [SubscriptionEntry.ASSET_COUNT, 'subscription-error.limit-reached.asset-count']
      ]
    )],
    [SubscriptionErrorCode.FEATURE_DISABLED, new Map<SubscriptionEntry, string>(
      [
        [SubscriptionEntry.WHITE_LABELING, 'subscription-error.feature-disabled.white-labeling']
      ]
    )]
  ]
);

export enum PlanUiType {
  TbMaker = 'TbMaker',
  TbPrototype = 'TbPrototype',
  TbStartup = 'TbStartup',
  TbBusiness = 'TbBusiness',
  TbBusinessPlus = 'TbBusinessPlus',
  TbPerpetual = 'TbPerpetual'
}

export interface SubscriptionInfo {
  subscriptionId: string;
  subscriptionPlanName: string;
  planUiType: PlanUiType;
  perpetual: boolean;
  offline: boolean;
  currentPeriodStartTs: number;
  currentPeriodEndTs: number;
  endTs: number;
  upcomingInvoiceDate: number;
  upcomingInvoiceAmountDue: number;
  planExtraDeviceEnabled: boolean;
  planEdgeEnabled: boolean;
  planTrendzEnabled: boolean;

  dataTs: number;
  licenseServerEndpoint: string;

  maxDevices: number;
  maxAssets: number;
  whiteLabelingEnabled: boolean;
  edgeEnabled: boolean;
  trendzEnabled: boolean;
  development: boolean;

  devicesCount: number;
  assetsCount: number;
}
