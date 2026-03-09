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
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString
@Setter
public class Agent extends BaseData<AgentId> implements HasId<AgentId>, HasTenantId, HasCustomerId, HasVersion, HasName {

    private TenantId tenantId;
    private CustomerId customerId;
    @NoXss
    @Length(fieldName = "name")
    private String name;
    @NoXss
    @Length(fieldName = "routingKey")
    private String routingKey;
    @NoXss
    @Length(fieldName = "secret")
    private String secret;
    @Getter
    private Long version;

    public Agent() {
        super();
    }

    public Agent(AgentId id) {
        this.id = id;
    }

    public Agent(Agent agent) {
        super(agent);
        this.tenantId = agent.getTenantId();
        this.customerId = agent.getCustomerId();
        this.name = agent.getName();
        this.routingKey = agent.getRoutingKey();
        this.secret = agent.getSecret();
        this.version = agent.getVersion();
    }

    @Schema(description = "JSON object with the Agent Id. " +
            "Specify this field to update the Agent. " +
            "Referencing non-existing Agent Id will cause error. " +
            "Omit this field to create new Agent." )
    @Override
    public AgentId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the edge creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "JSON object with Tenant Id. Use 'assignAgentToTenant' to change the Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public TenantId getTenantId() {
        return this.tenantId;
    }

    @Schema(description = "JSON object with Customer Id. Use 'assignAgentToCustomer' to change the Customer Id.", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public CustomerId getCustomerId() {
        return this.customerId;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Unique Agent Name in scope of Tenant", example = "Silo_A_Agent")
    @Override
    public String getName() {
        return this.name;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Agent routing key used for authentication", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    public String getRoutingKey() {
        return this.routingKey;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Agent secret used for authentication")
    public String getSecret() {
        return this.secret;
    }
}
