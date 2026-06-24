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
package org.thingsboard.server.service.agent.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventDeliveryState;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.AgentAppEventStatusUpdate;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.ErrorOrigin;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.AgentAppStepType;
import org.thingsboard.server.common.data.agent.step.state.RollBackStepState;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppEventStepsResolver;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class AgentEventErrorHandler {

    private final AgentAppEventService appEventService;
    private final AgentApplicationService appService;
    private final AgentAppEventStepsResolver stepsResolver;
    private final AgentEventWatchdog eventWatchdog;
    @Lazy
    private final AgentEventProcessor agentEventProcessor;
    private final TransactionTemplate transactionTemplate;

    public void onFailure(TenantId tenantId, AgentId agentId, AgentAppEventId failedEventId, ErrorOrigin errorOrigin, String errorMsg) {
        try {
            boolean shouldDispatchNext = doOnFailure(tenantId, agentId, failedEventId, errorOrigin, errorMsg);
            eventWatchdog.cancel(agentId, failedEventId);
            if (shouldDispatchNext) {
                dispatchNextIfAppExists(tenantId, agentId, failedEventId);
            }
        } catch (Exception e) {
            appEventService.updateStatus(failedEventId, AgentAppEventStatusUpdate.builder()
                    .status(AgentAppEventStatus.ERROR)
                    .build());
            log.error("[{}][{}] Failed to process error for event {}", tenantId, agentId, failedEventId, e);
        }
    }

    private boolean doOnFailure(TenantId tenantId, AgentId agentId, AgentAppEventId failedEventId, ErrorOrigin errorOrigin, String errorMsg) {
        return Boolean.TRUE.equals(transactionTemplate.execute(__ -> {
            log.trace("[{}][{}] Processing after error for event {}", tenantId, agentId, failedEventId);

            boolean transitionedToError = appEventService.updateStatus(failedEventId, AgentAppEventStatusUpdate.builder()
                    .status(AgentAppEventStatus.ERROR)
                    .errorMessage(errorMsg)
                    .build());
            if (!transitionedToError) {
                log.info("[{}][{}] Event {} already in terminal state, skipping error side effects", tenantId, agentId, failedEventId);
                return false;
            }
            boolean dispatchNextEvent = true;

            AgentAppEvent event = appEventService.findById(tenantId, failedEventId);
            if (isDelete(event)) {
                rollbackPendingDeletion(tenantId, event.getApplicationId());
            }
            if (shouldEnqueueRollbackEvent(event, errorOrigin)) {
                boolean rolledBack = trySaveRollbackEvent(tenantId, event);
                dispatchNextEvent = !rolledBack;
            }
            if (shouldClearDesiredTemplateId(event, errorOrigin)) {
                clearDesiredTemplateId(tenantId, event.getApplicationId());
            }

            return dispatchNextEvent;
        }));
    }

    private boolean trySaveRollbackEvent(TenantId tenantId, AgentAppEvent event) {
        Optional<AgentAppEvent> rollback = buildRollbackEvent(tenantId, event);
        rollback.ifPresent(e -> appEventService.save(tenantId, e));
        return rollback.isPresent();
    }

    private boolean isDelete(AgentAppEvent event) {
        return event != null && event.hasActionType(AgentAppEventActionType.DELETE);
    }

    private boolean shouldEnqueueRollbackEvent(AgentAppEvent event, ErrorOrigin errorOrigin) {
        return event != null && event.getDeliveryState() == AgentAppEventDeliveryState.DELIVERED &&
                errorOrigin == ErrorOrigin.SERVER && event.getApplicationId() != null
                && (event.hasActionType(AgentAppEventActionType.UPDATE) || event.hasActionType(AgentAppEventActionType.UPGRADE));
    }

    private boolean shouldClearDesiredTemplateId(AgentAppEvent event, ErrorOrigin errorOrigin) {
        return event != null && errorOrigin == ErrorOrigin.AGENT && event.getApplicationId() != null
                && (event.hasActionType(AgentAppEventActionType.UPGRADE) || event.hasActionType(AgentAppEventActionType.ROLLBACK));
    }

    private void clearDesiredTemplateId(TenantId tenantId, AgentApplicationId applicationId) {
        AgentApplication app = appService.findById(tenantId, applicationId);
        if (app != null && app.getDesiredTemplateId() != null) {
            log.info("[{}] Clearing desiredTemplateId for application {} after agent error", tenantId, applicationId);
            app.setDesiredTemplateId(null);
            appService.save(tenantId, app);
        }
    }

    private void dispatchNextIfAppExists(TenantId tenantId, AgentId agentId, AgentAppEventId failedEventId) {
        AgentApplication application = appService.findByEventId(tenantId, failedEventId);
        if (application == null) {
            log.warn("[{}][{}] Application not found for failed event {}, skipping next event dispatch", tenantId, agentId, failedEventId);
            return;
        }
        agentEventProcessor.processNextEventForApp(tenantId, agentId, application);
    }

    private void rollbackPendingDeletion(TenantId tenantId, AgentApplicationId applicationId) {
        if (applicationId == null) {
            return;
        }
        try {
            AgentApplication app = appService.findById(tenantId, applicationId);
            if (app != null && app.isPendingDeletion()) {
                app.setPendingDeletion(false);
                appService.save(tenantId, app);
                log.info("[{}] Rolled back pendingDeletion for application {}", tenantId, applicationId);
            }
        } catch (Exception e) {
            log.error("[{}] Failed to rollback pendingDeletion for application {}", tenantId, applicationId, e);
        }
    }

    private Optional<AgentAppEvent> buildRollbackEvent(TenantId tenantId, AgentAppEvent failedEvent) {
        AgentAppEvent rollbackEvent = new AgentAppEvent();
        rollbackEvent.setTenantId(tenantId);
        rollbackEvent.setApplicationId(failedEvent.getApplicationId());
        rollbackEvent.setAgentId(failedEvent.getAgentId());
        rollbackEvent.setApplicationName(failedEvent.getApplicationName());
        rollbackEvent.setActionType(AgentAppEventActionType.ROLLBACK);
        rollbackEvent.setDeliveryState(AgentAppEventDeliveryState.DELIVERED);
        rollbackEvent.setStatus(AgentAppEventStatus.PENDING);
        rollbackEvent.setUpdatedTime(System.currentTimeMillis());

        AgentApplication app = appService.findById(tenantId, failedEvent.getApplicationId());
        if (app == null) {
            return Optional.empty();
        }
        List<AgentAppStep> rollbackSteps = stepsResolver.resolveSteps(app, AgentAppEventActionType.ROLLBACK);
        UUID rollbackStepId = rollbackSteps.stream()
                .filter(s -> s.getType() == AgentAppStepType.ROLLBACK)
                .findFirst()
                .map(AgentAppStep::getId)
                .orElseThrow(() -> new IllegalStateException("No rollback step found in template for application " + app.getId()));

        rollbackEvent.setStepStates(Map.of(rollbackStepId, new RollBackStepState(failedEvent.getId())));
        return Optional.of(rollbackEvent);
    }
}
