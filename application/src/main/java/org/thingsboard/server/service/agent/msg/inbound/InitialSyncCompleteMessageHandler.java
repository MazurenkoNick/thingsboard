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
package org.thingsboard.server.service.agent.msg.inbound;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.agent.AgentAutoInstallService;
import org.thingsboard.server.service.agent.AgentContextComponent;
import org.thingsboard.server.service.agent.AgentInboundMsgCtx;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.Lock;

@Component
@TbCoreComponent
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
        try {
            agentCtx.getAgentEventExecutor().execute(() -> runAutoInstall(tenantId, agentId));
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
