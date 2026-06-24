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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.gen.agent.v1.AgentMetricsSync;
import org.thingsboard.server.service.agent.AgentInboundMsgCtx;
import org.thingsboard.server.service.agent.AgentMetricsTimeseriesService;

import java.util.ArrayList;
import java.util.List;

import static org.thingsboard.server.service.agent.AgentMetricsKeys.CPU_PERCENT;
import static org.thingsboard.server.service.agent.AgentMetricsKeys.DISK_BYTES;
import static org.thingsboard.server.service.agent.AgentMetricsKeys.HOST_DISK_TOTAL;
import static org.thingsboard.server.service.agent.AgentMetricsKeys.HOST_MEMORY_BYTES;
import static org.thingsboard.server.service.agent.AgentMetricsKeys.MEMORY_BYTES;
import static org.thingsboard.server.service.agent.AgentMetricsKeys.ONLINE_CPUS;

@Component
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class AgentMetricsSyncMessageHandler implements AgentInboundMessageHandler {

    private final AgentMetricsTimeseriesService metricsTsService;

    @Override
    public boolean canHandle(AgentInboundMsgCtx msgCtx) {
        return msgCtx.msg().hasAgentMetricsSync();
    }

    @Override
    public void handle(AgentInboundMsgCtx msgCtx) {
        TenantId tenantId = msgCtx.sessionState().getTenantId();
        AgentId agentId = msgCtx.sessionState().getAgentId();
        AgentMetricsSync metrics = msgCtx.msg().getAgentMetricsSync();

        long ts = System.currentTimeMillis();
        List<TsKvEntry> kvEntries = new ArrayList<>(6);
        kvEntries.add(new BasicTsKvEntry(ts, new DoubleDataEntry(CPU_PERCENT, metrics.getCpuPercent())));
        kvEntries.add(new BasicTsKvEntry(ts, new LongDataEntry(MEMORY_BYTES, metrics.getMemoryBytes())));
        kvEntries.add(new BasicTsKvEntry(ts, new LongDataEntry(ONLINE_CPUS, (long) metrics.getOnlineCpus())));
        if (metrics.hasHostMemoryBytes()) {
            kvEntries.add(new BasicTsKvEntry(ts, new LongDataEntry(HOST_MEMORY_BYTES, metrics.getHostMemoryBytes())));
        }
        if (metrics.hasHostDiskTotal()) {
            kvEntries.add(new BasicTsKvEntry(ts, new LongDataEntry(HOST_DISK_TOTAL, metrics.getHostDiskTotal())));
        }
        if (metrics.hasDiskBytes()) {
            kvEntries.add(new BasicTsKvEntry(ts, new LongDataEntry(DISK_BYTES, metrics.getDiskBytes())));
        }

        metricsTsService.save(tenantId, agentId, kvEntries, "agent " + agentId);
    }
}
