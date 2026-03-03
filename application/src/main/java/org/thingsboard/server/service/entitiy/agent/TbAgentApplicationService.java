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

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.TenantId;

public interface TbAgentApplicationService {

    AgentApplication save(AgentApplication application, User user) throws Exception;

    AgentApplication execInstallEvent(TenantId tenantId, AgentAppEventRequest request, User user) throws Exception;

    void execActionEvent(TenantId tenantId, AgentApplicationId applicationId, AgentAppEventRequest request, User user) throws Exception;

    void cancelEvent(TenantId tenantId, AgentAppEventId eventId) throws Exception;

    AgentApplication mergeForPreview(TenantId tenantId, AgentApplication application, AgentAppTemplate template, String composeType);
}
