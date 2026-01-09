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

import lombok.Getter;

public enum EntityKeyType {

    ATTRIBUTE("attribute"),
    CLIENT_ATTRIBUTE("clientAttribute"),
    SHARED_ATTRIBUTE("sharedAttribute"),
    SERVER_ATTRIBUTE("serverAttribute"),
    TIME_SERIES("timeseries"),
    ENTITY_FIELD("entityField"),
    ALARM_FIELD("alarm");

    @Getter
    private final String name;

    EntityKeyType(String name) {
        this.name = name;
    }

    public static EntityKeyType fromName(String name) {
        for (EntityKeyType entityKeyType : EntityKeyType.values()) {
            if (entityKeyType.getName().equals(name)) {
                return entityKeyType;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported entity key type : %s", name));
    }

    public boolean isAttribute() {
        return this == ATTRIBUTE || this == CLIENT_ATTRIBUTE || this == SHARED_ATTRIBUTE || this == SERVER_ATTRIBUTE;
    }
}
