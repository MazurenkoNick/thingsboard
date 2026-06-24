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
package org.thingsboard.server.service.agent;

import com.google.common.util.concurrent.ListeningExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.AgentAppEventStatusUpdate;
import org.thingsboard.server.common.data.agent.ErrorOrigin;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.gen.agent.v1.AckStatus;
import org.thingsboard.server.gen.agent.v1.CommandAck;
import org.thingsboard.server.gen.agent.v1.CommandId;
import org.thingsboard.server.gen.agent.v1.CommandProgress;
import org.thingsboard.server.gen.agent.v1.CommandResult;
import org.thingsboard.server.gen.agent.v1.StepId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.event.AgentEventErrorHandler;
import org.thingsboard.server.service.agent.event.AgentEventProcessor;

import java.util.Objects;
import java.util.UUID;

@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class CommandFeedbackHandler {

    private final AgentAppEventService appEventService;
    private final AgentEventProcessor agentEventProcessor;
    private final AgentEventErrorHandler eventErrorHandler;
    private final AgentContextComponent agentCtx;

    public void onCommandAck(TenantId tenantId, AgentId agentId, CommandAck ack) {
        AgentAppEventId eventId = toEventId(ack.getCommandId());
        log.trace("[{}][{}] Received CommandAck for event {}, status: {}", tenantId, agentId, eventId, ack.getStatus());

        getExecutor().submit(() -> doOnCommandAck(tenantId, agentId, ack, eventId));
    }

    private void doOnCommandAck(TenantId tenantId, AgentId agentId, CommandAck ack, AgentAppEventId eventId) {
        doWithFallback(tenantId, agentId, eventId, () -> {
            if (ack.getStatus() == AckStatus.ACCEPTED) {
                appEventService.updateStatus(eventId, AgentAppEventStatusUpdate.builder()
                        .status(AgentAppEventStatus.QUEUED)
                        .currentActivity(ack.hasMessage() ? ack.getMessage() : null)
                        .build());
            } else {
                eventErrorHandler.onFailure(tenantId, agentId, eventId, ErrorOrigin.AGENT,
                        ack.hasMessage() ? ack.getMessage() : null);
            }
        });
    }

    public void onCommandProgress(TenantId tenantId, AgentId agentId, CommandProgress progress) {
        AgentAppEventId eventId = toEventId(progress.getCommandId());
        log.trace("[{}][{}] Received CommandProgress for event {}, stage: {}", tenantId, agentId, eventId, progress.getStage());
        getExecutor().submit(() ->
                doWithFallback(tenantId, agentId, eventId, () ->
                        appEventService.updateStatus(eventId, AgentAppEventStatusUpdate.builder()
                                .status(AgentAppEventStatus.PROCESSING)
                                .currentActivity(progress.hasMessage() ? progress.getMessage() : null)
                                .build()))
        );
    }

    public void onCommandResult(TenantId tenantId, AgentId agentId, CommandResult result) {
        AgentAppEventId eventId = toEventId(result.getCommandId());
        log.trace("[{}][{}] Received CommandResult for event {}, success: {}", tenantId, agentId, eventId, result.getSuccess());

        getExecutor().submit(() -> doOnCommandResult(tenantId, agentId, result, eventId));
    }

    private void doOnCommandResult(TenantId tenantId, AgentId agentId, CommandResult result, AgentAppEventId eventId) {
        doWithFallback(tenantId, agentId, eventId, () -> {
            AgentAppEvent event = appEventService.findById(tenantId, eventId);
            if (event == null) {
                log.warn("[{}] Event not found for CommandResult: {}", tenantId, eventId);
                return;
            }
            if (event.getStatus() != null && event.getStatus().isTerminated()) {
                log.info("[{}][{}] Ignoring CommandResult for already terminated event {} (status {})",
                        tenantId, agentId, eventId, event.getStatus());
                return;
            }
            if (!result.getSuccess()) {
                eventErrorHandler.onFailure(tenantId, agentId, event.getId(), ErrorOrigin.AGENT, result.getMessage());
                return;
            }
            UUID resultStepId = toStepId(result.getStep());
            if (result.hasStep() && !Objects.equals(resultStepId, event.getCurrentStepId())) {
                log.info("[{}][{}] Ignoring stale/duplicate CommandResult for event {}: result step {} does not match current step {}",
                        tenantId, agentId, eventId, resultStepId, event.getCurrentStepId());
                return;
            }
            appEventService.updateStatus(eventId, AgentAppEventStatusUpdate.builder()
                    .status(AgentAppEventStatus.PROCESSING)
                    .currentActivity(result.hasMessage() ? result.getMessage() : null)
                    .build());
            agentEventProcessor.processNextStepOrFinish(tenantId, agentId, event);
        });
    }

    private void doWithFallback(TenantId tenantId, AgentId agentId, AgentAppEventId eventId, Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.error("[{}][{}] Failed to process feedback for event {}", tenantId, agentId, eventId, e);
            eventErrorHandler.onFailure(tenantId, agentId, eventId, ErrorOrigin.SERVER, e.getMessage());
        }
    }

    private ListeningExecutorService getExecutor() {
        return agentCtx.getAgentEventExecutor();
    }

    private AgentAppEventId toEventId(CommandId commandId) {
        return new AgentAppEventId(new UUID(commandId.getIdMSB(), commandId.getIdLSB()));
    }

    private UUID toStepId(StepId stepId) {
        return new UUID(stepId.getIdMSB(), stepId.getIdLSB());
    }
}
