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
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.AGENT_APP_UNIT_TABLE_NAME)
public final class AgentAppUnitEntity extends BaseSqlEntity<AgentAppUnit> {

    @Column(name = ModelConstants.AGENT_APP_UNIT_AGENT_APPLICATION_ID_PROPERTY)
    private UUID agentApplicationId;

    @Column(name = ModelConstants.AGENT_APP_UNIT_IDENTIFIER_PROPERTY)
    private String identifier;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.AGENT_APP_UNIT_TYPE_PROPERTY)
    private AgentAppUnitType type;

    public AgentAppUnitEntity() {
        super();
    }

    public AgentAppUnitEntity(AgentAppUnit unit) {
        super(unit);
        if (unit.getAgentApplicationId() != null) {
            this.agentApplicationId = unit.getAgentApplicationId().getId();
        }
        this.identifier = unit.getIdentifier();
        this.type = unit.getType();
    }

    @Override
    public AgentAppUnit toData() {
        AgentAppUnit unit = new AgentAppUnit(new AgentAppUnitId(id));
        unit.setCreatedTime(createdTime);
        if (agentApplicationId != null) {
            unit.setAgentApplicationId(new AgentApplicationId(agentApplicationId));
        }
        unit.setIdentifier(identifier);
        unit.setType(type);
        return unit;
    }
}
