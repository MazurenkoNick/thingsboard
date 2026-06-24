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

import { DatePipe } from '@angular/common';
import { Injector, StaticProvider, ViewContainerRef } from '@angular/core';
import { Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';

import { AgentService } from '@core/http/agent.service';
import { DialogService } from '@core/services/dialog.service';
import {
  DateEntityTableColumn,
  EntityLinkTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { Direction } from '@shared/models/page/sort-order';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import {
  AgentAppEventActionType,
  agentAppEventActionTypeTranslationMap,
  agentAppEventDeliveryStateTranslationMap,
  AgentAppEventInfo,
  AgentAppEventStatus,
  agentAppEventStatusTranslationMap
} from '@shared/models/agent.models';
import {
  AgentAppEventProgressDialogComponent,
  AgentAppEventProgressDialogData
} from '@home/pages/agent/dialog/agent-app-event-progress-dialog.component';
import {
  AGENT_APP_EVENT_FILTER_PANEL_DATA,
  AgentAppEventFilterPanelComponent,
  AgentAppEventFilterPanelData,
  AgentAppEventFilterValue
} from './agent-app-event-filter-panel.component';
import {
  AgentEventsStatsHeaderComponent,
  EventsStatsFetcher
} from './agent-events-stats-header.component';

export class AgentBulkActionEventsTableConfig
  extends EntityTableConfig<AgentAppEventInfo>
  implements EventsStatsFetcher {

  filter: AgentAppEventFilterValue = { actionType: null, status: null };

  constructor(public readonly bulkActionId: string,
              private readonly agentService: AgentService,
              private readonly dialogService: DialogService,
              private readonly dialog: MatDialog,
              private readonly translate: TranslateService,
              private readonly datePipe: DatePipe,
              private readonly overlay: Overlay,
              private readonly viewContainerRef: ViewContainerRef) {
    super();

    this.tableTitle = this.translate.instant('agent.executions');
    this.headerComponent = AgentEventsStatsHeaderComponent;
    this.detailsPanelEnabled = false;
    this.selectionEnabled = false;
    this.searchEnabled = true;
    this.addEnabled = false;
    this.entitiesDeleteEnabled = false;
    this.pageMode = false;
    this.defaultSortOrder = { property: 'createdTime', direction: Direction.DESC };

    this.entityTranslations = { noEntities: 'agent.bulk-no-events' } as any;
    this.entityResources = {} as any;

    this.columns.push(
      new EntityLinkTableColumn<AgentAppEventInfo>('agentName',
        'agent.app-event-agent-name', '20%',
        (e) => e.agentName || '',
        (e) => e.agentId?.id
          ? `/edgeManagement/agents/all/${e.agentId.id}`
          : '',
        false),
      new EntityLinkTableColumn<AgentAppEventInfo>('applicationName',
        'agent.app-event-app-name', '25%',
        (e) => e.applicationName || this.translate.instant('agent.app-deleted'),
        (e) => e.agentId?.id && e.applicationId?.id
          ? `/edgeManagement/agents/all/${e.agentId.id}/applications/${e.applicationId.id}`
          : '',
        false),
      new EntityTableColumn<AgentAppEventInfo>('actionType',
        'agent.app-event-action', '140px',
        (e) => {
          const key = agentAppEventActionTypeTranslationMap.get(e.actionType) || e.actionType;
          return key ? this.translate.instant(key) : '';
        },
        () => ({}), true),
      new EntityTableColumn<AgentAppEventInfo>('deliveryState',
        'agent.app-event-execution', '140px',
        (e) => {
          const key = agentAppEventDeliveryStateTranslationMap.get(e.deliveryState);
          return key ? this.translate.instant(key) : '';
        },
        () => ({}), true),
      new EntityTableColumn<AgentAppEventInfo>('status',
        'agent.app-event-status', '160px',
        (e) => {
          const key = agentAppEventStatusTranslationMap.get(e.status) || e.status;
          const label = key ? this.translate.instant(key) : '';
          return this.canCancel(e)
            ? `<span class="tb-agent-app-event-inflight">${label}</span>`
            : label;
        },
        () => ({}), true),
      new DateEntityTableColumn<AgentAppEventInfo>('createdTime',
        'agent.app-event-created', this.datePipe, '180px', 'yyyy-MM-dd HH:mm:ss'),
      new DateEntityTableColumn<AgentAppEventInfo>('updatedTime',
        'agent.app-event-updated', this.datePipe, '180px', 'yyyy-MM-dd HH:mm:ss')
    );

    this.cellActionDescriptors.push({
      name: this.translate.instant('agent.app-event-cancel'),
      nameFunction: (e) => this.isErrorRow(e)
        ? this.translate.instant('agent.app-event-show-error')
        : this.translate.instant('agent.app-event-cancel'),
      icon: 'cancel',
      iconFunction: (e) => {
        if (this.isErrorRow(e)) { return 'more_horiz'; }
        if (this.canCancel(e))  { return 'cancel'; }
        return '';
      },
      style: {},
      isEnabled: (e) => this.canCancel(e) || this.isErrorRow(e),
      onAction: ($event, e) => {
        if (this.isErrorRow(e)) {
          this.showEventError($event, e);
        } else if (this.canCancel(e)) {
          this.cancelEvent($event, e);
        }
      }
    });

    this.headerActionDescriptors.push(
      {
        name: this.translate.instant('agent.app-event-filter'),
        icon: 'filter_list',
        isEnabled: () => true,
        onAction: ($event) => this.openFilterPanel($event)
      },
      {
        name: this.translate.instant('action.clear'),
        icon: 'mdi:filter-variant-remove',
        isEnabled: () => this.hasActiveFilter(),
        onAction: () => this.clearFilter()
      }
    );

    this.entitiesFetchFunction = (pageLink) => this.fetch(pageLink);

    this.handleRowClick = ($event, e) => this.onRowClick($event, e);
  }

  fetchAllEventsForStats(): Observable<PageData<AgentAppEventInfo>> {
    const pageLink = new PageLink(10000, 0, null, { property: 'updatedTime', direction: Direction.DESC });
    return this.agentService.getAgentBulkActionEvents(this.bulkActionId, pageLink);
  }

  private fetch(pageLink: PageLink): Observable<PageData<AgentAppEventInfo>> {
    return this.agentService.getAgentBulkActionEvents(
      this.bulkActionId,
      pageLink,
      this.filter.actionType || undefined,
      this.filter.status || undefined
    ) as Observable<PageData<AgentAppEventInfo>>;
  }

  private hasActiveFilter(): boolean {
    return !!(this.filter.actionType || this.filter.status);
  }

  private clearFilter(): void {
    if (!this.hasActiveFilter()) { return; }
    this.filter = { actionType: null, status: null };
    this.getTable().paginator.pageIndex = 0;
    this.updateData();
  }

  private openFilterPanel($event: MouseEvent): void {
    if ($event) { $event.stopPropagation(); }
    const target = ($event.target || $event.currentTarget) as HTMLElement;
    const config = new OverlayConfig({
      panelClass: 'tb-panel-container',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: true,
      height: 'fit-content',
      maxHeight: '65vh'
    });
    config.positionStrategy = this.overlay.position()
      .flexibleConnectedTo(target)
      .withPositions([
        { originX: 'start', originY: 'bottom', overlayX: 'start', overlayY: 'top' },
        { originX: 'end',   originY: 'bottom', overlayX: 'end',   overlayY: 'top' }
      ]);
    const overlayRef = this.overlay.create(config);
    overlayRef.backdropClick().subscribe(() => overlayRef.dispose());

    const providers: StaticProvider[] = [
      {
        provide: AGENT_APP_EVENT_FILTER_PANEL_DATA,
        useValue: { value: { ...this.filter } } as AgentAppEventFilterPanelData
      },
      { provide: OverlayRef, useValue: overlayRef }
    ];
    const injector = Injector.create({ parent: this.viewContainerRef.injector, providers });
    const ref = overlayRef.attach(new ComponentPortal(
      AgentAppEventFilterPanelComponent, this.viewContainerRef, injector));
    ref.onDestroy(() => {
      const result = ref.instance.result;
      if (result && (result.actionType !== this.filter.actionType || result.status !== this.filter.status)) {
        this.filter = result;
        this.getTable().paginator.pageIndex = 0;
        this.updateData();
      }
    });
  }

  private canCancel(e: AgentAppEventInfo): boolean {
    return e.status === AgentAppEventStatus.PENDING
      || e.status === AgentAppEventStatus.QUEUED
      || e.status === AgentAppEventStatus.PROCESSING;
  }

  private isErrorRow(e: AgentAppEventInfo): boolean {
    return (e.status === AgentAppEventStatus.ERROR || e.status === AgentAppEventStatus.START_FAILED)
      && !!e.errorMessage;
  }

  private cancelEvent($event: Event, e: AgentAppEventInfo): void {
    if ($event) { $event.stopPropagation(); }
    if (!e.applicationId) { return; }
    this.dialogService.confirm(
      this.translate.instant('agent.app-event-cancel-title'),
      this.translate.instant('agent.app-event-cancel-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe(res => {
      if (res) {
        this.agentService.cancelAgentAppEvent(e.applicationId.id, e.id.id).subscribe(() => this.updateData());
      }
    });
  }

  private showEventError($event: Event, e: AgentAppEventInfo): void {
    if ($event) { $event.stopPropagation(); }
    this.dialogService.alert(
      this.translate.instant('agent.app-event-error-title'),
      e.errorMessage || ''
    );
  }

  private onRowClick($event: Event, e: AgentAppEventInfo): boolean {
    if (!this.canCancel(e) || !e.applicationId) {
      return false;
    }
    if ($event) { $event.stopPropagation(); }
    this.agentService.getAgentApplicationById(e.applicationId.id, { ignoreErrors: true }).subscribe({
      next: application => this.openProgress(application, e),
      error: () => this.openProgress(null, e)
    });
    return true;
  }

  private openProgress(application: any, event: AgentAppEventInfo): void {
    this.dialog.open<AgentAppEventProgressDialogComponent, AgentAppEventProgressDialogData, boolean>(
      AgentAppEventProgressDialogComponent, {
        disableClose: false,
        panelClass: ['tb-dialog'],
        data: { application, event }
      }
    ).afterClosed().subscribe(() => this.updateData());
  }
}
