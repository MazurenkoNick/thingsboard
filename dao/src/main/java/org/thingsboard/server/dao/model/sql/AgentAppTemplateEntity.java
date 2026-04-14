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
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.AGENT_APP_TEMPLATE_TABLE_NAME)
public final class AgentAppTemplateEntity extends BaseVersionedEntity<AgentAppTemplate> {

    @Column(name = ModelConstants.AGENT_APP_TEMPLATE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.AGENT_APP_TEMPLATE_APP_TYPE_PROPERTY)
    private AgentApplicationType appType;

    @Column(name = ModelConstants.AGENT_APP_TEMPLATE_CURRENT_VERSION_PROPERTY)
    private String currentVersion;

    @Column(name = ModelConstants.AGENT_APP_TEMPLATE_NEXT_VERSION_PROPERTY)
    private String nextVersion;

    @Column(name = ModelConstants.AGENT_APP_TEMPLATE_IMAGE_DIGEST_PROPERTY)
    private String imageDigest;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.AGENT_APP_TEMPLATE_CONFIG_PROPERTY, columnDefinition = "jsonb")
    private JsonNode config;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.AGENT_APP_TEMPLATE_START_STEPS_PROPERTY, columnDefinition = "jsonb")
    private JsonNode startSteps;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.AGENT_APP_TEMPLATE_UPGRADE_STEPS_PROPERTY, columnDefinition = "jsonb")
    private JsonNode upgradeSteps;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.AGENT_APP_TEMPLATE_DELETE_STEPS_PROPERTY, columnDefinition = "jsonb")
    private JsonNode deleteSteps;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.AGENT_APP_TEMPLATE_ROLLBACK_STEPS_PROPERTY, columnDefinition = "jsonb")
    private JsonNode rollbackSteps;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.AGENT_APP_TEMPLATE_RESTART_STEPS_PROPERTY, columnDefinition = "jsonb")
    private JsonNode restartSteps;

    public AgentAppTemplateEntity() {
        super();
    }

    public AgentAppTemplateEntity(AgentAppTemplate template) {
        super(template);
        if (template.getTenantId() != null) {
            this.tenantId = template.getTenantId().getId();
        }
        this.appType = template.getAppType();
        this.currentVersion = template.getCurrentVersion();
        this.nextVersion = template.getNextVersion();
        this.imageDigest = template.getImageDigest();
        this.config = template.getConfig() != null ? JacksonUtil.valueToTree(template.getConfig()) : null;
        this.startSteps = template.getStartSteps() != null ? JacksonUtil.valueToTree(template.getStartSteps()) : null;
        this.upgradeSteps = template.getUpgradeSteps() != null ? JacksonUtil.valueToTree(template.getUpgradeSteps()) : null;
        this.deleteSteps = template.getDeleteSteps() != null ? JacksonUtil.valueToTree(template.getDeleteSteps()) : null;
        this.rollbackSteps = template.getRollbackSteps() != null ? JacksonUtil.valueToTree(template.getRollbackSteps()) : null;
        this.restartSteps = template.getRestartSteps() != null ? JacksonUtil.valueToTree(template.getRestartSteps()) : null;
    }

    @Override
    public AgentAppTemplate toData() {
        AgentAppTemplate template = new AgentAppTemplate(new AgentAppTemplateId(id));
        template.setCreatedTime(createdTime);
        template.setVersion(version);
        if (tenantId != null) {
            template.setTenantId(TenantId.fromUUID(tenantId));
        }
        template.setAppType(appType);
        template.setCurrentVersion(currentVersion);
        template.setNextVersion(nextVersion);
        template.setImageDigest(imageDigest);
        template.setConfig(config != null ? JacksonUtil.treeToValue(config, AgentAppConfig.class) : null);
        template.setStartSteps(startSteps != null ? JacksonUtil.convertValue(startSteps, new TypeReference<List<AgentAppStep>>() {}) : null);
        template.setUpgradeSteps(upgradeSteps != null ? JacksonUtil.convertValue(upgradeSteps, new TypeReference<List<AgentAppStep>>() {}) : null);
        template.setDeleteSteps(deleteSteps != null ? JacksonUtil.convertValue(deleteSteps, new TypeReference<List<AgentAppStep>>() {}) : null);
        template.setRollbackSteps(rollbackSteps != null ? JacksonUtil.convertValue(rollbackSteps, new TypeReference<List<AgentAppStep>>() {}) : null);
        template.setRestartSteps(restartSteps != null ? JacksonUtil.convertValue(restartSteps, new TypeReference<List<AgentAppStep>>() {}) : null);
        return template;
    }
}
