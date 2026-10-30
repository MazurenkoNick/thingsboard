// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  BooleanOperation,
  EntityData,
  EntityKey,
  EntityKeyType,
  EntityKeyValueType,
  FilterPredicateType,
  KeyFilter,
  NumericOperation,
  StringOperation
} from '@shared/models/query/query.models';
import { IntervalType, TimeSeriesCmd } from '@shared/models/telemetry/telemetry.models';
import { AggregationType, DAY, getDefaultTimezone, HOUR, MINUTE } from '@shared/models/time/time.models';
import { NotificationInfo } from '@shared/models/notification.models';
import { isDefinedAndNotNull } from '@core/utils';
import { Subject } from 'rxjs';
import type { EdgeMonitoringQuery } from '@home/pages/edge/monitoring/edge-monitoring-datasource';

export const EDGE_ACTIVE_KEY = 'active';
export const EDGE_LAST_CONNECT_TIME_KEY = 'lastConnectTime';
export const EDGE_LAST_DISCONNECT_TIME_KEY = 'lastDisconnectTime';
export const EDGE_VERSION_KEY = 'edgeVersion';
export const EDGE_SYNC_IN_PROGRESS_KEY = 'syncInProgress';

export const EDGE_MIN_UPGRADABLE_VERSION = '3.6.0';

export const AGENTS_URL = '/edgeManagement/agents/all';

// Row properties holding the monitoring data. Prefixed so they never clash with group column properties.
export const EDGE_MONITORING_PROP = '$edgeMonitoring';
export const EDGE_MANAGED_BY_PROP = '$edgeManagedBy';

export enum EdgeStatsDirection {
  UPLINK = 'uplink',
  DOWNLINK = 'downlink'
}

export enum EdgeStatsMetric {
  ADDED = 'Added',
  PUSHED = 'Pushed',
  TMP_FAILED = 'TmpFailed',
  PERMANENTLY_FAILED = 'PermanentlyFailed',
  LAG = 'Lag'
}

export const edgeStatsKey = (direction: EdgeStatsDirection, metric: EdgeStatsMetric): string =>
  `${direction}Msgs${metric}`;

export const EDGE_UPLINK_LAG_KEY = edgeStatsKey(EdgeStatsDirection.UPLINK, EdgeStatsMetric.LAG);
export const EDGE_DOWNLINK_LAG_KEY = edgeStatsKey(EdgeStatsDirection.DOWNLINK, EdgeStatsMetric.LAG);

// Edges report state (active, connect/disconnect time) as server attributes, or as telemetry when
// EDGES_PERSIST_STATE_TO_TELEMETRY is enabled, so both are requested and the newest value wins.
const edgeStateKeys = [EDGE_ACTIVE_KEY, EDGE_LAST_CONNECT_TIME_KEY, EDGE_LAST_DISCONNECT_TIME_KEY];

const edgeTrafficMetrics = [EdgeStatsMetric.PUSHED, EdgeStatsMetric.TMP_FAILED, EdgeStatsMetric.PERMANENTLY_FAILED];

const edgeTrafficKeys = (): string[] =>
  [EdgeStatsDirection.UPLINK, EdgeStatsDirection.DOWNLINK].flatMap(direction =>
    edgeTrafficMetrics.map(metric => edgeStatsKey(direction, metric)));

export const EDGE_TRAFFIC_WINDOW = DAY;
export const EDGE_TRAFFIC_INTERVAL = HOUR;
const EDGE_TRAFFIC_BUCKETS = EDGE_TRAFFIC_WINDOW / EDGE_TRAFFIC_INTERVAL;

export interface EdgeTrafficStats {
  series: number[];
  pushed: number;
  failed: number;
}

export interface EdgeMonitoringState {
  active?: boolean;
  activeFromTelemetry?: boolean;
  lastConnectTime?: number;
  lastDisconnectTime?: number;
  version?: string;
  syncInProgress?: boolean;
  uplinkLag?: number;
  downlinkLag?: number;
  uplink: EdgeTrafficStats;
  downlink: EdgeTrafficStats;
  latest: {[entityKeyType: string]: {[key: string]: {ts: number; value: string}}};
  traffic: {[key: string]: {[ts: number]: number}};
}

export interface EdgeManagedBy {
  agentId: string;
  agentName: string;
  appId: string;
  appName: string;
}

export interface EdgeMonitoringRowData {
  [EDGE_MONITORING_PROP]?: EdgeMonitoringState;
  [EDGE_MANAGED_BY_PROP]?: EdgeManagedBy | null;
}

