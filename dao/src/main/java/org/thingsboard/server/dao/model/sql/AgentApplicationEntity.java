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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.AGENT_APPLICATION_TABLE_NAME)
public final class AgentApplicationEntity extends BaseVersionedEntity<AgentApplication> {

    @Column(name = ModelConstants.AGENT_APPLICATION_AGENT_ID_PROPERTY)
    private UUID agentId;

    @Column(name = ModelConstants.AGENT_APPLICATION_NAME_PROPERTY)
    private String name;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.AGENT_APPLICATION_PLACEHOLDERS_PROPERTY)
    private JsonNode placeholders;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.AGENT_APPLICATION_CONFIGURATION_PROPERTY)
    private JsonNode configuration;

    public AgentApplicationEntity() {
        super();
    }

    public AgentApplicationEntity(AgentApplication application) {
        super(application);
        if (application.getAgentId() != null) {
            this.agentId = application.getAgentId().getId();
        }
        this.name = application.getName();
        this.placeholders = application.getPlaceholders() != null ? JacksonUtil.valueToTree(application.getPlaceholders()) : null;
        this.configuration = application.getConfiguration() != null ? JacksonUtil.valueToTree(application.getConfiguration()) : null;
    }

    @Override
    public AgentApplication toData() {
        AgentApplication application = new AgentApplication(new AgentApplicationId(id));
        application.setCreatedTime(createdTime);
        application.setVersion(version);
        if (agentId != null) {
            application.setAgentId(new AgentId(agentId));
        }
        application.setName(name);
        application.setPlaceholders(placeholders != null ? JacksonUtil.convertValue(placeholders, new TypeReference<>() {}) : null);
        application.setConfiguration(configuration != null ? JacksonUtil.convertValue(configuration, new TypeReference<>() {}) : null);
        return application;
    }
}
