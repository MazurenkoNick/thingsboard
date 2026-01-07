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
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntryType;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class PropagationArgumentEntry implements ArgumentEntry {

    private Set<EntityId> entityIds;
    private transient List<EntityId> added;
    private transient EntityId removed;

    private transient boolean forceResetPrevious;
    private transient boolean syncWithDb;

    public PropagationArgumentEntry() {
        this.entityIds = new HashSet<>();
        this.added = null;
        this.removed = null;
    }

    public PropagationArgumentEntry(List<EntityId> entityIds) {
        this.entityIds = new HashSet<>(entityIds);
    }

    @Override
    public ArgumentEntryType getType() {
        return ArgumentEntryType.PROPAGATION;
    }

    @Override
    public Object getValue() {
        return entityIds;
    }

    @Override
    public boolean updateEntry(ArgumentEntry entry, CalculatedFieldCtx ctx) {
        if (!(entry instanceof PropagationArgumentEntry updated)) {
            throw new IllegalArgumentException("Unsupported argument entry type for propagation argument entry: " + entry.getType());
        }
        if (updated.getAdded() != null) {
            return checkAdded(updated.getAdded());
        }
        if (updated.getRemoved() != null) {
            return entityIds.remove(updated.getRemoved());
        }
        if (updated.isSyncWithDb()) {
            Set<EntityId> dbEntityIds = updated.getEntityIds();
            if (dbEntityIds.isEmpty()) {
                if (entityIds.isEmpty()) {
                    return false;
                }
                entityIds.clear();
                return true;
            }
            boolean retained = entityIds.retainAll(dbEntityIds);
            boolean added = checkAdded(dbEntityIds);
            return retained || added;
        }
        if (updated.isEmpty()) {
            entityIds.clear();
            return true;
        }
        entityIds = updated.getEntityIds();
        return true;
    }

    private boolean checkAdded(Collection<EntityId> updatedIds) {
        for (EntityId id : updatedIds) {
            if (entityIds.add(id)) {
                if (added == null) {
                    added = new ArrayList<>();
                }
                added.add(id);
            }
        }
        return added != null && !added.isEmpty();
    }

    @Override
    public boolean isEmpty() {
        return entityIds.isEmpty();
    }

    @Override
    public TbelCfArg toTbelCfArg() {
        return new TbelCfPropagationArg(entityIds);
    }

}
