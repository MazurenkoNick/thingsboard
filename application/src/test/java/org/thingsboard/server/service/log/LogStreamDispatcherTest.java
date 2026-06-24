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
package org.thingsboard.server.service.log;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.cache.logexternal.LogChunkBuffer;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.logexternal.LogChunk;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.service.log.sub.LogsSubscriptionUpdate;
import org.thingsboard.server.service.subscription.TbLocalSubscriptionService;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogStreamDispatcherTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final EntityId ENTITY_ID = new AgentAppUnitId(UUID.randomUUID());

    @Mock
    private LogChunkBuffer logChunkBuffer;
    @Mock
    private TbLocalSubscriptionService localSubscriptionService;
    @Mock
    private StatsFactory statsFactory;

    private LogStreamDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new LogStreamDispatcher(logChunkBuffer, localSubscriptionService, statsFactory);
        org.springframework.test.util.ReflectionTestUtils.setField(dispatcher, "deliveryExecutor",
                com.google.common.util.concurrent.MoreExecutors.newDirectExecutorService());
    }

    @Test
    void emitsChunkRangeAsSingleUpdate() {
        LogChunk chunkA = chunkWithLines("A", 3);
        LogChunk chunkB = chunkWithLines("B", 2);

        when(logChunkBuffer.range(TENANT_ID, ENTITY_ID, 1L, 2L)).thenReturn(List.of(chunkA, chunkB));

        dispatcher.onWatermark(TENANT_ID, ENTITY_ID, 2L, TbCallback.EMPTY);

        ArgumentCaptor<LogsSubscriptionUpdate> captor = ArgumentCaptor.forClass(LogsSubscriptionUpdate.class);
        verify(localSubscriptionService).onLogsUpdate(eq(ENTITY_ID), captor.capture(), any(TbCallback.class));

        LogsSubscriptionUpdate update = captor.getValue();
        assertThat(update.getLatestSeq()).isEqualTo(2L);
        assertThat(update.getLines()).containsExactly("A0", "A1", "A2", "B0", "B1");
        assertThat(update.getDroppedLines()).isZero();
        assertThat(update.getEvictedChunks()).isZero();
    }

    @Test
    void aggregatesDroppedAcrossChunks() {
        LogChunk dropped = new LogChunk();
        dropped.setLines(List.of());
        dropped.setDropped(7);

        when(logChunkBuffer.range(TENANT_ID, ENTITY_ID, 1L, 1L)).thenReturn(List.of(dropped));

        dispatcher.onWatermark(TENANT_ID, ENTITY_ID, 1L, TbCallback.EMPTY);

        ArgumentCaptor<LogsSubscriptionUpdate> captor = ArgumentCaptor.forClass(LogsSubscriptionUpdate.class);
        verify(localSubscriptionService).onLogsUpdate(eq(ENTITY_ID), captor.capture(), any(TbCallback.class));

        assertThat(captor.getValue().getLines()).isEmpty();
        assertThat(captor.getValue().getDroppedLines()).isEqualTo(7);
        assertThat(captor.getValue().getEvictedChunks()).isZero();
        assertThat(captor.getValue().getLatestSeq()).isEqualTo(1L);
    }

    @Test
    void reportsEvictedChunksSeparatelyFromDroppedLines() {
        LogChunk first = chunkWithLines("a", 2);
        when(logChunkBuffer.range(TENANT_ID, ENTITY_ID, 1L, 2L)).thenReturn(List.of(first));
        dispatcher.onWatermark(TENANT_ID, ENTITY_ID, 2L, TbCallback.EMPTY);

        LogChunk later = chunkWithLines("b", 1);
        when(logChunkBuffer.range(TENANT_ID, ENTITY_ID, 3L, 5L)).thenReturn(List.of(later));
        dispatcher.onWatermark(TENANT_ID, ENTITY_ID, 5L, TbCallback.EMPTY);

        ArgumentCaptor<LogsSubscriptionUpdate> captor = ArgumentCaptor.forClass(LogsSubscriptionUpdate.class);
        verify(localSubscriptionService, org.mockito.Mockito.times(2))
                .onLogsUpdate(eq(ENTITY_ID), captor.capture(), any(TbCallback.class));

        LogsSubscriptionUpdate second = captor.getAllValues().get(1);
        assertThat(second.getEvictedChunks()).isEqualTo(2);
        assertThat(second.getDroppedLines()).isZero();
    }

    @Test
    void noUpdateWhenSeqAlreadySeen() {
        when(logChunkBuffer.range(TENANT_ID, ENTITY_ID, 1L, 5L)).thenReturn(List.of(chunkWithLines("x", 1)));

        dispatcher.onWatermark(TENANT_ID, ENTITY_ID, 5L, TbCallback.EMPTY);
        dispatcher.onWatermark(TENANT_ID, ENTITY_ID, 5L, TbCallback.EMPTY);

        verify(localSubscriptionService).onLogsUpdate(eq(ENTITY_ID), any(), any(TbCallback.class));
    }

    @Test
    void streamLogTailEmitsNothingWhenTailEmpty() {
        when(logChunkBuffer.latestTailSeq(TENANT_ID, ENTITY_ID)).thenReturn(0L);

        List<LogsSubscriptionUpdate> collected = new java.util.ArrayList<>();
        dispatcher.streamLogTail(TENANT_ID, ENTITY_ID, 0L, collected::add);

        assertThat(collected).isEmpty();
    }

    @Test
    void streamLogTailEmitsOneUpdatePerRetainedChunkWithLatestSeqStamped() {
        when(logChunkBuffer.latestTailSeq(TENANT_ID, ENTITY_ID)).thenReturn(10L);

        LogChunk a = chunkWithLines("a", 2);
        LogChunk b = chunkWithLines("b", 3);
        when(logChunkBuffer.range(TENANT_ID, ENTITY_ID, 6L, 10L)).thenReturn(List.of(a, b));

        List<LogsSubscriptionUpdate> collected = new java.util.ArrayList<>();
        dispatcher.streamLogTail(TENANT_ID, ENTITY_ID, 0L, collected::add);

        assertThat(collected).hasSize(2);
        assertThat(collected.get(0).getLines()).containsExactly("a0", "a1");
        assertThat(collected.get(0).getLatestSeq()).isEqualTo(10L);
        assertThat(collected.get(1).getLines()).containsExactly("b0", "b1", "b2");
        assertThat(collected.get(1).getLatestSeq()).isEqualTo(10L);
    }

    @Test
    void streamLogTailSkipsChunksUpToClientWatermark() {
        when(logChunkBuffer.latestTailSeq(TENANT_ID, ENTITY_ID)).thenReturn(10L);

        dispatcher.streamLogTail(TENANT_ID, ENTITY_ID, 7L, u -> { });

        verify(logChunkBuffer).range(TENANT_ID, ENTITY_ID, 8L, 10L);
    }

    @Test
    void streamLogTailEmitsNothingWhenClientWatermarkAlreadyCaughtUp() {
        when(logChunkBuffer.latestTailSeq(TENANT_ID, ENTITY_ID)).thenReturn(10L);

        List<LogsSubscriptionUpdate> collected = new java.util.ArrayList<>();
        dispatcher.streamLogTail(TENANT_ID, ENTITY_ID, 10L, collected::add);

        assertThat(collected).isEmpty();
        verify(logChunkBuffer, org.mockito.Mockito.never())
                .range(any(), any(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void streamLogTailAdvancesStateToBlockImmediateRedeliveryOfSameSeq() {
        when(logChunkBuffer.latestTailSeq(TENANT_ID, ENTITY_ID)).thenReturn(8L);

        dispatcher.streamLogTail(TENANT_ID, ENTITY_ID, 0L, u -> { });

        // A watermark arriving for the same seq should be a no-op (already delivered via snapshot).
        dispatcher.onWatermark(TENANT_ID, ENTITY_ID, 8L, TbCallback.EMPTY);

        verifyNoInteractions(localSubscriptionService);
    }

    private static LogChunk chunkWithLines(String prefix, int count) {
        LogChunk chunk = new LogChunk();
        chunk.setLines(IntStream.range(0, count).mapToObj(i -> prefix + i).toList());
        return chunk;
    }
}
