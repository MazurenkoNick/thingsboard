// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgZone } from '@angular/core';
import { CollectionViewer } from '@angular/cdk/collections';
import { Observable, ReplaySubject, Subject } from 'rxjs';
import { EntitiesDataSource } from '@home/models/datasource/entity-datasource';
import { EntityBooleanFunction } from '@home/models/entity/entities-table-config.models';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { EntityDataCmd, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import { EntityData, EntityDataPageLink, EntityFilter, EntityKey, EntityKeyType, KeyFilter } from '@shared/models/query/query.models';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { BaseData, HasId } from '@shared/models/base-data';
import { EdgeId } from '@shared/models/id/edge-id';
import {
  EntityGroupColumn,
  entityGroupColumnToEntityKey,
  entityDataToShortEntityView,
  prepareEntityDataColumnMap,
  ShortEntityView
} from '@shared/models/entity-group.models';
import { isDefined } from '@core/utils';
import {
  EDGE_MONITORING_PROP,
  EdgeMonitoringContext,
  edgeMonitoringLatestKeys,
  EdgeMonitoringRowData,
  edgeMonitoringTsCmd,
  mergeEdgeMonitoringData
} from '@home/pages/edge/monitoring/edge-monitoring.models';

export type EdgeMonitoringRow = BaseData<HasId> & EdgeMonitoringRowData;

export interface EdgeMonitoringQuery {
  entityFilter: EntityFilter;
  keyFilters: KeyFilter[];
}

/** How table rows are read from the entity data query, besides the monitoring data. */
export interface EdgeMonitoringRowsSpec {
  entityFields: EntityKey[];
  latestValues: EntityKey[];
  sortKeys: {[columnKey: string]: EntityKey};
  toRow: (entityData: EntityData) => EdgeMonitoringRow;
  updateRow: (row: EdgeMonitoringRow, entityData: EntityData) => void;
}

const entityField = (key: string): EntityKey => ({type: EntityKeyType.ENTITY_FIELD, key});

/** Rows of the all-edges / customer edges table: edge entity fields only. */
export const edgeRowsSpec = (sortKeys: {[columnKey: string]: EntityKey}): EdgeMonitoringRowsSpec => ({
  entityFields: ['name', 'type', 'label', 'createdTime'].map(entityField),
  latestValues: [],
  sortKeys: {...sortKeys, createdTime: entityField('createdTime'), name: entityField('name')},
  toRow: entityData => {
    const fields = entityData.latest?.[EntityKeyType.ENTITY_FIELD] ?? {};
    return {
      id: new EdgeId(entityData.entityId.id),
      name: fields.name?.value ?? '',
      type: fields.type?.value ?? '',
      label: fields.label?.value ?? '',
      createdTime: Number(fields.createdTime?.value) || null
    } as EdgeMonitoringRow;
  },
  updateRow: () => {}
});

/** Rows of an edge group table: the group's configured columns, read as GroupEntitiesDataSource reads them. */
export const edgeGroupRowsSpec = (columns: EntityGroupColumn[],
                                  sortKeys: {[columnKey: string]: EntityKey}): EdgeMonitoringRowsSpec => {
  const columnsMap = prepareEntityDataColumnMap(columns);
  const entityFields = (columnsMap[EntityKeyType.ENTITY_FIELD] ?? []).map(entityGroupColumnToEntityKey);
  if (!entityFields.some(key => key.key === 'name')) {
    entityFields.push(entityField('name'));
  }
  const latestValues = Object.keys(columnsMap)
    .filter(type => type !== EntityKeyType.ENTITY_FIELD)
    .flatMap(type => columnsMap[type].map(entityGroupColumnToEntityKey));
  const columnSortKeys: {[columnKey: string]: EntityKey} = {};
  columns.forEach(column => columnSortKeys[column.columnKey] = entityGroupColumnToEntityKey(column));
  return {
    entityFields,
    latestValues,
    sortKeys: {...columnSortKeys, ...sortKeys},
    toRow: entityData => entityDataToShortEntityView(entityData, columnsMap) as ShortEntityView & EdgeMonitoringRow,
    updateRow: (row, entityData) => {
      const update = entityDataToShortEntityView(entityData, columnsMap, true);
      Object.keys(update).filter(property => property !== 'id' && property !== 'name')
        .forEach(property => row[property] = update[property]);
    }
  };
};

const mergeKeys = (keys: EntityKey[], extra: EntityKey[]): EntityKey[] =>
  keys.concat(extra.filter(e => !keys.some(k => k.type === e.type && k.key === e.key)));

class EdgeMonitoringSubscription {

  private pageDataSubject: Subject<PageData<EdgeMonitoringRow>>;
  private updatesSubject = new Subject<EdgeMonitoringRow[]>();
  public updates$ = this.updatesSubject.asObservable();

  private subscriber: TelemetrySubscriber;
  private rows: EdgeMonitoringRow[] = [];
  private rowIndexById: {[id: string]: number} = {};

  constructor(private telemetryService: TelemetryWebsocketService,
              private zone: NgZone,
              private ctx: EdgeMonitoringContext,
              private spec: EdgeMonitoringRowsSpec) {
  }

  getEntities(pageLink: PageLink): Observable<PageData<EdgeMonitoringRow>> {
    this.doUnsubscribe();
    const query = this.ctx.baseQuery();
    const latestValues = mergeKeys(this.spec.latestValues, edgeMonitoringLatestKeys());
    const cmd = new EntityDataCmd();
    cmd.query = {
      entityFilter: query.entityFilter,
      keyFilters: [...query.keyFilters, ...this.ctx.selectedCardKeyFilters()],
      entityFields: this.spec.entityFields,
      latestValues,
      pageLink: this.toEntityDataPageLink(pageLink)
    };
    cmd.latestCmd = {keys: latestValues};
    cmd.tsCmd = edgeMonitoringTsCmd();
    this.subscriber = new TelemetrySubscriber(this.telemetryService, this.zone);
    this.pageDataSubject = new ReplaySubject<PageData<EdgeMonitoringRow>>();
    this.subscriber.subscriptionCommands.push(cmd);
    this.subscriber.entityData$.subscribe((update) => {
      if (update.data) {
        this.onPageData(update.data);
      } else if (update.update) {
        this.onDataUpdate(update.update);
      }
    });
    this.subscriber.subscribe();
    return this.pageDataSubject.asObservable();
  }

  unsubscribe() {
    this.doUnsubscribe();
    this.updatesSubject.complete();
  }

  private doUnsubscribe() {
    this.subscriber?.unsubscribe();
    this.pageDataSubject?.complete();
    this.subscriber = null;
    this.pageDataSubject = null;
  }

  private toEntityDataPageLink(pageLink: PageLink): EntityDataPageLink {
    const sortKey = pageLink.sortOrder?.property ? this.spec.sortKeys[pageLink.sortOrder.property] : null;
    return {
      dynamic: false,
      pageSize: pageLink.pageSize,
      page: pageLink.page,
      textSearch: pageLink.textSearch,
      sortOrder: sortKey ? {key: sortKey, direction: pageLink.sortOrder.direction} : null
    };
  }

  private onPageData(pageData: PageData<EntityData>) {
    this.rows = pageData.data.map(entityData => {
      const row = this.spec.toRow(entityData);
      row[EDGE_MONITORING_PROP] = mergeEdgeMonitoringData(undefined, entityData);
      return row;
    });
    this.rowIndexById = {};
    this.rows.forEach((row, index) => this.rowIndexById[row.id.id] = index);
    this.ctx.detectStateStorage(this.rows.map(row => row[EDGE_MONITORING_PROP]));
    this.pageDataSubject?.next({
      data: this.rows,
      totalElements: pageData.totalElements,
      totalPages: pageData.totalPages,
      hasNext: pageData.hasNext
    });
  }

  private onDataUpdate(update: EntityData[]) {
    let changed = false;
    for (const entityData of update) {
      const index = this.rowIndexById[entityData.entityId.id];
      if (isDefined(index)) {
        const row = this.rows[index];
        this.spec.updateRow(row, entityData);
        row[EDGE_MONITORING_PROP] = mergeEdgeMonitoringData(row[EDGE_MONITORING_PROP], entityData);
        changed = true;
      }
    }
    if (changed) {
      this.updatesSubject.next(this.rows);
    }
  }
}

export class EdgeMonitoringDataSource<T extends EdgeMonitoringRow> extends EntitiesDataSource<T> {

  private subscription: EdgeMonitoringSubscription;

  constructor(telemetryService: TelemetryWebsocketService,
              zone: NgZone,
              ctx: EdgeMonitoringContext,
              spec: EdgeMonitoringRowsSpec,
              private pageLoaded: (rows: T[], refresh: () => void) => void,
              private refreshed: () => void,
              protected selectionEnabledFunction: EntityBooleanFunction<T>,
              protected dataLoadedFunction: (col?: number, row?: number) => void) {
    super(pageLink => this.subscription.getEntities(pageLink) as Observable<PageData<T>>,
      selectionEnabledFunction, dataLoadedFunction);
    this.subscription = new EdgeMonitoringSubscription(telemetryService, zone, ctx, spec);
    this.subscription.updates$.subscribe(rows => this.refresh(rows as T[]));
  }

  loadEntities(pageLink: PageLink): Observable<PageData<T>> {
    const result = super.loadEntities(pageLink);
    result.subscribe(pageData => this.pageLoaded(pageData.data, () => this.refresh(pageData.data)));
    return result;
  }

  disconnect(collectionViewer: CollectionViewer): void {
    super.disconnect(collectionViewer);
    this.subscription.unsubscribe();
  }

  private refresh(rows: T[]) {
    this.dataLoadedFunction();
    this.onEntities(rows);
    this.refreshed();
  }
}
