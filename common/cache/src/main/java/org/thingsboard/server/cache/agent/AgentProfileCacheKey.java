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
package org.thingsboard.server.cache.agent;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serial;
import java.io.Serializable;

@Getter
@EqualsAndHashCode
public class AgentProfileCacheKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final TenantId tenantId;
    private final String name;
    private final boolean defaultProfile;

    private AgentProfileCacheKey(TenantId tenantId, String name, boolean defaultProfile) {
        this.tenantId = tenantId;
        this.name = name;
        this.defaultProfile = defaultProfile;
    }

    public static AgentProfileCacheKey forName(TenantId tenantId, String name) {
        return new AgentProfileCacheKey(tenantId, name, false);
    }

    public static AgentProfileCacheKey forDefaultProfile(TenantId tenantId) {
        return new AgentProfileCacheKey(tenantId, null, true);
    }

    /**
     * IMPORTANT: toString() must return a value that cannot collide with another key form.
     * The default-profile case returns the bare tenantId so it can never equal the
     * "tenantId_name" form (which always carries a "_name" suffix), even for a profile named "default".
     */
    @Override
    public String toString() {
        if (defaultProfile) {
            return tenantId.toString();
        }
        return tenantId + "_" + name;
    }
}
