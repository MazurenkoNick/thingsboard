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
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { forkJoin, Observable, of } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { EntityId } from '@shared/models/id/entity-id';
import {
  Agent,
  AgentAppEvent,
  AgentAppEventActionType,
  AgentAppEventInfo,
  AgentAppEventRequest,
  AgentAppInstallResponse,
  AgentAppProfile,
  AgentAppProfileInfo,
  AgentAppProfileRelationInfo,
  AgentAppTemplate,
  AgentAppUnit,
  AgentApplication,
  AgentApplicationInfo,
  AgentBulkAction,
  AgentInstructions,
  AgentProfile,
  AgentProfileInfo,
  AgentInfo,
  BulkOperationPreview,
  BulkOperationRequest,
  BulkOperationResult
} from '@shared/models/agent.models';

@Injectable({
  providedIn: 'root'
})
export class AgentService {

  constructor(
    private http: HttpClient
  ) {
  }

  // --- Agent CRUD ---

  public getAgentById(agentId: string, config?: RequestConfig): Observable<Agent> {
    return this.http.get<Agent>(`/api/agent/${agentId}`, defaultHttpOptionsFromConfig(config));
  }

  public getAgentInfoById(agentId: string, config?: RequestConfig): Observable<AgentInfo> {
    return this.http.get<AgentInfo>(`/api/agent/info/${agentId}`, defaultHttpOptionsFromConfig(config));
  }

  public getAgentInstallInstructions(agentId: string, method: string = 'docker', config?: RequestConfig): Observable<AgentInstructions> {
    return this.http.get<AgentInstructions>(`/api/agent/instructions/install/${agentId}/${method}`, defaultHttpOptionsFromConfig(config));
  }

  public getAgentProvisionInstructions(agentProfileId: string, method: string = 'docker', config?: RequestConfig): Observable<AgentInstructions> {
    return this.http.get<AgentInstructions>(`/api/agent/profile/instructions/provision/${agentProfileId}/${method}`, defaultHttpOptionsFromConfig(config));
  }

  public saveAgent(agent: Agent, entityGroupIds?: string | string[], config?: RequestConfig): Observable<Agent> {
    let url = '/api/agent';
    if (entityGroupIds) {
      const ids = Array.isArray(entityGroupIds) ? entityGroupIds.join(',') : entityGroupIds;
      if (ids) {
        url += `?entityGroupIds=${ids}`;
      }
    }
    return this.http.post<Agent>(url, agent, defaultHttpOptionsFromConfig(config));
  }