export const edgeMonitoringLatestKeys = (): EntityKey[] => [
  ...edgeStateKeys.flatMap(key => [
    {type: EntityKeyType.SERVER_ATTRIBUTE, key},
    {type: EntityKeyType.TIME_SERIES, key}
  ]),
  {type: EntityKeyType.SERVER_ATTRIBUTE, key: EDGE_VERSION_KEY},
  {type: EntityKeyType.SERVER_ATTRIBUTE, key: EDGE_SYNC_IN_PROGRESS_KEY},
  {type: EntityKeyType.TIME_SERIES, key: EDGE_UPLINK_LAG_KEY},
  {type: EntityKeyType.TIME_SERIES, key: EDGE_DOWNLINK_LAG_KEY}
];

export const edgeMonitoringTsCmd = (): TimeSeriesCmd => ({
  keys: edgeTrafficKeys(),
  startTs: Date.now() - EDGE_TRAFFIC_WINDOW,
  timeWindow: EDGE_TRAFFIC_WINDOW,
  interval: EDGE_TRAFFIC_INTERVAL,
  intervalType: IntervalType.MILLISECONDS,
  timeZoneId: getDefaultTimezone(),
  limit: EDGE_TRAFFIC_BUCKETS * 2,
  agg: AggregationType.SUM
});

const emptyTraffic = (): EdgeTrafficStats => ({series: new Array(EDGE_TRAFFIC_BUCKETS).fill(0), pushed: 0, failed: 0});

const toNumber = (value: string): number => {
  if (!isDefinedAndNotNull(value) || value === '') {
    return undefined;
  }
  const num = Number(value);
  return isNaN(num) ? undefined : num;
};

const latestValue = (state: EdgeMonitoringState, key: string, ...types: EntityKeyType[]): {ts: number; value: string; type: EntityKeyType} => {
  let result: {ts: number; value: string; type: EntityKeyType};
  for (const type of types) {
    const tsValue = state.latest[type]?.[key];
    if (tsValue && isDefinedAndNotNull(tsValue.value) && tsValue.value !== '' && (!result || tsValue.ts > result.ts)) {
      result = {...tsValue, type};
    }
  }
  return result;
};

const trafficSeries = (state: EdgeMonitoringState, key: string, now: number): number[] => {
  const series = new Array(EDGE_TRAFFIC_BUCKETS).fill(0);
  const windowStart = now - EDGE_TRAFFIC_WINDOW;
  const values = state.traffic[key];
  if (values) {
    for (const tsStr of Object.keys(values)) {
      const bucket = Math.floor((Number(tsStr) - windowStart) / EDGE_TRAFFIC_INTERVAL);
      if (bucket >= 0 && bucket < EDGE_TRAFFIC_BUCKETS) {
        series[bucket] += values[tsStr];
      }
    }
  }
  return series;
};

const sum = (values: number[]): number => values.reduce((acc, v) => acc + v, 0);

const directionTraffic = (state: EdgeMonitoringState, direction: EdgeStatsDirection, now: number): EdgeTrafficStats => {
  const series = trafficSeries(state, edgeStatsKey(direction, EdgeStatsMetric.PUSHED), now);
  const failed = sum(trafficSeries(state, edgeStatsKey(direction, EdgeStatsMetric.TMP_FAILED), now)) +
    sum(trafficSeries(state, edgeStatsKey(direction, EdgeStatsMetric.PERMANENTLY_FAILED), now));
  return {series, pushed: sum(series), failed};
};

/**
 * Merges an entity data message (initial page or update) into the row's monitoring state.
 * Always returns a new object, so cell components bound to it see the change.
 */
export const mergeEdgeMonitoringData = (prev: EdgeMonitoringState | undefined, entityData: EntityData): EdgeMonitoringState => {
  const state: EdgeMonitoringState = {
    uplink: emptyTraffic(),
    downlink: emptyTraffic(),
    latest: {...(prev?.latest ?? {})},
    traffic: {...(prev?.traffic ?? {})}
  };
  if (entityData.latest) {
    for (const type of Object.keys(entityData.latest)) {
      state.latest[type] = {...(state.latest[type] ?? {}), ...entityData.latest[type]};
    }
  }
  if (entityData.timeseries) {
    for (const key of Object.keys(entityData.timeseries)) {
      const values = {...(state.traffic[key] ?? {})};
      for (const tsValue of entityData.timeseries[key]) {
        const num = toNumber(tsValue.value);
        if (isDefinedAndNotNull(num)) {
          values[tsValue.ts] = num;
        }
      }
      state.traffic[key] = values;
    }
  }
  const stateTypes = [EntityKeyType.SERVER_ATTRIBUTE, EntityKeyType.TIME_SERIES];
  const active = latestValue(state, EDGE_ACTIVE_KEY, ...stateTypes);
  if (active) {
    state.active = active.value === 'true';
    state.activeFromTelemetry = active.type === EntityKeyType.TIME_SERIES;
  }
  state.lastConnectTime = toNumber(latestValue(state, EDGE_LAST_CONNECT_TIME_KEY, ...stateTypes)?.value);
  state.lastDisconnectTime = toNumber(latestValue(state, EDGE_LAST_DISCONNECT_TIME_KEY, ...stateTypes)?.value);
  state.version = latestValue(state, EDGE_VERSION_KEY, EntityKeyType.SERVER_ATTRIBUTE)?.value;
  state.syncInProgress = latestValue(state, EDGE_SYNC_IN_PROGRESS_KEY, EntityKeyType.SERVER_ATTRIBUTE)?.value === 'true';
  state.uplinkLag = toNumber(latestValue(state, EDGE_UPLINK_LAG_KEY, EntityKeyType.TIME_SERIES)?.value);
  state.downlinkLag = toNumber(latestValue(state, EDGE_DOWNLINK_LAG_KEY, EntityKeyType.TIME_SERIES)?.value);
  const now = Date.now();
  state.uplink = directionTraffic(state, EdgeStatsDirection.UPLINK, now);
  state.downlink = directionTraffic(state, EdgeStatsDirection.DOWNLINK, now);
  return state;
};

