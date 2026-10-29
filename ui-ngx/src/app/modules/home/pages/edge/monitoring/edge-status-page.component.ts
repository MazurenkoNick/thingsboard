// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectorRef, Component, NgZone, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { catchError, forkJoin, interval, Observable, of, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { EdgeService } from '@core/http/edge.service';
import { AgentService } from '@core/http/agent.service';
import { AttributeService } from '@core/http/attribute.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { EdgeInfo } from '@shared/models/edge.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EdgeId } from '@shared/models/id/edge-id';
import { AliasFilterType } from '@shared/models/alias.models';
import { EntityData, singleEntityDataPageLink } from '@shared/models/query/query.models';
import { EntityDataCmd, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import { AggregationType, MINUTE } from '@shared/models/time/time.models';
import { Direction } from '@shared/models/page/sort-order';
import { TimePageLink } from '@shared/models/page/page-link';
import { NotificationRequestInfo, NotificationType } from '@shared/models/notification.models';
import { MillisecondsToTimeStringPipe } from '@shared/pipe/milliseconds-to-time-string.pipe';
import {
  EdgeInstructionsDialogComponent,
  EdgeInstructionsDialogData
} from '@home/pages/edge/edge-instructions-dialog.component';
import { EdgeMonitoringTableService } from '@home/pages/edge/monitoring/edge-monitoring-table.service';
import { EdgeTrafficChartData } from '@home/pages/edge/monitoring/edge-traffic-chart.component';
import {
  AGENTS_URL,
  EDGE_DOWNLINK_MIN_INTERVAL,
  EDGE_STATUS_DEFAULT_RANGE,
  EdgeManagedBy,
  EdgeMonitoringContext,
  edgeMonitoringLatestKeys,
  EdgeMonitoringState,
  EdgeNotificationInfo,
  EdgeStatsDirection,
  EdgeStatsMetric,
  edgeStatsKey,
  edgeStatsMetrics,
  EdgeStatusRange,
  edgeStatusRanges,
  formatCount,
  formatEdgeVersion,
  isEdgeUpgradeAvailable,
  mergeEdgeMonitoringData
} from '@home/pages/edge/monitoring/edge-monitoring.models';

const REFRESH_INTERVAL = MINUTE;
const NOTIFICATIONS_PAGE_SIZE = 20;
// Lag is "growing" when it is higher than it was this long ago.
const LAG_GROWTH_WINDOW = 30 * MINUTE;

interface DirectionView {
  chart: EdgeTrafficChartData;
  pushed: number;
  tmpFailed: number;
  permanentlyFailed: number;
  // Minutes the lag has been rising for, or 0.
  lagGrowingFor: number;
}

interface NotificationRow {
  time: number;
  kind: 'connected' | 'disconnected' | 'failure';
  message: string;
}

@Component({
  selector: 'tb-edge-status-page',
  templateUrl: './edge-status-page.component.html',
  styleUrls: ['./edge-status-page.component.scss'],
  standalone: false
})
export class EdgeStatusPageComponent implements OnInit, OnDestroy {

  readonly ranges = edgeStatusRanges;
  readonly agentsUrl = AGENTS_URL;

  edgeId: string;
  edge: EdgeInfo;
  ctx: EdgeMonitoringContext;
  state: EdgeMonitoringState;
  // undefined while loading, null when the edge isn't managed by an agent.
  managedBy: EdgeManagedBy | null;

  range = edgeStatusRanges.find(r => r.id === EDGE_STATUS_DEFAULT_RANGE);
  uplink: DirectionView;
  downlink: DirectionView;
  notifications: NotificationRow[];
  updatedTime: number;

  private subscriber: TelemetrySubscriber;
  private refreshSubscription: Subscription;
  private statsSubscription: Subscription;

  constructor(private route: ActivatedRoute,
              private router: Router,
              private store: Store<AppState>,
              private dialog: MatDialog,
              private translate: TranslateService,
              private edgeService: EdgeService,
              private agentService: AgentService,
              private attributeService: AttributeService,
              private telemetryWsService: TelemetryWebsocketService,
              private edgeMonitoring: EdgeMonitoringTableService,
              private millisecondsToTimeString: MillisecondsToTimeStringPipe,
              private zone: NgZone,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit() {
    this.edgeId = this.route.snapshot.params.edgeId;
    forkJoin([this.edgeService.getEdgeInfo(this.edgeId), this.edgeMonitoring.createContext()]).subscribe(([edge, ctx]) => {
      this.edge = edge;
      this.ctx = ctx;
      this.subscribeState();
      this.loadManagedBy();
      this.loadStats();
      this.refreshSubscription = interval(REFRESH_INTERVAL).subscribe(() => this.loadStats());
    });
  }

  ngOnDestroy() {
    this.subscriber?.unsubscribe();
    this.refreshSubscription?.unsubscribe();
    this.statsSubscription?.unsubscribe();
  }

  get version(): string {
    return formatEdgeVersion(this.state?.version);
  }

  get upgradeAvailable(): boolean {
    return this.ctx?.upgradeEnabled && isEdgeUpgradeAvailable(this.state?.version, this.ctx.latestVersion);
  }

  get statusText(): string {
    if (this.state?.active === undefined) {
      return null;
    }
    const now = Date.now();
    if (this.state.active) {
      return this.state.lastConnectTime ?
        this.translate.instant('edge.monitoring.active-connected-for', {time: this.duration(now - this.state.lastConnectTime)}) :
        this.translate.instant('edge.monitoring.active');
    }
    return this.state.lastDisconnectTime ?
      this.translate.instant('edge.monitoring.inactive-last-seen', {time: this.duration(now - this.state.lastDisconnectTime)}) :
      this.translate.instant('edge.monitoring.inactive');
  }

  get offlineFor(): string {
    const {lastConnectTime, lastDisconnectTime} = this.state ?? {};
    if (!lastConnectTime || !lastDisconnectTime || lastConnectTime < lastDisconnectTime) {
      return null;
    }
    return this.duration(lastConnectTime - lastDisconnectTime);
  }

  lag(direction: string): number {
    return direction === 'uplink' ? this.state?.uplinkLag : this.state?.downlinkLag;
  }

  format(value: number): string {
    return value === undefined || value === null ? '—' : formatCount(value);
  }

  selectRange(range: EdgeStatusRange) {
    if (this.range !== range) {
      this.range = range;
      this.loadStats();
    }
  }

  syncEdge() {
    this.edgeService.syncEdge(this.edgeId).subscribe(() => {
      this.store.dispatch(new ActionNotificationShow({
        message: this.translate.instant('edge.sync-process-started-successfully'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
    });
  }

  openEdgeDetails() {
    this.router.navigate(['..'], {relativeTo: this.route});
  }

  upgrade() {
    if (this.managedBy) {
      this.edgeMonitoring.openAgentAppUpgrade(this.managedBy.appId);
    } else {
      this.openInstructions(true);
    }
  }

  openInstructions(upgradeAvailable: boolean) {
    this.dialog.open<EdgeInstructionsDialogComponent, EdgeInstructionsDialogData>(EdgeInstructionsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {edge: this.edge, afterAdd: false, upgradeAvailable}
    });
  }

  private duration(millis: number): string {
    return this.millisecondsToTimeString.transform(Math.max(millis, 0), true, true).trim();
  }

  private subscribeState() {
    const latestValues = edgeMonitoringLatestKeys();
    const cmd = new EntityDataCmd();
    cmd.query = {
      entityFilter: {type: AliasFilterType.singleEntity, singleEntity: new EdgeId(this.edgeId)},
      pageLink: singleEntityDataPageLink,
      latestValues
    };
    cmd.latestCmd = {keys: latestValues};
    this.subscriber = new TelemetrySubscriber(this.telemetryWsService, this.zone);
    this.subscriber.subscriptionCommands.push(cmd);
    this.subscriber.entityData$.subscribe(update => {
      const entityData: EntityData = update.data?.data?.[0] ?? update.update?.[0];
      if (entityData) {
        this.state = mergeEdgeMonitoringData(this.state, entityData);
        this.cd.markForCheck();
      }
    });
    this.subscriber.subscribe();
  }

  private loadManagedBy() {
    if (!this.ctx.agentsEnabled) {
      this.managedBy = null;
      return;
    }
    this.agentService.getAgentApplicationInfosByRelatedEntities(EntityType.EDGE, [this.edgeId],
      {ignoreErrors: true, ignoreLoading: true}).pipe(
      catchError(() => of([]))
    ).subscribe(apps => {
      const app = apps[0];
      this.managedBy = app ? {agentId: app.agentId?.id, agentName: app.agentName, appId: app.id?.id, appName: app.name} : null;
      this.cd.markForCheck();
    });
  }

  private loadStats() {
    const end = Date.now();
    const range = this.range;
    this.statsSubscription?.unsubscribe();
    this.statsSubscription = forkJoin([
      this.loadDirection(EdgeStatsDirection.UPLINK, range, end, range.interval),
      this.loadDirection(EdgeStatsDirection.DOWNLINK, range, end, Math.max(range.interval, EDGE_DOWNLINK_MIN_INTERVAL)),
      this.loadNotifications(range, end)
    ]).subscribe(([uplink, downlink, notifications]) => {
      this.uplink = uplink;
      this.downlink = downlink;
      this.notifications = notifications;
      this.updatedTime = end;
      this.cd.markForCheck();
    });
  }

  private loadDirection(direction: EdgeStatsDirection, range: EdgeStatusRange, end: number,
                        bucket: number): Observable<DirectionView> {
    const start = end - range.window;
    const buckets = Math.ceil(range.window / bucket);
    const entityId = new EdgeId(this.edgeId);
    const counterKeys = edgeStatsMetrics.filter(m => m !== EdgeStatsMetric.LAG).map(m => edgeStatsKey(direction, m));
    const lagKey = edgeStatsKey(direction, EdgeStatsMetric.LAG);
    const fetch = (keys: string[], agg: AggregationType) =>
      this.attributeService.getEntityTimeseries(entityId, keys, start, end, buckets + 1, agg, bucket,
        undefined, true, {ignoreLoading: true}).pipe(catchError(() => of({})));
    return forkJoin([fetch(counterKeys, AggregationType.SUM), fetch([lagKey], AggregationType.MAX)]).pipe(
      map(([counters, lag]) => {
        const data = {...counters, ...lag};
        const series: EdgeTrafficChartData['series'] = {};
        for (const metric of edgeStatsMetrics) {
          const values: Array<number | null> = new Array(buckets).fill(metric === EdgeStatsMetric.LAG ? null : 0);
          for (const tsValue of data[edgeStatsKey(direction, metric)] ?? []) {
            const index = Math.min(Math.floor((tsValue.ts - start) / bucket), buckets - 1);
            const value = Number(tsValue.value);
            if (index >= 0 && !isNaN(value)) {
              values[index] = value;
            }
          }
          series[metric] = values;
        }
        const total = (metric: EdgeStatsMetric) => series[metric].reduce((acc, v) => acc + (v ?? 0), 0);
        return {
          chart: {start, end, interval: bucket, series},
          pushed: total(EdgeStatsMetric.PUSHED),
          tmpFailed: total(EdgeStatsMetric.TMP_FAILED),
          permanentlyFailed: total(EdgeStatsMetric.PERMANENTLY_FAILED),
          lagGrowingFor: this.lagGrowingFor(series[EdgeStatsMetric.LAG], bucket)
        };
      })
    );
  }

  /** How long, in minutes, the lag has risen bucket over bucket up to the latest report; 0 if it isn't growing. */
  private lagGrowingFor(lag: Array<number | null>, bucket: number): number {
    const reported = lag.map((value, index) => ({value, index})).filter(p => p.value !== null);
    if (reported.length < 2 || reported[reported.length - 1].value <= 0) {
      return 0;
    }
    let i = reported.length - 1;
    while (i > 0 && reported[i - 1].value < reported[i].value) {
      i--;
    }
    const growingFor = (reported[reported.length - 1].index - reported[i].index) * bucket;
    return growingFor >= Math.min(LAG_GROWTH_WINDOW, bucket) ? Math.round(growingFor / MINUTE) : 0;
  }

  private loadNotifications(range: EdgeStatusRange, end: number): Observable<NotificationRow[]> {
    if (!this.ctx.isTenantAdmin) {
      return of(null);
    }
    const pageLink = new TimePageLink(NOTIFICATIONS_PAGE_SIZE, 0, null,
      {property: 'createdTime', direction: Direction.DESC}, end - range.window, end);
    return this.edgeService.getEdgeNotificationRequests(this.edgeId, pageLink, {ignoreErrors: true, ignoreLoading: true}).pipe(
      map(page => page.data.map(request => this.toNotificationRow(request))),
      catchError(() => of([]))
    );
  }

  private toNotificationRow(request: NotificationRequestInfo): NotificationRow {
    const info = (request.info ?? {}) as EdgeNotificationInfo;
    if (info.type === NotificationType.EDGE_COMMUNICATION_FAILURE) {
      return {time: request.createdTime, kind: 'failure', message: info.failureMsg ?? request.templateName};
    }
    const connected = info.eventType?.toLowerCase() === 'connected';
    return {
      time: request.createdTime,
      kind: connected ? 'connected' : 'disconnected',
      message: this.translate.instant(connected ? 'edge.monitoring.edge-connected' : 'edge.monitoring.edge-disconnected',
        {name: info.edgeName ?? this.edge.name})
    };
  }
}
