/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.agent.AgentGroup;
import org.thingsboard.server.common.data.agent.AgentProvisionType;
import org.thingsboard.server.common.data.id.AgentGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.AGENT_GROUP_TABLE_NAME)
public class AgentGroupEntity extends BaseVersionedEntity<AgentGroup> {

    @Column(name = ModelConstants.AGENT_GROUP_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.AGENT_GROUP_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.AGENT_GROUP_DESCRIPTION_PROPERTY)
    private String description;

    @Column(name = ModelConstants.AGENT_GROUP_PROVISION_KEY_PROPERTY)
    private String provisionKey;

    @Column(name = ModelConstants.AGENT_GROUP_PROVISION_SECRET_PROPERTY)
    private String provisionSecret;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.AGENT_GROUP_PROVISION_TYPE_PROPERTY)
    private AgentProvisionType provisionType;

    public AgentGroupEntity() {
        super();
    }

    public AgentGroupEntity(AgentGroup group) {
        super(group);
        if (group.getTenantId() != null) {
            this.tenantId = group.getTenantId().getId();
        }
        this.name = group.getName();
        this.description = group.getDescription();
        this.provisionKey = group.getProvisionKey();
        this.provisionSecret = group.getProvisionSecret();
        this.provisionType = group.getProvisionType();
    }

    @Override
    public AgentGroup toData() {
        AgentGroup group = new AgentGroup(new AgentGroupId(id));
        group.setCreatedTime(createdTime);
        group.setVersion(version);
        if (tenantId != null) {
            group.setTenantId(TenantId.fromUUID(tenantId));
        }
        group.setName(name);
        group.setDescription(description);
        group.setProvisionKey(provisionKey);
        group.setProvisionSecret(provisionSecret);
        group.setProvisionType(provisionType);
        return group;
    }
}
