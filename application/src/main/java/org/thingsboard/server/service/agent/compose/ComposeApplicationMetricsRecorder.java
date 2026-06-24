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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.gen.agent.v1.ApplicationMetrics;
import org.thingsboard.server.service.agent.AgentMetricsTimeseriesService;

import java.util.ArrayList;
import java.util.List;

import static org.thingsboard.server.service.agent.AgentMetricsKeys.CPU_PERCENT;
import static org.thingsboard.server.service.agent.AgentMetricsKeys.MEMORY_BYTES;
import static org.thingsboard.server.service.agent.AgentMetricsKeys.VOLUME_BYTES;

@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class ComposeApplicationMetricsRecorder {

    private final AgentMetricsTimeseriesService metricsTsService;

    public void record(TenantId tenantId, AgentApplicationId appId, ApplicationMetrics metrics) {
        long ts = System.currentTimeMillis();
        List<TsKvEntry> kvEntries = new ArrayList<>(3);
        kvEntries.add(new BasicTsKvEntry(ts, new DoubleDataEntry(CPU_PERCENT, metrics.getCpuPercent())));
        kvEntries.add(new BasicTsKvEntry(ts, new LongDataEntry(MEMORY_BYTES, metrics.getMemoryBytes())));
        if (metrics.hasVolumeBytes()) {
            kvEntries.add(new BasicTsKvEntry(ts, new LongDataEntry(VOLUME_BYTES, metrics.getVolumeBytes())));
        }
        metricsTsService.save(tenantId, appId, kvEntries, "app " + appId);
    }
}
