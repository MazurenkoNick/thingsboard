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
package org.thingsboard.server.common.data.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.HasId;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString
@Setter
public class AgentAppUnit extends BaseData<AgentAppUnitId> implements HasId<AgentAppUnitId> {

    private AgentApplicationId agentApplicationId;
    private String identifier;
    private String type;

    public AgentAppUnit() {
        super();
    }

    public AgentAppUnit(AgentAppUnitId id) {
        super(id);
    }

    public AgentAppUnit(AgentAppUnit unit) {
        super(unit);
        this.agentApplicationId = unit.getAgentApplicationId();
        this.identifier = unit.getIdentifier();
        this.type = unit.getType();
    }

    @Schema(description = "JSON object with the Agent App Unit Id.")
    @Override
    public AgentAppUnitId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the agent app unit creation, in milliseconds", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "Agent application this unit belongs to", requiredMode = Schema.RequiredMode.REQUIRED)
    public AgentApplicationId getAgentApplicationId() {
        return agentApplicationId;
    }

    @Schema(description = "Unit identifier", requiredMode = Schema.RequiredMode.REQUIRED)
    public String getIdentifier() {
        return identifier;
    }

    @Schema(description = "Unit type", requiredMode = Schema.RequiredMode.REQUIRED)
    public String getType() {
        return type;
    }
}
