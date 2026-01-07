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
package org.thingsboard.server.common.data.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;

@JsonDeserialize(using = AliasEntityIdDeserializer.class)
@JsonSerialize(using = AliasEntityIdSerializer.class)
@Schema
public interface AliasEntityId extends EntityId {

    AliasEntityType getAliasEntityType();

    EntityId defaultEntityId();

    EntityId toEntityId();

    @JsonIgnore
    default boolean isAliasEntityId() {
        return getAliasEntityType() != null;
    }

    static AliasEntityId fromEntityId(EntityId entityId) {
        if (entityId != null) {
            return new AliasEntityIdImpl(entityId);
        } else {
            return null;
        }
    }

    static AliasEntityId resolveAliasEntityId(AliasEntityId aliasEntityId, TenantId tenantId, UserId userId, EntityId userOwnerId) {
        if (aliasEntityId != null) {
            if (aliasEntityId.isAliasEntityId()) {
                AliasEntityType aliasEntityType = aliasEntityId.getAliasEntityType();
                switch (aliasEntityType) {
                    case CURRENT_CUSTOMER -> {
                        if (EntityType.CUSTOMER.equals(userOwnerId.getEntityType())) {
                            return fromEntityId(userOwnerId);
                        } else {
                            return fromEntityId(aliasEntityId.defaultEntityId());
                        }
                    }
                    case CURRENT_TENANT -> {
                        return fromEntityId(tenantId);
                    }
                    case CURRENT_USER -> {
                        return fromEntityId(userId);
                    }
                    case CURRENT_USER_OWNER -> {
                        return fromEntityId(userOwnerId);
                    }
                }
            } else {
                return aliasEntityId;
            }
        }
        return null;
    }

}
