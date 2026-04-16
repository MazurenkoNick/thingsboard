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
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.id.AgentGroupId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString
@Setter
public class AgentGroup extends BaseData<AgentGroupId> implements HasId<AgentGroupId>, HasTenantId, HasVersion, HasName {

    private TenantId tenantId;
    @NoXss
    @Length(fieldName = "name")
    private String name;
    @NoXss
    @Length(fieldName = "description")
    private String description;
    @NoXss
    @Length(fieldName = "provisionKey")
    private String provisionKey;
    @NoXss
    @Length(fieldName = "provisionSecret")
    private String provisionSecret;
    private AgentProvisionType provisionType;
    @Getter
    private Long version;

    public AgentGroup() {
        super();
    }

    public AgentGroup(AgentGroupId id) {
        super(id);
    }

    public AgentGroup(AgentGroup group) {
        super(group);
        this.tenantId = group.getTenantId();
        this.name = group.getName();
        this.description = group.getDescription();
        this.provisionKey = group.getProvisionKey();
        this.provisionSecret = group.getProvisionSecret();
        this.provisionType = group.getProvisionType();
        this.version = group.getVersion();
    }

    @Schema(description = "JSON object with the Agent Group Id.")
    @Override
    public AgentGroupId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the group creation, in milliseconds", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "JSON object with Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public TenantId getTenantId() {
        return tenantId;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Unique group name within tenant")
    @Override
    public String getName() {
        return name;
    }

    @Schema(description = "Group description")
    public String getDescription() {
        return description;
    }

    @Schema(description = "Provision key for future auto-provisioning")
    public String getProvisionKey() {
        return provisionKey;
    }

    @Schema(description = "Provision secret for future auto-provisioning")
    public String getProvisionSecret() {
        return provisionSecret;
    }

    @Schema(description = "Provisioning strategy. DISABLED by default.")
    public AgentProvisionType getProvisionType() {
        return provisionType;
    }
}