  public deleteAgent(agentId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/agent/${agentId}`, defaultHttpOptionsFromConfig(config));
  }

  public getTenantAgentInfos(pageLink: PageLink, config?: RequestConfig): Observable<PageData<AgentInfo>> {
    return this.http.get<PageData<AgentInfo>>(`/api/tenant/agentInfos${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getCustomerAgentInfos(customerId: string, pageLink: PageLink, config?: RequestConfig): Observable<PageData<AgentInfo>> {
    return this.http.get<PageData<AgentInfo>>(`/api/customer/${customerId}/agentInfos${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  // --- Agent Profile CRUD ---

  public getAgentProfileById(agentProfileId: string, config?: RequestConfig): Observable<AgentProfile> {
    return this.http.get<AgentProfile>(`/api/agent/profile/${agentProfileId}`, defaultHttpOptionsFromConfig(config));
  }

  public getAgentProfileInfoById(agentProfileId: string, config?: RequestConfig): Observable<AgentProfileInfo> {
    return this.http.get<AgentProfileInfo>(`/api/agent/profile/info/${agentProfileId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveAgentProfile(agentProfile: AgentProfile, config?: RequestConfig, appProfileIds?: string[]): Observable<AgentProfile> {
    let url = '/api/agent/profile';
    if (appProfileIds && appProfileIds.length) {
      const params = appProfileIds.map(id => `appProfileIds=${encodeURIComponent(id)}`).join('&');
      url = `${url}?${params}`;
    }
    return this.http.post<AgentProfile>(url, agentProfile, defaultHttpOptionsFromConfig(config));
  }

  public deleteAgentProfile(agentProfileId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/agent/profile/${agentProfileId}`, defaultHttpOptionsFromConfig(config));
  }

  public setDefaultAgentProfile(agentProfileId: string, config?: RequestConfig): Observable<AgentProfile> {
    return this.http.post<AgentProfile>(`/api/agent/profile/${agentProfileId}/default`, null,
      defaultHttpOptionsFromConfig(config));
  }

  public getDefaultAgentProfileInfo(config?: RequestConfig): Observable<AgentProfileInfo> {
    return this.http.get<AgentProfileInfo>('/api/agent/profile/info/default', defaultHttpOptionsFromConfig(config));
  }

  public getTenantAgentProfiles(pageLink: PageLink, config?: RequestConfig): Observable<PageData<AgentProfile>> {
    return this.http.get<PageData<AgentProfile>>(`/api/tenant/agent/profiles${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getTenantAgentProfileInfos(pageLink: PageLink, config?: RequestConfig): Observable<PageData<AgentProfileInfo>> {
    return this.http.get<PageData<AgentProfileInfo>>(`/api/tenant/agent/profileInfos${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAgentProfileAppProfileInfos(agentProfileId: string, config?: RequestConfig): Observable<AgentAppProfileRelationInfo[]> {
    return this.http.get<AgentAppProfileRelationInfo[]>(`/api/agent/profile/${agentProfileId}/appProfilesInfo`,
      defaultHttpOptionsFromConfig(config));
  }

  public assignAppProfileToAgentProfile(agentProfileId: string, applicationProfileId: string, config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/agent/profile/${agentProfileId}/appProfile/${applicationProfileId}`, null,
      defaultHttpOptionsFromConfig(config));
  }

  public assignAppProfilesToAgentProfile(agentProfileId: string, appProfileIds: string[], config?: RequestConfig): Observable<void> {
    const params = appProfileIds.map(id => `appProfileIds=${encodeURIComponent(id)}`).join('&');
    return this.http.post<void>(`/api/agent/profile/${agentProfileId}/appProfiles/assign?${params}`, null,
      defaultHttpOptionsFromConfig(config));
  }

  public unassignAppProfileFromAgentProfile(agentProfileId: string, applicationProfileId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/agent/profile/${agentProfileId}/appProfile/${applicationProfileId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public setAppProfileRelatesOnAutoDiscovery(agentProfileId: string, applicationProfileId: string, relate: boolean,
                                             config?: RequestConfig): Observable<void> {
    return this.http.post<void>(
      `/api/agent/profile/${agentProfileId}/appProfile/${applicationProfileId}/autoDiscovery?relate=${relate}`, null,
      defaultHttpOptionsFromConfig(config));
  }

  public previewBulkOperation(agentProfileId: string, applicationProfileId: string, request: BulkOperationRequest,
                              config?: RequestConfig): Observable<BulkOperationPreview> {
    return this.http.post<BulkOperationPreview>(
      `/api/agent/profile/${agentProfileId}/appProfile/${applicationProfileId}/bulk/preview`, request,
      defaultHttpOptionsFromConfig(config));
  }

  public bulkOperation(agentProfileId: string, applicationProfileId: string, request: BulkOperationRequest,
                       config?: RequestConfig): Observable<BulkOperationResult> {
    return this.http.post<BulkOperationResult>(
      `/api/agent/profile/${agentProfileId}/appProfile/${applicationProfileId}/bulk`, request,
      defaultHttpOptionsFromConfig(config));
  }

  // --- Agent Application ---

  public getAgentApplicationById(applicationId: string, config?: RequestConfig): Observable<AgentApplicationInfo> {
    return this.http.get<AgentApplicationInfo>(`/api/agent/app/${applicationId}`, defaultHttpOptionsFromConfig(config));
  }

  public getAgentApplicationInfoById(applicationId: string, config?: RequestConfig): Observable<AgentApplicationInfo> {
    return this.http.get<AgentApplicationInfo>(`/api/agent/app/${applicationId}`, defaultHttpOptionsFromConfig(config));
  }

  public getAgentApplicationsByAgentId(agentId: string, pageLink: PageLink, config?: RequestConfig): Observable<PageData<AgentApplicationInfo>> {
    return this.http.get<PageData<AgentApplicationInfo>>(`/api/agent/${agentId}/apps${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }


  public getAgentApplicationByRelatedEntity(entityType: string, entityId: string, config?: RequestConfig): Observable<AgentApplication> {
    return this.http.get<AgentApplication>(`/api/agent/apps/${entityType}/${entityId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public updateAgentApplication(application: AgentApplication, config?: RequestConfig): Observable<AgentApplication> {
    return this.http.put<AgentApplication>('/api/agent/app', application, defaultHttpOptionsFromConfig(config));
  }

  public installAgentApp(request: AgentAppEventRequest, config?: RequestConfig): Observable<AgentAppInstallResponse> {
    return this.http.post<AgentAppInstallResponse>('/api/agent/app/event', request, defaultHttpOptionsFromConfig(config));
  }

  public createAgentAppEvent(applicationId: string, request: AgentAppEventRequest, config?: RequestConfig): Observable<AgentAppEvent> {
    return this.http.post<AgentAppEvent>(`/api/agent/app/${applicationId}/event`, request, defaultHttpOptionsFromConfig(config));
  }

  public cancelAgentAppEvent(applicationId: string, eventId: string, config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/agent/app/${applicationId}/event/${eventId}/cancel`, null,
      defaultHttpOptionsFromConfig(config));
  }

  public getAgentAppEventById(eventId: string, config?: RequestConfig): Observable<AgentAppEvent> {
    return this.http.get<AgentAppEvent>(`/api/agent/app/event/${eventId}`, defaultHttpOptionsFromConfig(config));
  }

  public getAgentAppEvents(applicationId: string, pageLink: PageLink,
                           actionType?: string, status?: string,
                           config?: RequestConfig): Observable<PageData<AgentAppEvent>> {
    let url = `/api/agent/app/${applicationId}/events${pageLink.toQuery()}`;
    if (actionType) {
      url += `&actionType=${actionType}`;
    }
    if (status) {
      url += `&status=${status}`;
    }
    return this.http.get<PageData<AgentAppEvent>>(url, defaultHttpOptionsFromConfig(config));
  }

  public getAgentAppEventsByAgentId(agentId: string, pageLink: PageLink, config?: RequestConfig): Observable<PageData<AgentAppEvent>> {
    return this.http.get<PageData<AgentAppEvent>>(`/api/agent/${agentId}/events${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAgentAppEventInfosByAgentId(agentId: string, pageLink: PageLink,
                                        actionType?: string, status?: string,
                                        config?: RequestConfig): Observable<PageData<AgentAppEventInfo>> {
    let url = `/api/agent/${agentId}/eventInfos${pageLink.toQuery()}`;
    if (actionType) {
      url += `&actionType=${actionType}`;
    }
    if (status) {
      url += `&status=${status}`;
    }
    return this.http.get<PageData<AgentAppEventInfo>>(url, defaultHttpOptionsFromConfig(config));
  }

  public mergeForPreview(templateId: string, application?: AgentApplication,
                          composeType?: string, relatedEntityId?: EntityId,
                          actionType?: AgentAppEventActionType,
                          setHostValues?: boolean,
                          config?: RequestConfig): Observable<AgentApplication> {
    let url = `/api/agent/app/merge/${templateId}/preview`;
    const params: string[] = [];
    if (composeType) {
      params.push(`composeType=${encodeURIComponent(composeType)}`);
    }
    if (actionType) {
      params.push(`actionType=${encodeURIComponent(actionType)}`);
    }
    if (setHostValues) {
      params.push('setHostValues=true');
    }
    if (relatedEntityId?.entityType && relatedEntityId?.id) {
      params.push(`relatedEntityType=${encodeURIComponent(relatedEntityId.entityType)}`);
      params.push(`relatedEntityId=${encodeURIComponent(relatedEntityId.id)}`);
    }
    if (params.length) {
      url += `?${params.join('&')}`;
    }
    return this.http.post<AgentApplication>(url, application || null, defaultHttpOptionsFromConfig(config));
  }

  public getManagedRelatedEntityIds(entityType: string, config?: RequestConfig): Observable<EntityId[]> {
    return this.http.get<EntityId[]>(
      `/api/agent/app/managedRelatedEntities/${entityType}`, defaultHttpOptionsFromConfig(config));
  }

  public assignRelatedEntity(applicationId: string, relatedEntityId: EntityId,
                             config?: RequestConfig): Observable<AgentApplication> {
    return this.http.post<AgentApplication>(
      `/api/agent/app/${applicationId}/relatedEntity/${relatedEntityId.entityType}/${relatedEntityId.id}`,
      null, defaultHttpOptionsFromConfig(config));
  }

  public unassignRelatedEntity(applicationId: string, config?: RequestConfig): Observable<AgentApplication> {
    return this.http.delete<AgentApplication>(
      `/api/agent/app/${applicationId}/relatedEntity`, defaultHttpOptionsFromConfig(config));
  }

  public detachFromProfile(applicationId: string, config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/agent/app/${applicationId}/detach`, null, defaultHttpOptionsFromConfig(config));
  }

  public attachToProfile(applicationId: string, profileId: string, config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/agent/app/${applicationId}/attach/${profileId}`, null,
      defaultHttpOptionsFromConfig(config));
  }

  // --- Agent App Template (read-only) ---

  public getAgentAppTemplateById(templateId: string, config?: RequestConfig): Observable<AgentAppTemplate> {
    return this.http.get<AgentAppTemplate>(`/api/agent/app/template/${templateId}`, defaultHttpOptionsFromConfig(config));
  }

  public getLatestAgentAppTemplate(appType: string, configType: string, config?: RequestConfig): Observable<AgentAppTemplate> {
    return this.http.get<AgentAppTemplate>(`/api/agent/app/template/${appType}/${configType}/latest`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAgentAppTemplateByVersion(appType: string, configType: string, version: string, config?: RequestConfig): Observable<AgentAppTemplate> {
    return this.http.get<AgentAppTemplate>(`/api/agent/app/template/${appType}/${configType}/${version}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAgentAppTemplates(config?: RequestConfig): Observable<AgentAppTemplate[]> {
    return this.http.get<AgentAppTemplate[]>('/api/agent/app/templates',
      defaultHttpOptionsFromConfig(config));
  }

  public getAgentAppProfilesByAppType(appType: string, config?: RequestConfig): Observable<AgentAppProfileInfo[]> {
    return this.http.get<AgentAppProfileInfo[]>(`/api/agent/app/profiles/${appType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAgentAppTemplatesByAppType(appType: string, configType: string = 'DOCKER_COMPOSE', config?: RequestConfig): Observable<AgentAppTemplate[]> {
    return this.http.get<AgentAppTemplate[]>(`/api/agent/app/templates/${appType}/${configType}`,
      defaultHttpOptionsFromConfig(config));
  }

  // --- Agent App Profile ---

  public getAgentAppProfileById(profileId: string, config?: RequestConfig): Observable<AgentAppProfile> {
    return this.http.get<AgentAppProfile>(`/api/agent/app/profile/${profileId}`, defaultHttpOptionsFromConfig(config));
  }

  public getAgentAppProfileInfoById(profileId: string, config?: RequestConfig): Observable<AgentAppProfileInfo> {
    return this.http.get<AgentAppProfileInfo>(`/api/agent/app/profile/info/${profileId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveAgentAppProfile(profile: AgentAppProfile, config?: RequestConfig): Observable<AgentAppProfile> {
    return this.http.post<AgentAppProfile>('/api/agent/app/profile', profile, defaultHttpOptionsFromConfig(config));
  }

  public deleteAgentAppProfile(profileId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/agent/app/profile/${profileId}`, defaultHttpOptionsFromConfig(config));
  }

  public getTenantAgentAppProfiles(pageLink: PageLink, config?: RequestConfig): Observable<PageData<AgentAppProfile>> {
    return this.http.get<PageData<AgentAppProfile>>(`/api/tenant/agent/app/profiles${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAgentAppProfilesByIds(profileIds: string[], config?: RequestConfig): Observable<AgentAppProfile[]> {
    if (!profileIds?.length) {
      return of([]);
    }
    return forkJoin(profileIds.map(id => this.getAgentAppProfileById(id, config)));
  }

  public mergeProfileForPreview(templateId: string, profile?: AgentAppProfile,
                                composeType?: string, actionType?: AgentAppEventActionType,
                                config?: RequestConfig): Observable<AgentAppProfile> {
    let url = `/api/agent/app/profiles/merge/${templateId}/preview`;
    const params: string[] = [];
    if (composeType) {
      params.push(`composeType=${encodeURIComponent(composeType)}`);
    }
    if (actionType) {
      params.push(`actionType=${encodeURIComponent(actionType)}`);
    }
    if (params.length) {
      url += `?${params.join('&')}`;
    }
    return this.http.post<AgentAppProfile>(url, profile || null, defaultHttpOptionsFromConfig(config));
  }

  // --- Agent App Unit (read-only) ---

  public getAgentAppUnits(applicationId: string, pageLink: PageLink,
                          type?: string,
                          config?: RequestConfig): Observable<PageData<AgentAppUnit>> {
    let url = `/api/agent/app/${applicationId}/units${pageLink.toQuery()}`;
    if (type) {
      url += `&type=${type}`;
    }
    return this.http.get<PageData<AgentAppUnit>>(url, defaultHttpOptionsFromConfig(config));
  }

  // --- Agent Bulk Action ---

  public getAgentBulkAction(bulkActionId: string, config?: RequestConfig): Observable<AgentBulkAction> {
    return this.http.get<AgentBulkAction>(`/api/agent/bulk/${bulkActionId}`, defaultHttpOptionsFromConfig(config));
  }

  public getAgentProfileBulkActions(agentProfileId: string, pageLink: PageLink,
                             config?: RequestConfig): Observable<PageData<AgentBulkAction>> {
    return this.http.get<PageData<AgentBulkAction>>(
      `/api/agent/profile/${agentProfileId}/bulk${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAgentProfileAppProfileBulkActions(agentProfileId: string, applicationProfileId: string,
                                              pageLink: PageLink,
                                              config?: RequestConfig): Observable<PageData<AgentBulkAction>> {
    return this.http.get<PageData<AgentBulkAction>>(
      `/api/agent/profile/${agentProfileId}/appProfile/${applicationProfileId}/bulk${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAgentBulkActionEvents(bulkActionId: string, pageLink: PageLink,
                                  actionType?: string, status?: string,
                                  config?: RequestConfig): Observable<PageData<AgentAppEvent>> {
    let url = `/api/agent/bulk/${bulkActionId}/events${pageLink.toQuery()}`;
    if (actionType) {
      url += `&actionType=${actionType}`;
    }
    if (status) {
      url += `&status=${status}`;
    }
    return this.http.get<PageData<AgentAppEvent>>(url, defaultHttpOptionsFromConfig(config));
  }
}
