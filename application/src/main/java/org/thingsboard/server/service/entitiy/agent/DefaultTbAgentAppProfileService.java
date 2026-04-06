/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
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
    public AgentAppProfile mergeForPreview(TenantId tenantId, AgentAppProfile appProfile, AgentAppTemplate template, String composeType) {
        log.trace("Executing mergeForPreview, tenantId [{}], appProfileId [{}], templateId [{}], composeType [{}]",
                tenantId, appProfile.getId(), template.getId(), composeType);

        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder()
                .template(template)
                .selectedComposeType(composeType)
                .build();
        configMergeOrchestrator.merge(appProfile, ctx);
        return appProfile;
    }
}
