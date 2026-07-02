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

import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig,
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { Observable, of } from 'rxjs';
import { select, Store } from '@ngrx/store';
import { selectAuthUser } from '@core/auth/auth.selectors';
import { catchError, map, switchMap, take, tap } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';
import { AgentAppProfile, AgentAppTemplate } from '@shared/models/agent.models';
import { versionTag } from '@home/pages/agent/util/version-tag';
import { AgentService } from '@core/http/agent.service';
import { AgentAppProfileComponent } from '@home/pages/agent/agent-app-profile.component';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import {
  AgentAppProfileWizardComponent,
  AgentAppProfileWizardData
} from '@home/pages/agent/wizard/agent-app-profile-wizard.component';

@Injectable()
export class AgentAppProfilesTableConfigResolver {

  private readonly config: EntityTableConfig<AgentAppProfile> = new EntityTableConfig<AgentAppProfile>();
  private templateCache = new Map<string, AgentAppTemplate>();

  constructor(private store: Store<AppState>,
              private agentService: AgentService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private dialog: MatDialog,
              private dialogService: DialogService) {

    this.config.entityType = EntityType.AGENT_APP_PROFILE;
    this.config.entityComponent = AgentAppProfileComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.AGENT_APP_PROFILE);
    this.config.entityResources = entityTypeResources.get(EntityType.AGENT_APP_PROFILE);
    this.config.rowPointer = true;

    this.config.deleteEntityTitle = profile => this.translate.instant('agent.delete-app-profile-title', {profileName: profile.name});
    this.config.deleteEntityContent = () => this.translate.instant('agent.delete-app-profile-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('agent.delete-app-profiles-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('agent.delete-app-profiles-text');

    this.config.loadEntity = id => this.agentService.getAgentAppProfileById(id.id);
    this.config.saveEntity = profile => this.agentService.saveAgentAppProfile(profile).pipe(
      tap(saved => {
        if (saved?.id?.id) {
          this.showAssignedAppsPropagationHint();
        }
      })
    );
    this.config.onEntityAction = action => this.onProfileAction(action);
    this.config.addEntity = () => { this.openProfileWizard(); return of(null); };
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<AgentAppProfile>> {
    return this.store.pipe(select(selectAuthUser), take(1)).pipe(
      map((authUser) => {
        this.config.tableTitle = this.translate.instant('agent.app-profiles');
        this.config.columns = this.configureColumns();
        this.config.entitiesFetchFunction = pageLink =>
          this.agentService.getTenantAgentAppProfiles(pageLink).pipe(
            switchMap(page => this.enrichWithTemplates(page))
          );
        this.config.deleteEntity = id => this.agentService.deleteAgentAppProfile(id.id);
        this.config.addEnabled = authUser.authority === Authority.TENANT_ADMIN;
        this.config.entitiesDeleteEnabled = authUser.authority === Authority.TENANT_ADMIN;
        this.config.deleteEnabled = () => authUser.authority === Authority.TENANT_ADMIN;
        this.config.detailsReadonly = () => authUser.authority !== Authority.TENANT_ADMIN;
        return this.config;
      })
    );
  }

  configureColumns(): Array<EntityTableColumn<AgentAppProfile>> {
    return [
      new DateEntityTableColumn<AgentAppProfile>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<AgentAppProfile>('name', 'agent.name', '30%'),
      new EntityTableColumn<AgentAppProfile>('appType', 'agent.app-type', '180px',
        entity => this.appTypeBadge(entity.appType), () => ({}), false),
      new EntityTableColumn<AgentAppProfile>('templateId', 'agent.template', '30%',
        entity => this.templateCell(entity), () => ({}), false),
    ];
  }

  private templateCell(entity: AgentAppProfile): string {
    if (!entity.templateId) {
      return versionTag(null);
    }
    const template = this.templateCache.get(entity.templateId.id);
    return versionTag(template?.currentVersion);
  }

  private enrichWithTemplates(page: any): Observable<any> {
    const hasMissing = page.data.some((p: AgentAppProfile) =>
      p.templateId?.id && !this.templateCache.has(p.templateId.id)
    );
    if (!hasMissing) {
      return of(page);
    }
    return this.agentService.getAgentAppTemplates().pipe(
      map(templates => {
        templates.forEach(t => this.templateCache.set(t.id.id, t));
        return page;
      }),
      catchError(() => of(page))
    );
  }

  private appTypeBadge(appType: string): string {
    const typeClasses: Record<string, string> = {
      EDGE: 'background:#e8eaf6;color:#283593;',
      GATEWAY: 'background:#e0f2f1;color:#00695c;',
      GENERIC: 'background:#f3e5f5;color:#6a1b9a;'
    };
    const style = typeClasses[appType] || 'background:#eeeeee;color:#616161;';
    return `<span style="display:inline-flex;align-items:center;padding:2px 10px;border-radius:12px;font-size:11px;font-weight:600;letter-spacing:0.5px;${style}">${appType}</span>`;
  }

  private openProfileWizard() {
    this.dialog.open<AgentAppProfileWizardComponent, AgentAppProfileWizardData, AgentAppProfile>(
      AgentAppProfileWizardComponent, {
        disableClose: false,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {}
      }
    ).afterClosed().subscribe(saved => {
      if (saved) {
        this.templateCache.clear();
        this.config.updateData();
      }
    });
  }

  private showAssignedAppsPropagationHint() {
    this.dialogService.alert(
      this.translate.instant('agent.app-profile-saved-title'),
      this.translate.instant('agent.app-profile-saved-propagation-text')
    );
  }

  onProfileAction(action: EntityAction<AgentAppProfile>): boolean {
    return false;
  }
}
