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
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.AGENT_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AGENT_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AGENT_ROUTING_KEY_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AGENT_SECRET_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AGENT_TENANT_ID_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractAgentEntity<T extends Agent> extends BaseVersionedEntity<T> {

    @Column(name = AGENT_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = AGENT_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @Column(name = AGENT_NAME_PROPERTY)
    private String name;

    @Column(name = AGENT_ROUTING_KEY_PROPERTY)
    private String routingKey;

    @Column(name = AGENT_SECRET_PROPERTY)
    private String secret;

    public AbstractAgentEntity() {
        super();
    }

    public AbstractAgentEntity(T agent) {
        super(agent);
        if (agent.getTenantId() != null) {
            this.tenantId = agent.getTenantId().getId();
        }
        if (agent.getCustomerId() != null) {
            this.customerId = agent.getCustomerId().getId();
        }
        this.name = agent.getName();
        this.routingKey = agent.getRoutingKey();
        this.secret = agent.getSecret();
    }

    public AbstractAgentEntity(AgentEntity agentEntity) {
        super(agentEntity);
        this.tenantId = agentEntity.getTenantId();
        this.customerId = agentEntity.getCustomerId();
        this.name = agentEntity.getName();
        this.routingKey = agentEntity.getRoutingKey();
        this.secret = agentEntity.getSecret();
    }

    protected Agent toAgent() {
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
        agent.setRoutingKey(routingKey);
        agent.setSecret(secret);
        return agent;
    }

}
