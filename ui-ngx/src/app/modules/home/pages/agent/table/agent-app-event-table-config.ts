///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
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
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { Direction } from '@shared/models/page/sort-order';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import {
  AgentAppEvent,
  AgentAppEventActionType,
  agentAppEventActionTypeTranslationMap,
  AgentAppEventStatus,
  agentAppEventStatusTranslationMap,
  AgentApplicationInfo
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

export class AgentAppEventTableConfig extends EntityTableConfig<AgentAppEvent> {

  private filter: AgentAppEventFilterValue = { actionType: null, status: null };

  constructor(private readonly application: AgentApplicationInfo,
              private readonly agentService: AgentService,
              private readonly dialogService: DialogService,
              private readonly dialog: MatDialog,
              private readonly translate: TranslateService,
              private readonly datePipe: DatePipe,
              private readonly overlay: Overlay,
              private readonly viewContainerRef: ViewContainerRef) {
    super();

    this.tableTitle = this.translate.instant('agent.app-events');
    this.detailsPanelEnabled = false;
    this.selectionEnabled = false;
    this.searchEnabled = true;
    this.addEnabled = false;
    this.entitiesDeleteEnabled = false;
    // Per-tab table, not a page-level table — don't let router query params
    // drive our paginator/sort (the parent Applications list shares the URL).
    this.pageMode = false;
    this.defaultSortOrder = { property: 'createdTime', direction: Direction.DESC };

    this.entityTranslations = { noEntities: 'agent.app-no-events' } as any;
    this.entityResources = {} as any;

    // Emit a marker span in the status cell for in-flight rows. The wrapper
    // component's SCSS uses `:has()` to detect this marker and paint the
    // whole mat-row (including the action cell) amber with a hand cursor —
    // no modifications to shared EntityTableConfig / entities-table needed.
    this.columns.push(
      new EntityTableColumn<AgentAppEvent>('actionType',
        'agent.app-event-action', '160px',
        (e) => this.translate.instant(agentAppEventActionTypeTranslationMap.get(e.actionType) || e.actionType),
        () => ({}), true),
      new EntityTableColumn<AgentAppEvent>('status',
        'agent.app-event-status', '160px',
        (e) => {
          const label = this.translate.instant(agentAppEventStatusTranslationMap.get(e.status) || e.status);
          return this.canCancel(e)
            ? `<span class="tb-agent-app-event-inflight">${label}</span>`
            : label;
        },
        () => ({}), true),
      new DateEntityTableColumn<AgentAppEvent>('createdTime',
        'agent.app-event-created', this.datePipe, '180px', 'yyyy-MM-dd HH:mm:ss'),
      new DateEntityTableColumn<AgentAppEvent>('updatedTime',
        'agent.app-event-updated', this.datePipe, '180px', 'yyyy-MM-dd HH:mm:ss')
    );

    // One cell-action column with mutually exclusive semantics:
    //   PENDING/QUEUED/PROCESSING → cancel button (red)
    //   ERROR (with message)      → "more_horiz" → error dialog
    //   FINISHED / other          → hidden (icon function returns empty,
    //                               action disabled via isEnabled)
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

  private fetch(pageLink: PageLink): Observable<PageData<AgentAppEvent>> {
    return this.agentService.getAgentAppEvents(
      this.application.id.id,
      pageLink,
      this.filter.actionType || undefined,
      this.filter.status || undefined
    );
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

  private canCancel(e: AgentAppEvent): boolean {
    return e.status === AgentAppEventStatus.PENDING
      || e.status === AgentAppEventStatus.QUEUED
      || e.status === AgentAppEventStatus.PROCESSING;
  }

  private isErrorRow(e: AgentAppEvent): boolean {
    return e.status === AgentAppEventStatus.ERROR && !!e.errorMessage;
  }

  private cancelEvent($event: Event, e: AgentAppEvent): void {
    if ($event) { $event.stopPropagation(); }
    this.dialogService.confirm(
      this.translate.instant('agent.app-event-cancel-title'),
      this.translate.instant('agent.app-event-cancel-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe(res => {
      if (res) {
        this.agentService.cancelAgentAppEvent(this.application.id.id, e.id.id).subscribe(() => this.updateData());
      }
    });
  }

  private showEventError($event: Event, e: AgentAppEvent): void {
    if ($event) { $event.stopPropagation(); }
    this.dialogService.alert(
      this.translate.instant('agent.app-event-error-title'),
      e.errorMessage || ''
    );
  }

  private onRowClick($event: Event, e: AgentAppEvent): boolean {
    if (!this.canCancel(e)) {
      return false;
    }
    if ($event) { $event.stopPropagation(); }
    this.dialog.open<AgentAppEventProgressDialogComponent, AgentAppEventProgressDialogData, boolean>(
      AgentAppEventProgressDialogComponent, {
        disableClose: false,
        panelClass: ['tb-dialog'],
        data: { application: this.application, event: e }
      }
    ).afterClosed().subscribe(() => this.updateData());
    return true;
  }
}
