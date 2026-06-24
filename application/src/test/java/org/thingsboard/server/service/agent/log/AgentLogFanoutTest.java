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
package org.thingsboard.server.service.agent.log;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.cache.logexternal.LogChunkBuffer;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.logexternal.LogChunk;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.agent.AgentAppUnitService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.agent.AgentGrpcService;
import org.thingsboard.server.service.agent.session.AgentSessionState;
import org.thingsboard.server.service.subscription.SubscriptionManagerService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentLogFanoutTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentId AGENT_ID = new AgentId(UUID.randomUUID());
    private static final AgentAppUnitId UNIT_ID = new AgentAppUnitId(UUID.randomUUID());
    private static final String PROJECT_NAME = "my-project";
    private static final String UNIT_IDENTIFIER = "my-service";

    @Mock
    private AgentGrpcService agentGrpcService;
    @Mock
    private AgentAppUnitService unitService;
    @Mock
    private LogChunkBuffer logChunkBuffer;
    @Mock
    private SubscriptionManagerService subscriptionManagerService;
    @Mock
    private PartitionService partitionService;
    @Mock
    private TbClusterService clusterService;
    @Mock
    private TopicPartitionInfo tpi;

    @InjectMocks
    private AgentLogFanout fanout;

    private AgentSessionState session;

    @BeforeEach
    void setUp() {
        AgentAppUnit unit = new AgentAppUnit(UNIT_ID);
        when(unitService.findByAgentAndProjectAndIdentifier(TENANT_ID, AGENT_ID, PROJECT_NAME, UNIT_IDENTIFIER))
                .thenReturn(unit);
        Agent agent = new Agent();
        agent.setTenantId(TENANT_ID);
        agent.setId(AGENT_ID);
        session = new AgentSessionState();
        session.setAgent(agent);
    }

    private org.thingsboard.server.gen.agent.v1.AgentLogChunk protoChunk(String... lines) {
        return org.thingsboard.server.gen.agent.v1.AgentLogChunk.newBuilder()
                .setUnitId(UNIT_IDENTIFIER).setProjectName(PROJECT_NAME)
                .addAllLines(java.util.Arrays.asList(lines))
                .build();
    }

    @Test
    void fanoutWritesChunkAndEmitsLocalWatermark() {
        when(partitionService.resolve(eq(ServiceType.TB_CORE), eq(TENANT_ID), eq(UNIT_ID))).thenReturn(tpi);
        when(tpi.isMyPartition()).thenReturn(true);
        when(logChunkBuffer.append(eq(TENANT_ID), eq(UNIT_ID), any(LogChunk.class))).thenReturn(7L);

        fanout.fanout(session, protoChunk("a", "b"));

        ArgumentCaptor<LogChunk> chunkCaptor = ArgumentCaptor.forClass(LogChunk.class);
        verify(logChunkBuffer).append(eq(TENANT_ID), eq(UNIT_ID), chunkCaptor.capture());
        assertThat(chunkCaptor.getValue().getLines()).containsExactly("a", "b");

        verify(subscriptionManagerService).onLogStreamUpdate(eq(TENANT_ID), eq(UNIT_ID), eq(7L), any(TbCallback.class));
        verify(clusterService, never()).pushMsgToCore(any(), any(UUID.class), any(), any());
    }

    @Test
    void fanoutForwardsViaClusterServiceWhenRemote() {
        when(partitionService.resolve(eq(ServiceType.TB_CORE), eq(TENANT_ID), eq(UNIT_ID))).thenReturn(tpi);
        when(tpi.isMyPartition()).thenReturn(false);
        when(logChunkBuffer.append(eq(TENANT_ID), eq(UNIT_ID), any(LogChunk.class))).thenReturn(42L);

        fanout.fanout(session, protoChunk("x"));

        verify(logChunkBuffer).append(eq(TENANT_ID), eq(UNIT_ID), any(LogChunk.class));
        verify(clusterService).pushMsgToCore(eq(tpi), eq(UNIT_ID.getId()), any(), eq(null));
        verify(subscriptionManagerService, never()).onLogStreamUpdate(any(), any(), org.mockito.ArgumentMatchers.anyLong(), any());
    }

    @Test
    void fanoutEmptyChunkNoOp() {
        fanout.fanout(session, protoChunk());

        verify(logChunkBuffer, never()).append(any(), any(), any());
        verify(subscriptionManagerService, never()).onLogStreamUpdate(any(), any(), org.mockito.ArgumentMatchers.anyLong(), any());
        verify(clusterService, never()).pushMsgToCore(any(), any(UUID.class), any(), any());
    }

    @Test
    void fanoutUnitNotFoundSendsStopToAgent() {
        when(unitService.findByAgentAndProjectAndIdentifier(TENANT_ID, AGENT_ID, PROJECT_NAME, UNIT_IDENTIFIER))
                .thenReturn(null);

        fanout.fanout(session, protoChunk("x", "y"));

        verify(agentGrpcService).stopLogStream(AGENT_ID, PROJECT_NAME, UNIT_IDENTIFIER);
        verify(logChunkBuffer, never()).append(any(), any(), any());
    }
}
