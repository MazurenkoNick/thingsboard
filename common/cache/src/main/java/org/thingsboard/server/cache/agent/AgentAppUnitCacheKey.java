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

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serial;
import java.io.Serializable;

@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class AgentAppUnitCacheKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 433030109879120547L;

    private final AgentAppUnitId agentAppUnitId;

    private final TenantId tenantId;
    private final AgentId agentId;
    private final String projectName;
    private final String identifier;

    public static AgentAppUnitCacheKey from(AgentAppUnitId id) {
        return new AgentAppUnitCacheKey(id, null, null, null, null);
    }

    public static AgentAppUnitCacheKey from(AgentAppUnitCacheEvictEvent event) {
        return from(event.getTenantId(), event.getAgentId(), event.getProjectName(), event.getIdentifier());
    }

    public static AgentAppUnitCacheKey from(TenantId tenantId, AgentId agentId, String projectName, String identifier) {
        return new AgentAppUnitCacheKey(null, tenantId, agentId, projectName, identifier);
    }

    @Override
    public String toString() {
        return agentAppUnitId != null
                ? agentAppUnitId.toString()
                : tenantId + ":" + agentId + ":" + projectName + ":" + identifier;
    }
}
