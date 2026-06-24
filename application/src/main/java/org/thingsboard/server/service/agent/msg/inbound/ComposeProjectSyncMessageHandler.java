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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.service.agent.compose.AgentAppUnitKey;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.gen.agent.v1.ComposeState;
import org.thingsboard.server.gen.agent.v1.ContainerInfo;
import org.thingsboard.server.gen.agent.v1.ProjectStateSync;
import org.thingsboard.server.gen.agent.v1.VolumeInfo;
import org.thingsboard.server.service.agent.AgentInboundMsgCtx;
import org.thingsboard.server.service.agent.compose.ComposeAgentAppCreator;
import org.thingsboard.server.service.agent.compose.ComposeApplicationMetricsRecorder;
import org.thingsboard.server.service.agent.compose.ComposeUnitMetricsRecorder;
import org.thingsboard.server.service.agent.compose.ComposeUnitStateWriter;
import org.thingsboard.server.service.agent.compose.ComposeUnitsSynchronizer;
import org.thingsboard.server.service.agent.compose.ImageDigestChecker;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class ComposeProjectSyncMessageHandler implements AgentInboundMessageHandler {

    private final AgentApplicationService appService;
    private final ComposeAgentAppCreator appCreator;
    private final ComposeUnitsSynchronizer unitsSynchronizer;
    private final ComposeUnitStateWriter unitStateWriter;
    private final ComposeUnitMetricsRecorder unitMetricsRecorder;
    private final ComposeApplicationMetricsRecorder appMetricsRecorder;
    private final ImageDigestChecker imageDigestChecker;
    private final AgentAppAutoInstallLockRegistry autoInstallLockRegistry;
    private final Map<AgentId, ReentrantLock> appCreationLock
            = new ConcurrentReferenceHashMap<>(16, ConcurrentReferenceHashMap.ReferenceType.WEAK);

    @Override
    public boolean canHandle(AgentInboundMsgCtx msgCtx) {
        var msg = msgCtx.msg();
        return msg.hasProjectSync()
                && msg.getProjectSync().hasCompose()
                && !msg.getProjectSync().getRemoved();
    }

    @Override
    public void handle(AgentInboundMsgCtx msgCtx) {
        ProjectStateSync projectSync = msgCtx.msg().getProjectSync();
        TenantId tenantId = msgCtx.sessionState().getTenantId();
        AgentId agentId = msgCtx.sessionState().getAgentId();

        Lock readLock = autoInstallLockRegistry.forAgent(agentId).readLock();
        readLock.lock();
        try {
            doHandle(tenantId, agentId, projectSync);
        } catch (Exception e) {
            log.error("[{}][{}] Failed to process compose sync for project: {}", tenantId, agentId, projectSync.getProjectName(), e);
        } finally {
            readLock.unlock();
        }
    }

    private void doHandle(TenantId tenantId, AgentId agentId, ProjectStateSync projectSync) {
        String projectName = projectSync.getProjectName();
        ComposeState composeState = projectSync.getCompose();
        log.trace("[{}][{}] Processing composeState sync for project [{}], hasComposeJson: {}, containerStates: {}",
                tenantId, agentId, projectName, composeState.hasComposeJson(), composeState.getContainerStatesMap().keySet());

        AgentApplication app = appService.findByProjectName(tenantId, agentId, projectName);
        if (app == null) {
            if (!composeState.hasComposeJson()) {
                log.warn("[{}][{}] No agent application found for project [{}] and no composeJson provided, skipping",
                        tenantId, agentId, projectName);
                return;
            }
            app = getOrCreateApplication(tenantId, agentId, composeState, projectName);
        }

        Map<String, ContainerInfo> containerStates = composeState.getContainerStatesMap();
        Map<String, VolumeInfo> volumeStates = composeState.getVolumeStatesMap();
        JsonNode composeJson = composeState.hasComposeJson() ? jsonStringToJsonNode(composeState.getComposeJson()) : null;

        Map<AgentAppUnitKey, AgentAppUnit> units;
        if (composeJson != null) {
            units = unitsSynchronizer.syncUnits(tenantId, app.getId(), composeJson);
        } else if (!containerStates.isEmpty() || !volumeStates.isEmpty()) {
            units = unitsSynchronizer.loadUnits(tenantId, app.getId());
        } else {
            units = Map.of();
        }

        unitStateWriter.writeStates(tenantId, units, containerStates);
        unitMetricsRecorder.record(tenantId, units, containerStates, volumeStates);
        imageDigestChecker.checkImageDigest(tenantId, app, containerStates, composeJson);
        if (composeState.hasApplicationMetrics()) {
            appMetricsRecorder.record(tenantId, app.getId(), composeState.getApplicationMetrics());
        }
    }

    private AgentApplication getOrCreateApplication(TenantId tenantId, AgentId agentId,
                                                    ComposeState externalCompose, String projectName) {
        ReentrantLock lock = appCreationLock.computeIfAbsent(agentId, k -> new ReentrantLock());
        lock.lock();
        try {
            AgentApplication app = appService.findByProjectName(tenantId, agentId, projectName);
            if (app != null) {
                return app;
            }
            return appCreator.createApp(tenantId, agentId, projectName, externalCompose);
        } finally {
            lock.unlock();
        }
    }

    private JsonNode jsonStringToJsonNode(String json) {
        try {
            return JacksonUtil.OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Couldn't parse docker compose JSON", e);
        }
    }
}