export const formatCount = (value: number): string => {
  if (value >= 1000000) {
    return (value / 1000000).toFixed(1) + 'M';
  }
  if (value >= 10000) {
    return Math.round(value / 1000) + 'k';
  }
  return value.toLocaleString();
};

export const edgeHasLag = (state: EdgeMonitoringState): boolean =>
  !!state && ((state.uplinkLag ?? 0) > 0 || (state.downlinkLag ?? 0) > 0);

// Stored as an enum name like V_4_4_0_1; anything else (e.g. UNRECOGNIZED) has no displayable version.
export const formatEdgeVersion = (version: string): string => {
  if (!version || !/^V_\d+(_\d+)*$/.test(version)) {
    return null;
  }
  return version.substring(2).replace(/_/g, '.');
};

export const toEdgeVersionKey = (version: string): string => 'V_' + version.replace(/\./g, '_');

export const compareEdgeVersions = (a: string, b: string): number => {
  const pa = a.split('.').map(Number);
  const pb = b.split('.').map(Number);
  for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
    const diff = (pa[i] || 0) - (pb[i] || 0);
    if (diff !== 0) {
      return diff;
    }
  }
  return 0;
};

export const isEdgeUpgradeAvailable = (version: string, latestVersion: string): boolean => {
  const current = formatEdgeVersion(version);
  if (!current || !latestVersion) {
    return false;
  }
  return compareEdgeVersions(current, EDGE_MIN_UPGRADABLE_VERSION) >= 0 && compareEdgeVersions(current, latestVersion) < 0;
};

export enum EdgeMonitoringCard {
  ALL = 'ALL',
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  UPLINK_LAGGING = 'UPLINK_LAGGING',
  DOWNLINK_LAGGING = 'DOWNLINK_LAGGING',
  UPGRADE_AVAILABLE = 'UPGRADE_AVAILABLE'
}

export interface EdgeMonitoringCardInfo {
  card: EdgeMonitoringCard;
  label: string;
  hint: string;
  color: string;
}

export const edgeMonitoringCards: EdgeMonitoringCardInfo[] = [
  {card: EdgeMonitoringCard.ALL, label: 'edge.monitoring.card-all', hint: 'edge.monitoring.card-all-hint', color: '#8a94a3'},
  {card: EdgeMonitoringCard.ACTIVE, label: 'edge.monitoring.card-active', hint: 'edge.monitoring.card-active-hint', color: '#2e9e4f'},
  {card: EdgeMonitoringCard.INACTIVE, label: 'edge.monitoring.card-inactive', hint: 'edge.monitoring.card-inactive-hint', color: '#d93025'},
  {card: EdgeMonitoringCard.UPLINK_LAGGING, label: 'edge.monitoring.card-uplink-lagging',
    hint: 'edge.monitoring.card-uplink-lagging-hint', color: '#e8891c'},
  {card: EdgeMonitoringCard.DOWNLINK_LAGGING, label: 'edge.monitoring.card-downlink-lagging',
    hint: 'edge.monitoring.card-downlink-lagging-hint', color: '#b25e00'},
  {card: EdgeMonitoringCard.UPGRADE_AVAILABLE, label: 'edge.monitoring.card-upgrade-available',
    hint: 'edge.monitoring.card-upgrade-available-hint', color: '#305680'}
];

const lagKeyFilter = (key: string): KeyFilter => ({
  key: {type: EntityKeyType.TIME_SERIES, key},
  valueType: EntityKeyValueType.NUMERIC,
  predicate: {type: FilterPredicateType.NUMERIC, operation: NumericOperation.GREATER, value: {defaultValue: 0}}
});

