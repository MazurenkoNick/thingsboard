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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLJsonPGObjectJsonbType;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.AGENT_APPLICATION_TABLE_NAME)
public final class AgentApplicationEntity extends BaseVersionedEntity<AgentApplication> {

    @Column(name = ModelConstants.AGENT_APPLICATION_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.AGENT_APPLICATION_AGENT_ID_PROPERTY)
    private UUID agentId;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.AGENT_APPLICATION_APP_TYPE_PROPERTY)
    private AgentApplicationType appType;

    @Column(name = ModelConstants.AGENT_APPLICATION_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.AGENT_APPLICATION_TEMPLATE_ID_PROPERTY)
    private UUID templateId;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.AGENT_APPLICATION_CONFIG_PROPERTY, columnDefinition = "jsonb")
    private JsonNode config;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.AGENT_APPLICATION_START_STEPS_PROPERTY, columnDefinition = "jsonb")
    private JsonNode startSteps;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.AGENT_APPLICATION_UPGRADE_STEPS_PROPERTY, columnDefinition = "jsonb")
    private JsonNode upgradeSteps;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.AGENT_APPLICATION_DELETE_STEPS_PROPERTY, columnDefinition = "jsonb")
    private JsonNode deleteSteps;

    @Column(name = ModelConstants.AGENT_APPLICATION_PENDING_DELETION_PROPERTY)
    private boolean pendingDeletion;

    public AgentApplicationEntity() {
        super();
    }

    public AgentApplicationEntity(AgentApplication application) {
        super(application);
        if (application.getTenantId() != null) {
            this.tenantId = application.getTenantId().getId();
        }
        if (application.getAgentId() != null) {
            this.agentId = application.getAgentId().getId();
        }
        this.appType = application.getAppType();
        this.name = application.getName();
        if (application.getTemplateId() != null) {
            this.templateId = application.getTemplateId().getId();
        }
        this.config = application.getConfig() != null ? JacksonUtil.valueToTree(application.getConfig()) : null;
        this.startSteps = application.getStartSteps() != null ? JacksonUtil.valueToTree(application.getStartSteps()) : null;
        this.upgradeSteps = application.getUpgradeSteps() != null ? JacksonUtil.valueToTree(application.getUpgradeSteps()) : null;
        this.deleteSteps = application.getDeleteSteps() != null ? JacksonUtil.valueToTree(application.getDeleteSteps()) : null;
        this.pendingDeletion = application.isPendingDeletion();
    }

    @Override
    public AgentApplication toData() {
        AgentApplication application = new AgentApplication(new AgentApplicationId(id));
        application.setCreatedTime(createdTime);
        application.setVersion(version);
        if (tenantId != null) {
            application.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (agentId != null) {
            application.setAgentId(new AgentId(agentId));
        }
        application.setAppType(appType);
        application.setName(name);
        if (templateId != null) {
            application.setTemplateId(new AgentAppTemplateId(templateId));
        }
        application.setConfig(config != null ? JacksonUtil.treeToValue(config, AgentAppConfig.class) : null);
        application.setStartSteps(startSteps != null ? JacksonUtil.convertValue(startSteps, new TypeReference<>() {}) : null);
        application.setUpgradeSteps(upgradeSteps != null ? JacksonUtil.convertValue(upgradeSteps, new TypeReference<>() {}) : null);
        application.setDeleteSteps(deleteSteps != null ? JacksonUtil.convertValue(deleteSteps, new TypeReference<>() {}) : null);
        application.setPendingDeletion(pendingDeletion);
        return application;
    }
}
