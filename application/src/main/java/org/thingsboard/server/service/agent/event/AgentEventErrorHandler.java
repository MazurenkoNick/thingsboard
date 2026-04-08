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
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            log.trace("[{}][{}] Processing after error for event {}", tenantId, agentId, failedEventId);

            appEventService.updateStatus(failedEventId, AgentAppEventStatusUpdate.builder()
                    .status(AgentAppEventStatus.ERROR)
                    .errorMessage(errorMsg)
                    .build());
            boolean dispatchNextEvent = true;

            AgentAppEvent event = appEventService.findById(tenantId, failedEventId);
            if (isDelete(event)) {
                rollbackPendingDeletion(tenantId, event.getApplicationId());
            }
            if (shouldEnqueueRollbackEvent(event, errorOrigin)) {
                appEventService.save(tenantId, buildRollbackEvent(tenantId, event));
                dispatchNextEvent = false;
            }
            if (shouldClearDesiredTemplateId(event, errorOrigin)) {
                clearDesiredTemplateId(tenantId, event.getApplicationId());
            }

            return dispatchNextEvent;
        }));
    }

    private boolean isDelete(AgentAppEvent event) {
        return event != null && event.hasActionType(AgentAppEventActionType.DELETE);
    }

    private boolean shouldEnqueueRollbackEvent(AgentAppEvent event, ErrorOrigin errorOrigin) {
        return errorOrigin == ErrorOrigin.SERVER && event != null
                && (event.hasActionType(AgentAppEventActionType.UPDATE) || event.hasActionType(AgentAppEventActionType.UPGRADE));
    }

    private boolean shouldClearDesiredTemplateId(AgentAppEvent event, ErrorOrigin errorOrigin) {
        return errorOrigin == ErrorOrigin.AGENT && event != null
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

    private AgentAppEvent buildRollbackEvent(TenantId tenantId, AgentAppEvent failedEvent) {
        AgentAppEvent rollbackEvent = new AgentAppEvent();
        rollbackEvent.setTenantId(tenantId);
        rollbackEvent.setApplicationId(failedEvent.getApplicationId());
        rollbackEvent.setActionType(AgentAppEventActionType.ROLLBACK);
        rollbackEvent.setDeliveryState(AgentAppEventDeliveryState.DELIVERED);
        rollbackEvent.setStatus(AgentAppEventStatus.PENDING);
        rollbackEvent.setUpdatedTime(System.currentTimeMillis());

        AgentApplication app = appService.findById(tenantId, failedEvent.getApplicationId());
        List<AgentAppStep> rollbackSteps = stepsResolver.resolveSteps(app, AgentAppEventActionType.ROLLBACK);
        UUID rollbackStepId = rollbackSteps.stream()
                .filter(s -> s.getType() == AgentAppStepType.ROLLBACK)
                .findFirst()
                .map(AgentAppStep::getId)
                .orElseThrow(() -> new IllegalStateException("No rollback step found in template for application " + app.getId()));

        rollbackEvent.setStepStates(Map.of(rollbackStepId, new RollBackStepState(failedEvent.getId())));
        return rollbackEvent;
    }
}