const versionKeyFilter = (operation: StringOperation, value: string): KeyFilter => ({
  key: {type: EntityKeyType.SERVER_ATTRIBUTE, key: EDGE_VERSION_KEY},
  valueType: EntityKeyValueType.STRING,
  predicate: {type: FilterPredicateType.STRING, operation, value: {defaultValue: value}, ignoreCase: false}
});

/**
 * Shared state of one monitoring table: the selected card, the latest edge version and where
 * the edge state lives. `changed$` fires when the cards or table must re-query.
 */
export class EdgeMonitoringContext {

  selectedCard = EdgeMonitoringCard.ALL;
  latestVersion: string = null;
  activeKeyType = EntityKeyType.SERVER_ATTRIBUTE;
  changed$ = new Subject<void>();
  baseQuery: () => EdgeMonitoringQuery = null;
  reload: () => void = () => {};

  constructor(public readonly isTenantAdmin: boolean,
              public readonly agentsEnabled: boolean) {
  }

  get upgradeEnabled(): boolean {
    return this.isTenantAdmin && !!this.latestVersion;
  }

  cardKeyFilters(card: EdgeMonitoringCard): KeyFilter[] {
    switch (card) {
      case EdgeMonitoringCard.ACTIVE:
      case EdgeMonitoringCard.INACTIVE:
        return [{
          key: {type: this.activeKeyType, key: EDGE_ACTIVE_KEY},
          valueType: EntityKeyValueType.BOOLEAN,
          predicate: {type: FilterPredicateType.BOOLEAN, operation: BooleanOperation.EQUAL,
            value: {defaultValue: card === EdgeMonitoringCard.ACTIVE}}
        }];
      case EdgeMonitoringCard.UPLINK_LAGGING:
        return [lagKeyFilter(EDGE_UPLINK_LAG_KEY)];
      case EdgeMonitoringCard.DOWNLINK_LAGGING:
        return [lagKeyFilter(EDGE_DOWNLINK_LAG_KEY)];
      case EdgeMonitoringCard.UPGRADE_AVAILABLE:
        return this.latestVersion ? [
          versionKeyFilter(StringOperation.STARTS_WITH, 'V_'),
          versionKeyFilter(StringOperation.NOT_EQUAL, toEdgeVersionKey(this.latestVersion))
        ] : [];
      default:
        return [];
    }
  }

  selectedCardKeyFilters(): KeyFilter[] {
    return this.cardKeyFilters(this.selectedCard);
  }

  /** Switches the Active/Inactive filters to telemetry once a row shows the state is stored there. */
  detectStateStorage(states: EdgeMonitoringState[]) {
    const withActive = states.find(s => isDefinedAndNotNull(s?.active));
    if (withActive) {
      const keyType = withActive.activeFromTelemetry ? EntityKeyType.TIME_SERIES : EntityKeyType.SERVER_ATTRIBUTE;
      if (keyType !== this.activeKeyType) {
        this.activeKeyType = keyType;
        this.changed$.next();
        if (this.selectedCard === EdgeMonitoringCard.ACTIVE || this.selectedCard === EdgeMonitoringCard.INACTIVE) {
          this.reload();
        }
      }
    }
  }
}

export interface EdgeStatusRange {
  id: string;
  window: number;
  interval: number;
  label: string;
}

// Downlink stats are reported every 10 min, so a shorter bucket would leave most of them empty.
export const EDGE_DOWNLINK_MIN_INTERVAL = 10 * MINUTE;

export const edgeStatusRanges: EdgeStatusRange[] = [
  {id: '1h', window: HOUR, interval: 5 * MINUTE, label: 'edge.monitoring.range-1h'},
  {id: '6h', window: 6 * HOUR, interval: 15 * MINUTE, label: 'edge.monitoring.range-6h'},
  {id: '24h', window: DAY, interval: HOUR, label: 'edge.monitoring.range-24h'},
  {id: '7d', window: 7 * DAY, interval: 6 * HOUR, label: 'edge.monitoring.range-7d'}
];

export const EDGE_STATUS_DEFAULT_RANGE = '24h';

export const edgeStatsMetrics = [EdgeStatsMetric.ADDED, EdgeStatsMetric.PUSHED, EdgeStatsMetric.TMP_FAILED,
  EdgeStatsMetric.PERMANENTLY_FAILED, EdgeStatsMetric.LAG];

/** Parameters of the EDGE_CONNECTION and EDGE_COMMUNICATION_FAILURE notifications. */
export interface EdgeNotificationInfo extends NotificationInfo {
  eventType?: string;
  edgeName?: string;
  failureMsg?: string;
}
