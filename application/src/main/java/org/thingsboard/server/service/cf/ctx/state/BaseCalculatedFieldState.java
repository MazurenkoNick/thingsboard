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
package org.thingsboard.server.service.cf.ctx.state;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.aggregation.RelatedEntitiesArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.aggregation.single.EntityAggregationArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingZoneState;
import org.thingsboard.server.utils.CalculatedFieldUtils;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public abstract class BaseCalculatedFieldState implements CalculatedFieldState, Closeable {

    protected static final long DEFAULT_LAST_UPDATE_TS = -1L;

    protected final EntityId entityId;
    protected CalculatedFieldCtx ctx;
    protected TbActorRef actorCtx;
    protected List<String> requiredArguments;

    protected Map<String, ArgumentEntry> arguments = new HashMap<>();
    protected boolean sizeExceedsLimit;
    protected long latestTimestamp = DEFAULT_LAST_UPDATE_TS;
    protected ReadinessStatus readinessStatus;

    @Setter
    private TopicPartitionInfo partition;

    public BaseCalculatedFieldState(EntityId entityId) {
        this.entityId = entityId;
    }

    @Override
    public void setCtx(CalculatedFieldCtx ctx, TbActorRef actorCtx) {
        this.ctx = ctx;
        this.actorCtx = actorCtx;
        this.requiredArguments = ctx.getArgNames();
        this.readinessStatus = checkReadiness(requiredArguments, arguments);
    }

    @Override
    public void init(boolean restored) {
    }

    @Override
    public Map<String, ArgumentEntry> update(Map<String, ArgumentEntry> argumentValues, CalculatedFieldCtx ctx) {
        Map<String, ArgumentEntry> updatedArguments = null;

        for (Map.Entry<String, ArgumentEntry> entry : argumentValues.entrySet()) {
            String key = entry.getKey();
            ArgumentEntry newEntry = entry.getValue();

            checkArgumentSize(key, newEntry, ctx);

            ArgumentEntry existingEntry = arguments.get(key);
            boolean entryUpdated;

            if (existingEntry == null || newEntry.isForceResetPrevious()) {
                validateNewEntry(key, newEntry);
                if (existingEntry instanceof RelatedEntitiesArgumentEntry relatedEntitiesArgumentEntry) {
                    relatedEntitiesArgumentEntry.updateEntry(newEntry);
                } else if (existingEntry instanceof EntityAggregationArgumentEntry entityAggArgumentEntry) {
                    entityAggArgumentEntry.updateEntry(newEntry);
                } else {
                    arguments.put(key, newEntry);
                }
                entryUpdated = true;
            } else {
                entryUpdated = existingEntry.updateEntry(newEntry);
            }

            if (entryUpdated) {
                if (updatedArguments == null) {
                    updatedArguments = new HashMap<>(argumentValues.size());
                }
                updatedArguments.put(key, newEntry);
                updateLastUpdateTimestamp(newEntry);
            }

        }

        if (updatedArguments == null) {
            return Collections.emptyMap();
        }
        readinessStatus = checkReadiness(requiredArguments, arguments);
        return updatedArguments;
    }

    @Override
    public void reset() { // must reset everything dependent on arguments
        requiredArguments = null;
        arguments.clear();
        sizeExceedsLimit = false;
        latestTimestamp = DEFAULT_LAST_UPDATE_TS;
    }

    @Override
    public boolean isReady() {
        return readinessStatus.ready();
    }

    @Override
    public void checkStateSize(CalculatedFieldEntityCtxId ctxId, long maxStateSize) {
        if (!sizeExceedsLimit && maxStateSize > 0 && CalculatedFieldUtils.toProto(ctxId, this).getSerializedSize() > maxStateSize) {
            arguments.clear();
            sizeExceedsLimit = true;
        }
    }

    @Override
    public void close() {
    }

    protected void validateNewEntry(String key, ArgumentEntry newEntry) {
    }

    protected ObjectNode toSimpleResult(boolean useLatestTs, ObjectNode valuesNode) {
        if (!useLatestTs) {
            return valuesNode;
        }
        long latestTs = getLatestTimestamp();
        if (latestTs == DEFAULT_LAST_UPDATE_TS) {
            return valuesNode;
        }
        ObjectNode resultNode = JacksonUtil.newObjectNode();
        resultNode.put("ts", latestTs);
        resultNode.set("values", valuesNode);
        return resultNode;
    }

    private void updateLastUpdateTimestamp(ArgumentEntry entry) {
        long newTs = this.latestTimestamp;
        if (entry instanceof SingleValueArgumentEntry singleValueArgumentEntry) {
            newTs = singleValueArgumentEntry.getTs();
        } else if (entry instanceof TsRollingArgumentEntry tsRollingArgumentEntry) {
            Map.Entry<Long, Double> lastEntry = tsRollingArgumentEntry.getTsRecords().lastEntry();
            newTs = (lastEntry != null) ? lastEntry.getKey() : DEFAULT_LAST_UPDATE_TS;
        } else if (entry instanceof RelatedEntitiesArgumentEntry relatedEntitiesArgumentEntry) {
            newTs = relatedEntitiesArgumentEntry.getEntityInputs().values().stream()
                    .mapToLong(e -> (e instanceof SingleValueArgumentEntry s) ? s.getTs() : DEFAULT_LAST_UPDATE_TS)
                    .max()
                    .orElse(DEFAULT_LAST_UPDATE_TS);
        } else if (entry instanceof GeofencingArgumentEntry geofencingArgumentEntry) {
            newTs = geofencingArgumentEntry.getZoneStates().values().stream()
                    .mapToLong(GeofencingZoneState::getTs).max().orElse(DEFAULT_LAST_UPDATE_TS);
        }
        this.latestTimestamp = Math.max(this.latestTimestamp, newTs);
    }

    protected ReadinessStatus checkReadiness(List<String> requiredArguments, Map<String, ArgumentEntry> currentArguments) {
        if (currentArguments == null) {
            return ReadinessStatus.from(requiredArguments);
        }
        List<String> emptyArguments = null;
        for (String requiredArgumentKey : requiredArguments) {
            ArgumentEntry argumentEntry = currentArguments.get(requiredArgumentKey);
            if (argumentEntry == null || argumentEntry.isEmpty()) {
                if (emptyArguments == null) {
                    emptyArguments = new ArrayList<>();
                }
                emptyArguments.add(requiredArgumentKey);
            }
        }
        return ReadinessStatus.from(emptyArguments);
    }

    @Override
    public JsonNode getArgumentsJson() {
        return JacksonUtil.valueToTree(arguments.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().jsonValue())));
    }

}
