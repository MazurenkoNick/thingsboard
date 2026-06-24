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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.AgentProvisionType;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.AGENT_PROFILE_TABLE_NAME)
public class AgentProfileEntity extends BaseVersionedEntity<AgentProfile> {

    @Column(name = ModelConstants.AGENT_PROFILE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.AGENT_PROFILE_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.AGENT_PROFILE_DESCRIPTION_PROPERTY)
    private String description;

    @Column(name = ModelConstants.AGENT_PROFILE_PROVISION_KEY_PROPERTY)
    private String provisionKey;

    @Column(name = ModelConstants.AGENT_PROFILE_PROVISION_SECRET_PROPERTY)
    private String provisionSecret;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.AGENT_PROFILE_PROVISION_TYPE_PROPERTY)
    private AgentProvisionType provisionType;

    @Column(name = ModelConstants.AGENT_PROFILE_IS_DEFAULT_PROPERTY)
    private boolean isDefault;

    public AgentProfileEntity() {
        super();
    }

    public AgentProfileEntity(AgentProfile agentProfile) {
        super(agentProfile);
        if (agentProfile.getTenantId() != null) {
            this.tenantId = agentProfile.getTenantId().getId();
        }
        this.name = agentProfile.getName();
        this.description = agentProfile.getDescription();
        this.provisionKey = agentProfile.getProvisionKey();
        this.provisionSecret = agentProfile.getProvisionSecret();
        this.provisionType = agentProfile.getProvisionType();
        this.isDefault = agentProfile.isDefault();
    }

    @Override
    public AgentProfile toData() {
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
        return agentProfile;
    }
}
