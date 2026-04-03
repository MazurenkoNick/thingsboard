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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventDeliveryState;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.ProfileConfigResolver;
import org.thingsboard.server.service.agent.template.merge.AgentAppConfigMergeOrchestrator;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.agent.action.AgentAppActionContext;
import org.thingsboard.server.service.agent.action.AgentAppActionHandler;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@TbCoreComponent
@Service
@Slf4j
public class DefaultTbAgentApplicationService extends AbstractTbEntityService implements TbAgentApplicationService {

    private final AgentAppConfigMergeOrchestrator configMergeOrchestrator;
    private final ProfileConfigResolver profileConfigResolver;
    private final AgentApplicationService applicationService;
    private final AgentAppEventService appEventService;
    private final TbClusterService tbClusterService;
    private Map<AgentAppEventActionType, AgentAppActionHandler> actionHandlers;

    @Autowired
    public DefaultTbAgentApplicationService(AgentAppConfigMergeOrchestrator configMergeOrchestrator,
                                            ProfileConfigResolver profileConfigResolver,
                                            AgentApplicationService applicationService,
                                            AgentAppEventService appEventService,
                                            TbClusterService tbClusterService) {
        this.configMergeOrchestrator = configMergeOrchestrator;
        this.profileConfigResolver = profileConfigResolver;
        this.applicationService = applicationService;
        this.appEventService = appEventService;
        this.tbClusterService = tbClusterService;
    }

    @Autowired
    public void setActionHandlers(List<AgentAppActionHandler> handlers) {
        this.actionHandlers = handlers.stream()
                .collect(Collectors.toMap(AgentAppActionHandler::getActionType, Function.identity()));
    }

    @Override
    @Transactional
    public AgentApplication update(AgentApplication application, User user) throws Exception {
        if (application.getId() == null) {
            throw new IllegalStateException("Can't update state of the non-existent application!");
        }
        TenantId tenantId = user.getTenantId();
        AgentApplication old = applicationService.findById(tenantId, application.getId());

        if (isApplicationProfileChangedOrAdded(application, old)) {
            log.trace("[{}] Agent profile is added or changed for application {}", application.getTenantId(), application.getTenantId());
            profileConfigResolver.resolve(tenantId, application, application.getRelatedEntityId());
        } else if (application.getApplicationProfileId() != null) {
            log.trace("[{}] Restoring old profile-based configuration for application {}", application.getTenantId(), application.getTenantId());
            AgentAppConfig oldConfig = old.getConfig();
            if (!Objects.equals(application.getConfig(), oldConfig)) {
                throw new DataValidationException("Direct config update is not allowed for profile-managed applications!");
            }
            application.setConfig(oldConfig);
            if (application.getRelatedEntityId() != null) {
                profileConfigResolver.updateRelatedEntityIdTemplateFields(application, application.getRelatedEntityId());
            }
        }

        try {
            AgentApplication savedApp = checkNotNull(applicationService.save(tenantId, application));
            logEntityActionService.logEntityAction(tenantId, savedApp.getId(), savedApp, ActionType.UPDATED, user);
            return savedApp;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.AGENT_APPLICATION), application, ActionType.UPDATED, user, e);
            throw e;
        }
    }

    @Transactional
    @Override
    public AgentApplication install(TenantId tenantId, AgentAppEventRequest request, User user) throws Exception {
        AgentApplication application = request.getApplication();
        application.setId(null);
        application.setTenantId(tenantId);
        application.setOrigin(AgentApplicationOrigin.INSTALLED);

        profileConfigResolver.resolve(tenantId, application, application.getRelatedEntityId());

        AgentApplication savedApp = checkNotNull(applicationService.save(tenantId, application));

        saveEvent(tenantId, savedApp.getId(), AgentAppEventActionType.INSTALL, request);

        logEntityActionService.logEntityAction(tenantId, savedApp.getId(), savedApp, ActionType.ADDED, user);
        return savedApp;
    }

    @Transactional
    @Override
    public void execActionEvent(TenantId tenantId, AgentApplicationId applicationId, AgentAppEventRequest request, User user) throws Exception {
        execActionEvent(tenantId, applicationId, request, user, false);
    }

    @Transactional
    @Override
    public void execActionEvent(TenantId tenantId, AgentApplicationId applicationId, AgentAppEventRequest request, User user, boolean skipActiveEventCheck) throws Exception {
        AgentAppEventActionType actionType = request.getActionType();
        if (!skipActiveEventCheck && appEventService.hasActiveEventForApplication(applicationId)) {
            throw new ThingsboardException("Cannot create event while another event is being processed", ThingsboardErrorCode.TOO_MANY_REQUESTS);
        }

        AgentApplication application = checkNotNull(applicationService.findById(tenantId, applicationId));
        application.setDesiredTemplateId(null);

        AgentAppActionHandler handler = actionHandlers.get(actionType);
        if (handler != null) {
            handler.handle(application, request, new AgentAppActionContext(tenantId));
        }

        applicationService.save(tenantId, application);
        saveEvent(tenantId, applicationId, actionType, request);
    }

    @Override
    public void cancelEvent(TenantId tenantId, AgentAppEventId eventId) throws Exception {
        AgentAppEvent event = appEventService.findById(tenantId, eventId);
        if (event == null) {
            throw new ThingsboardException("Agent app event not found", ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        AgentAppEventStatus status = event.getStatus();
        if (status == AgentAppEventStatus.FINISHED || status == AgentAppEventStatus.ERROR) {
            throw new ThingsboardException("Cannot cancel event in terminal state: " + status, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        AgentApplication application = checkNotNull(applicationService.findById(tenantId, event.getApplicationId()));
        tbClusterService.onAgentAppEventCancelled(tenantId, application.getAgentId(), event);
    }

    @Override
    public AgentApplication mergeForPreview(TenantId tenantId, AgentApplication application, AgentAppTemplate template, String composeType) {
        EntityId relatedEntityId = application.getRelatedEntityId();
        log.trace("Executing mergeForPreview, tenantId [{}], applicationId [{}], templateId [{}], composeType [{}], relatedEntityId [{}]",
                tenantId, application.getId(), template.getId(), composeType, relatedEntityId);

        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder()
                .template(template)
                .selectedComposeType(composeType)
                .relatedEntityId(relatedEntityId)
                .build();
        configMergeOrchestrator.merge(application, ctx);
        return application;
    }

    private boolean isApplicationProfileChangedOrAdded(AgentApplication application, AgentApplication old) {
        return application.getApplicationProfileId() != null && !application.getApplicationProfileId().equals(old.getApplicationProfileId());
    }

    private void saveEvent(TenantId tenantId, AgentApplicationId applicationId, AgentAppEventActionType actionType, AgentAppEventRequest request) {
        AgentAppEvent event = new AgentAppEvent();
        event.setTenantId(tenantId);
        event.setApplicationId(applicationId);
        event.setActionType(actionType);
        event.setDeliveryState(AgentAppEventDeliveryState.PENDING);
        event.setUpdatedTime(System.currentTimeMillis());
        event.setStepStates(request.getStepInputs());
        event.setBulkActionId(request.getBulkActionId());
        appEventService.save(tenantId, event);
    }
}
