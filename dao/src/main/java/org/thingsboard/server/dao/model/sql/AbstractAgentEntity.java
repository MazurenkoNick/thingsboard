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
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.AGENT_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AGENT_DESCRIPTION_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AGENT_PROFILE_ID_PROPERTY;
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

    @Column(name = AGENT_DESCRIPTION_PROPERTY)
    private String description;

    @Column(name = AGENT_ROUTING_KEY_PROPERTY)
    private String routingKey;

    @Column(name = AGENT_SECRET_PROPERTY)
    private String secret;

    @Column(name = AGENT_PROFILE_ID_PROPERTY)
    private UUID agentProfileId;

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
        this.description = agent.getDescription();
        this.routingKey = agent.getRoutingKey();
        this.secret = agent.getSecret();
        if (agent.getAgentProfileId() != null) {
            this.agentProfileId = agent.getAgentProfileId().getId();
        }
    }

    public AbstractAgentEntity(AgentEntity agentEntity) {
        super(agentEntity);
        this.tenantId = agentEntity.getTenantId();
        this.customerId = agentEntity.getCustomerId();
        this.name = agentEntity.getName();
        this.description = agentEntity.getDescription();
        this.routingKey = agentEntity.getRoutingKey();
        this.secret = agentEntity.getSecret();
        this.agentProfileId = agentEntity.getAgentProfileId();
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
        agent.setDescription(description);
        agent.setRoutingKey(routingKey);
        agent.setSecret(secret);
        if (agentProfileId != null) {
            agent.setAgentProfileId(new AgentProfileId(agentProfileId));
        }
        return agent;
    }

}
