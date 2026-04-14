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

@Component
@RequiredArgsConstructor
public class UpdateActionHandler implements AgentAppActionHandler {

    private final ProfileConfigResolver profileConfigResolver;

    @Override
    public AgentAppEventActionType getActionType() {
        return AgentAppEventActionType.UPDATE;
    }

    @Override
    public void handle(AgentApplication application, AgentAppEventRequest request, AgentAppActionContext ctx) {
        AgentApplication incoming = request.getApplication();
        if (application.getApplicationProfileId() != null) {
            // Profile-managed: by default re-resolve compose from the profile so any profile/template drift is picked up.
            // When the caller asks to skip the refetch (credentials-only update), keep the existing compose and
            // apply just the incoming creds via setConfig.
            if (request.isSkipProfileRefetch()) {
                if (incoming != null && incoming.getConfig() != null) {
                    application.setConfig(incoming.getConfig());
                }
            } else {
                profileConfigResolver.resolve(ctx.getTenantId(), application);
            }
            if (incoming != null && incoming.getName() != null && !incoming.getName().isBlank()) {
                application.setName(incoming.getName());
            }
            return;
        }
        if (incoming != null) {
            if (incoming.getName() != null && !incoming.getName().isBlank()) {
                application.setName(incoming.getName());
            }
            if (incoming.getConfig() != null) {
                application.setConfig(incoming.getConfig());
            }
        }
    }
}
