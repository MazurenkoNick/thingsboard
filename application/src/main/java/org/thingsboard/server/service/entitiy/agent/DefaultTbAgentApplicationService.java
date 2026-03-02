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
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventDeliveryState;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentApplication;
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

@TbCoreComponent
@Service
@Slf4j
public class DefaultTbAgentApplicationService extends AbstractTbEntityService implements TbAgentApplicationService {

    private final AgentAppTemplateMergeOrchestrator templateMergeOrchestrator;
    private final AgentApplicationService agentApplicationService;
    private final AgentAppEventService agentAppEventService;

    public DefaultTbAgentApplicationService(AgentAppTemplateMergeOrchestrator templateMergeOrchestrator,
                                            AgentApplicationService agentApplicationService,
                                            AgentAppEventService agentAppEventService) {
        this.templateMergeOrchestrator = templateMergeOrchestrator;
        this.agentApplicationService = agentApplicationService;
        this.agentAppEventService = agentAppEventService;
    }

    @Override
    public AgentApplication save(AgentApplication application, User user) throws Exception {
        boolean isUpdate = application.getId() != null;
        ActionType actionType = isUpdate ? ActionType.UPDATED : ActionType.ADDED;
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
    public AgentApplication execInstallEvent(TenantId tenantId, AgentAppEventRequest request, User user) throws Exception {
        AgentApplication application = request.getApplication();
        if (application == null) {
            throw new DataValidationException("Install request must include an application");
        }
        application.setId(null);
        application.setTenantId(tenantId);
        AgentApplication savedApp = checkNotNull(agentApplicationService.save(tenantId, application));

        saveEvent(tenantId, savedApp.getId(), AgentAppEventActionType.INSTALL, request);

        logEntityActionService.logEntityAction(tenantId, savedApp.getId(), savedApp, ActionType.ADDED, user);
        return savedApp;
    }

    @Transactional
    @Override
    public void execActionEvent(TenantId tenantId, AgentApplicationId applicationId, AgentAppEventRequest request, User user) throws Exception {
        AgentAppEventActionType actionType = request.getActionType();
        if (actionType == null) {
            throw new DataValidationException("Action type must not be null");
        }
        if (actionType == AgentAppEventActionType.INSTALL) {
            throw new DataValidationException("Use the install endpoint for INSTALL events");
        }
        if (agentAppEventService.hasActiveEventForApplication(applicationId)) {
            throw new ThingsboardException("Cannot create event while another event is being processed", ThingsboardErrorCode.TOO_MANY_REQUESTS);
        }

        AgentApplication application = checkNotNull(agentApplicationService.findById(tenantId, applicationId));
        throwIfPendingForDelete(application);

        if (actionType == AgentAppEventActionType.DELETE) {
            agentAppEventService.deleteAllPendingByApplicationId(applicationId);
            application.setPendingDeletion(true);
            agentApplicationService.save(tenantId, application);
        } else if (actionType == AgentAppEventActionType.UPGRADE) {
            AgentApplication upgradedApp = request.getApplication();
            if (upgradedApp == null) {
                throw new DataValidationException("Upgrade request must include an application");
            }
            upgradedApp.setId(applicationId);
            upgradedApp.setTenantId(tenantId);
            agentApplicationService.save(tenantId, upgradedApp);
        }

        saveEvent(tenantId, applicationId, actionType, request);
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

    private void saveEvent(TenantId tenantId, AgentApplicationId applicationId, AgentAppEventActionType actionType, AgentAppEventRequest request) {
        AgentAppEvent event = new AgentAppEvent();
        event.setTenantId(tenantId);
        event.setApplicationId(applicationId);
        event.setActionType(actionType);
        event.setDeliveryState(AgentAppEventDeliveryState.PENDING);
        event.setUpdatedTime(System.currentTimeMillis());
        event.setStepStates(request.getStepInputs());
        agentAppEventService.save(tenantId, event);
    }
}
