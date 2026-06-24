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
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.AgentProfileInfo;
import org.thingsboard.server.common.data.agent.AgentProvisionType;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class AgentProfileInfoEntity extends BaseVersionedEntity<AgentProfileInfo> {

    private UUID tenantId;
    private String name;
    private String description;
    private String provisionKey;
    private String provisionSecret;
    private AgentProvisionType provisionType;
    private boolean isDefault;

    public AgentProfileInfoEntity() {
        super();
    }

    public AgentProfileInfoEntity(AgentProfileEntity profileEntity) {
        this.id = profileEntity.getId();
        this.createdTime = profileEntity.getCreatedTime();
        this.version = profileEntity.getVersion();
        this.tenantId = profileEntity.getTenantId();
        this.name = profileEntity.getName();
        this.description = profileEntity.getDescription();
        this.provisionKey = profileEntity.getProvisionKey();
        this.provisionSecret = profileEntity.getProvisionSecret();
        this.provisionType = profileEntity.getProvisionType();
        this.isDefault = profileEntity.isDefault();
    }

    @Override
    public AgentProfileInfo toData() {
        AgentProfile agentProfile = new AgentProfile(new AgentProfileId(id));
        agentProfile.setCreatedTime(createdTime);
        agentProfile.setVersion(version);
        if (tenantId != null) {
            agentProfile.setTenantId(TenantId.fromUUID(tenantId));
        }
        agentProfile.setName(name);
        agentProfile.setDescription(description);
        agentProfile.setProvisionKey(provisionKey);
        agentProfile.setProvisionSecret(provisionSecret);
        agentProfile.setProvisionType(provisionType);
        agentProfile.setDefault(isDefault);
        return new AgentProfileInfo(agentProfile);
    }
}
