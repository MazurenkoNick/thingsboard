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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.logexternal.LogChunk;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Slf4j
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("LogChunkBuffer")
public class LogChunkRedisBuffer implements LogChunkBuffer {

    private static final String KEY_PREFIX = "logChunks";
    private static final String SEQ_KEY_PREFIX = "logChunksSeq";

    private final RedisConnectionFactory connectionFactory;
    private final long ttlSeconds;
    private final int maxChunksPerUnit;

    public LogChunkRedisBuffer(RedisConnectionFactory connectionFactory,
                               @Value("${external.logStream.bufferTtlSeconds:900}") long ttlSeconds,
                               @Value("${external.logStream.bufferMaxChunksPerUnit:10}") int maxChunksPerUnit) {
        this.connectionFactory = connectionFactory;
        this.ttlSeconds = ttlSeconds;
        this.maxChunksPerUnit = maxChunksPerUnit;
    }

    @Override
    public long append(TenantId tenantId, EntityId unitId, LogChunk chunk) {
        byte[] key = unitKey(KEY_PREFIX, tenantId, unitId);
        byte[] seqKey = unitKey(SEQ_KEY_PREFIX, tenantId, unitId);
        try (RedisConnection connection = connectionFactory.getConnection()) {
            Long incremented = connection.stringCommands().incr(seqKey);
            long seq = incremented != null ? incremented : 0L;
            byte[] member = encodeMember(seq, chunk);
            connection.zSetCommands().zAdd(key, seq, member);
            connection.zSetCommands().zRemRange(key, 0, -(maxChunksPerUnit + 1L));
            connection.keyCommands().expire(key, ttlSeconds);
            connection.keyCommands().expire(seqKey, ttlSeconds);
            return seq;
        }
    }

    @Override
    public List<LogChunk> range(TenantId tenantId, EntityId unitId, long fromSeqInclusive, long toSeqInclusive) {
        if (toSeqInclusive < fromSeqInclusive) {
            return List.of();
        }
        byte[] key = unitKey(KEY_PREFIX, tenantId, unitId);
        try (RedisConnection connection = connectionFactory.getConnection()) {
            return readRange(connection, key, fromSeqInclusive, toSeqInclusive);
        }
    }

    @Override
    public long latestTailSeq(TenantId tenantId, EntityId unitId) {
        byte[] key = unitKey(KEY_PREFIX, tenantId, unitId);
        try (RedisConnection connection = connectionFactory.getConnection()) {
            Set<byte[]> last = connection.zSetCommands().zRevRange(key, 0, 0);
            if (last == null || last.isEmpty()) {
                return 0;
            }
            byte[] member = last.iterator().next();
            return decodeSeq(member);
        }
    }

    @Override
    public long latestTailLineTs(TenantId tenantId, EntityId unitId) {
        byte[] key = unitKey(KEY_PREFIX, tenantId, unitId);
        try (RedisConnection connection = connectionFactory.getConnection()) {
            Set<byte[]> last = connection.zSetCommands().zRevRange(key, 0, 0);
            if (last == null || last.isEmpty()) {
                return 0;
            }
            LogChunk chunk = decodeMember(last.iterator().next());
            return chunk == null ? 0 : chunk.getLastLineTs();
        }
    }

    @Override
    public void deleteUnit(TenantId tenantId, EntityId unitId) {
        byte[] key = unitKey(KEY_PREFIX, tenantId, unitId);
        byte[] seqKey = unitKey(SEQ_KEY_PREFIX, tenantId, unitId);
        try (RedisConnection connection = connectionFactory.getConnection()) {
            connection.keyCommands().del(key, seqKey);
        }
    }

    private static List<LogChunk> readRange(RedisConnection connection, byte[] key, double minScore, double maxScore) {
        Set<byte[]> members = connection.zSetCommands().zRangeByScore(key, minScore, maxScore);
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        List<LogChunk> result = new ArrayList<>(members.size());
        for (byte[] m : members) {
            LogChunk decoded = decodeMember(m);
            if (decoded != null) {
                result.add(decoded);
            }
        }
        return result;
    }

    private static long decodeSeq(byte[] member) {
        if (member == null || member.length < Long.BYTES) {
            return Long.MIN_VALUE;
        }
        return ByteBuffer.wrap(member, 0, Long.BYTES).getLong();
    }

    private static byte[] unitKey(String prefix, TenantId tenantId, EntityId unitId) {
        return (prefix + ":tenant:" + tenantId.getId() + ":unit:" + unitId.getId())
                .getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] encodeMember(long seq, LogChunk chunk) {
        byte[] payload = JacksonUtil.writeValueAsBytes(chunk);
        return ByteBuffer.allocate(Long.BYTES + payload.length).putLong(seq).put(payload).array();
    }

    private static LogChunk decodeMember(byte[] member) {
        if (member == null || member.length <= Long.BYTES) {
            return null;
        }
        byte[] payload = Arrays.copyOfRange(member, Long.BYTES, member.length);
        try {
            return JacksonUtil.IGNORE_UNKNOWN_PROPERTIES_JSON_MAPPER.readValue(payload, LogChunk.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize log chunk", e);
            return null;
        }
    }

}
