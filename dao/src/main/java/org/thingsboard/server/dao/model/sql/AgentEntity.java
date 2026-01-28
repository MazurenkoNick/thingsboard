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
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.AGENT_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.ASSET_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ASSET_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ASSET_TENANT_ID_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = AGENT_TABLE_NAME)
public class AgentEntity extends BaseVersionedEntity<Agent> {

    @Column(name = ASSET_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ASSET_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @Column(name = ASSET_NAME_PROPERTY)
    private String name;

    @Override
    public Agent toData() {
        Agent agent = new Agent(new AgentId(id));
        agent.setCreatedTime(createdTime);
        agent.setVersion(version);
        if (tenantId != null) {
            agent.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            agent.setCustomerId(new CustomerId(customerId));
        }
        agent.setName(name);
        return agent;
    }
}
