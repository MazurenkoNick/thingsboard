/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasLabel;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString
@Setter
public class Agent extends BaseDataWithAdditionalInfo<AgentId> implements HasLabel, HasTenantId, HasCustomerId, HasVersion {

    private TenantId tenantId;
    private CustomerId customerId;
    @NoXss
    @Length(fieldName = "name")
    private String name;
    @NoXss
    @Length(fieldName = "label")
    private String label;
    @Getter
    private Long version;

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

    @Schema(description = "Label that may be used in widgets", example = "Silo Agent on far field")
    public String getLabel() {
        return this.label;
    }
}
