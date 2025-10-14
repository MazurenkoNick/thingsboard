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
package org.thingsboard.server.service.cf.ctx.state.geofencing;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.geo.Coordinates;
import org.thingsboard.common.util.geo.PerimeterDefinition;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingPresenceStatus;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingTransitionEvent;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos.GeofencingZoneProto;

import static org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingPresenceStatus.INSIDE;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingPresenceStatus.OUTSIDE;

@Data
public class GeofencingZoneState {

    private final EntityId zoneId;

    private long ts;
    private Long version;
    private PerimeterDefinition perimeterDefinition;

    @EqualsAndHashCode.Exclude
    private GeofencingPresenceStatus lastPresence;

    public GeofencingZoneState(EntityId zoneId, KvEntry entry) {
        this.zoneId = zoneId;
        if (!(entry instanceof AttributeKvEntry attributeKvEntry)) {
            throw new IllegalArgumentException("Unsupported KvEntry type for geofencing zone state: " + entry.getClass().getSimpleName());
        }
        this.ts = attributeKvEntry.getLastUpdateTs();
        this.version = attributeKvEntry.getVersion();
        this.perimeterDefinition = JacksonUtil.fromString(entry.getValueAsString(), PerimeterDefinition.class);
    }

    public GeofencingZoneState(GeofencingZoneProto proto) {
        this.zoneId = ProtoUtils.fromProto(proto.getZoneId());
        this.ts = proto.getTs();
        this.version = proto.getVersion();
        this.perimeterDefinition = JacksonUtil.fromString(proto.getPerimeterDefinition(), PerimeterDefinition.class);
        if (proto.hasInside()) {
            this.lastPresence = proto.getInside() ? INSIDE : OUTSIDE;
        }
    }

    public boolean update(GeofencingZoneState newZoneState) {
        if (newZoneState.getTs() <= this.ts) {
            return false;
        }
        Long newVersion = newZoneState.getVersion();
        if (newVersion == null || this.version == null || newVersion > this.version) {
            this.ts = newZoneState.getTs();
            this.version = newVersion;
            this.perimeterDefinition = newZoneState.getPerimeterDefinition();
            this.lastPresence = null;
            return true;
        }
        return false;
    }

    public GeofencingEvalResult evaluate(Coordinates entityCoordinates) {
        boolean nowInside = perimeterDefinition.checkMatches(entityCoordinates);

        GeofencingPresenceStatus status = nowInside ? INSIDE : OUTSIDE;

        // first evaluation
        if (this.lastPresence == null) {
            this.lastPresence = status;
            GeofencingTransitionEvent transition = null;
            if (status == GeofencingPresenceStatus.INSIDE) {
                transition = GeofencingTransitionEvent.ENTERED;
            }
            return new GeofencingEvalResult(transition, status);
        }
        // State changed
        if (this.lastPresence != status) {
            this.lastPresence = status;
            GeofencingTransitionEvent transition = (status == GeofencingPresenceStatus.INSIDE) ?
                    GeofencingTransitionEvent.ENTERED : GeofencingTransitionEvent.LEFT;
            return new GeofencingEvalResult(transition, status);
        }
        // State unchanged
        return new GeofencingEvalResult(null, status);
    }

}
