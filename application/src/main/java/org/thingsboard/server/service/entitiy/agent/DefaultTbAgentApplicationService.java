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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.template.TemplateMergeCtx;
import org.thingsboard.server.common.data.agent.template.TemplateMergeRequest;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.template.merge.AgentAppTemplateMergeOrchestrator;

@RequiredArgsConstructor
@TbCoreComponent
@Service
@Slf4j
public class DefaultTbAgentApplicationService implements TbAgentApplicationService {

    private final AgentAppTemplateMergeOrchestrator templateMergeOrchestrator;
    private final AgentApplicationService agentApplicationService;

    @Override
    public AgentApplication mergeForPreview(TenantId tenantId, AgentApplication application,
                                            AgentAppTemplate template, TemplateMergeRequest request) {
        log.trace("Executing mergeForPreview, tenantId [{}], applicationId [{}], templateId [{}]",
                tenantId, application.getId(), template.getId());

        TemplateMergeCtx ctx = new TemplateMergeCtx(request.getSelectedComposeType());
        templateMergeOrchestrator.merge(application, template, ctx);
        return application;
    }
}
