/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.service.entitiy.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;
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
import org.thingsboard.server.common.data.agent.AgentAppInstallResponse;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.agent.config.AgentAppConfigMergeOrchestrator;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.AgentEventRateLimiter;
import org.thingsboard.server.service.agent.action.AgentAppActionContext;
import org.thingsboard.server.service.agent.action.AgentAppActionHandler;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@TbCoreComponent
@Service
@Slf4j
public class DefaultTbAgentApplicationService extends AbstractTbEntityService implements TbAgentApplicationService {

    private final AgentAppConfigMergeOrchestrator configMergeOrchestrator;
    private final AgentApplicationService applicationService;
    private final AgentAppEventService appEventService;
    private final TbClusterService tbClusterService;
    private final AgentEventRateLimiter agentEventRateLimiter;
    private Map<AgentAppEventActionType, AgentAppActionHandler> actionHandlers;

    @Autowired
    public DefaultTbAgentApplicationService(AgentAppConfigMergeOrchestrator configMergeOrchestrator,
                                            AgentApplicationService applicationService,
                                            AgentAppEventService appEventService,
                                            TbClusterService tbClusterService,
                                            AgentEventRateLimiter agentEventRateLimiter) {
        this.configMergeOrchestrator = configMergeOrchestrator;
        this.applicationService = applicationService;
        this.appEventService = appEventService;
        this.tbClusterService = tbClusterService;
        this.agentEventRateLimiter = agentEventRateLimiter;
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
        if (!application.isPendingDeletion() && appEventService.hasActiveEventForApplication(application.getId())) {
            throw new DataValidationException("Cannot update application while an event is being processed");
        }
        TenantId tenantId = user.getTenantId();

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
    public AgentAppInstallResponse install(TenantId tenantId, AgentAppEventRequest request, User user) throws Exception {
        AgentApplication application = request.getApplication();
        application.setId(null);
        application.setTenantId(tenantId);
        application.setOrigin(AgentApplicationOrigin.INSTALLED);
        application.setProjectName(AgentApplication.generateProjectName());

        AgentApplication savedApp = checkNotNull(applicationService.saveWithRelatedEntity(tenantId, application, request.getRelatedEntityId()));

        AgentAppEvent event = saveEvent(tenantId, savedApp, AgentAppEventActionType.INSTALL, request);

        logEntityActionService.logEntityAction(tenantId, savedApp.getId(), savedApp, ActionType.ADDED, user);
        return new AgentAppInstallResponse(savedApp, event);
    }

    @Transactional
    @Override
    public AgentAppEvent execActionEvent(TenantId tenantId, AgentApplicationId applicationId, AgentAppEventRequest request) throws Exception {
        return execActionEvent(tenantId, applicationId, request, false);
    }

    @Transactional
    @Override
    public AgentAppEvent execActionEvent(TenantId tenantId, AgentApplicationId applicationId, AgentAppEventRequest request, boolean skipActiveEventCheck) throws Exception {
        AgentAppEventActionType actionType = request.getActionType();
        AgentApplication application;
        try {
            application = applicationService.findByIdForUpdate(tenantId, applicationId);
        } catch (PessimisticLockingFailureException e) {
            throw new ThingsboardException(EVENT_IN_PROGRESS_ERROR_MSG, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        checkNotNull(application);
        if (!skipActiveEventCheck && appEventService.hasActiveOrPendingEventForApplication(applicationId)) {
            throw new ThingsboardException(EVENT_IN_PROGRESS_ERROR_MSG, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        UUID bulkActionId = request.getBulkActionId();
        if (bulkActionId != null && appEventService.existsByApplicationIdAndBulkActionId(applicationId, bulkActionId)) {
            log.info("[{}] Skipping duplicate bulk event for app {} (bulkActionId {})", tenantId, applicationId, bulkActionId);
            return null;
        }

        application.setDesiredTemplateId(null);

        AgentAppActionHandler handler = actionHandlers.get(actionType);
        if (handler != null) {
            handler.handle(application, request, new AgentAppActionContext(tenantId));
        }

        applicationService.save(tenantId, application);
        return saveEvent(tenantId, application, actionType, request);
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
    public AgentApplication mergeForPreview(TenantId tenantId, AgentApplication application, AppConfigMergeCtx ctx) {
        AgentAppTemplateId templateId = ctx.getTemplate() != null ? ctx.getTemplate().getId() : null;
        log.trace("Executing mergeForPreview, tenantId [{}], applicationId [{}], templateId [{}], composeType [{}], relatedEntityId [{}]",
                tenantId, application.getId(), templateId, ctx.getSelectedComposeType(), ctx.getRelatedEntityId());
        configMergeOrchestrator.merge(application, ctx);
        return application;
    }

    @Override
    public AgentApplication assignRelatedEntity(TenantId tenantId, AgentApplicationId agentApplicationId, EntityId relatedEntityId) {
        log.trace("Executing assignRelatedEntity, tenantId [{}], appId [{}], relatedEntityId [{}]", tenantId, agentApplicationId, relatedEntityId);
        return applicationService.assignRelatedEntity(tenantId, agentApplicationId, relatedEntityId);
    }

    @Override
    public AgentApplication unassignRelatedEntity(TenantId tenantId, AgentApplicationId agentApplicationId) {
        log.trace("Executing unassignRelatedEntity, tenantId [{}], appId [{}]", tenantId, agentApplicationId);
        return applicationService.unassignRelatedEntity(tenantId, agentApplicationId);
    }

    private AgentAppEvent saveEvent(TenantId tenantId, AgentApplication application, AgentAppEventActionType actionType, AgentAppEventRequest request) {
        agentEventRateLimiter.checkOrThrow(tenantId, application.getAgentId());
        AgentAppEvent event = new AgentAppEvent();
        event.setTenantId(tenantId);
        event.setApplicationId(application.getId());
        event.setAgentId(application.getAgentId());
        event.setApplicationName(application.getName());
        event.setActionType(actionType);
        event.setDeliveryState(AgentAppEventDeliveryState.PENDING);
        event.setUpdatedTime(System.currentTimeMillis());
        event.setStepStates(request.getStepInputs());
        event.setBulkActionId(request.getBulkActionId());
        return appEventService.save(tenantId, event);
    }
}
