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
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.ErrorOrigin;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class AgentEventErrorHandler {

    private final AgentAppEventService appEventService;
    private final AgentApplicationService appService;
    private final AgentEventWatchdog eventWatchdog;
    @Lazy
    private final AgentEventProcessor agentEventProcessor;
    private final TransactionTemplate transactionTemplate;

    public void onFailure(TenantId tenantId, AgentId agentId, AgentAppEventId failedEventId, ErrorOrigin errorOrigin) {
        try {
            boolean shouldDispatchNext = doOnFailure(tenantId, agentId, failedEventId, errorOrigin);
            eventWatchdog.cancel(agentId, failedEventId);
            if (shouldDispatchNext) {
                dispatchNextIfAppExists(tenantId, agentId, failedEventId);
            }
        } catch (Exception e) {
            appEventService.updateStatus(failedEventId, AgentAppEventStatus.ERROR, null);
            log.error("[{}][{}] Failed to process error for event {}", tenantId, agentId, failedEventId, e);
        }
    }

    private boolean doOnFailure(TenantId tenantId, AgentId agentId, AgentAppEventId failedEventId, ErrorOrigin errorOrigin) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            log.trace("[{}][{}] Processing after error for event {}", tenantId, agentId, failedEventId);

            appEventService.updateStatus(failedEventId, AgentAppEventStatus.ERROR, null);
            boolean dispatchNextEvent = true;

            AgentAppEvent event = appEventService.findById(tenantId, failedEventId);
            if (shouldRollbackPendingDeletion(event)) {
                rollbackPendingDeletion(tenantId, event.getApplicationId());
            } else if (shouldEnqueueRollbackEvent(event, errorOrigin)) {
                appEventService.save(tenantId, buildRollbackEvent(tenantId, event));
                dispatchNextEvent = false;
            }

            return dispatchNextEvent;
        }));
    }

    private boolean shouldEnqueueRollbackEvent(AgentAppEvent event, ErrorOrigin errorOrigin) {
        return errorOrigin == ErrorOrigin.SERVER
                && event != null
                && event.getActionType() == AgentAppEventActionType.UPDATE;
    }

    private boolean shouldRollbackPendingDeletion(AgentAppEvent event) {
        return event != null && event.getActionType() == AgentAppEventActionType.DELETE;
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
        rollbackEvent.setUpdatedTime(System.currentTimeMillis());
        return rollbackEvent;
    }
}
