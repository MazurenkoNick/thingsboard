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
package org.thingsboard.server.service.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.service.agent.template.merge.MergeCredentialsToConfigRule;

@Component
@RequiredArgsConstructor
public class ProfileConfigResolver {

    private final AgentAppProfileService profileService;
    private final MergeCredentialsToConfigRule mergeCredentialsToConfigRule;

    public void resolve(TenantId tenantId, AgentApplication application, EntityId relatedEntityId) {
        if (application.getApplicationProfileId() == null) {
            return;
        }
        AgentAppProfile profile = profileService.findProfileById(tenantId, application.getApplicationProfileId());
        if (profile == null || profile.getConfig() == null) {
            return;
        }
        if (relatedEntityId == null && application.getAppType().getRelatedEntityType() != null) {
            throw new DataValidationException("Related entity id must be specified!");
        }
        application.setConfig(profile.getConfig().copy());
        application.setProfileConfigVersion(profile.getVersion());

        if (relatedEntityId != null) {
            updateRelatedEntityIdTemplateFields(application, relatedEntityId);
        }
    }

    public void updateRelatedEntityIdTemplateFields(AgentApplication application, EntityId relatedEntityId) {
        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder()
                .relatedEntityId(relatedEntityId)
                .build();
        if (mergeCredentialsToConfigRule.supports(application, ctx)) {
            mergeCredentialsToConfigRule.apply(application, ctx);
        }
    }
}
