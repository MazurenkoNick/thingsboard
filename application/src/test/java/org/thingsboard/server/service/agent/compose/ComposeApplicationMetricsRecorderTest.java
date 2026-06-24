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
package org.thingsboard.server.service.agent.compose;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.gen.agent.v1.ApplicationMetrics;
import org.thingsboard.server.service.agent.AgentMetricsTimeseriesService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ComposeApplicationMetricsRecorderTest {

    @Mock
    private AgentMetricsTimeseriesService metricsTsService;

    @InjectMocks
    private ComposeApplicationMetricsRecorder recorder;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentApplicationId APP_ID = new AgentApplicationId(UUID.randomUUID());

    @Test
    void record_savesBothCpuAndMemoryAgainstApplicationId() {
        ApplicationMetrics metrics = ApplicationMetrics.newBuilder()
                .setCpuPercent(42.0)
                .setMemoryBytes(8192L)
                .build();

        recorder.record(TENANT_ID, APP_ID, metrics);

        ArgumentCaptor<List<TsKvEntry>> captor = entriesCaptor();
        verify(metricsTsService).save(eq(TENANT_ID), eq(APP_ID), captor.capture(), anyString());

        List<TsKvEntry> entries = captor.getValue();
        assertThat(entries).hasSize(2);
        assertThat(entries).extracting(TsKvEntry::getKey).containsExactlyInAnyOrder("cpuPercent", "memoryBytes");
    }

    @Test
    void record_persistsVolumeBytesWhenPresent() {
        ApplicationMetrics metrics = ApplicationMetrics.newBuilder()
                .setCpuPercent(1.0)
                .setMemoryBytes(1L)
                .setVolumeBytes(7_777L)
                .build();

        recorder.record(TENANT_ID, APP_ID, metrics);

        ArgumentCaptor<List<TsKvEntry>> captor = entriesCaptor();
        verify(metricsTsService).save(eq(TENANT_ID), eq(APP_ID), captor.capture(), anyString());

        assertThat(captor.getValue())
                .extracting(TsKvEntry::getKey)
                .containsExactlyInAnyOrder("cpuPercent", "memoryBytes", "volumeBytes");
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<TsKvEntry>> entriesCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }
}
