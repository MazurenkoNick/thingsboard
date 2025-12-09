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
package org.thingsboard.server.dao.edge.stats;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.concurrent.ConcurrentHashMap;

@ConditionalOnProperty(prefix = "edges.stats", name = "enabled", havingValue = "true", matchIfMissing = false)
@Service
@Slf4j
@Getter
public class EdgeStatsCounterService {

    private final ConcurrentHashMap<EdgeId, EdgeStats> statsByEdge = new ConcurrentHashMap<>();

    public void recordEvent(EdgeStatsKey type, TenantId tenantId, EdgeId edgeId, long value) {
        EdgeStats edgeStats = getOrCreateEdgeStats(tenantId, edgeId);
        MsgCounters counters = edgeStats.getMsgCounters();
        switch (type) {
            case DOWNLINK_MSGS_ADDED -> counters.getMsgsAdded().addAndGet(value);
            case DOWNLINK_MSGS_PUSHED -> counters.getMsgsPushed().addAndGet(value);
            case DOWNLINK_MSGS_PERMANENTLY_FAILED -> counters.getMsgsPermanentlyFailed().addAndGet(value);
            case DOWNLINK_MSGS_TMP_FAILED -> counters.getMsgsTmpFailed().addAndGet(value);
            case DOWNLINK_MSGS_LAG -> counters.getMsgsLag().set(value);
        }
    }

    public EdgeStats getOrCreateEdgeStats(TenantId tenantId, EdgeId edgeId) {
        return statsByEdge.computeIfAbsent(edgeId, id -> new EdgeStats(tenantId));
    }

    public void clear(EdgeId edgeId) {
        statsByEdge.remove(edgeId);
    }

}
