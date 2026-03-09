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
package org.thingsboard.server.service.agent;

import com.google.common.util.concurrent.ListeningExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.ErrorOrigin;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.gen.agent.v1.AckStatus;
import org.thingsboard.server.gen.agent.v1.CommandAck;
import org.thingsboard.server.gen.agent.v1.CommandId;
import org.thingsboard.server.gen.agent.v1.CommandProgress;
import org.thingsboard.server.gen.agent.v1.CommandResult;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.event.AgentEventErrorHandler;
import org.thingsboard.server.service.agent.event.AgentEventProcessor;

import java.util.UUID;

@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class CommandFeedbackHandler {

    private final AgentAppEventService appEventService;
    private final AgentEventProcessor agentEventProcessor;
    private final AgentEventErrorHandler eventErrorHandler;
    private final AgentApplicationService appService;
    private final AgentContextComponent agentCtx;

    // todo: handle errors for each onCommand*

    public void onCommandAck(TenantId tenantId, AgentId agentId, CommandAck ack) {
        AgentAppEventId eventId = toEventId(ack.getCommandId());
        log.trace("[{}][{}] Received CommandAck for event {}, status: {}", tenantId, agentId, eventId, ack.getStatus());

        if (ack.getStatus() == AckStatus.ACCEPTED) {
            appEventService.updateStatus(eventId, AgentAppEventStatus.QUEUED, null);
        } else {
            getExecutor().submit(() -> eventErrorHandler.onFailure(tenantId, agentId, eventId, ErrorOrigin.AGENT));
        }
    }

    public void onCommandProgress(TenantId tenantId, AgentId agentId, CommandProgress progress) {
        AgentAppEventId eventId = toEventId(progress.getCommandId());
        log.trace("[{}][{}] Received CommandProgress for event {}, stage: {}", tenantId, agentId, eventId, progress.getStage());
        appEventService.updateStatus(eventId, AgentAppEventStatus.PROCESSING, null);
    }

    public void onCommandResult(TenantId tenantId, AgentId agentId, CommandResult result) {
        AgentAppEventId eventId = toEventId(result.getCommandId());
        log.trace("[{}][{}] Received CommandResult for event {}, success: {}", tenantId, agentId, eventId, result.getSuccess());

        AgentAppEvent event = appEventService.findById(tenantId, eventId);
        if (event == null) {
            log.warn("[{}] Event not found for CommandResult: {}", tenantId, eventId);
            return;
        }
        if (!result.getSuccess()) {
            getExecutor().submit(() -> eventErrorHandler.onFailure(tenantId, agentId, event.getId(), ErrorOrigin.AGENT));
            return;
        }
        if (event.getActionType() == AgentAppEventActionType.DELETE) {
            appService.delete(tenantId, event.getApplicationId());
            return;
        }
        appEventService.updateStatus(eventId, AgentAppEventStatus.PROCESSING, null);
        getExecutor().submit(() -> agentEventProcessor.processNextStepOrFinish(tenantId, agentId, event));
    }

    private ListeningExecutorService getExecutor() {
        return agentCtx.getAgentEventExecutor();
    }

    private AgentAppEventId toEventId(CommandId commandId) {
        return new AgentAppEventId(new UUID(commandId.getIdMSB(), commandId.getIdLSB()));
    }
}
