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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ConcurrentReferenceHashMap.ReferenceType;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.cache.logexternal.LogChunkBuffer;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.logexternal.LogChunk;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.gen.transport.TransportProtos.TbLogStreamUpdateProto;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.log.sub.LogsSubscriptionUpdate;
import org.thingsboard.server.service.subscription.TbLocalSubscriptionService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@TbCoreComponent
@Component
public class LogStreamDispatcher {

    /**
     * Caps initial backlog replayed per new subscriber. Kept below buffer retention
     * (external.logStream.bufferMaxChunksPerUnit) to bound the per-subscription
     * heap/deserialization burst of materializing the range; older chunks still reach the live path.
     */
    private static final long MAX_TAIL_REPLAY_CHUNKS = 5;

    private final LogChunkBuffer logChunkBuffer;
    private final TbLocalSubscriptionService localSubscriptionService;
    private final StatsFactory statsFactory;
    private ExecutorService deliveryExecutor;
    private DefaultCounter droppedWatermarks;

    private final Map<EntityId, LogStreamState> states = new ConcurrentReferenceHashMap<>(16, ReferenceType.SOFT);

    public LogStreamDispatcher(LogChunkBuffer logChunkBuffer, @Lazy TbLocalSubscriptionService localSubscriptionService, StatsFactory statsFactory) {
        this.logChunkBuffer = logChunkBuffer;
        this.localSubscriptionService = localSubscriptionService;
        this.statsFactory = statsFactory;
    }

    @PostConstruct
    public void init() {
        if (deliveryExecutor == null) {
            ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 8,
                    60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(500),
                    ThingsBoardThreadFactory.forName("log-stream-dispatcher"));
            executor.allowCoreThreadTimeOut(true);
            deliveryExecutor = executor;
            droppedWatermarks = statsFactory.createDefaultCounter("logStreamDroppedWatermarks");
            statsFactory.createGauge("logStream", "deliveryQueueSize", executor.getQueue(), Collection::size);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (deliveryExecutor != null) {
            deliveryExecutor.shutdownNow();
        }
    }

    public void onWatermark(TbLogStreamUpdateProto proto, TbCallback callback) {
        TenantId tenantId = TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(proto.getEntityType(), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));
        onWatermark(tenantId, entityId, proto.getLatestSeq(), callback);
    }

    public void onWatermark(TenantId tenantId, EntityId entityId, long latestSeq, TbCallback callback) {
        try {
            deliveryExecutor.execute(() -> doOnWatermark(tenantId, entityId, latestSeq, callback));
        } catch (RejectedExecutionException e) {
            droppedWatermarks.increment();
            log.warn("[{}][{}] Log stream delivery queue is full, dropping watermark {}", tenantId, entityId, latestSeq);
            callback.onSuccess();
        }
    }

    private void doOnWatermark(TenantId tenantId, EntityId entityId, long latestSeq, TbCallback callback) {
        LogStreamState state = getState(tenantId, entityId);
        // double-checked: unlocked fast path acks stale watermarks without parking on the monitor
        // (safe: lastSeenSeq is volatile and monotonic); the re-check under the lock is authoritative
        if (latestSeq <= state.lastSeenSeq) {
            callback.onSuccess();
            return;
        }
        try {
            synchronized (state) {
                if (latestSeq <= state.lastSeenSeq) {
                    long actualTail = logChunkBuffer.latestTailSeq(tenantId, entityId);
                    if (actualTail >= state.lastSeenSeq) { // genuine stale/reordered watermark
                        callback.onSuccess();
                        return;
                    }
                    state.lastSeenSeq = 0; // buffer rewound -> resync, fall through to deliver
                }
                long previousSeq = state.lastSeenSeq;
                long fromSeq = previousSeq + 1;
                state.lastSeenSeq = latestSeq;
                try {
                    List<LogChunk> chunks = logChunkBuffer.range(tenantId, entityId, fromSeq, latestSeq);
                    int evicted = fromSeq > 1 ? (int) (latestSeq - fromSeq + 1) - chunks.size() : 0;
                    if (chunks.isEmpty() && evicted == 0) {
                        callback.onSuccess();
                        return;
                    }
                    localSubscriptionService.onLogsUpdate(entityId, toUpdate(latestSeq, chunks, evicted), callback);
                } catch (Exception e) {
                    state.lastSeenSeq = previousSeq;
                    throw e;
                }
            }
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to deliver log stream update for seq {}", tenantId, entityId, latestSeq, e);
            callback.onFailure(e);
        }
    }

    /**
     * Replays the retained log tail to {@code consumer} as a one-time snapshot. Call this before
     * registering the live subscription: the snapshot is bounded by the current per-entity watermark
     * ({@code lastSeenSeq}), so if the live subscription is registered first an incoming watermark can
     * advance that watermark and the snapshot would re-deliver chunks already sent on the live path.
     */
    public void streamLogTail(TenantId tenantId, EntityId entityId, long fromSeqExclusive, Consumer<LogsSubscriptionUpdate> consumer) {
        LogStreamState state = getState(tenantId, entityId);
        long toSeq = state.lastSeenSeq;
        if (toSeq <= 0 || fromSeqExclusive >= toSeq) {
            return;
        }
        long fromExclusive = Math.max(toSeq - MAX_TAIL_REPLAY_CHUNKS, fromSeqExclusive);
        for (LogChunk chunk : logChunkBuffer.range(tenantId, entityId, fromExclusive + 1, toSeq)) {
            consumer.accept(toUpdate(toSeq, chunk));
        }
    }

    private LogStreamState getState(TenantId tenantId, EntityId entityId) {
        return states.computeIfAbsent(entityId, e -> getInitialLogStreamState(tenantId, e));
    }

    private LogStreamState getInitialLogStreamState(TenantId tenantId, EntityId entityId) {
        long tailSeq = logChunkBuffer.latestTailSeq(tenantId, entityId);
        return new LogStreamState(tailSeq);
    }

    private LogsSubscriptionUpdate toUpdate(long latestSeq, List<LogChunk> chunks, int evictedChunks) {
        List<String> lines = new ArrayList<>();
        int droppedLines = 0;
        for (LogChunk chunk : chunks) {
            if (chunk.getDropped() > 0) {
                droppedLines += chunk.getDropped();
            }
            if (chunk.getLines() != null) {
                lines.addAll(chunk.getLines());
            }
        }
        return new LogsSubscriptionUpdate(latestSeq, lines, droppedLines, evictedChunks);
    }

    private LogsSubscriptionUpdate toUpdate(long latestSeq, LogChunk chunk) {
        List<String> lines = chunk.getLines() != null ? chunk.getLines() : List.of();
        return new LogsSubscriptionUpdate(latestSeq, lines, chunk.getDropped(), 0);
    }

    @AllArgsConstructor
    private static class LogStreamState {
        volatile long lastSeenSeq;
    }

}
