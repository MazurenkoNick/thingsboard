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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.config.AgentAppConfigMergeOrchestrator;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@AllArgsConstructor
@TbCoreComponent
@Service
@Slf4j
public class DefaultTbAgentAppProfileService extends AbstractTbEntityService implements TbAgentAppProfileService {

    private final AgentAppProfileService profileService;
    private final AgentAppConfigMergeOrchestrator configMergeOrchestrator;

    @Override
    public AgentAppProfile save(AgentAppProfile profile, User user) throws Exception {
        ActionType actionType = profile.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = profile.getTenantId();
        try {
            AgentAppProfile saved = checkNotNull(profileService.saveProfile(profile));
            logEntityActionService.logEntityAction(tenantId, saved.getId(), saved, actionType, user);
            return saved;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.AGENT_APP_PROFILE), profile, actionType, user, e);
            throw e;
        }
    }

    @Transactional
    @Override
    public void delete(AgentAppProfile profile, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = profile.getTenantId();
        try {
            profileService.deleteProfile(tenantId, profile.getId());
            logEntityActionService.logEntityAction(tenantId, profile.getId(), profile, actionType, user, profile.getId().toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.AGENT_APP_PROFILE), profile, actionType, user, e, profile.getId().toString());
            throw e;
        }
    }

    @Override
    public AgentAppProfile mergeForPreview(TenantId tenantId, AgentAppProfile appProfile, AppConfigMergeCtx ctx) {
        log.trace("Executing mergeForPreview, tenantId [{}], appProfileId [{}], templateId [{}]",
                tenantId, appProfile.getId(), ctx.getTemplate() != null ? ctx.getTemplate().getId() : null);
        configMergeOrchestrator.merge(appProfile, ctx);
        return appProfile;
    }
}
