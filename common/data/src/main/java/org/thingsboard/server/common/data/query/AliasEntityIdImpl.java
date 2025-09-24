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
package org.thingsboard.server.common.data.query;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.UUIDBased;

import java.util.UUID;

public class AliasEntityIdImpl implements AliasEntityId {

    private UUID id;
    private EntityType entityType;
    private AliasEntityType aliasEntityType;
    private EntityId defaultEntityId;

    public AliasEntityIdImpl(EntityId entityId) {
        this.id = entityId.getId();
        this.entityType = entityId.getEntityType();
    }

    public AliasEntityIdImpl(AliasEntityType aliasEntityType, UUID id) {
        this.aliasEntityType = aliasEntityType;
        if (id != null) {
            switch (this.aliasEntityType) {
                case CURRENT_CUSTOMER:
                    this.defaultEntityId = new CustomerId(id);
                    break;
            }
        }
    }

    @Override
    public AliasEntityType getAliasEntityType() {
        return aliasEntityType;
    }

    @Override
    public EntityId defaultEntityId() {
        return defaultEntityId;
    }

    @Override
    public EntityId toEntityId() {
        return EntityIdFactory.getByTypeAndUuid(entityType, id);
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public EntityType getEntityType() {
        return entityType;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof EntityId otherEntityId))
            return false;
        if (obj instanceof AliasEntityId otherAliasEntityId) {
            if (otherAliasEntityId.isAliasEntityId()) {
                if (!this.isAliasEntityId()) {
                    return false;
                }
                if (this.aliasEntityType != otherAliasEntityId.getAliasEntityType()) {
                    return false;
                }
                if (this.defaultEntityId != null && !this.defaultEntityId.equals(otherAliasEntityId.defaultEntityId())) {
                    return false;
                }
                if (this.defaultEntityId == null && otherAliasEntityId.defaultEntityId() != null) {
                    return false;
                }
            }
        }
        if (this.isAliasEntityId()) {
            return false;
        }
        if (id == null) {
            return otherEntityId.getId() == null;
        } else return id.equals(otherEntityId.getId());
    }
}
