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

import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, map, mergeMap } from 'rxjs/operators';
import {
  CellActionDescriptor,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { Direction } from '@shared/models/page/sort-order';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { AgentService } from '@core/http/agent.service';
import {
  AgentAppProfile,
  AgentGroupInfo,
  agentApplicationTypeTranslationMap
} from '@shared/models/agent.models';
import {
  AgentGroupAssignProfileDialogComponent,
  AgentGroupAssignProfileDialogData
} from '@home/pages/agent/dialog/agent-group-assign-profile-dialog.component';

const typeBadgeStyles: Record<string, string> = {
  EDGE:    'background:#e8eaf6;color:#283593;',
  GATEWAY: 'background:#e0f2f1;color:#00695c;',
  GENERIC: 'background:#f3e5f5;color:#6a1b9a;'
};

function typeBadge(appType: string, label: string): string {
  const style = typeBadgeStyles[appType] || 'background:#eeeeee;color:#616161;';
  return `<span style="display:inline-flex;align-items:center;padding:2px 10px;border-radius:12px;`
    + `font-size:11px;font-weight:600;letter-spacing:0.5px;${style}">${label}</span>`;
}

export class AgentGroupProfilesTableConfig extends EntityTableConfig<AgentAppProfile> {

  constructor(private readonly group: AgentGroupInfo,
              private readonly agentService: AgentService,
              private readonly translate: TranslateService,
              private readonly dialog: MatDialog,
              private readonly dialogService: DialogService,
              private readonly router: Router) {
    super();

    this.tableTitle = this.translate.instant('agent.application-profiles');
    this.detailsPanelEnabled = false;
    this.selectionEnabled = false;
    this.searchEnabled = true;
    this.addEnabled = true;
    this.entitiesDeleteEnabled = false;
    this.pageMode = false;
    this.defaultSortOrder = { property: 'name', direction: Direction.ASC };

    this.entityTranslations = { noEntities: 'agent.no-profiles-assigned' } as any;
    this.entityResources = {} as any;

    this.handleRowClick = ($event: Event, profile: AgentAppProfile) => {
      if ($event) { $event.stopPropagation(); }
      this.router.navigateByUrl(`/edgeManagement/agentAppProfiles/${profile.id.id}`);
      return true;
    };

    this.columns.push(
      new EntityTableColumn<AgentAppProfile>('name',
        'agent.profile-name', '40%',
        (p) => p.name, () => ({}), true),
      new EntityTableColumn<AgentAppProfile>('appType',
        'agent.app-type', '140px',
        (p) => typeBadge(
          p.appType,
          this.translate.instant(agentApplicationTypeTranslationMap.get(p.appType) || p.appType)
        ),
        () => ({}), true),
      new EntityTableColumn<AgentAppProfile>('description',
        'agent.description', '40%',
        (p) => p.description || '', () => ({}), false)
    );

    this.cellActionDescriptors = this.buildActions();

    this.addEntity = () => {
      this.openAssignDialog();
      return of(null);
    };

    this.entitiesFetchFunction = (pageLink) => this.fetch(pageLink);
  }

  private buildActions(): Array<CellActionDescriptor<AgentAppProfile>> {
    return [
      {
        name: this.translate.instant('agent.unassign-profile'),
        icon: 'link_off',
        isEnabled: () => true,
        onAction: ($event, p) => this.unassign($event, p)
      }
    ];
  }

  private fetch(pageLink: PageLink): Observable<PageData<AgentAppProfile>> {
    return this.agentService.getGroupProfileRelations(this.group.id.id).pipe(
      mergeMap((relations: any[]) => {
        if (!relations || !relations.length) {
          return of([] as AgentAppProfile[]);
        }
        const fetches = relations.map((rel: any) =>
          this.agentService.getAgentAppProfileById(rel.to.id, { ignoreErrors: true } as any).pipe(
            catchError(() => of(null as AgentAppProfile))
          )
        );
        return forkJoin(fetches);
      }),
      map((profiles: AgentAppProfile[]) => {
        const data = (profiles || []).filter(p => !!p);
        return pageLink.filterData(data);
      })
    );
  }

  private openAssignDialog() {
    const pageLink = new PageLink(1024);
    this.agentService.getTenantAgentAppProfiles(pageLink).subscribe((page) => {
      this.agentService.getGroupProfileRelations(this.group.id.id).subscribe((relations: any[]) => {
        const assignedIds = new Set((relations || []).map((r: any) => r.to.id));
        const available = page.data.filter(p => !assignedIds.has(p.id.id));
        this.dialog.open<AgentGroupAssignProfileDialogComponent, AgentGroupAssignProfileDialogData, AgentAppProfile>(
          AgentGroupAssignProfileDialogComponent, {
            disableClose: true,
            panelClass: ['tb-dialog'],
            data: { profiles: available }
          }
        ).afterClosed().subscribe((selected) => {
          if (selected) {
            this.agentService.assignProfileToGroup(this.group.id.id, selected.id.id)
              .subscribe(() => this.updateData());
          }
        });
      });
    });
  }

  private unassign($event: Event, profile: AgentAppProfile) {
    if ($event) { $event.stopPropagation(); }
    this.dialogService.confirm(
      this.translate.instant('agent.unassign-profile'),
      this.translate.instant('agent.unassign-profile-confirm', { profile: profile.name }),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((confirm) => {
      if (confirm) {
        this.agentService.unassignProfileFromGroup(this.group.id.id, profile.id.id)
          .subscribe(() => this.updateData());
      }
    });
  }
}
