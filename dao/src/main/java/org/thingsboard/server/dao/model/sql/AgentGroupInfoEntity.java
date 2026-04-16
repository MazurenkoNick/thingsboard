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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.agent.AgentGroup;
import org.thingsboard.server.common.data.agent.AgentGroupInfo;
import org.thingsboard.server.common.data.agent.AgentProvisionType;
import org.thingsboard.server.common.data.id.AgentGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class AgentGroupInfoEntity extends BaseVersionedEntity<AgentGroupInfo> {

    private UUID tenantId;
    private String name;
    private String description;
    private String provisionKey;
    private String provisionSecret;
    private AgentProvisionType provisionType;

    public AgentGroupInfoEntity() {
        super();
    }

    public AgentGroupInfoEntity(AgentGroupEntity groupEntity) {
        this.id = groupEntity.getId();
        this.createdTime = groupEntity.getCreatedTime();
        this.version = groupEntity.getVersion();
        this.tenantId = groupEntity.getTenantId();
        this.name = groupEntity.getName();
        this.description = groupEntity.getDescription();
        this.provisionKey = groupEntity.getProvisionKey();
        this.provisionSecret = groupEntity.getProvisionSecret();
        this.provisionType = groupEntity.getProvisionType();
    }

    @Override
    public AgentGroupInfo toData() {
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
        return new AgentGroupInfo(group);
    }
}
