export enum SubscriptionErrorCode {
  LIMIT_REACHED = 'LIMIT_REACHED',
  FEATURE_DISABLED = 'FEATURE_DISABLED'
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
