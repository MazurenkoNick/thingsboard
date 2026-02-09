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
package org.thingsboard.server.service.agent.template;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.InstallationAppType;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppTemplateService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultTbAgentAppTemplateService implements TbAgentAppTemplateService {

    private final AgentAppTemplateService agentAppTemplateService;

    @Override
    public List<AgentAppTemplate> findAll(TenantId tenantId) {
        return agentAppTemplateService.findAll(tenantId);
    }

    @Override
    public AgentAppTemplate findById(TenantId tenantId, AgentAppTemplateId templateId) {
        return agentAppTemplateService.findById(tenantId, templateId);
    }

    @Override
    public AgentAppTemplate findByAppTypeAndInstallationTypeAndCurrentVersion(AgentApplicationType appType,
                                                                              InstallationAppType installationType,
                                                                              String currentVersion) {
        return agentAppTemplateService.findByAppTypeAndInstallTypeAndVersion(appType, installationType, currentVersion);
    }
}
