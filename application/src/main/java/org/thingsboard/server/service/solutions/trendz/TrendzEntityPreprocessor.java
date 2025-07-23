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
package org.thingsboard.server.service.solutions.trendz;

import org.thingsboard.server.service.solutions.trendz.data.TrendzEntityType;
import org.thingsboard.server.service.solutions.trendz.data.TrendzPreprocessConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class TrendzEntityPreprocessor {

    public abstract TrendzEntityType getEntityType();

    public abstract void preprocess(TrendzPreprocessConfig config);


    protected void setNewId(Map<UUID, UUID> oldToNewIdMap, Object object, String idField) {
        setNewIdWithDefault(oldToNewIdMap, object, idField, UUID.randomUUID());
    }

    protected void setNewIdWithDefault(Map<UUID, UUID> oldToNewIdMap, Object object, String idField, UUID defaultId) {
        Map<String, Object> objectMap = (Map<String, Object>) object;
        Object idValue = objectMap.get(idField);
        if (idValue != null) {
            UUID oldId = UUID.fromString(idValue.toString());
            UUID newId = oldToNewIdMap.computeIfAbsent(oldId, id -> defaultId);
            objectMap.put(idField, newId.toString());
        }
    }

    protected void setNewIdList(Map<UUID, UUID> oldToNewIdMap, Object object, String idField) {
        Map<String, Object> objectMap = (Map<String, Object>) object;
        List<Object> idList = (List<Object>) objectMap.get(idField);
        if (idList != null) {
            List<String> newIdList = idList.stream()
                    .map(id -> UUID.fromString(id.toString()))
                    .map(oldId -> oldToNewIdMap.computeIfAbsent(oldId, id -> UUID.randomUUID()))
                    .map(UUID::toString)
                    .toList();

            objectMap.put(idField, newIdList);
        }
    }

    protected void setNewIdMap(Map<UUID, UUID> oldToNewIdMap, Object object, String idField) {
        Map<String, Object> objectMap = (Map<String, Object>) object;
        Map<String, Object> idMap = (Map<String, Object>) objectMap.get(idField);
        if (idMap == null) {
            return;
        }
        Map<String, Object> newIdMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : idMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Key
            String newKey = key;
            try {
                UUID keyUuid = UUID.fromString(key);
                UUID newUUID = oldToNewIdMap.computeIfAbsent(keyUuid, id -> UUID.randomUUID());
                newKey = newUUID.toString();
            } catch (IllegalArgumentException e) {
                // ignore
            }

            // Value
            Object newValue = value;
            if (value != null) {
                if (value instanceof String) {
                    try {
                        UUID valueUuid = UUID.fromString((String) value);
                        UUID newUUID = oldToNewIdMap.computeIfAbsent(valueUuid, id -> UUID.randomUUID());
                        newValue = newUUID.toString();
                    } catch (IllegalArgumentException e) {
                        // ignore
                    }
                } else if (value instanceof UUID) {
                    UUID valueUuid = (UUID) value;
                    UUID newUUID = oldToNewIdMap.computeIfAbsent(valueUuid, id -> UUID.randomUUID());
                    newValue = newUUID;
                }
            }
            newIdMap.put(newKey, newValue);
        }
        objectMap.put(idField, newIdMap);
    }


    protected void setUserIds(Map<UUID, UUID> oldToNewIdMap, Object object, TrendzPreprocessConfig config) {
        UUID tenantId = config.getTenantId();
        UUID customerId = config.getCustomerId();
        UUID userId = config.getUserId();

        setNewIdWithDefault(oldToNewIdMap, object, "tenantId", tenantId);
        setNewIdWithDefault(oldToNewIdMap, object, "customerId", customerId);
        setNewIdWithDefault(oldToNewIdMap, object, "userId", userId);
    }
}
