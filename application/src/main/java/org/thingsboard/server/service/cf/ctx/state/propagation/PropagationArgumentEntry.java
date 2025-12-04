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
package org.thingsboard.server.service.cf.ctx.state.propagation;

import lombok.Data;
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.script.api.tbel.TbelCfPropagationArg;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntryType;

import java.util.ArrayList;
import java.util.List;

@Data
public class PropagationArgumentEntry implements ArgumentEntry {

    private List<EntityId> propagationEntityIds;

    private boolean forceResetPrevious;

    public PropagationArgumentEntry(List<EntityId> propagationEntityIds) {
        this.propagationEntityIds = new ArrayList<>(propagationEntityIds);
    }

    @Override
    public ArgumentEntryType getType() {
        return ArgumentEntryType.PROPAGATION;
    }

    @Override
    public Object getValue() {
        return propagationEntityIds;
    }

    @Override
    public boolean updateEntry(ArgumentEntry entry) {
        if (!(entry instanceof PropagationArgumentEntry propagationArgumentEntry)) {
            throw new IllegalArgumentException("Unsupported argument entry type for propagation argument entry: " + entry.getType());
        }
        if (propagationArgumentEntry.isEmpty()) {
            propagationEntityIds.clear();
        } else {
            propagationEntityIds = propagationArgumentEntry.getPropagationEntityIds();
        }
        return true;
    }

    @Override
    public boolean isEmpty() {
        return CollectionsUtil.isEmpty(propagationEntityIds);
    }

    @Override
    public TbelCfArg toTbelCfArg() {
        return new TbelCfPropagationArg(propagationEntityIds);
    }

    public boolean addPropagationEntityId(EntityId propagationEntityId) {
        if (propagationEntityIds.contains(propagationEntityId)) {
            return false;
        }
        return propagationEntityIds.add(propagationEntityId);
    }

    public boolean removePropagationEntityId(EntityId relatedEntityId) {
        return propagationEntityIds.remove(relatedEntityId);
    }

}
