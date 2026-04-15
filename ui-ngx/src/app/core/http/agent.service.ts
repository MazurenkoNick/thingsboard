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
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import {
  Agent,
  AgentAppEvent,
  AgentAppEventInfo,
  AgentAppEventRequest,
  AgentAppProfile,
  AgentAppTemplate,
  AgentAppUnit,
  AgentApplication,
  AgentApplicationInfo,
  AgentBulkAction,
  AgentGroup,
  AgentGroupInfo,
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

  public saveAgent(agent: Agent, config?: RequestConfig): Observable<Agent> {
    return this.http.post<Agent>('/api/agent', agent, defaultHttpOptionsFromConfig(config));
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

  public assignAgentToCustomer(customerId: string, agentId: string, config?: RequestConfig): Observable<Agent> {
    return this.http.post<Agent>(`/api/customer/${customerId}/agent/${agentId}`, null,
      defaultHttpOptionsFromConfig(config));
  }

  public unassignAgentFromCustomer(agentId: string, config?: RequestConfig): Observable<any> {
    return this.http.delete(`/api/customer/agent/${agentId}`, defaultHttpOptionsFromConfig(config));
  }

  // --- Agent Group CRUD ---

  public getAgentGroupById(groupId: string, config?: RequestConfig): Observable<AgentGroup> {
    return this.http.get<AgentGroup>(`/api/agent/group/${groupId}`, defaultHttpOptionsFromConfig(config));
  }

  public getAgentGroupInfoById(groupId: string, config?: RequestConfig): Observable<AgentGroupInfo> {
    return this.http.get<AgentGroupInfo>(`/api/agent/group/info/${groupId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveAgentGroup(group: AgentGroup, config?: RequestConfig): Observable<AgentGroup> {
    return this.http.post<AgentGroup>('/api/agent/group', group, defaultHttpOptionsFromConfig(config));
  }

  public deleteAgentGroup(groupId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/agent/group/${groupId}`, defaultHttpOptionsFromConfig(config));
  }

  public getTenantAgentGroups(pageLink: PageLink, config?: RequestConfig): Observable<PageData<AgentGroup>> {
    return this.http.get<PageData<AgentGroup>>(`/api/tenant/agent/groups${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getTenantAgentGroupInfos(pageLink: PageLink, config?: RequestConfig): Observable<PageData<AgentGroupInfo>> {
    return this.http.get<PageData<AgentGroupInfo>>(`/api/tenant/agent/groupInfos${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getCustomerAgentGroups(customerId: string, pageLink: PageLink, config?: RequestConfig): Observable<PageData<AgentGroup>> {
    return this.http.get<PageData<AgentGroup>>(`/api/customer/${customerId}/agent/groups${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public assignAgentGroupToCustomer(customerId: string, groupId: string, config?: RequestConfig): Observable<AgentGroup> {
    return this.http.post<AgentGroup>(`/api/customer/${customerId}/agent/group/${groupId}`, null,
      defaultHttpOptionsFromConfig(config));
  }

  public unassignAgentGroupFromCustomer(groupId: string, config?: RequestConfig): Observable<any> {
    return this.http.delete(`/api/customer/agent/group/${groupId}`, defaultHttpOptionsFromConfig(config));
  }

  public getGroupProfileRelations(groupId: string, config?: RequestConfig): Observable<any[]> {
    return this.http.get<any[]>(`/api/agent/group/${groupId}/profiles`, defaultHttpOptionsFromConfig(config));
  }

  public assignProfileToGroup(groupId: string, profileId: string, config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/agent/group/${groupId}/profile/${profileId}`, null,
      defaultHttpOptionsFromConfig(config));
  }

  public unassignProfileFromGroup(groupId: string, profileId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/agent/group/${groupId}/profile/${profileId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public previewBulkOperation(groupId: string, profileId: string, request: BulkOperationRequest,
                              config?: RequestConfig): Observable<BulkOperationPreview> {
    return this.http.post<BulkOperationPreview>(
      `/api/agent/group/${groupId}/profile/${profileId}/bulk/preview`, request,
      defaultHttpOptionsFromConfig(config));
  }

  public bulkOperation(groupId: string, profileId: string, request: BulkOperationRequest,
                       config?: RequestConfig): Observable<BulkOperationResult> {
    return this.http.post<BulkOperationResult>(
      `/api/agent/group/${groupId}/profile/${profileId}/bulk`, request,
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

  public installAgentApp(request: AgentAppEventRequest, config?: RequestConfig): Observable<AgentApplication> {
    return this.http.post<AgentApplication>('/api/agent/app/event', request, defaultHttpOptionsFromConfig(config));
  }

  public createAgentAppEvent(applicationId: string, request: AgentAppEventRequest, config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/agent/app/${applicationId}/event`, request, defaultHttpOptionsFromConfig(config));
  }

  public cancelAgentAppEvent(applicationId: string, eventId: string, config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/agent/app/${applicationId}/event/${eventId}/cancel`, null,
      defaultHttpOptionsFromConfig(config));
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
                          composeType?: string, config?: RequestConfig): Observable<AgentApplication> {
    let url = `/api/agent/app/merge/${templateId}/preview`;
    if (composeType) {
      url += `?composeType=${encodeURIComponent(composeType)}`;
    }
    return this.http.post<AgentApplication>(url, application || null, defaultHttpOptionsFromConfig(config));
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

  public getAgentAppProfilesByAppType(appType: string, config?: RequestConfig): Observable<AgentAppProfile[]> {
    return this.http.get<AgentAppProfile[]>(`/api/agent/app/profiles/${appType}`,
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

  public mergeProfileForPreview(templateId: string, profile?: AgentAppProfile,
                                composeType?: string, config?: RequestConfig): Observable<AgentAppProfile> {
    let url = `/api/agent/app/profiles/merge/${templateId}/preview`;
    if (composeType) {
      url += `?composeType=${encodeURIComponent(composeType)}`;
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

  public getGroupBulkActions(groupId: string, pageLink: PageLink,
                             config?: RequestConfig): Observable<PageData<AgentBulkAction>> {
    return this.http.get<PageData<AgentBulkAction>>(
      `/api/agent/group/${groupId}/bulk${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAgentBulkActionEvents(bulkActionId: string, pageLink: PageLink, status?: string,
                                  config?: RequestConfig): Observable<PageData<AgentAppEvent>> {
    let url = `/api/agent/bulk/${bulkActionId}/events${pageLink.toQuery()}`;
    if (status) {
      url += `&status=${status}`;
    }
    return this.http.get<PageData<AgentAppEvent>>(url, defaultHttpOptionsFromConfig(config));
  }
}
