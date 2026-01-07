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
package org.thingsboard.server.common.data.util;

import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DataSourceUtils {

    public static List<EntityKey> setEntityKeyIfNotExists(List<EntityKey> entityKeys, EntityKeyType type, String keyName) {
        EntityKey targetEntityKey =
                entityKeys.stream().filter(key -> key.getKey().equals(keyName) && key.getType().equals(type)).findAny().orElse(null);
        if (targetEntityKey == null) {
            entityKeys = new ArrayList<>(entityKeys);
            entityKeys.add(new EntityKey(type, keyName));
        }
        return entityKeys;
    }

    public static Optional<String> getEntityLatestValue(EntityData entity, EntityKeyType keyType, String key) {
        return getLatestValue(entity.getLatest(), keyType, key);
    }

    public static Optional<String> getAlarmLatestValue(AlarmData alarm, EntityKeyType keyType, String key) {
        return getLatestValue(alarm.getLatest(), keyType, key);
    }

    private static Optional<String> getLatestValue(Map<EntityKeyType, Map<String, TsValue>> latest, EntityKeyType keyType, String key) {
        if (latest != null && latest.containsKey(keyType)) {
            Map<String, TsValue> values = latest.get(keyType);
            TsValue value = values.get(key);
            if (value != null) {
                return Optional.ofNullable(value.getValue());
            }
        }
        return Optional.empty();
    }

    public static EntityData entityDataFromEntityId(EntityId entityId) {
        return new EntityData(entityId, false, false, Collections.emptyMap(), Collections.emptyMap());
    }
}
