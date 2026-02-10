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
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString
@Setter
public class AgentApplication extends BaseData<AgentApplicationId> implements HasId<AgentApplicationId>, HasTenantId, HasVersion {

    private TenantId tenantId;
    private AgentId agentId;
    private AgentApplicationType appType;
    private String name;
    private AgentAppTemplateId templateId;
    private AgentAppConfig config;
    private List<AgentAppStep> installSteps; // todo: rename to startSteps
    private List<AgentAppStep> updateSteps;
    @Getter
    private Long version;

    public AgentApplication() {
        super();
    }

    public AgentApplication(AgentApplicationId id) {
        super(id);
    }

    public AgentApplication(AgentApplication application) {
        super(application);
        this.tenantId = application.getTenantId();
        this.agentId = application.getAgentId();
        this.appType = application.getAppType();
        this.name = application.getName();
        this.templateId = application.getTemplateId();
        this.installSteps = application.getInstallSteps();
        this.updateSteps = application.getUpdateSteps();
        this.version = application.getVersion();
    }

    @Schema(description = "JSON object with the Agent Application Id.")
    @Override
    public AgentApplicationId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the agent application creation, in milliseconds", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "JSON object with Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public TenantId getTenantId() {
        return tenantId;
    }

    @Schema(description = "Agent this application belongs to", requiredMode = Schema.RequiredMode.REQUIRED)
    public AgentId getAgentId() {
        return agentId;
    }

    @Schema(description = "Application type", requiredMode = Schema.RequiredMode.REQUIRED)
    public AgentApplicationType getAppType() {
        return appType;
    }

    @Schema(description = "Application name (not unique across tenant)")
    public String getName() {
        return name;
    }

    @Schema(description = "Config (with compose field and type = 'DOCKER_COMPOSE' for EDGE/GATEWAY)")
    public AgentAppConfig getConfig() {
        return config;
    }

    @Schema(description = "Template this application is based on", requiredMode = Schema.RequiredMode.REQUIRED)
    public AgentAppTemplateId getTemplateId() {
        return templateId;
    }

    @Schema(description = "Application install steps with resolved configuration")
    public List<AgentAppStep> getInstallSteps() {
        return installSteps;
    }

    @Schema(description = "Application update steps with resolved configuration")
    public List<AgentAppStep> getUpdateSteps() {
        return updateSteps;
    }
}
