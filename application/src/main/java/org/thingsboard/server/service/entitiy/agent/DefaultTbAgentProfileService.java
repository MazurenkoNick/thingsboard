/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.service.entitiy.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentAppProfileRelationInfo;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentProfileService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@RequiredArgsConstructor
@TbCoreComponent
@Service
@Slf4j
public class DefaultTbAgentProfileService extends AbstractTbEntityService implements TbAgentProfileService {

    private final AgentProfileService profileService;
    private final AgentAppProfileService appProfileService;

    @Override
    public AgentProfile save(AgentProfile agentProfile, User user) throws Exception {
        return save(agentProfile, null, user);
    }

    @Transactional
    @Override
    public AgentProfile save(AgentProfile agentProfile, List<AgentAppProfileId> appProfileIds, User user) throws Exception {
        ActionType actionType = agentProfile.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = agentProfile.getTenantId();
        try {
            AgentProfile saved = checkNotNull(profileService.saveProfile(agentProfile));
            logEntityActionService.logEntityAction(tenantId, saved.getId(), saved, actionType, user);
            if (appProfileIds != null) {
                syncAppProfiles(tenantId, saved, appProfileIds, user);
            }
            return saved;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.AGENT_PROFILE), agentProfile, actionType, user, e);
            throw e;
        }
    }

    @Transactional
    @Override
    public void assignAppProfiles(AgentProfile agentProfile, List<AgentAppProfileId> appProfileIds, User user) throws Exception {
        TenantId tenantId = agentProfile.getTenantId();
        try {
            syncAppProfiles(tenantId, agentProfile, appProfileIds, user);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.AGENT_PROFILE), agentProfile, ActionType.RELATION_ADD_OR_UPDATE, user, e);
            throw e;
        }
    }

    @Transactional
    @Override
    public void setAppProfileRelatesOnAutoDiscovery(AgentProfile agentProfile, AgentAppProfileId appProfileId, boolean enable, User user) throws ThingsboardException {
        TenantId tenantId = agentProfile.getTenantId();
        try {
            AgentAppProfile appProfile = checkNotNull(appProfileService.findProfileById(tenantId, appProfileId));
            if (enable) {
                boolean conflict = appProfileService.findProfileRelationInfosByAgentProfileIdAndAppTypeAndTemplateId(
                                tenantId, agentProfile.getId(), appProfile.getAppType(), appProfile.getTemplateId())
                        .stream()
                        .filter(relationInfo -> !relationInfo.getId().equals(appProfileId))
                        .anyMatch(relationInfo -> relationInfo.getAdditionalInfo() != null
                                && relationInfo.getAdditionalInfo().path(AgentProfileService.RELATES_ON_AUTO_DISCOVERY).asBoolean(false));
                if (conflict) {
                    throw new DataValidationException("Another application profile based on the same template already relates on auto-discovery for this agent profile!");
                }
            }
            profileService.setAppProfileRelatesOnAutoDiscovery(tenantId, agentProfile.getId(), appProfileId, enable);
            logEntityActionService.logEntityAction(tenantId, agentProfile.getId(), agentProfile, ActionType.RELATION_ADD_OR_UPDATE, user, appProfileId.toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.AGENT_PROFILE), agentProfile, ActionType.RELATION_ADD_OR_UPDATE, user, e);
            throw e;
        }
    }

    private void syncAppProfiles(TenantId tenantId, AgentProfile agentProfile, List<AgentAppProfileId> requestedIds, User user) {
        Set<AgentAppProfileId> requested = new HashSet<>(requestedIds == null ? Collections.emptyList() : requestedIds);
        Set<AgentAppProfileId> current = appProfileService.findProfileRelationInfosByAgentProfileId(tenantId, agentProfile.getId()).stream()
                .map(AgentAppProfileRelationInfo::getId)
                .collect(Collectors.toSet());

        for (AgentAppProfileId toAdd : requested) {
            if (!current.contains(toAdd)) {
                profileService.assignAppProfileToAgentProfile(tenantId, agentProfile.getId(), toAdd);
                logEntityActionService.logEntityAction(tenantId, agentProfile.getId(), agentProfile, ActionType.RELATION_ADD_OR_UPDATE, user, toAdd.toString());
            }
        }
        for (AgentAppProfileId toRemove : current) {
            if (!requested.contains(toRemove)) {
                profileService.unassignAppProfileFromAgentProfile(tenantId, agentProfile.getId(), toRemove);
                logEntityActionService.logEntityAction(tenantId, agentProfile.getId(), agentProfile, ActionType.RELATION_DELETED, user, toRemove.toString());
            }
        }
    }

    @Transactional
    @Override
    public void delete(AgentProfile agentProfile, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = agentProfile.getTenantId();
        try {
            profileService.deleteProfile(tenantId, agentProfile.getId());
            logEntityActionService.logEntityAction(tenantId, agentProfile.getId(), agentProfile, actionType, user, agentProfile.getId().toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.AGENT_PROFILE), agentProfile, actionType, user, e, agentProfile.getId().toString());
            throw e;
        }
    }

    @Override
    public AgentProfile setDefaultAgentProfile(AgentProfile agentProfile, AgentProfile previousDefaultAgentProfile, User user) throws ThingsboardException {
        TenantId tenantId = agentProfile.getTenantId();
        AgentProfileId agentProfileId = agentProfile.getId();
        try {
            if (profileService.setDefaultAgentProfile(tenantId, agentProfileId)) {
                if (previousDefaultAgentProfile != null) {
                    previousDefaultAgentProfile = profileService.findProfileById(tenantId, previousDefaultAgentProfile.getId());
                    logEntityActionService.logEntityAction(tenantId, previousDefaultAgentProfile.getId(), previousDefaultAgentProfile,
                            ActionType.UPDATED, user);
                }
                agentProfile = profileService.findProfileById(tenantId, agentProfileId);
                logEntityActionService.logEntityAction(tenantId, agentProfileId, agentProfile, ActionType.UPDATED, user);
            }
            return agentProfile;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.AGENT_PROFILE), ActionType.UPDATED,
                    user, e, agentProfileId.toString());
            throw e;
        }
    }
}
