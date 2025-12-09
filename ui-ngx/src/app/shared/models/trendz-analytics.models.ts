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

export interface TrendzSummary {
    metricSummaryItems: MetricSummaryItem[],
    anomalyModelSummaryItems: AnomalyModelSummaryItem[],
    calculationFieldSummaryItems: CalculationFieldSummaryItem[],
    predictionModelSummaryItems: PredictionModelSummaryItem[],
    viewSummaryItems: ViewSummaryItem[],
    aiSummaryItems: AiSummaryItem[],
}

export interface BaseTrendzSummaryItem {
    entityId: string,
    entityName: string,
    updatedTs: number,
}

export interface MetricSummaryItem extends BaseTrendzSummaryItem {
    itemId: string,
    itemName: string,
    metricData: {
        metricId: string,
        metricName: string,
    },
    fieldData: {
        fieldId: string,
        fieldName: string,
        fieldType: string
    }
}

export interface AnomalyModelSummaryItem extends BaseTrendzSummaryItem {
    modelName: string,
    enabled: boolean,
    modelId: string,
}

export interface CalculationFieldSummaryItem extends BaseTrendzSummaryItem {
    calculationName: string,
    enabled: boolean,
    calculationId: string,
}

export interface PredictionModelSummaryItem extends BaseTrendzSummaryItem {
    modelName: string,
    enabled: boolean,
    modelId: string,
}

export interface ViewSummaryItem extends BaseTrendzSummaryItem {
    viewName: string,
    viewType: string,
    viewConfigId: string,
}

export interface AiSummaryItem extends BaseTrendzSummaryItem {
    chatSummary: string,
    lastMessage: string,
    messageCount: number,
    chatId: string,
}

export interface TrendzSummaryItemParam {
    name: string,
    enabled?: boolean,
    updatedTs?: number,
    entityId?: string,
    itemId?: string
}

export interface TrendzSynchronization {
    version: string,
    message: string;
    type: TrendzSynchronizationResultType,
    status: TrendzSynchronizationStatus
}

export enum TrendzSynchronizationStatus {
    NOT_AVAILABLE = 'NOT_AVAILABLE',
    AVAILABLE = 'AVAILABLE',
    SYNCED = 'SYNCED',
}

export enum TrendzSynchronizationResultType {
    SYNC_NOT_INITIALIZED = 'trendz-analytics.sync.sync-not-initialized',
    SYNC_COMPLETED = 'trendz-analytics.sync.sync-completed',
    SYNC_DISABLED = 'trendz-analytics.sync.sync-disabled',
    TRENDZ_UNSUPPORTED_VERSION = 'trendz-analytics.sync.trendz-unsupported-version',
    TRENDZ_AUTH_INVALID = 'trendz-analytics.sync.trendz-auth-invalid',
    TRENDZ_URL_UNREACHABLE = 'trendz-analytics.sync.trendz-url-unreachable',
    TB_URL_MISMATCH = 'trendz-analytics.sync.tb-url-mismatch',
    TB_URL_UNREACHABLE = 'trendz-analytics.sync.tb-url-unreachable',
    TB_AUTH_INVALID = 'trendz-analytics.sync.tb-auth-invalid',
    SYNC_INTERNAL_ERROR = 'trendz-analytics.sync.sync-internal-error',
}

export enum TrendzViewType {
    BAR = 'trendz-analytics.view-type.bar',
    LINE = 'trendz-analytics.view-type.line',
    TABLE = 'trendz-analytics.view-type.table',
    HEATMAP = 'trendz-analytics.view-type.heatmap',
    HEATMAP_CALENDAR = 'trendz-analytics.view-type.heatmap-calendar',
    PIE = 'trendz-analytics.view-type.pie',
    SCATTER_PLOT = 'trendz-analytics.view-type.scatter-plot',
    CARD = 'trendz-analytics.view-type.card',
    CARD_WITH_LINE = 'trendz-analytics.view-type.card-with-line',
    AI_CARD = 'trendz-analytics.view-type.ai-card',
}

export const getMetricLink = (metric: MetricSummaryItem) => {
    const metricId = metric.metricData?.metricId ?? metric.fieldData?.fieldId;
    return `/trendz/metricExplorer?itemId=${encodeURIComponent(metric.itemId)}&metricId=${encodeURIComponent(metricId)}`;
}