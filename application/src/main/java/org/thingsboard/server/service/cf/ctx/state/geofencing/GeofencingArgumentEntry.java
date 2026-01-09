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
package org.thingsboard.server.service.cf.ctx.state.geofencing;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.script.api.tbel.TbelCfGeofencingArg;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntryType;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.HasLatestTs;

import java.util.Map;
import java.util.stream.Collectors;

import static org.thingsboard.server.service.cf.ctx.state.BaseCalculatedFieldState.DEFAULT_LAST_UPDATE_TS;

@Data
@Slf4j
public class GeofencingArgumentEntry implements ArgumentEntry, HasLatestTs {

    private Map<EntityId, GeofencingZoneState> zoneStates;

    private boolean forceResetPrevious;

    public GeofencingArgumentEntry() {
    }

    public GeofencingArgumentEntry(EntityId entityId, TransportProtos.AttributeValueProto entry) {
        this(Map.of(entityId, ProtoUtils.fromProto(entry)));
    }

    public GeofencingArgumentEntry(Map<EntityId, KvEntry> entityIdkvEntryMap) {
        this.zoneStates = toZones(entityIdkvEntryMap);
    }

    @Override
    public ArgumentEntryType getType() {
        return ArgumentEntryType.GEOFENCING;
    }

    @Override
    public Object getValue() {
        return zoneStates;
    }

    @Override
    public long getLatestTs() {
        return zoneStates.values().stream()
                .mapToLong(GeofencingZoneState::getTs).max().orElse(DEFAULT_LAST_UPDATE_TS);
    }

    @Override
    public boolean updateEntry(ArgumentEntry entry, CalculatedFieldCtx ctx) {
        if (!(entry instanceof GeofencingArgumentEntry geofencingArgumentEntry)) {
            throw new IllegalArgumentException("Unsupported argument entry type for geofencing argument entry: " + entry.getType());
        }
        if (geofencingArgumentEntry.isEmpty()) {
            zoneStates.clear();
            return true;
        }
        boolean updated = false;
        for (var zoneEntry : geofencingArgumentEntry.getZoneStates().entrySet()) {
            if (updateZone(zoneEntry)) {
                updated = true;
            }
        }
        return updated;
    }

    @Override
    public boolean isEmpty() {
        return zoneStates == null || zoneStates.isEmpty();
    }

    @Override
    public TbelCfArg toTbelCfArg() {
        return new TbelCfGeofencingArg(zoneStates);
    }

    private Map<EntityId, GeofencingZoneState> toZones(Map<EntityId, KvEntry> entityIdKvEntryMap) {
        return entityIdKvEntryMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> new GeofencingZoneState(entry.getKey(), entry.getValue())));
    }

    private boolean updateZone(Map.Entry<EntityId, GeofencingZoneState> zoneEntry) {
        EntityId zoneId = zoneEntry.getKey();
        GeofencingZoneState newZoneState = zoneEntry.getValue();

        GeofencingZoneState existingZoneState = zoneStates.get(zoneId);
        if (existingZoneState == null) {
            zoneStates.put(zoneId, newZoneState);
            return true;
        }
        if (newZoneState.getPerimeterDefinition() == null) {
            zoneStates.remove(zoneId);
            return true;
        }
        return existingZoneState.update(newZoneState);
    }

}
