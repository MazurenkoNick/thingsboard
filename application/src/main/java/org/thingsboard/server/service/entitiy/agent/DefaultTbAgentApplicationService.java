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
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventDeliveryState;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.template.TemplateMergeCtx;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.template.merge.AgentAppTemplateMergeOrchestrator;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.List;

@TbCoreComponent
@Service
@Slf4j
public class DefaultTbAgentApplicationService extends AbstractTbEntityService implements TbAgentApplicationService {

    private final AgentAppTemplateMergeOrchestrator templateMergeOrchestrator;
    private final AgentApplicationService agentApplicationService;
    private final AgentAppEventService agentAppEventService;
    private final TbClusterService tbClusterService;

    public DefaultTbAgentApplicationService(AgentAppTemplateMergeOrchestrator templateMergeOrchestrator,
                                            AgentApplicationService agentApplicationService,
                                            AgentAppEventService agentAppEventService,
                                            TbClusterService tbClusterService) {
        this.templateMergeOrchestrator = templateMergeOrchestrator;
        this.agentApplicationService = agentApplicationService;
        this.agentAppEventService = agentAppEventService;
        this.tbClusterService = tbClusterService;
    }

    @Transactional
    @Override
    public AgentApplication save(AgentApplication application, User user) throws Exception {
        boolean isUpdate = application.getId() != null;
        ActionType actionType = isUpdate ? ActionType.UPDATED : ActionType.ADDED;
        TenantId tenantId = application.getTenantId();
        try {
            if (isUpdate && agentAppEventService.hasActiveEventForApplication(application.getId())) {
                throw new ThingsboardException("Cannot update application while an event is being processed", ThingsboardErrorCode.TOO_MANY_REQUESTS);
            } else if (isUpdate) {
                AgentApplication existing = agentApplicationService.findById(tenantId, application.getId());
                throwIfPendingForDelete(existing);
            }
            AgentApplication savedApp = checkNotNull(agentApplicationService.save(tenantId, application));

            AgentAppEventActionType eventAction = isUpdate ? AgentAppEventActionType.UPDATE : AgentAppEventActionType.INSTALL;
            List<AgentAppStep> steps = isUpdate ? savedApp.getUpgradeSteps() : savedApp.getStartSteps();
            createEvent(tenantId, savedApp.getId(), eventAction, steps);

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
            throwIfPendingForDelete(application);
            agentAppEventService.deleteAllPendingByApplicationId(applicationId);
            createEvent(tenantId, applicationId, AgentAppEventActionType.DELETE, null);

            application.setPendingDeletion(true);
            agentApplicationService.save(tenantId, application);

            logEntityActionService.logEntityAction(tenantId, applicationId, actionType, user, null, applicationId.toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.AGENT_APPLICATION), actionType, user, e, applicationId.toString());
            throw e;
        }
    }


    @Override
    public void restart(TenantId tenantId, AgentApplicationId applicationId, User user) throws Exception {
        if (agentAppEventService.hasActiveEventForApplication(applicationId)) {
            throw new ThingsboardException("Cannot restart application while an event is being processed", ThingsboardErrorCode.TOO_MANY_REQUESTS);
        }
        AgentApplication application = checkNotNull(agentApplicationService.findById(tenantId, applicationId));
        throwIfPendingForDelete(application);
        AgentAppEvent event = createEvent(tenantId, applicationId, AgentAppEventActionType.RESTART, application.getStartSteps());

        tbClusterService.onAgentAppEvent(tenantId, application.getAgentId(), event);
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

    private void throwIfPendingForDelete(AgentApplication application) {
        if (application.isPendingDeletion()) {
            throw new DataValidationException("Application is already pending for removal");
        }
    }

    private AgentAppEvent createEvent(TenantId tenantId, AgentApplicationId applicationId,
                                      AgentAppEventActionType actionType, List<AgentAppStep> steps) {
        AgentAppEvent event = new AgentAppEvent();
        event.setTenantId(tenantId);
        event.setApplicationId(applicationId);
        event.setActionType(actionType);
        event.setDeliveryState(AgentAppEventDeliveryState.PENDING);
        event.setUpdatedTime(System.currentTimeMillis());
        return agentAppEventService.save(tenantId, event);
    }
}
