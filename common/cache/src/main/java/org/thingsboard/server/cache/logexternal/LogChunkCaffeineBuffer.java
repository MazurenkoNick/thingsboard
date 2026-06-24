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
package org.thingsboard.server.cache.logexternal;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.logexternal.LogChunk;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("LogChunkBuffer")
public class LogChunkCaffeineBuffer implements LogChunkBuffer {

    private final Cache<UnitKey, UnitBuffer> buffers;
    private final int maxChunksPerUnit;

    public LogChunkCaffeineBuffer(@Value("${external.logStream.bufferTtlSeconds:900}") long ttlSeconds,
                                  @Value("${external.logStream.bufferMaxChunksPerUnit:10}") int maxChunksPerUnit,
                                  @Value("${external.logStream.bufferMaxUnits:100000}") long maxUnits) {
        this.buffers = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .maximumSize(maxUnits)
                .build();
        this.maxChunksPerUnit = maxChunksPerUnit;
    }

    @Override
    public long append(TenantId tenantId, EntityId unitId, LogChunk chunk) {
        UnitKey key = new UnitKey(tenantId.getId(), unitId.getId());
        AtomicLong assignedSeq = new AtomicLong();
        buffers.asMap().compute(key, (k, existing) -> {
            UnitBuffer unit = existing != null ? existing : new UnitBuffer();
            long seq = unit.seq().incrementAndGet();
            assignedSeq.set(seq);
            unit.chunks().put(seq, chunk);
            while (unit.chunks().size() > maxChunksPerUnit) {
                unit.chunks().pollFirstEntry();
            }
            return unit;
        });
        return assignedSeq.get();
    }

    @Override
    public List<LogChunk> range(TenantId tenantId, EntityId unitId, long fromSeqInclusive, long toSeqInclusive) {
        if (toSeqInclusive < fromSeqInclusive) {
            return List.of();
        }
        UnitBuffer unit = buffers.getIfPresent(new UnitKey(tenantId.getId(), unitId.getId()));
        if (unit == null) {
            return List.of();
        }
        return new ArrayList<>(unit.chunks().subMap(fromSeqInclusive, true, toSeqInclusive, true).values());
    }

    @Override
    public long latestTailSeq(TenantId tenantId, EntityId unitId) {
        UnitBuffer unit = buffers.getIfPresent(new UnitKey(tenantId.getId(), unitId.getId()));
        return (unit == null || unit.chunks().isEmpty()) ? 0 : unit.chunks().lastKey();
    }

    @Override
    public long latestTailLineTs(TenantId tenantId, EntityId unitId) {
        UnitBuffer unit = buffers.getIfPresent(new UnitKey(tenantId.getId(), unitId.getId()));
        if (unit == null || unit.chunks().isEmpty()) {
            return 0;
        }
        return unit.chunks().lastEntry().getValue().getLastLineTs();
    }

    @Override
    public void deleteUnit(TenantId tenantId, EntityId unitId) {
        buffers.invalidate(new UnitKey(tenantId.getId(), unitId.getId()));
    }

    private record UnitKey(UUID tenantId, UUID unitId) {}

    private record UnitBuffer(AtomicLong seq, NavigableMap<Long, LogChunk> chunks) {
        UnitBuffer() {
            this(new AtomicLong(), new ConcurrentSkipListMap<>());
        }
    }

}
