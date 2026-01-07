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
package org.thingsboard.server.service.cf.ctx.state;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.service.cf.ctx.state.aggregation.RelatedEntitiesArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.aggregation.single.EntityAggregationArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.propagation.PropagationArgumentEntry;

import java.util.List;
import java.util.Map;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SingleValueArgumentEntry.class, name = "SINGLE_VALUE"),
        @JsonSubTypes.Type(value = TsRollingArgumentEntry.class, name = "TS_ROLLING"),
        @JsonSubTypes.Type(value = GeofencingArgumentEntry.class, name = "GEOFENCING"),
        @JsonSubTypes.Type(value = PropagationArgumentEntry.class, name = "PROPAGATION"),
        @JsonSubTypes.Type(value = RelatedEntitiesArgumentEntry.class, name = "RELATED_ENTITIES"),
        @JsonSubTypes.Type(value = EntityAggregationArgumentEntry.class, name = "ENTITY_AGGREGATION")
})
public interface ArgumentEntry {

    @JsonIgnore
    ArgumentEntryType getType();

    Object getValue();

    boolean updateEntry(ArgumentEntry entry, CalculatedFieldCtx ctx);

    boolean isEmpty();

    default JsonNode jsonValue() {
        return JacksonUtil.valueToTree(toTbelCfArg());
    }

    TbelCfArg toTbelCfArg();

    boolean isForceResetPrevious();

    void setForceResetPrevious(boolean forceResetPrevious);

    static ArgumentEntry createSingleValueArgument(KvEntry kvEntry) {
        return new SingleValueArgumentEntry(kvEntry);
    }

    static ArgumentEntry createSingleValueArgument(EntityId entityId, ArgumentEntry argumentEntry) {
        return new SingleValueArgumentEntry(entityId, argumentEntry);
    }

    static ArgumentEntry createTsRollingArgument(List<TsKvEntry> kvEntries, int limit, long timeWindow) {
        if (kvEntries == null) {
            return new TsRollingArgumentEntry(limit, timeWindow);
        }
        return new TsRollingArgumentEntry(kvEntries, limit, timeWindow);
    }

    static ArgumentEntry createGeofencingValueArgument(Map<EntityId, KvEntry> entityIdkvEntryMap) {
        return new GeofencingArgumentEntry(entityIdkvEntryMap);
    }

    static ArgumentEntry createPropagationArgument(List<EntityId> entityIds) {
        return new PropagationArgumentEntry(entityIds);
    }

    static ArgumentEntry createAggArgument(Map<EntityId, ArgumentEntry> entityIdkvEntryMap) {
        return new RelatedEntitiesArgumentEntry(entityIdkvEntryMap, false);
    }

}
