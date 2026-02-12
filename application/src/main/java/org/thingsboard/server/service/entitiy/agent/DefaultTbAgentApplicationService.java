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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.template.TemplateMergeCtx;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.template.merge.AgentAppTemplateMergeOrchestrator;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@TbCoreComponent
@Service
@Slf4j
public class DefaultTbAgentApplicationService extends AbstractTbEntityService implements TbAgentApplicationService {

    private final AgentAppTemplateMergeOrchestrator templateMergeOrchestrator;
    private final AgentApplicationService agentApplicationService;

    public DefaultTbAgentApplicationService(AgentAppTemplateMergeOrchestrator templateMergeOrchestrator,
                                            AgentApplicationService agentApplicationService) {
        this.templateMergeOrchestrator = templateMergeOrchestrator;
        this.agentApplicationService = agentApplicationService;
    }

    @Override
    public AgentApplication save(AgentApplication application, User user) throws Exception {
        ActionType actionType = application.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = application.getTenantId();
        try {
            AgentApplication savedApp = checkNotNull(agentApplicationService.save(tenantId, application));
            logEntityActionService.logEntityAction(tenantId, savedApp.getId(), savedApp, actionType, user);
            return savedApp;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.AGENT_APPLICATION), application, actionType, user, e);
            throw e;
        }
    }

    @Transactional
    @Override
    public void delete(AgentApplication application, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = application.getTenantId();
        AgentApplicationId applicationId = application.getId();
        try {
            agentApplicationService.delete(tenantId, applicationId);
            logEntityActionService.logEntityAction(tenantId, applicationId, actionType, user, null, applicationId.toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.AGENT_APPLICATION), actionType, user, e, applicationId.toString());
            throw e;
        }
    }

    @Override
    public AgentApplication mergeForPreview(TenantId tenantId, AgentApplication application, AgentAppTemplate template, String composeType) {
        log.trace("Executing mergeForPreview, tenantId [{}], applicationId [{}], templateId [{}], composeType [{}]",
                tenantId, application.getId(), template.getId(), composeType);

        TemplateMergeCtx ctx = TemplateMergeCtx.builder()
                .selectedComposeType(composeType)
                .build();
        templateMergeOrchestrator.merge(application, template, ctx);
        return application;
    }
}
