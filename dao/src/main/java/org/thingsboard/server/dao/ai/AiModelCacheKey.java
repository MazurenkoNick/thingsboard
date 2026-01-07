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
package org.thingsboard.server.dao.ai;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.thingsboard.server.cache.VersionedCacheKey;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

record AiModelCacheKey(UUID tenantId, UUID modelId) implements VersionedCacheKey {

    AiModelCacheKey {
        requireNonNull(tenantId);
        requireNonNull(modelId);

        if (TenantId.SYS_TENANT_ID.getId().equals(tenantId)) {
            throw new IllegalArgumentException("Tenant ID must not be the system tenant ID");
        }
        if (EntityId.NULL_UUID.equals(modelId)) {
            throw new IllegalArgumentException("Model ID must not be reserved null UUID");
        }
    }

    static AiModelCacheKey of(TenantId tenantId, AiModelId modelId) {
        return new AiModelCacheKey(tenantId.getId(), modelId.getId());
    }

    @Override
    public boolean isVersioned() {
        return true;
    }

    @NonNull
    @Override
    public String toString() {
        return /* cache name */ "_" + tenantId + "_" + modelId;
    }

}
