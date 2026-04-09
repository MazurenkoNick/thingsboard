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

import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import {
  CellActionDescriptor,
  EntityTableColumn,
  EntityTableConfig,
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { Observable, of } from 'rxjs';
import { map, mergeMap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import {
  AgentApplicationInfo,
  AgentAppEventActionType,
  AgentInfo
} from '@shared/models/agent.models';
import { AgentService } from '@core/http/agent.service';
import { AgentApplicationComponent } from '@home/pages/agent/agent-application.component';
import { AgentApplicationTabsComponent } from '@home/pages/agent/agent-application-tabs.component';
import {
  AgentAppDeleteDialogComponent,
  AgentAppDeleteDialogData
} from '@home/pages/agent/dialog/agent-app-delete-dialog.component';
import {
  AgentAppInstallWizardComponent,
  AgentAppInstallWizardData
} from '@home/pages/agent/wizard/agent-app-install-wizard.component';

@Injectable()
export class AgentApplicationsTableConfigResolver {

  private readonly config: EntityTableConfig<AgentApplicationInfo> = new EntityTableConfig<AgentApplicationInfo>();
  private agentId: string;
  private agent: AgentInfo;

  constructor(private agentService: AgentService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private dialog: MatDialog,
              private dialogService: DialogService) {

    this.config.entityType = EntityType.AGENT_APPLICATION;
    this.config.entityComponent = AgentApplicationComponent;
    this.config.entityTabsComponent = AgentApplicationTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.AGENT_APPLICATION);
    this.config.entityResources = entityTypeResources.get(EntityType.AGENT_APPLICATION);
    this.config.addEnabled = true;
    this.config.entitiesDeleteEnabled = false;
    this.config.selectionEnabled = false;
    this.config.loadEntity = id => this.agentService.getAgentApplicationInfoById(id.id);
    this.config.saveEntity = (app: any) => this.agentService.updateAgentApplication(app)
      .pipe(mergeMap((saved: any) => this.agentService.getAgentApplicationInfoById(saved.id.id)));
    this.config.addEntity = () => { this.openInstallWizard(); return of(null); };
    this.config.handleRowClick = ($event: Event, app) => {
      if ($event) { $event.stopPropagation(); }
      this.router.navigateByUrl(`/edgeManagement/agents/${this.agentId}/applications/${app.id.id}`);
      return true;
    };
    this.config.onEntityAction = (action) => {
      if (action.action === 'open') {
        this.router.navigateByUrl(`/edgeManagement/agents/${this.agentId}/applications/${action.entity.id.id}`);
        return true;
      }
      return false;
    };
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<AgentApplicationInfo>> {
    this.agentId = route.params.agentId;
    return this.agentService.getAgentInfoById(this.agentId).pipe(
      map(agent => {
        this.agent = agent;
        this.config.tableTitle = agent.name + ': ' + this.translate.instant('agent.applications');
        this.config.componentsData = { agentId: this.agentId, agent };
        this.config.columns = this.configureColumns();
        this.config.cellActionDescriptors = this.configureCellActions();
        this.config.entitiesFetchFunction = pageLink =>
          this.agentService.getAgentApplicationsByAgentId(this.agentId, pageLink);
        return this.config;
      })
    );
  }

  private configureColumns(): Array<EntityTableColumn<AgentApplicationInfo>> {
    return [
      new EntityTableColumn<AgentApplicationInfo>('name', 'agent.app-name', '30%'),
      new EntityTableColumn<AgentApplicationInfo>('appType', 'agent.app-type', '140px',
        e => this.appTypeBadge(e.appType), () => ({}), false),
      new EntityTableColumn<AgentApplicationInfo>('currentVersion', 'agent.app-template', '25%',
        e => this.templateCell(e), () => ({}), false),
      new EntityTableColumn<AgentApplicationInfo>('origin', 'agent.app-origin', '140px',
        e => this.originBadge((e as any).origin), () => ({}), false),
    ];
  }

  private originBadge(origin: string | undefined): string {
    if (!origin) {
      return `<span style="color:rgba(0,0,0,0.38);font-size:12px;">—</span>`;
    }
    const styles: Record<string, string> = {
      INSTALLED: 'background:#e8f5e9;color:#2e7d32;',
      DISCOVERED: 'background:#e3f2fd;color:#1565c0;',
      AUTO_PROVISIONED: 'background:#f3e5f5;color:#6a1b9a;'
    };
    const style = styles[origin] || 'background:#eeeeee;color:#616161;';
    const label = origin.charAt(0) + origin.slice(1).toLowerCase().replace('_', ' ');
    return `<span style="display:inline-flex;align-items:center;padding:2px 10px;border-radius:12px;font-size:11px;font-weight:600;letter-spacing:0.5px;${style}">${label}</span>`;
  }

  private appTypeBadge(appType: string): string {
    const styles: Record<string, string> = {
      EDGE: 'background:#e8eaf6;color:#283593;',
      GATEWAY: 'background:#e0f2f1;color:#00695c;',
      GENERIC: 'background:#f3e5f5;color:#6a1b9a;'
    };
    const style = styles[appType] || 'background:#eeeeee;color:#616161;';
    return `<span style="display:inline-flex;align-items:center;padding:2px 10px;border-radius:12px;font-size:11px;font-weight:600;letter-spacing:0.5px;${style}">${appType}</span>`;
  }

  private templateCell(e: AgentApplicationInfo): string {
    if (!e.currentVersion) {
      return `<span style="font-family:'Roboto Mono',monospace;font-size:12px;color:rgba(0,0,0,0.38);">—</span>`;
    }
    return `<span style="font-family:'Roboto Mono',monospace;font-size:12px;">${e.currentVersion}</span>`;
  }

  private configureCellActions(): Array<CellActionDescriptor<AgentApplicationInfo>> {
    return [
      {
        name: this.translate.instant('agent.app-restart'),
        icon: 'restart_alt',
        isEnabled: () => true,
        onAction: ($event, e) => this.restart($event, e)
      },
      {
        name: this.translate.instant('agent.app-update'),
        icon: 'sync_alt',
        isEnabled: () => true,
        onAction: ($event, e) => this.update($event, e)
      },
      {
        name: this.translate.instant('agent.app-upgrade'),
        icon: 'arrow_upward',
        isEnabled: e => !!e.nextVersion,
        onAction: ($event, e) => this.openUpgradeWizard($event, e)
      },
      {
        name: this.translate.instant('agent.app-delete'),
        icon: 'delete',
        isEnabled: () => true,
        onAction: ($event, e) => this.openDeleteDialog($event, e)
      }
    ];
  }

  private update($event: Event, app: AgentApplicationInfo) {
    if ($event) { $event.stopPropagation(); }
    this.agentService.getAgentApplicationById(app.id.id).subscribe(full => {
      this.dialog.open<AgentAppInstallWizardComponent, AgentAppInstallWizardData, boolean>(
        AgentAppInstallWizardComponent, {
          disableClose: false,
          panelClass: ['tb-dialog'],
          data: {
            agentId: this.agentId,
            agent: this.agent,
            mode: 'update',
            application: full
          }
        }
      ).afterClosed().subscribe(confirmed => {
        if (confirmed) {
          this.config.updateData();
        }
      });
    });
  }

  private restart($event: Event, app: AgentApplicationInfo) {
    if ($event) { $event.stopPropagation(); }
    this.dialogService.confirm(
      this.translate.instant('agent.app-restart-title', { name: app.name }),
      this.translate.instant('agent.app-restart-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe(res => {
      if (res) {
        this.agentService.createAgentAppEvent(app.id.id, {
          actionType: AgentAppEventActionType.RESTART
        }).subscribe(() => this.config.updateData());
      }
    });
  }

  private openDeleteDialog($event: Event, app: AgentApplicationInfo) {
    if ($event) { $event.stopPropagation(); }
    // Fetch the full application so the dialog can render volume keys from
    // config.compose. Falls back to the list-row entity if the fetch fails.
    this.agentService.getAgentApplicationById(app.id.id).subscribe({
      next: full => this.showDeleteDialog(full),
      error: () => this.showDeleteDialog(app)
    });
  }

  private showDeleteDialog(application: any) {
    this.dialog.open<AgentAppDeleteDialogComponent, AgentAppDeleteDialogData, boolean>(
      AgentAppDeleteDialogComponent, {
        disableClose: false,
        panelClass: ['tb-dialog'],
        data: {
          application,
          agentName: this.agent?.name
        }
      }
    ).afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.config.updateData();
      }
    });
  }

  private openUpgradeWizard($event: Event, app: AgentApplicationInfo) {
    if ($event) { $event.stopPropagation(); }
    this.agentService.getAgentApplicationById(app.id.id).subscribe({
      next: full => this.showUpgradeWizard(full),
      error: () => this.showUpgradeWizard(app as any)
    });
  }

  private showUpgradeWizard(application: any) {
    this.dialog.open<AgentAppInstallWizardComponent, AgentAppInstallWizardData, boolean>(
      AgentAppInstallWizardComponent, {
        disableClose: false,
        panelClass: ['tb-dialog'],
        data: {
          agentId: this.agentId,
          agent: this.agent,
          mode: 'upgrade',
          application
        }
      }
    ).afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.config.updateData();
      }
    });
  }

  private openInstallWizard() {
    this.dialog.open<AgentAppInstallWizardComponent, AgentAppInstallWizardData, boolean>(
      AgentAppInstallWizardComponent, {
        disableClose: false,
        panelClass: ['tb-dialog'],
        data: { agentId: this.agentId, agent: this.agent }
      }
    ).afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.config.updateData();
      }
    });
  }
}
