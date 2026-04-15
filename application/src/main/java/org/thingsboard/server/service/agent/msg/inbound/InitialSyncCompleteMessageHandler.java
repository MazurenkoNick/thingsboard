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
package org.thingsboard.server.service.agent.msg.inbound;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.agent.AgentAutoInstallService;
import org.thingsboard.server.service.agent.AgentContextComponent;
import org.thingsboard.server.service.agent.AgentInboundMsgCtx;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.Lock;

@Component
@Slf4j
@RequiredArgsConstructor
public class InitialSyncCompleteMessageHandler implements AgentInboundMessageHandler {

    private final AgentAutoInstallService autoInstallService;
    private final AgentContextComponent agentCtx;
    private final AgentAppAutoInstallLockRegistry autoInstallLockRegistry;

    @Override
    public boolean canHandle(AgentInboundMsgCtx msgCtx) {
        return msgCtx.msg().hasInitialSyncComplete();
    }

    @Override
    public void handle(AgentInboundMsgCtx msgCtx) {
        TenantId tenantId = msgCtx.sessionState().getTenantId();
        AgentId agentId = msgCtx.sessionState().getAgentId();
        log.debug("[{}][{}] Received InitialSyncComplete, scheduling auto-install", tenantId, agentId);
        // Offload to the event executor so the gRPC thread is not blocked waiting on
        // in-flight syncs to drain before the write lock can be acquired.
        try {
            agentCtx.getAgentEventExecutor().submit(() -> runAutoInstall(tenantId, agentId));
        } catch (RejectedExecutionException e) {
            log.warn("[{}][{}] Failed to schedule auto-install: {}", tenantId, agentId, e.getMessage());
        }
    }

    private void runAutoInstall(TenantId tenantId, AgentId agentId) {
        Lock writeLock = autoInstallLockRegistry.forAgent(agentId).writeLock();
        writeLock.lock();
        try {
            autoInstallService.autoInstall(tenantId, agentId);
        } catch (Exception e) {
            log.error("[{}][{}] Auto-install failed", tenantId, agentId, e);
        } finally {
            writeLock.unlock();
        }
    }
}
