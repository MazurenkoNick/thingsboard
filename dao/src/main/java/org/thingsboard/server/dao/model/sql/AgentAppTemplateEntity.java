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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.InstallationAppType;
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

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.AGENT_APP_TEMPLATE_INSTALLATION_TYPE_PROPERTY)
    private InstallationAppType installationType;

    @Column(name = ModelConstants.AGENT_APP_TEMPLATE_CURRENT_VERSION_PROPERTY)
    private String currentVersion;

    @Column(name = ModelConstants.AGENT_APP_TEMPLATE_PREVIOUS_VERSION_PROPERTY)
    private String previousVersion;

    @Column(name = ModelConstants.AGENT_APP_TEMPLATE_NEXT_VERSION_PROPERTY)
    private String nextVersion;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.AGENT_APP_TEMPLATE_CONFIG_PROPERTY)
    private JsonNode config;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.AGENT_APP_TEMPLATE_INSTALL_STEPS_PROPERTY)
    private JsonNode installSteps;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.AGENT_APP_TEMPLATE_UPGRADE_STEPS_PROPERTY)
    private JsonNode upgradeSteps;

    public AgentAppTemplateEntity() {
        super();
    }

    public AgentAppTemplateEntity(AgentAppTemplate template) {
        super(template);
        if (template.getTenantId() != null) {
            this.tenantId = template.getTenantId().getId();
        }
        this.appType = template.getAppType();
        this.installationType = template.getType();
        this.currentVersion = template.getCurrentVersion();
        this.previousVersion = template.getPreviousVersion();
        this.nextVersion = template.getNextVersion();
        this.config = template.getConfig() != null ? JacksonUtil.valueToTree(template.getConfig()) : null;
        this.installSteps = template.getInstallSteps() != null ? JacksonUtil.valueToTree(template.getInstallSteps()) : null;
        this.upgradeSteps = template.getUpgradeSteps() != null ? JacksonUtil.valueToTree(template.getUpgradeSteps()) : null;
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
        template.setType(installationType);
        template.setCurrentVersion(currentVersion);
        template.setPreviousVersion(previousVersion);
        template.setNextVersion(nextVersion);
        template.setConfig(config != null ? JacksonUtil.treeToValue(config, AgentAppConfig.class) : null);
        template.setInstallSteps(installSteps != null ? JacksonUtil.convertValue(installSteps, new TypeReference<List<AgentAppStep>>() {}) : null);
        template.setUpgradeSteps(upgradeSteps != null ? JacksonUtil.convertValue(upgradeSteps, new TypeReference<List<AgentAppStep>>() {}) : null);
        return template;
    }
}
