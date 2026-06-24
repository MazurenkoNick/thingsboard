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
import { Observable, of, throwError } from 'rxjs';
import { map, mergeMap, tap } from 'rxjs/operators';
import { AgentService } from '@core/http/agent.service';
import { EntityId } from '@shared/models/id/entity-id';
import {
  AgentApplication,
  AgentAppEventActionType,
  AgentAppProfile,
  AgentApplicationType,
  AgentAppTemplate
} from '@shared/models/agent.models';

export interface UpgradeTemplateResult {
  template: AgentAppTemplate;
  // Resolved from the linked template's currentVersion; null on the
  // desiredTemplateId path where ngOnInit's seeded value must be kept.
  fromVersion: string | null;
}

// Thrown by resolveUpgradeTemplate for known, non-HTTP failures so the caller
// can surface the matching translated message instead of a generic error.
export interface UpgradeResolveError {
  messageKey: string;
}

// Per-wizard-instance loader: owns the template/profile caches and wraps the
// AgentService IO the install/update/upgrade flows perform. Provided at the
// dispatcher level so caches are shared across the active flow but don't leak
// across separate wizard openings.
@Injectable()
export class AgentAppWizardLoaderService {

  private templateCache = new Map<AgentApplicationType, AgentAppTemplate>();
  private templateByIdCache = new Map<string, AgentAppTemplate>();
  private profilesCache = new Map<AgentApplicationType, AgentAppProfile[]>();

  constructor(private agentService: AgentService) {}

  // Warm the per-type template cache so the first type-card click in install
  // mode doesn't flash a "Loading template…" state.
  prewarmTemplates(types: AgentApplicationType[]): void {
    types.forEach(type => {
      if (this.templateCache.has(type)) {
        return;
      }
      this.agentService.getLatestAgentAppTemplate(type, 'DOCKER_COMPOSE').subscribe({
        next: tpl => this.templateCache.set(type, tpl),
        error: () => { /* swallow — loadTemplate retries on demand */ }
      });
    });
  }

  loadTemplate(type: AgentApplicationType): Observable<AgentAppTemplate> {
    const cached = this.templateCache.get(type);
    if (cached) {
      return of(cached);
    }
    return this.agentService.getLatestAgentAppTemplate(type, 'DOCKER_COMPOSE').pipe(
      tap(tpl => this.templateCache.set(type, tpl))
    );
  }

  loadTemplateById(id: string): Observable<AgentAppTemplate> {
    const cached = this.templateByIdCache.get(id);
    if (cached) {
      return of(cached);
    }
    return this.agentService.getAgentAppTemplateById(id).pipe(
      tap(tpl => this.templateByIdCache.set(tpl.id.id, tpl))
    );
  }

  loadProfiles(type: AgentApplicationType): Observable<AgentAppProfile[]> {
    const cached = this.profilesCache.get(type);
    if (cached) {
      return of(cached);
    }
    return this.agentService.getAgentAppProfilesByAppType(type).pipe(
      tap(profiles => this.profilesCache.set(type, profiles as any))
    ) as Observable<AgentAppProfile[]>;
  }

  cacheProfiles(type: AgentApplicationType, profiles: AgentAppProfile[]): void {
    this.profilesCache.set(type, profiles);
  }

  // Reads the linked profile via the info-by-id endpoint, which is gated by
  // tenant ownership only (no per-entity AGENT_APP_PROFILE permission check),
  // so roles with AGENT but not AGENT_APP_PROFILE access can still load it.
  loadProfileById(id: string): Observable<AgentAppProfile> {
    return this.agentService.getAgentAppProfileInfoById(id);
  }

  merge(templateId: string, draft: AgentApplication, composeType?: string,
        relatedEntityId?: EntityId, actionType?: AgentAppEventActionType): Observable<AgentApplication> {
    return this.agentService.mergeForPreview(templateId, draft, composeType, relatedEntityId, actionType);
  }

  loadManagedApp(entityType: string, entityId: string): Observable<AgentApplication | null> {
    return this.agentService.getAgentApplicationByRelatedEntity(
      entityType, entityId, { ignoreErrors: true, ignoreLoading: true } as any
    ).pipe(map(app => app || null));
  }

  // Resolves the single-hop upgrade target. Follows the template's nextVersion
  // pointer rather than jumping to "latest" so intermediate versions (and their
  // upgradeSteps / migrations) aren't skipped on multi-hop chains.
  resolveUpgradeTemplate(app: AgentApplication): Observable<UpgradeTemplateResult> {
    const desiredId = (app as any).desiredTemplateId?.id;
    if (desiredId) {
      return this.loadTemplateById(desiredId).pipe(
        map(template => ({ template, fromVersion: null }))
      );
    }
    if (!app.templateId?.id) {
      return throwError(() => ({ messageKey: 'agent.app-upgrade-no-template' } as UpgradeResolveError));
    }
    return this.agentService.getAgentAppTemplateById(app.templateId.id).pipe(
      mergeMap(current => {
        if (!current.nextVersion) {
          return throwError(() => ({ messageKey: 'agent.app-upgrade-no-next-version' } as UpgradeResolveError));
        }
        const fromVersion = current.currentVersion || null;
        const configType = current.config?.type || 'DOCKER_COMPOSE';
        return this.agentService.getAgentAppTemplateByVersion(
          current.appType, configType, current.nextVersion
        ).pipe(map(template => ({ template, fromVersion })));
      })
    );
  }
}
