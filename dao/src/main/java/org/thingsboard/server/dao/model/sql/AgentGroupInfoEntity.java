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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.agent.AgentGroup;
import org.thingsboard.server.common.data.agent.AgentGroupInfo;
import org.thingsboard.server.common.data.id.AgentGroupId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class AgentGroupInfoEntity extends BaseVersionedEntity<AgentGroupInfo> {

    public static final Map<String, String> agentGroupInfoColumnMap = new HashMap<>();
    static {
        agentGroupInfoColumnMap.put("customerTitle", "c.title");
    }

    private UUID tenantId;
    private UUID customerId;
    private String name;
    private String description;
    private String provisionKey;
    private String provisionSecret;
    private String customerTitle;
    private boolean customerIsPublic;

    public AgentGroupInfoEntity() {
        super();
    }

    public AgentGroupInfoEntity(AgentGroupEntity groupEntity,
                                String customerTitle,
                                Object customerAdditionalInfo) {
        this.id = groupEntity.getId();
        this.createdTime = groupEntity.getCreatedTime();
        this.version = groupEntity.getVersion();
        this.tenantId = groupEntity.getTenantId();
        this.customerId = groupEntity.getCustomerId();
        this.name = groupEntity.getName();
        this.description = groupEntity.getDescription();
        this.provisionKey = groupEntity.getProvisionKey();
        this.provisionSecret = groupEntity.getProvisionSecret();
        this.customerTitle = customerTitle;
        if (customerAdditionalInfo != null && ((JsonNode) customerAdditionalInfo).has("isPublic")) {
            this.customerIsPublic = ((JsonNode) customerAdditionalInfo).get("isPublic").asBoolean();
        } else {
            this.customerIsPublic = false;
        }
    }

    @Override
    public AgentGroupInfo toData() {
        AgentGroup group = new AgentGroup(new AgentGroupId(id));
        group.setCreatedTime(createdTime);
        group.setVersion(version);
        if (tenantId != null) {
            group.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            group.setCustomerId(new CustomerId(customerId));
        }
        group.setName(name);
        group.setDescription(description);
        group.setProvisionKey(provisionKey);
        group.setProvisionSecret(provisionSecret);
        return new AgentGroupInfo(group, customerTitle, customerIsPublic);
    }
}
