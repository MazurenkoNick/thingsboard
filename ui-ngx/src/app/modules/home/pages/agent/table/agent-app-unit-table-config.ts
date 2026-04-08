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

import { Injector, StaticProvider, ViewContainerRef } from '@angular/core';
import { Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

import { AgentService } from '@core/http/agent.service';
import { AttributeService } from '@core/http/attribute.service';
import {
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { Direction } from '@shared/models/page/sort-order';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import {
  AgentApplicationInfo,
  AgentAppUnit,
  AgentAppUnitType,
  agentAppUnitTypeTranslationMap
} from '@shared/models/agent.models';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { EntityType } from '@shared/models/entity-type.models';
import {
  AGENT_APP_UNIT_FILTER_PANEL_DATA,
  AgentAppUnitFilterPanelComponent,
  AgentAppUnitFilterPanelData,
  AgentAppUnitFilterValue
} from './agent-app-unit-filter-panel.component';

const typeBadgeStyles: Record<string, string> = {
  CONTAINER: 'background:#e3f2fd;color:#1565c0;',
  VOLUME:    'background:#e8f5e9;color:#2e7d32;',
  NETWORK:   'background:#f3e5f5;color:#6a1b9a;'
};

const stateBadgeStyles: Record<string, string> = {
  running:    'background:#e8f5e9;color:#2e7d32;',
  exited:     'background:#ffebee;color:#c62828;',
  dead:       'background:#ffebee;color:#c62828;',
  paused:     'background:#fff8e1;color:#f57c00;',
  restarting: 'background:#fff8e1;color:#f57c00;',
  created:    'background:#fff8e1;color:#f57c00;'
};

function badge(value: string, style: string): string {
  return `<span style="display:inline-flex;align-items:center;padding:2px 10px;border-radius:12px;`
    + `font-size:11px;font-weight:600;letter-spacing:0.5px;${style}">${value}</span>`;
}

function muted(): string {
  return `<span style="color:rgba(0,0,0,0.38);">—</span>`;
}

export class AgentAppUnitTableConfig extends EntityTableConfig<AgentAppUnit> {

  private filter: AgentAppUnitFilterValue = { type: null };

  constructor(private readonly application: AgentApplicationInfo,
              private readonly agentService: AgentService,
              private readonly attributeService: AttributeService,
              private readonly translate: TranslateService,
              private readonly overlay: Overlay,
              private readonly viewContainerRef: ViewContainerRef) {
    super();

    this.tableTitle = this.translate.instant('agent.app-units');
    this.detailsPanelEnabled = false;
    this.selectionEnabled = false;
    this.searchEnabled = true;
    this.addEnabled = false;
    this.entitiesDeleteEnabled = false;
    // Per-tab table, not a page-level table — don't let router query params
    // drive our paginator/sort (the parent Applications list shares the URL).
    this.pageMode = false;
    this.defaultSortOrder = { property: 'identifier', direction: Direction.ASC };

    this.entityTranslations = { noEntities: 'agent.app-no-units' } as any;
    this.entityResources = {} as any;

    this.columns.push(
      new EntityTableColumn<AgentAppUnit>('identifier',
        'agent.app-unit-identifier', '40%',
        (u) => u.identifier, () => ({}), true),
      new EntityTableColumn<AgentAppUnit>('type',
        'agent.app-type', '140px',
        (u) => badge(
          this.translate.instant(agentAppUnitTypeTranslationMap.get(u.type) || u.type),
          typeBadgeStyles[u.type] || 'background:#eeeeee;color:#616161;'
        ),
        () => ({}),
        true
      ),
      new EntityTableColumn<AgentAppUnit>('image',
        'agent.app-unit-image', '30%',
        (u) => u.image || muted(), () => ({}), false),
      new EntityTableColumn<AgentAppUnit>('state',
        'agent.app-unit-state', '140px',
        (u) => u.state
          ? badge(u.state, stateBadgeStyles[u.state] || 'background:#eeeeee;color:#616161;')
          : muted(),
        () => ({}), false
      )
    );

    this.headerActionDescriptors.push(
      {
        name: this.translate.instant('agent.app-unit-filter'),
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
  }

  private fetch(pageLink: PageLink): Observable<PageData<AgentAppUnit>> {
    return this.agentService
      .getAgentAppUnits(this.application.id.id, pageLink, this.filter.type || undefined)
      .pipe(switchMap(page => this.enrich(page)));
  }

  /**
   * Only CONTAINER units carry live `image`/`state` attributes, populated on
   * the unit entity in SERVER_SCOPE by ComposeUnitsSynchronizer. Fetch them
   * as part of the same observable chain so the data source receives rows
   * with the attributes already merged in — no mutation-after-render dance.
   */
  private enrich(page: PageData<AgentAppUnit>): Observable<PageData<AgentAppUnit>> {
    const containers = page.data.filter(u => u.type === AgentAppUnitType.CONTAINER);
    if (!containers.length) {
      return of(page);
    }
    const requests = containers.map(u =>
      this.attributeService.getEntityAttributes(
        { entityType: EntityType.AGENT_APP_UNIT, id: u.id.id },
        AttributeScope.SERVER_SCOPE,
        ['image', 'state'],
        { ignoreLoading: true, ignoreErrors: true }
      ).pipe(catchError(() => of([])))
    );
    return forkJoin(requests).pipe(map(results => {
      results.forEach((attrs, i) => {
        const unit = containers[i];
        for (const attr of attrs) {
          if (attr.key === 'image') { unit.image = attr.value as string; }
          if (attr.key === 'state') { unit.state = attr.value as string; }
        }
      });
      return page;
    }));
  }

  private hasActiveFilter(): boolean {
    return !!this.filter.type;
  }

  private clearFilter(): void {
    if (!this.hasActiveFilter()) { return; }
    this.filter = { type: null };
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
        provide: AGENT_APP_UNIT_FILTER_PANEL_DATA,
        useValue: { value: { ...this.filter } } as AgentAppUnitFilterPanelData
      },
      { provide: OverlayRef, useValue: overlayRef }
    ];
    const injector = Injector.create({ parent: this.viewContainerRef.injector, providers });
    const ref = overlayRef.attach(new ComponentPortal(
      AgentAppUnitFilterPanelComponent, this.viewContainerRef, injector));
    ref.onDestroy(() => {
      const result = ref.instance.result;
      if (result && result.type !== this.filter.type) {
        this.filter = result;
        this.getTable().paginator.pageIndex = 0;
        this.updateData();
      }
    });
  }
}
