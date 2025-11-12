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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.aggregation.RelatedEntitiesAggregationCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.alarm.AlarmCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.propagation.PropagationCalculatedFieldState;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

import static org.thingsboard.server.utils.CalculatedFieldUtils.toSingleValueArgumentProto;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @Type(value = SimpleCalculatedFieldState.class, name = "SIMPLE"),
        @Type(value = ScriptCalculatedFieldState.class, name = "SCRIPT"),
        @Type(value = GeofencingCalculatedFieldState.class, name = "GEOFENCING"),
        @Type(value = AlarmCalculatedFieldState.class, name = "ALARM"),
        @Type(value = PropagationCalculatedFieldState.class, name = "PROPAGATION"),
        @Type(value = RelatedEntitiesAggregationCalculatedFieldState.class, name = "RELATED_ENTITIES_AGGREGATION")
})
public interface CalculatedFieldState extends Closeable {

    @JsonIgnore
    CalculatedFieldType getType();

    EntityId getEntityId();

    Map<String, ArgumentEntry> getArguments();

    long getLatestTimestamp();

    void setCtx(CalculatedFieldCtx ctx, TbActorRef actorCtx);

    void init();

    Map<String, ArgumentEntry> update(Map<String, ArgumentEntry> arguments, CalculatedFieldCtx ctx);

    void reset();

    ListenableFuture<CalculatedFieldResult> performCalculation(Map<String, ArgumentEntry> updatedArgs, CalculatedFieldCtx ctx) throws Exception;

    @JsonIgnore
    boolean isReady();

    ReadinessStatus getReadinessStatus();

    boolean isSizeExceedsLimit();

    @JsonIgnore
    default boolean isSizeOk() {
        return !isSizeExceedsLimit();
    }

    TopicPartitionInfo getPartition();

    void setPartition(TopicPartitionInfo partition);

    void checkStateSize(CalculatedFieldEntityCtxId ctxId, long maxStateSize);

    default void checkArgumentSize(String name, ArgumentEntry entry, CalculatedFieldCtx ctx) {
        if (entry instanceof TsRollingArgumentEntry || entry instanceof GeofencingArgumentEntry) {
            return;
        }
        if (entry instanceof SingleValueArgumentEntry singleValueArgumentEntry) {
            if (ctx.getMaxSingleValueArgumentSize() > 0 && toSingleValueArgumentProto(name, singleValueArgumentEntry).getSerializedSize() > ctx.getMaxSingleValueArgumentSize()) {
                throw new IllegalArgumentException("Single value size exceeds the maximum allowed limit. The argument will not be used for calculation.");
            }
        }
    }

    record ReadinessStatus(boolean ready, String errorMsg) {

        private static final String ERROR_MESSAGE = "Required arguments are missing: ";
        private static final ReadinessStatus READY = new ReadinessStatus(true, null);

        public static ReadinessStatus from(List<String> emptyOrMissingArguments) {
            if (CollectionsUtil.isEmpty(emptyOrMissingArguments)) {
                return ReadinessStatus.READY;
            }
            return new ReadinessStatus(false, ERROR_MESSAGE + String.join(", ", emptyOrMissingArguments));
        }
    }

}
