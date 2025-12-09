/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.edge;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.TimeseriesSaveResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.edge.stats.EdgeStats;
import org.thingsboard.server.dao.edge.stats.EdgeStatsCounterService;
import org.thingsboard.server.dao.edge.stats.MsgCounters;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.KafkaAdmin;
import org.thingsboard.server.service.edge.stats.EdgeStatsService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_ADDED;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_LAG;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_PERMANENTLY_FAILED;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_PUSHED;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_TMP_FAILED;

@ExtendWith(MockitoExtension.class)
public class EdgeStatsTest {

    private static final int TTL_DAYS = 30;
    private static final long REPORT_INTERVAL_MILLIS = 600_000L;

    @Mock
    private TimeseriesService tsService;
    @Mock
    private TopicService topicService;
    @Mock
    private EdgeStatsCounterService statsCounterService;
    private EdgeStatsService edgeStatsService;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final EdgeId edgeId = new EdgeId(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        edgeStatsService = createEdgeStatsService(Optional.empty());
    }

    private EdgeStatsService createEdgeStatsService(Optional<KafkaAdmin> kafkaAdmin) {
        EdgeStatsService service = new EdgeStatsService(
                tsService,
                statsCounterService,
                topicService,
                kafkaAdmin
        );
        ReflectionTestUtils.setField(service, "edgesStatsTtlDays", TTL_DAYS);
        ReflectionTestUtils.setField(service, "reportIntervalMillis", REPORT_INTERVAL_MILLIS);
        return service;
    }

    @Test
    public void testReportStatsSavesTelemetry() {
        EdgeStats edgeStats = new EdgeStats(tenantId);
        MsgCounters counters = edgeStats.getMsgCounters();
        counters.getMsgsAdded().set(5);
        counters.getMsgsPushed().set(3);
        counters.getMsgsPermanentlyFailed().set(1);
        counters.getMsgsTmpFailed().set(0);
        counters.getMsgsLag().set(10);

        ConcurrentHashMap<EdgeId, EdgeStats> edgeStatsByEdge = new ConcurrentHashMap<>();
        edgeStatsByEdge.put(edgeId, edgeStats);

        when(statsCounterService.getStatsByEdge()).thenReturn(edgeStatsByEdge);

        ArgumentCaptor<List<TsKvEntry>> captor = ArgumentCaptor.forClass((Class) List.class);
        when(tsService.save(eq(tenantId), eq(edgeId), captor.capture(), anyLong()))
                .thenReturn(Futures.immediateFuture(mock(TimeseriesSaveResult.class)));

        edgeStatsService.reportStats();

        verify(tsService, times(1)).save(eq(tenantId), eq(edgeId), anyList(), anyLong());
        verify(statsCounterService, times(1)).clear(edgeId);

        List<TsKvEntry> entries = captor.getValue();
        Assertions.assertEquals(5, entries.size());

        Map<String, Long> valuesByKey = entries.stream()
                .collect(Collectors.toMap(TsKvEntry::getKey, e -> e.getLongValue().orElse(-1L)));

        Assertions.assertEquals(5L, valuesByKey.get(DOWNLINK_MSGS_ADDED.getKey()).longValue());
        Assertions.assertEquals(3L, valuesByKey.get(DOWNLINK_MSGS_PUSHED.getKey()).longValue());
        Assertions.assertEquals(1L, valuesByKey.get(DOWNLINK_MSGS_PERMANENTLY_FAILED.getKey()).longValue());
        Assertions.assertEquals(0L, valuesByKey.get(DOWNLINK_MSGS_TMP_FAILED.getKey()).longValue());
        Assertions.assertEquals(10L, valuesByKey.get(DOWNLINK_MSGS_LAG.getKey()).longValue());
    }

    @Test
    public void testReportStatsWithKafkaLag() {
        EdgeStats edgeStats = new EdgeStats(tenantId);
        MsgCounters counters = edgeStats.getMsgCounters();
        counters.getMsgsAdded().set(2);
        counters.getMsgsPushed().set(2);
        counters.getMsgsPermanentlyFailed().set(0);
        counters.getMsgsTmpFailed().set(1);
        counters.getMsgsLag().set(0);

        ConcurrentHashMap<EdgeId, EdgeStats> edgeStatsByEdge = new ConcurrentHashMap<>();
        edgeStatsByEdge.put(edgeId, edgeStats);

        when(statsCounterService.getStatsByEdge()).thenReturn(edgeStatsByEdge);

        String topic = "edge-topic";
        TopicPartitionInfo partitionInfo = new TopicPartitionInfo(topic, tenantId, 0, false);
        when(topicService.buildEdgeEventNotificationsTopicPartitionInfo(tenantId, edgeId)).thenReturn(partitionInfo);

        KafkaAdmin kafkaAdmin = mock(KafkaAdmin.class);
        when(kafkaAdmin.getTotalLagForGroupsBulk(Set.of(topic)))
                .thenReturn(Map.of(topic, 15L));

        ArgumentCaptor<List<TsKvEntry>> captor = ArgumentCaptor.forClass((Class) List.class);
        when(tsService.save(eq(tenantId), eq(edgeId), captor.capture(), anyLong()))
                .thenReturn(Futures.immediateFuture(mock(TimeseriesSaveResult.class)));

        edgeStatsService = createEdgeStatsService(Optional.of(kafkaAdmin));

        edgeStatsService.reportStats();

        verify(tsService, times(1)).save(eq(tenantId), eq(edgeId), anyList(), anyLong());
        verify(statsCounterService, times(1)).clear(edgeId);

        List<TsKvEntry> entries = captor.getValue();
        Map<String, Long> valuesByKey = entries.stream()
                .collect(Collectors.toMap(TsKvEntry::getKey, e -> e.getLongValue().orElse(-1L)));

        Assertions.assertEquals(15L, valuesByKey.get(DOWNLINK_MSGS_LAG.getKey()));
    }

}
