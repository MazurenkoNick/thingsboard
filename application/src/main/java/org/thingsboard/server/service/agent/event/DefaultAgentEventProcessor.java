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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.agent.StepLinkedListUtils;
import org.thingsboard.server.gen.agent.v1.ServerToAgent;
import org.thingsboard.server.gen.transport.TransportProtos.AgentAppEventNotificationProto;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.AgentAppEventStepsResolver;
import org.thingsboard.server.service.agent.AgentMsgConstructorUtils;
import org.thingsboard.server.service.agent.AgentRpcService;
import org.thingsboard.server.service.agent.AgentSessionNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class DefaultAgentEventProcessor implements AgentEventProcessor {

    private static final int TRY_SEND_MAX_ATTEMPTS = 3;

    @Value("${agents.event.retry_send_delay_ms:1000}")
    private long retrySendDelayMs;

    @Lazy
    private final AgentRpcService agentRpcService;
    private final AgentAppEventService appEventService;
    private final AgentApplicationService appService;
    private final AgentEventWatchdog eventWatchdog;
    private final AgentAppEventStepsResolver eventStepsResolver;

    @Override
    public void onEventNotification(AgentAppEventNotificationProto notification) {
        TenantId tenantId = TenantId.fromUUID(new UUID(notification.getTenantIdMSB(), notification.getTenantIdLSB()));
        AgentId agentId = AgentId.fromMsgAndLsb(notification.getAgentIdMSB(), notification.getAgentIdLSB());
        AgentApplicationId applicationId = AgentApplicationId.fromMsgAndLsb(notification.getApplicationIdMSB(), notification.getApplicationIdLSB());
        AgentApplication application = appService.findById(tenantId, applicationId);

        log.trace("[{}][{}] Processing agent app event notification for application {}", tenantId, agentId, application);
        processNextEventForApp(tenantId, agentId, application);
    }

    @Override
    public void resumeEventsOnReconnect(TenantId tenantId, AgentId agentId) {
        log.trace("[{}] Resuming events on reconnect for agent", agentId);
        forEachApplication(tenantId, agentId, app -> {
            try {
                log.trace("[{}][{}] Checking in-flight events for application {}", tenantId, agentId, app.getId());
                boolean inFlightPresent = resumeInFlightEvent(tenantId, agentId, app);
                if (!inFlightPresent) {
                    log.trace("[{}][{}] No in-flight event found, dispatching next event for application {}", tenantId, agentId, app.getId());
                    dispatchNextEventIfPossible(tenantId, agentId, app);
                }
            } catch (Exception e) {
                log.warn("[{}][{}] Failed to resume events on agent reconnection for application {}", tenantId, agentId, app.getId(), e);
            }
        });
    }

    @Override
    public void processAfterError(TenantId tenantId, AgentId agentId, AgentAppEventId failedEventId) {
        // todo: handle 'retry errors' logic
        log.trace("[{}][{}] Processing after error for event {}", tenantId, agentId, failedEventId);
        AgentApplication application = appService.findByEventId(tenantId, failedEventId);
        eventWatchdog.cancel(agentId, failedEventId);
        processNextEventForApp(tenantId, agentId, application);
    }

    @Override
    public void processNextEventForApp(TenantId tenantId, AgentId agentId, AgentApplication application) {
        log.trace("[{}][{}] Processing next agent app event notification for application {}", tenantId, agentId, application.getId());
        dispatchNextEventIfPossible(tenantId, agentId, application);
    }

    @Override
    public void processNextStepOrFinish(TenantId tenantId, AgentId agentId, AgentAppEvent event) {
        log.trace("[{}][{}] Processing next step or finish for event {}, currentStepId: {}", tenantId, agentId, event.getId(), event.getCurrentStepId());
        AgentApplication application = appService.findById(tenantId, event.getApplicationId());
        if (application == null) {
            log.warn("[{}] Application not found for event {}", tenantId, event.getApplicationId());
            appEventService.updateStatus(event.getId(), AgentAppEventStatus.ERROR, event.getCurrentStepId());
            return;
        }
        List<AgentAppStep> steps = resolveSteps(application, event.getActionType());
        Optional<AgentAppStep> nextStep = StepLinkedListUtils.getNextStep(event.getCurrentStepId(), steps);
        nextStep.ifPresentOrElse(
                step -> sendStep(event, application, step),
                () -> finishEvent(tenantId, agentId, event, application)
        );
    }

    private void finishEvent(TenantId tenantId, AgentId agentId, AgentAppEvent event, AgentApplication application) {
        log.trace("[{}][{}] Finishing event {} with action type {}", tenantId, agentId, event.getId(), event.getActionType());
        appEventService.updateStatus(event.getId(), AgentAppEventStatus.FINISHED, event.getCurrentStepId());
        eventWatchdog.cancel(agentId, event.getId());
        if (event.getActionType() == AgentAppEventActionType.DELETE) {
            log.trace("[{}][{}] Deleting application {} after DELETE event", tenantId, agentId, application.getId());
            appService.delete(tenantId, event.getApplicationId());
        }
        processNextEventForApp(tenantId, agentId, application);
    }

    private void forEachApplication(TenantId tenantId, AgentId agentId, Consumer<AgentApplication> action) {
        PageLink pageLink = new PageLink(100);
        PageData<AgentApplication> pageData;
        do {
            pageData = appService.findByAgentId(tenantId, agentId, pageLink);
            pageData.getData().forEach(action);
            pageLink = pageLink.nextPageLink();
        } while (pageData.hasNext());
    }

    private boolean resumeInFlightEvent(TenantId tenantId, AgentId agentId, AgentApplication application) {
        var inFlightEvent = appEventService.findActiveDeliveredByApplicationId(application.getId());
        inFlightEvent.ifPresent(event -> {
            log.info("[{}][{}] Resuming in-flight event {} for application {}", tenantId, agentId, event.getId(), application.getId());
            AgentAppStep currentStep = resolveCurrentStep(application, event);
            sendStep(event, application, currentStep);
        });
        return inFlightEvent.isPresent();
    }

    private AgentAppStep resolveCurrentStep(AgentApplication application, AgentAppEvent event) {
        List<AgentAppStep> steps = resolveSteps(application, event.getActionType());
        return Optional.ofNullable(StepLinkedListUtils.findByStepId(steps, event.getCurrentStepId()))
                .orElseGet(() -> StepLinkedListUtils.findFirstStep(steps));
    }

    private void dispatchNextEventIfPossible(TenantId tenantId, AgentId agentId, AgentApplication application) {
        AgentApplicationId applicationId = application.getId();
        if (appEventService.hasActiveEventForApplication(applicationId)) {
            log.trace("[{}][{}] Active event exists for application, skipping", tenantId, applicationId);
            return;
        }
        appEventService.findOldestPendingByApplicationId(applicationId).ifPresentOrElse(
                pendingEvent -> dispatchNextEvent(tenantId, agentId, application, pendingEvent),
                () -> log.trace("[{}][{}] No pending events for application", tenantId, applicationId)
        );
    }

    private void dispatchNextEvent(TenantId tenantId, AgentId agentId,
                                   AgentApplication application, AgentAppEvent event) {
        log.trace("[{}][{}] Dispatching event {} with action type {} for application {}", tenantId, agentId, event.getId(), event.getActionType(), application.getId());
        if (!appEventService.markDelivered(event.getId())) {
            log.trace("[{}][{}] Failed to claim event {} (already claimed by another node or thread)", tenantId, agentId, event.getId());
            return;
        }
        List<AgentAppStep> steps = resolveSteps(application, event.getActionType());
        AgentAppStep firstStep = StepLinkedListUtils.findFirstStep(steps);
        log.trace("[{}][{}] Resolved {} steps for event {}, first step: {}", tenantId, agentId, steps.size(), event.getId(), firstStep.getId());

        sendStep(event, application, firstStep);
    }

    private void sendStep(AgentAppEvent event, AgentApplication application, AgentAppStep step) {
        log.trace("[{}][{}] Sending step {} for event {}", application.getTenantId(), application.getAgentId(), step.getId(), event.getId());
        ServerToAgent msg = AgentMsgConstructorUtils.buildAppCommand(event, application, step);
        appEventService.updateStatus(event.getId(), AgentAppEventStatus.PENDING, step.getId());
        eventWatchdog.schedule(application, event, this::sendStep);
        trySend(application, event, msg);
    }

    private void trySend(AgentApplication application, AgentAppEvent event, ServerToAgent msg) {
        int firstAttempt = 0;
        trySend(application, event, msg, firstAttempt);
    }

    private void trySend(AgentApplication application, AgentAppEvent event, ServerToAgent msg, int attempt) {
        try {
            if (attempt++ >= TRY_SEND_MAX_ATTEMPTS) {
                log.warn("[{}][{}] Couldn't send msg to agent; retry limit reached: {}",
                        application.getTenantId(), application.getAgentId(), TRY_SEND_MAX_ATTEMPTS);
                return;
            }
            log.trace("[{}][{}] Trying to send event {} to agent, attempt {}", application.getTenantId(), application.getAgentId(), event.getId(), attempt);
            final int nextAttempt = attempt;
            boolean pushed = agentRpcService.push(application.getAgentId(), msg);
            if (!pushed) {
                log.warn("[{}][{}] Failed to push to agent (backpressure), scheduling retry",
                        application.getTenantId(), application.getAgentId());
                eventWatchdog.getScheduler().schedule(
                        () -> trySend(application, event, msg, nextAttempt),
                        retrySendDelayMs, TimeUnit.MILLISECONDS);
            }
        } catch (AgentSessionNotFoundException e) {
            log.trace("[{}] No active session for agent. Will resend on reconnect", application.getAgentId());
            eventWatchdog.cancel(application.getAgentId(), event.getId());
        }
    }

    private List<AgentAppStep> resolveSteps(AgentApplication application, AgentAppEventActionType actionType) {
        return eventStepsResolver.resolveSteps(application, actionType);
    }
}
