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

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.gen.agent.v1.AgentToServer;
import org.thingsboard.server.gen.agent.v1.ApplicationMetrics;
import org.thingsboard.server.gen.agent.v1.ComposeState;
import org.thingsboard.server.gen.agent.v1.ContainerInfo;
import org.thingsboard.server.gen.agent.v1.ProjectStateSync;
import org.thingsboard.server.service.agent.AgentInboundMsgCtx;
import org.thingsboard.server.service.agent.compose.AgentAppUnitKey;
import org.thingsboard.server.service.agent.compose.ComposeAgentAppCreator;
import org.thingsboard.server.service.agent.compose.ComposeApplicationMetricsRecorder;
import org.thingsboard.server.service.agent.compose.ComposeUnitMetricsRecorder;
import org.thingsboard.server.service.agent.compose.ComposeUnitStateWriter;
import org.thingsboard.server.service.agent.compose.ComposeUnitsSynchronizer;
import org.thingsboard.server.service.agent.compose.ImageDigestChecker;
import org.thingsboard.server.service.agent.session.AgentSession;
import org.thingsboard.server.service.agent.session.AgentSessionState;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComposeProjectSyncMessageHandlerTest {

    @Mock
    private AgentApplicationService appService;
    @Mock
    private ComposeAgentAppCreator appCreator;
    @Mock
    private ComposeUnitsSynchronizer unitsSynchronizer;
    @Mock
    private ComposeUnitStateWriter unitStateWriter;
    @Mock
    private ComposeUnitMetricsRecorder unitMetricsRecorder;
    @Mock
    private ComposeApplicationMetricsRecorder appMetricsRecorder;
    @Mock
    private ImageDigestChecker imageDigestChecker;

    private final AgentAppAutoInstallLockRegistry autoInstallLockRegistry = new AgentAppAutoInstallLockRegistry();

    @Mock
    private AgentSession session;
    @Mock
    private AgentSessionState sessionState;

    private ComposeProjectSyncMessageHandler handler;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentId AGENT_ID = new AgentId(UUID.randomUUID());
    private static final AgentApplicationId APP_ID = new AgentApplicationId(UUID.randomUUID());
    private static final String PROJECT_NAME = "tb-edge";

    private AgentApplication existingApp;

    @BeforeEach
    void setUp() {
        existingApp = new AgentApplication();
        existingApp.setId(APP_ID);
        existingApp.setName(PROJECT_NAME);

        handler = new ComposeProjectSyncMessageHandler(
                appService, appCreator, unitsSynchronizer, unitStateWriter, unitMetricsRecorder,
                appMetricsRecorder, imageDigestChecker, autoInstallLockRegistry);

        when(session.getState()).thenReturn(sessionState);
        when(sessionState.getTenantId()).thenReturn(TENANT_ID);
        when(sessionState.getAgentId()).thenReturn(AGENT_ID);
    }

    @Test
    void composeJsonPresent_callsSyncUnitsAndAllDownstream() {
        Map<String, ContainerInfo> containerStates = Map.of(
                "web", ContainerInfo.newBuilder().setState("running").setCpuPercent(5.0).build());
        ApplicationMetrics appMetrics = ApplicationMetrics.newBuilder().setCpuPercent(10.0).setMemoryBytes(1024L).build();
        AgentToServer msg = projectSync(composeStateWithJson(containerStates, appMetrics));

        Map<AgentAppUnitKey, AgentAppUnit> units =
                Map.of(new AgentAppUnitKey(AgentAppUnitType.CONTAINER, "web"), newUnit("web"));
        when(appService.findByProjectName(TENANT_ID, AGENT_ID, PROJECT_NAME)).thenReturn(existingApp);
        when(unitsSynchronizer.syncUnits(eq(TENANT_ID), eq(APP_ID), any(JsonNode.class))).thenReturn(units);

        handler.handle(new AgentInboundMsgCtx(session, msg));

        ArgumentCaptor<JsonNode> jsonCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(unitsSynchronizer).syncUnits(eq(TENANT_ID), eq(APP_ID), jsonCaptor.capture());
        verify(unitsSynchronizer, never()).loadUnits(any(), any());
        verify(unitStateWriter).writeStates(TENANT_ID, units, containerStates);
        verify(unitMetricsRecorder).record(TENANT_ID, units, containerStates, Map.of());
        verify(imageDigestChecker).checkImageDigest(eq(TENANT_ID), eq(existingApp), eq(containerStates), eq(jsonCaptor.getValue()));
        verify(appMetricsRecorder).record(TENANT_ID, APP_ID, appMetrics);
    }

    @Test
    void stateOnly_callsLoadUnitsAndDownstreamWithNullComposeJson() {
        Map<String, ContainerInfo> containerStates = Map.of(
                "web", ContainerInfo.newBuilder().setState("running").build());
        AgentToServer msg = projectSync(composeStateWithoutJson(containerStates, null));

        Map<AgentAppUnitKey, AgentAppUnit> units =
                Map.of(new AgentAppUnitKey(AgentAppUnitType.CONTAINER, "web"), newUnit("web"));
        when(appService.findByProjectName(TENANT_ID, AGENT_ID, PROJECT_NAME)).thenReturn(existingApp);
        when(unitsSynchronizer.loadUnits(TENANT_ID, APP_ID)).thenReturn(units);

        handler.handle(new AgentInboundMsgCtx(session, msg));

        verify(unitsSynchronizer, never()).syncUnits(any(), any(), any());
        verify(unitsSynchronizer).loadUnits(TENANT_ID, APP_ID);
        verify(unitStateWriter).writeStates(TENANT_ID, units, containerStates);
        verify(unitMetricsRecorder).record(TENANT_ID, units, containerStates, Map.of());
        verify(imageDigestChecker).checkImageDigest(TENANT_ID, existingApp, containerStates, null);
        verify(appMetricsRecorder, never()).record(any(), any(), any());
    }

    @Test
    void emptyContainerStatesAndNoComposeJson_skipsUnitResolution() {
        AgentToServer msg = projectSync(composeStateWithoutJson(Map.of(), null));

        when(appService.findByProjectName(TENANT_ID, AGENT_ID, PROJECT_NAME)).thenReturn(existingApp);

        handler.handle(new AgentInboundMsgCtx(session, msg));

        verify(unitsSynchronizer, never()).syncUnits(any(), any(), any());
        verify(unitsSynchronizer, never()).loadUnits(any(), any());
        verify(unitStateWriter).writeStates(TENANT_ID, Map.of(), Map.of());
        verify(unitMetricsRecorder).record(TENANT_ID, Map.of(), Map.of(), Map.of());
        verify(imageDigestChecker).checkImageDigest(TENANT_ID, existingApp, Map.of(), null);
        verify(appMetricsRecorder, never()).record(any(), any(), any());
    }

    @Test
    void applicationMetricsAbsent_skipsAppMetricsRecorder() {
        Map<String, ContainerInfo> containerStates = Map.of(
                "web", ContainerInfo.newBuilder().setState("running").build());
        AgentToServer msg = projectSync(composeStateWithoutJson(containerStates, null));

        when(appService.findByProjectName(TENANT_ID, AGENT_ID, PROJECT_NAME)).thenReturn(existingApp);
        when(unitsSynchronizer.loadUnits(TENANT_ID, APP_ID))
                .thenReturn(Map.of(new AgentAppUnitKey(AgentAppUnitType.CONTAINER, "web"), newUnit("web")));

        handler.handle(new AgentInboundMsgCtx(session, msg));

        verify(appMetricsRecorder, never()).record(any(), any(), any());
    }

    private static AgentToServer projectSync(ComposeState composeState) {
        return AgentToServer.newBuilder()
                .setProjectSync(ProjectStateSync.newBuilder()
                        .setProjectName(PROJECT_NAME)
                        .setCompose(composeState)
                        .build())
                .build();
    }

    private static ComposeState composeStateWithJson(Map<String, ContainerInfo> containerStates, ApplicationMetrics appMetrics) {
        ComposeState.Builder builder = ComposeState.newBuilder()
                .setComposeJson("""
                        {"services": {"web": {"image": "nginx:latest"}}}
                        """)
                .putAllContainerStates(containerStates);
        if (appMetrics != null) {
            builder.setApplicationMetrics(appMetrics);
        }
        return builder.build();
    }

    private static ComposeState composeStateWithoutJson(Map<String, ContainerInfo> containerStates, ApplicationMetrics appMetrics) {
        ComposeState.Builder builder = ComposeState.newBuilder()
                .putAllContainerStates(containerStates);
        if (appMetrics != null) {
            builder.setApplicationMetrics(appMetrics);
        }
        return builder.build();
    }

    private static AgentAppUnit newUnit(String identifier) {
        AgentAppUnit unit = new AgentAppUnit(new AgentAppUnitId(UUID.randomUUID()));
        unit.setAgentApplicationId(APP_ID);
        unit.setIdentifier(identifier);
        unit.setType(AgentAppUnitType.CONTAINER);
        return unit;
    }
}
