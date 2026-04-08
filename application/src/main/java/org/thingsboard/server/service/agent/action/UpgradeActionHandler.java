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
package org.thingsboard.server.service.agent.action;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.dao.agent.config.ProfileConfigResolver;
import org.thingsboard.server.exception.DataValidationException;

@Component
@RequiredArgsConstructor
public class UpgradeActionHandler implements AgentAppActionHandler {

    private final ProfileConfigResolver profileConfigResolver;

    @Override
    public AgentAppEventActionType getActionType() {
        return AgentAppEventActionType.UPGRADE;
    }

    @Override
    public void handle(AgentApplication application, AgentAppEventRequest request, AgentAppActionContext ctx) {
        AgentApplication upgradedApp = request.getApplication();
        if (upgradedApp == null) {
            throw new DataValidationException("Upgrade request must include an application");
        }
        if (application.getApplicationProfileId() != null) {
            profileConfigResolver.resolve(ctx.getTenantId(), application, application.getRelatedEntityId());
        } else if (upgradedApp.getConfig() != null) {
            application.setConfig(upgradedApp.getConfig());
        }
        application.setDesiredTemplateId(upgradedApp.getTemplateId());
    }
}
