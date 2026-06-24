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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.gen.agent.v1.AgentLogChunk;
import org.thingsboard.server.gen.agent.v1.AgentMetricsSync;
import org.thingsboard.server.gen.agent.v1.AgentToServer;
import org.thingsboard.server.service.agent.AgentInboundMsgCtx;
import org.thingsboard.server.service.agent.log.AgentLogFanout;
import org.thingsboard.server.service.agent.session.AgentSession;
import org.thingsboard.server.service.agent.session.AgentSessionState;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentLogChunkMessageHandlerTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentId AGENT_ID = new AgentId(UUID.randomUUID());
    private static final String PROJECT_NAME = "my-project";
    private static final String UNIT_IDENTIFIER = "my-service";

    @Mock
    private AgentLogFanout logFanout;
    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private AgentSession session;
    @Mock
    private AgentSessionState sessionState;
    @Mock
    private TelemetrySubscriptionService tsSubService;

    private AgentLogChunkMessageHandler handler;

    @BeforeEach
    void setUp() {
        when(sessionState.getTenantId()).thenReturn(TENANT_ID);
        when(sessionState.getAgentId()).thenReturn(AGENT_ID);
        when(session.getState()).thenReturn(sessionState);
        when(rateLimitService.checkRateLimit(LimitedApi.AGENT_LOG_CHUNKS, TENANT_ID)).thenReturn(true);
        when(rateLimitService.checkRateLimit(LimitedApi.AGENT_LOG_CHUNKS_PER_AGENT, TENANT_ID, AGENT_ID)).thenReturn(true);
        handler = new AgentLogChunkMessageHandler(logFanout, rateLimitService);
    }

    @Test
    void canHandleReturnsTrueOnLogChunk() {
        AgentToServer msg = AgentToServer.newBuilder()
                .setLogChunk(AgentLogChunk.newBuilder().setUnitId(UNIT_IDENTIFIER).setProjectName(PROJECT_NAME).build())
                .build();
        AgentInboundMsgCtx ctx = AgentInboundMsgCtx.builder().session(session).msg(msg).build();

        assertThat(handler.canHandle(ctx)).isTrue();
    }

    @Test
    void canHandleReturnsFalseOnAgentMetricsSync() {
        AgentToServer msg = AgentToServer.newBuilder()
                .setAgentMetricsSync(AgentMetricsSync.newBuilder().build())
                .build();
        AgentInboundMsgCtx ctx = AgentInboundMsgCtx.builder().session(session).msg(msg).build();

        assertThat(handler.canHandle(ctx)).isFalse();
    }

    @Test
    void handleDelegatesToFanout() {
        AgentLogChunk chunk = AgentLogChunk.newBuilder()
                .setUnitId(UNIT_IDENTIFIER).setProjectName(PROJECT_NAME)
                .addLines("hello").addLines("world")
                .setDropped(3)
                .build();
        AgentToServer msg = AgentToServer.newBuilder().setLogChunk(chunk).build();
        AgentInboundMsgCtx ctx = AgentInboundMsgCtx.builder().session(session).msg(msg).build();

        handler.handle(ctx);

        ArgumentCaptor<AgentLogChunk> captor = ArgumentCaptor.forClass(AgentLogChunk.class);
        verify(logFanout).fanout(eq(sessionState), captor.capture());
        AgentLogChunk passed = captor.getValue();
        assertThat(passed.getUnitId()).isEqualTo(UNIT_IDENTIFIER);
        assertThat(passed.getProjectName()).isEqualTo(PROJECT_NAME);
        assertThat(passed.getLinesList()).containsExactly("hello", "world");
        assertThat(passed.getDropped()).isEqualTo(3);
        verifyNoInteractions(tsSubService);
    }

    @Test
    void handleDropsChunkWhenTenantRateLimited() {
        when(rateLimitService.checkRateLimit(LimitedApi.AGENT_LOG_CHUNKS, TENANT_ID)).thenReturn(false);
        AgentToServer msg = AgentToServer.newBuilder()
                .setLogChunk(AgentLogChunk.newBuilder().setUnitId(UNIT_IDENTIFIER).setProjectName(PROJECT_NAME).build())
                .build();
        AgentInboundMsgCtx ctx = AgentInboundMsgCtx.builder().session(session).msg(msg).build();

        handler.handle(ctx);

        verifyNoInteractions(logFanout);
    }

    @Test
    void handleDropsChunkWhenPerAgentRateLimited() {
        when(rateLimitService.checkRateLimit(LimitedApi.AGENT_LOG_CHUNKS_PER_AGENT, TENANT_ID, AGENT_ID)).thenReturn(false);
        AgentToServer msg = AgentToServer.newBuilder()
                .setLogChunk(AgentLogChunk.newBuilder().setUnitId(UNIT_IDENTIFIER).setProjectName(PROJECT_NAME).build())
                .build();
        AgentInboundMsgCtx ctx = AgentInboundMsgCtx.builder().session(session).msg(msg).build();

        handler.handle(ctx);

        verifyNoInteractions(logFanout);
    }
}
