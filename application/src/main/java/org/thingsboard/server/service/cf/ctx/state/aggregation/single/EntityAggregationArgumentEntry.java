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
package org.thingsboard.server.service.cf.ctx.state.aggregation.single;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntryType;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;

import java.util.Map;

@Data
public class EntityAggregationArgumentEntry implements ArgumentEntry {

    private Map<AggIntervalEntry, AggIntervalEntryStatus> aggIntervals;

    private boolean forceResetPrevious;

    public EntityAggregationArgumentEntry(Map<AggIntervalEntry, AggIntervalEntryStatus> aggIntervals) {
        this.aggIntervals = aggIntervals;
    }

    @Override
    public ArgumentEntryType getType() {
        return ArgumentEntryType.ENTITY_AGGREGATION;
    }

    @Override
    public Object getValue() {
        return aggIntervals;
    }

    @Override
    public boolean updateEntry(ArgumentEntry entry) {
        boolean updated = false;
        if (entry instanceof EntityAggregationArgumentEntry entityAggEntry) {
            aggIntervals.putAll(entityAggEntry.getAggIntervals());
        } else if (entry instanceof SingleValueArgumentEntry singleValueArgEntry) {
            long entryTs = singleValueArgEntry.getTs();
            long argUpdateTs = System.currentTimeMillis();
            for (Map.Entry<AggIntervalEntry, AggIntervalEntryStatus> aggIntervalEntry : aggIntervals.entrySet()) {
                if (singleValueArgEntry.isForceResetPrevious()) {
                    aggIntervalEntry.getValue().setLastArgsRefreshTs(argUpdateTs);
                    updated = true;
                    continue;
                }
                if (aggIntervalEntry.getKey().belongsToInterval(entryTs)) {
                    aggIntervalEntry.getValue().setLastArgsRefreshTs(argUpdateTs);
                    return true;
                }
            }
        }
        return updated;
    }

    @Override
    public boolean isEmpty() {
        return aggIntervals.isEmpty();
    }

    @Override
    public JsonNode jsonValue() {
        return JacksonUtil.valueToTree(aggIntervals);
    }

    @Override
    public TbelCfArg toTbelCfArg() {
        return null;
    }

}
