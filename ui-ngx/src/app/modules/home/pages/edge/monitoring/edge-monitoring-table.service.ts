// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable, NgZone } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { EdgeService } from '@core/http/edge.service';
import { AgentService } from '@core/http/agent.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { Operation, Resource } from '@shared/models/security.models';
import { EntityType } from '@shared/models/entity-type.models';
import { AgentApplicationInfo } from '@shared/models/agent.models';
import {
  AgentAppInstallWizardComponent,
  AgentAppInstallWizardData,
  AgentAppInstallWizardResult
} from '@home/components/agent/wizard/agent-app-install-wizard.component';
import {
  EntityKey,
  EntityKeyType,
  EntityKeyValueType,
  FilterPredicateType,
  KeyFilter,
  StringOperation
} from '@shared/models/query/query.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { Direction } from '@shared/models/page/sort-order';
import { EntityColumn, EntityTableColumn, EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import {
  compareEdgeVersions,
  EDGE_DOWNLINK_LAG_KEY,
  EDGE_MANAGED_BY_PROP,
  EDGE_MIN_UPGRADABLE_VERSION,
  EDGE_MONITORING_PROP,
  EDGE_UPLINK_LAG_KEY,
  EdgeManagedBy,
  AGENTS_URL,
  EdgeMonitoringContext,
  edgeHasLag,
  EdgeMonitoringState,
  EdgeStatsDirection,
  EdgeTrafficStats,
  formatCount,
  formatEdgeVersion,
  isEdgeUpgradeAvailable
} from '@home/pages/edge/monitoring/edge-monitoring.models';
import {
  EdgeMonitoringDataSource,
  EdgeMonitoringQuery,
  EdgeMonitoringRow,
  edgeGroupRowsSpec,
  edgeRowsSpec
} from '@home/pages/edge/monitoring/edge-monitoring-datasource';
import { EdgeMonitoringCardsComponent } from '@home/pages/edge/monitoring/edge-monitoring-cards.component';

export interface EdgeMonitoringActions<T> {
  openStatusPage: ($event: Event, entity: T) => void;
  openUpgradeInstructions: ($event: Event, entity: T) => void;
}

const EDGE_UPLINK_LAG_COLUMN = 'uplinkLag';
const EDGE_DOWNLINK_LAG_COLUMN = 'downlinkLag';

const AGENT_APP_LOOKUP_MAX_IDS = 100;
const EDGE_GROUP_ALL_NAME = 'All';

// Cells are plain HTML; clicks on elements carrying this attribute are routed by handleCellClick().
const ACTION_ATTR = 'data-edge-monitoring-action';
enum CellAction {
  UPGRADE = 'upgrade',
  STATUS_PAGE = 'statusPage',
  NAVIGATE = 'navigate'
}

const edgeMonitoringSortKeys: {[columnKey: string]: EntityKey} = {
  [EDGE_UPLINK_LAG_COLUMN]: {type: EntityKeyType.TIME_SERIES, key: EDGE_UPLINK_LAG_KEY},
  [EDGE_DOWNLINK_LAG_COLUMN]: {type: EntityKeyType.TIME_SERIES, key: EDGE_DOWNLINK_LAG_KEY}
};

const sparkLineColors = {
  [EdgeStatsDirection.UPLINK]: {line: '#305680', fill: 'rgba(48, 86, 128, 0.12)'},
  [EdgeStatsDirection.DOWNLINK]: {line: '#0b6b79', fill: 'rgba(11, 107, 121, 0.12)'}
};

const escapeHtml = (value: string): string =>
  (value ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

const MUTED = 'color: rgba(0,0,0,0.54)';
const LAGGING_ROW_COLOR = '#fff8eb';
const EMPTY_CELL = `<span style="${MUTED}">—</span>`;

@Injectable()
export class EdgeMonitoringTableService {

  constructor(private telemetryWsService: TelemetryWebsocketService,
              private zone: NgZone,
              private store: Store<AppState>,
              private router: Router,
              private dialog: MatDialog,
              private edgeService: EdgeService,
              private agentService: AgentService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private datePipe: DatePipe) {
  }

  createContext(): Observable<EdgeMonitoringContext> {
    const authUser = getCurrentAuthUser(this.store);
    const isTenantAdmin = authUser.authority === Authority.TENANT_ADMIN;
    const agentsEnabled = isTenantAdmin && this.userPermissionsService.hasGenericPermission(Resource.AGENT, Operation.READ);
    const ctx = new EdgeMonitoringContext(isTenantAdmin, agentsEnabled);
    if (!isTenantAdmin) {
      return of(ctx);
    }
    return this.edgeService.getLatestEdgeVersion({ignoreErrors: true, ignoreLoading: true}).pipe(
      catchError(() => of(null)),
      map(latestVersion => {
        ctx.latestVersion = latestVersion?.trim() || null;
        return ctx;
      })
    );
  }

  /** Columns, live datasource, cell clicks and default sort for the all-edges / customer edges table. */
  setupEdgesTable<T extends EdgeMonitoringRow>(config: EntityTableConfig<any, any, T>, ctx: EdgeMonitoringContext,
                                               actions: EdgeMonitoringActions<T>) {
    this.setupContext(config, ctx, () => this.allEdgesQuery(config));
    config.columns = [this.edgeColumn<T>(!ctx.agentsEnabled), ...this.monitoringColumns<T>(ctx)];
    config.defaultSortOrder = {property: EDGE_DOWNLINK_LAG_COLUMN, direction: Direction.DESC};
    config.handleRowClick = ($event, entity) => this.handleCellClick($event, entity, actions);
    config.dataSource = dataLoadedFunction => new EdgeMonitoringDataSource<T>(
      this.telemetryWsService, this.zone, ctx, edgeRowsSpec(edgeMonitoringSortKeys),
      (rows, refresh) => this.loadManagedBy(ctx, rows, refresh),
      () => config.getTable()?.detectChanges(),
      config.entitySelectionEnabled, dataLoadedFunction);
  }

  /**
   * Adds monitoring to a prepared edge group table: appends the monitoring columns after the group's columns
   * and swaps the group datasource for one that reads both. Call after GroupConfigTableConfigService.prepareConfiguration.
   */
  setupGroupTable(config: GroupEntityTableConfig<any>, ctx: EdgeMonitoringContext, actions: EdgeMonitoringActions<any>) {
    this.setupContext(config, ctx, () => ({
      entityFilter: {
        type: AliasFilterType.entityGroup,
        groupType: EntityType.EDGE,
        entityGroup: config.entityGroup.id.id
      },
      keyFilters: []
    }));
    const groupColumnKeys = new Set(config.columns.map(column => column.key));
    const groupColumns = config.entityGroup.configuration.columns.filter(column => groupColumnKeys.has(column.columnKey));
    config.columns = [...config.columns, ...this.monitoringColumns(ctx)];
    if (!config.defaultSortOrder) {
      config.defaultSortOrder = {property: EDGE_DOWNLINK_LAG_COLUMN, direction: Direction.DESC};
    }
    const groupRowClick = config.handleRowClick;
    config.handleRowClick = ($event, entity) => this.handleCellClick($event, entity, actions) || groupRowClick($event, entity);
    config.dataSource = dataLoadedFunction => new EdgeMonitoringDataSource(
      this.telemetryWsService, this.zone, ctx, edgeGroupRowsSpec(groupColumns, edgeMonitoringSortKeys),
      (rows, refresh) => this.loadManagedBy(ctx, rows, refresh),
      () => config.getTable()?.detectChanges(),
      config.entitySelectionEnabled, dataLoadedFunction);
  }

  private setupContext(config: EntityTableConfig<any, any, any>, ctx: EdgeMonitoringContext, baseQuery: () => EdgeMonitoringQuery) {
    config.componentsData = {...(config.componentsData ?? {}), edgeMonitoring: ctx};
    // On the row, not the cells, so the checkbox and the group's own columns are highlighted too.
    config.rowStyleFunction = entity => this.rowStyle(entity);
    config.topPanelComponent = EdgeMonitoringCardsComponent;
    ctx.baseQuery = baseQuery;
    ctx.reload = () => {
      const table = config.getTable();
      if (table) {
        if (table.paginator) {
          table.paginator.pageIndex = 0;
        }
        table.updateData(false);
      }
    };
  }

  private allEdgesQuery(config: EntityTableConfig<any, any, any>): EdgeMonitoringQuery {
    const authUser = getCurrentAuthUser(this.store);
    const edgeType: string = config.componentsData.edgeType;
    const keyFilters: KeyFilter[] = edgeType ? [{
      key: {type: EntityKeyType.ENTITY_FIELD, key: 'type'},
      valueType: EntityKeyValueType.STRING,
      predicate: {type: FilterPredicateType.STRING, operation: StringOperation.EQUAL, value: {defaultValue: edgeType}, ignoreCase: false}
    }] : [];
    // Without "include customers" (and in a customer view) only the owner's own edges are listed: its "All" edge group.
    let ownerId = null;
    if (config.customerId) {
      ownerId = {entityType: EntityType.CUSTOMER, id: config.customerId};
    } else if (!config.componentsData.includeCustomers) {
      ownerId = authUser.authority === Authority.CUSTOMER_USER ?
        {entityType: EntityType.CUSTOMER, id: authUser.customerId} :
        {entityType: EntityType.TENANT, id: authUser.tenantId};
    }
    const entityFilter = ownerId ? {
      type: AliasFilterType.entitiesByGroupName,
      groupType: EntityType.EDGE,
      ownerId,
      entityGroupNameFilter: EDGE_GROUP_ALL_NAME
    } : {
      type: AliasFilterType.entityType,
      entityType: EntityType.EDGE
    };
    return {entityFilter, keyFilters};
  }

  // Fixed when the Managed by column is shown, which then takes the remaining width; otherwise the Edge column takes it.
  private edgeColumn<T extends EdgeMonitoringRow>(flexible: boolean): EntityColumn<T> {
    const minWidth = flexible ? {minWidth: '240px'} : {};
    return new EntityTableColumn<T>('name', 'edge.monitoring.edge', flexible ? '0px' : '240px',
      entity => this.edgeCell(entity),
      () => minWidth, true,
      () => minWidth);
  }

  // Name, then the label when set, then the profile with the creation date.
  private edgeCell(entity: EdgeMonitoringRow): string {
    const label = entity['label'] ?
      `<div style="font-size: 12px; color: #4a5563">${escapeHtml(entity['label'])}</div>` : '';
    const createdTime = entity['createdTime'] ?
      ' · ' + escapeHtml(this.translate.instant('edge.monitoring.created-at',
        {time: this.datePipe.transform(entity['createdTime'], 'yyyy-MM-dd HH:mm')})) : '';
    return `<div style="font-weight: 500">${escapeHtml(entity.name)}</div>${label}` +
      `<div style="font-size: 12px; ${MUTED}">${escapeHtml(entity['type'])}${createdTime}</div>`;
  }

  private monitoringColumns<T extends EdgeMonitoringRow>(ctx: EdgeMonitoringContext): EntityColumn<T>[] {
    const state = (entity: T) => entity[EDGE_MONITORING_PROP];
    // Tighter than the table's default 28px right padding so all columns fit a laptop screen.
    const compact = {paddingRight: '16px'};
    const style = () => compact;
    const headerStyle = () => compact;
    const numberStyle = () => ({...style(), justifyContent: 'flex-end', textAlign: 'right'});
    const numberHeaderStyle = () => ({...compact, justifyContent: 'flex-end'});
    const columns: EntityColumn<T>[] = [
      new EntityTableColumn<T>('edgeStatus', 'edge.monitoring.status', '124px',
        entity => this.statusCell(state(entity)), style, false, headerStyle),
      new EntityTableColumn<T>('edgeVersion', 'edge.monitoring.version', '112px',
        entity => this.versionCell(state(entity), ctx), style, false, headerStyle),
      new EntityTableColumn<T>(EDGE_UPLINK_LAG_COLUMN, 'edge.monitoring.uplink-lag', '104px',
        entity => this.lagCell(state(entity)?.uplinkLag, state(entity)?.active === false), numberStyle, true,
        numberHeaderStyle),
      new EntityTableColumn<T>(EDGE_DOWNLINK_LAG_COLUMN, 'edge.monitoring.downlink-lag', '112px',
        entity => this.lagCell(state(entity)?.downlinkLag, false), numberStyle, true,
        numberHeaderStyle),
      new EntityTableColumn<T>('uplinkTraffic', 'edge.monitoring.uplink-24h', '96px',
        entity => this.trafficCell(state(entity)?.uplink, EdgeStatsDirection.UPLINK), style, false, headerStyle),
      new EntityTableColumn<T>('downlinkTraffic', 'edge.monitoring.downlink-24h', '96px',
        entity => this.trafficCell(state(entity)?.downlink, EdgeStatsDirection.DOWNLINK), style, false, headerStyle)
    ];
    if (ctx.agentsEnabled) {
      // Takes whatever width the fixed columns leave: agent application names are long.
      columns.push(new EntityTableColumn<T>('managedBy', 'edge.monitoring.managed-by', '0px',
        entity => this.managedByCell(entity[EDGE_MANAGED_BY_PROP]), () => ({...style(), minWidth: '200px'}), false,
        () => ({...headerStyle(), minWidth: '200px'})));
    }
    columns.push(new EntityTableColumn<T>('statusPage', 'edge.monitoring.status-page', '120px',
      entity => this.statusPageCell(state(entity)), () => ({justifyContent: 'flex-end'}), false,
      () => ({justifyContent: 'flex-end'})));
    return columns;
  }

  private handleCellClick<T>($event: Event, entity: T, actions: EdgeMonitoringActions<T>): boolean {
    const target = ($event?.target as HTMLElement)?.closest?.(`[${ACTION_ATTR}]`) as HTMLElement;
    if (!target) {
      return false;
    }
    $event.preventDefault();
    $event.stopPropagation();
    switch (target.getAttribute(ACTION_ATTR)) {
      case CellAction.UPGRADE: {
        const managedBy = (entity as unknown as EdgeMonitoringRow)[EDGE_MANAGED_BY_PROP];
        if (managedBy) {
          this.openAgentAppUpgrade(managedBy.appId);
        } else {
          actions.openUpgradeInstructions($event, entity);
        }
        break;
      }
      case CellAction.STATUS_PAGE:
        actions.openStatusPage($event, entity);
        break;
      case CellAction.NAVIGATE:
        this.router.navigateByUrl(target.getAttribute('href'));
        break;
    }
    return true;
  }

  // Opaque, because sticky cells (the checkbox) take the same color through the variable and must hide what scrolls under them.
  private rowStyle(entity: EdgeMonitoringRow): {[key: string]: string} {
    return edgeHasLag(entity[EDGE_MONITORING_PROP]) ?
      {backgroundColor: LAGGING_ROW_COLOR, '--tb-row-sticky-background': LAGGING_ROW_COLOR} : {};
  }

  private loadManagedBy(ctx: EdgeMonitoringContext, rows: EdgeMonitoringRow[], refresh: () => void) {
    if (!ctx.agentsEnabled || !rows.length) {
      return;
    }
    const ids = rows.map(row => row.id.id);
    const chunks: string[][] = [];
    for (let i = 0; i < ids.length; i += AGENT_APP_LOOKUP_MAX_IDS) {
      chunks.push(ids.slice(i, i + AGENT_APP_LOOKUP_MAX_IDS));
    }
    forkJoin(chunks.map(chunk => this.agentService.getAgentApplicationInfosByRelatedEntities(EntityType.EDGE, chunk,
      {ignoreErrors: true, ignoreLoading: true}).pipe(catchError(() => of([] as AgentApplicationInfo[]))))).subscribe(results => {
      const byEdgeId = new Map<string, EdgeManagedBy>();
      results.flat().forEach(app => {
        if (app.relatedEntityId?.id) {
          byEdgeId.set(app.relatedEntityId.id, {
            agentId: app.agentId?.id,
            agentName: app.agentName,
            appId: app.id?.id,
            appName: app.name
          });
        }
      });
      rows.forEach(row => row[EDGE_MANAGED_BY_PROP] = byEdgeId.get(row.id.id) ?? null);
      refresh();
    });
  }

  /** Agent-managed edges are upgraded by their agent application, the same wizard as on the edge details page. */
  openAgentAppUpgrade(appId: string, onUpgraded?: () => void) {
    this.agentService.getAgentApplicationInfoById(appId).subscribe(application => {
      this.dialog.open<AgentAppInstallWizardComponent, AgentAppInstallWizardData, AgentAppInstallWizardResult>(
        AgentAppInstallWizardComponent, {
          disableClose: false,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-agent-wizard-dialog'],
          data: {
            agentId: application.agentId?.id,
            agent: null,
            mode: 'upgrade',
            application
          }
        }).afterClosed().subscribe(result => {
          if (result) {
            onUpgraded?.();
          }
        });
    });
  }

  private statusCell(state: EdgeMonitoringState): string {
    if (state?.active === undefined) {
      return EMPTY_CELL;
    }
    const pill = state.active ?
      this.stateLabel(this.translate.instant('edge.monitoring.active'), 'rgba(25, 128, 56, 0.06)', 'rgb(25, 128, 56)') :
      this.stateLabel(this.translate.instant('edge.monitoring.inactive'), 'rgba(203, 37, 48, 0.06)', 'rgb(203, 37, 48)');
    const syncHtml = state.syncInProgress ?
      `<div style="font-size: 12px; ${MUTED}; margin-top: 4px">` +
      `${escapeHtml(this.translate.instant('edge.monitoring.sync-in-progress'))}</div>` : '';
    return `<div>${pill}${syncHtml}</div>`;
  }

  // Same look as the state column of the gateways list.
  private stateLabel(text: string, background: string, color: string): string {
    return `<div style="width: fit-content; padding: 5px 15px; border-radius: 16px; text-align: center; ` +
      `font-weight: 500; background: ${background}; color: ${color}">${escapeHtml(text)}</div>`;
  }

  private versionCell(state: EdgeMonitoringState, ctx: EdgeMonitoringContext): string {
    if (!state?.version) {
      return EMPTY_CELL;
    }
    const version = formatEdgeVersion(state.version);
    if (!version) {
      // E.g. UNRECOGNIZED: the edge runs a version newer than this server knows.
      return `<span style="${MUTED}" title="${escapeHtml(state.version)}">` +
        `${escapeHtml(this.translate.instant('edge.monitoring.version-unknown'))}</span>`;
    }
    let arrow = '';
    if (ctx.upgradeEnabled) {
      const iconStyle = 'font-size: 18px; width: 18px; height: 18px; line-height: 18px';
      if (isEdgeUpgradeAvailable(state.version, ctx.latestVersion)) {
        const title = escapeHtml(this.translate.instant('edge.monitoring.upgrade-available', {version: ctx.latestVersion}));
        arrow = `<button type="button" ${ACTION_ATTR}="${CellAction.UPGRADE}" title="${title}" aria-label="${title}" ` +
          `style="display: inline-flex; align-items: center; justify-content: center; width: 26px; height: 26px; padding: 0; ` +
          `border: 0; border-radius: 50%; background: #305680; color: #fff; cursor: pointer">` +
          `<span class="material-icons" style="${iconStyle}">arrow_upward</span></button>`;
      } else {
        const title = escapeHtml(this.translate.instant(compareEdgeVersions(version, EDGE_MIN_UPGRADABLE_VERSION) >= 0 ?
          'edge.monitoring.up-to-date' : 'edge.monitoring.upgrade-not-supported'));
        arrow = `<span class="material-icons" title="${title}" style="${iconStyle}; color: rgba(0,0,0,0.26)">arrow_upward</span>`;
      }
    }
    return `<span style="display: inline-flex; align-items: center; gap: 8px">` +
      `<span style="font-family: 'Roboto Mono', monospace; font-size: 13px">${escapeHtml(version)}</span>${arrow}</span>`;
  }

  private lagCell(lag: number, lastKnown: boolean): string {
    if (lag === undefined) {
      return EMPTY_CELL;
    }
    const value = lag.toLocaleString();
    let html = lag > 0 ?
      `<span style="background: #fff1d6; color: #7a4100; border-radius: 4px; padding: 2px 8px; font-weight: 500">${value}</span>` :
      `<span style="${MUTED}">${value}</span>`;
    if (lastKnown) {
      html += `<div style="font-size: 11px; ${MUTED}; margin-top: 4px" ` +
        `title="${escapeHtml(this.translate.instant('edge.monitoring.last-known-hint'))}">` +
        `${escapeHtml(this.translate.instant('edge.monitoring.last-known'))}</div>`;
    }
    return `<div>${html}</div>`;
  }

  private trafficCell(traffic: EdgeTrafficStats, direction: EdgeStatsDirection): string {
    if (!traffic) {
      return EMPTY_CELL;
    }
    const width = 72;
    const height = 20;
    const max = Math.max(1, ...traffic.series);
    const step = width / (traffic.series.length - 1);
    const points = traffic.series.map((v, i) => `${(i * step).toFixed(1)},${(height - 2 - v / max * (height - 4)).toFixed(1)}`);
    const line = 'M' + points.join(' L');
    const colors = sparkLineColors[direction];
    const chart = `<svg width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" aria-hidden="true">` +
      `<path d="${line} L${width},${height} L0,${height} Z" fill="${colors.fill}"></path>` +
      `<path d="${line}" fill="none" stroke="${colors.line}" stroke-width="1.5"></path></svg>`;
    // formatCount keeps it short (999k, 1.2M), so the text fits under the 72px chart.
    const pushed = traffic.pushed ?
      `<div>${escapeHtml(this.translate.instant('edge.monitoring.pushed-count', {count: formatCount(traffic.pushed)}))}</div>` :
      `<div style="${MUTED}">${escapeHtml(this.translate.instant('edge.monitoring.no-traffic'))}</div>`;
    const failed = traffic.failed ?
      `<div style="color: #9a4a00">${escapeHtml(this.translate.instant('edge.monitoring.failed-count',
        {count: formatCount(traffic.failed)}))}</div>` : '';
    return `<span style="display: inline-flex; flex-direction: column; gap: 2px">${chart}` +
      `<span style="font-size: 12px; line-height: 16px; white-space: nowrap">${pushed}${failed}</span></span>`;
  }

  private managedByCell(managedBy: EdgeManagedBy | null): string {
    // undefined while the agent application lookup is running.
    if (managedBy === undefined) {
      return '';
    }
    if (managedBy === null) {
      return `<span style="font-size: 13px"><span style="${MUTED}">` +
        `${escapeHtml(this.translate.instant('edge.monitoring.not-managed'))} · </span>` +
        this.link(AGENTS_URL, this.translate.instant('edge.monitoring.link-to-app')) + '</span>';
    }
    const agentUrl = `${AGENTS_URL}/${managedBy.agentId}`;
    // App names are long image-based identifiers that would widen the column (table layout keeps a cell as wide as its
    // longest unbreakable text), so the app link has a fixed text and the full name in its tooltip.
    return `<div style="font-size: 13px; line-height: 20px; overflow-wrap: anywhere">` +
      `<div>${this.link(agentUrl, managedBy.agentName)}</div>` +
      `<div>${this.link(`${agentUrl}/applications/${managedBy.appId}`, this.translate.instant('edge.monitoring.open-app'),
        managedBy.appName)}</div></div>`;
  }

  private link(url: string, text: string, title?: string): string {
    const titleAttr = title ? ` title="${escapeHtml(title)}"` : '';
    return `<a href="${escapeHtml(url)}" ${ACTION_ATTR}="${CellAction.NAVIGATE}"${titleAttr}>${escapeHtml(text)}</a>`;
  }

  private statusPageCell(state: EdgeMonitoringState): string {
    if (edgeHasLag(state)) {
      return `<button type="button" ${ACTION_ATTR}="${CellAction.STATUS_PAGE}" ` +
        `style="display: inline-flex; align-items: center; gap: 6px; height: 32px; padding: 0 12px; border: 0; ` +
        `border-radius: 6px; background: #ffe2b0; color: #6b3800; font: inherit; font-size: 13px; font-weight: 500; cursor: pointer">` +
        `<span class="material-icons" style="font-size: 16px; width: 16px; height: 16px">warning_amber</span>` +
        `${escapeHtml(this.translate.instant('edge.monitoring.investigate'))}</button>`;
    }
    const title = escapeHtml(this.translate.instant('edge.monitoring.open-status-page'));
    return `<button type="button" ${ACTION_ATTR}="${CellAction.STATUS_PAGE}" title="${title}" aria-label="${title}" ` +
      `style="display: inline-flex; align-items: center; justify-content: center; width: 32px; height: 32px; padding: 0; ` +
      `border: 1px solid rgba(0,0,0,0.12); border-radius: 6px; background: #fff; color: #305680; cursor: pointer">` +
      `<span class="material-icons" style="font-size: 18px; width: 18px; height: 18px">monitor_heart</span></button>`;
  }
}
