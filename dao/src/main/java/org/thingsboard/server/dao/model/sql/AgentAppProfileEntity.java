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
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLJsonPGObjectJsonbType;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.AGENT_APP_PROFILE_TABLE_NAME)
public class AgentAppProfileEntity extends BaseVersionedEntity<AgentAppProfile> {

    @Column(name = ModelConstants.AGENT_APP_PROFILE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.AGENT_APP_PROFILE_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.AGENT_APP_PROFILE_DESCRIPTION_PROPERTY)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.AGENT_APP_PROFILE_APP_TYPE_PROPERTY)
    private AgentApplicationType appType;

    @Column(name = ModelConstants.AGENT_APP_PROFILE_TEMPLATE_ID_PROPERTY)
    private UUID templateId;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.AGENT_APP_PROFILE_CONFIG_PROPERTY, columnDefinition = "jsonb")
    private JsonNode config;

    public AgentAppProfileEntity() {
        super();
    }

    public AgentAppProfileEntity(AgentAppProfile profile) {
        super(profile);
        if (profile.getTenantId() != null) {
            this.tenantId = profile.getTenantId().getId();
        }
        this.name = profile.getName();
        this.description = profile.getDescription();
        this.appType = profile.getAppType();
        if (profile.getTemplateId() != null) {
            this.templateId = profile.getTemplateId().getId();
        }
        this.config = profile.getConfig() != null ? JacksonUtil.valueToTree(profile.getConfig()) : null;
    }

    @Override
    public AgentAppProfile toData() {
        AgentAppProfile profile = new AgentAppProfile(new AgentAppProfileId(id));
        profile.setCreatedTime(createdTime);
        profile.setVersion(version);
        if (tenantId != null) {
            profile.setTenantId(TenantId.fromUUID(tenantId));
        }
        profile.setName(name);
        profile.setDescription(description);
        profile.setAppType(appType);
        if (templateId != null) {
            profile.setTemplateId(new AgentAppTemplateId(templateId));
        }
        profile.setConfig(config != null ? JacksonUtil.treeToValue(config, AgentAppConfig.class) : null);
        return profile;
    }
}
