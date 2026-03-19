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

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.concurrent.ThreadLocalRandom;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString
@Setter
public class AgentApplication extends BaseData<AgentApplicationId> implements HasId<AgentApplicationId>, HasTenantId, HasVersion, HasName {

    private TenantId tenantId;
    private AgentId agentId;
    private String name;
    private AgentAppTemplateId templateId;
    @JsonIgnore
    @Getter
    private AgentAppTemplateId desiredTemplateId;
    private AgentApplicationType appType;
    private AgentAppConfig config;
    @Getter
    private Long version;
    @Getter
    private String projectName;
    @JsonIgnore
    @Getter
    private boolean pendingDeletion;
    private AgentApplicationOrigin origin;

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
        this.desiredTemplateId = application.getDesiredTemplateId();
        this.projectName = application.getProjectName();
        this.version = application.getVersion();
        this.pendingDeletion = application.isPendingDeletion();
        this.origin = application.getOrigin();
    }

    public static AgentApplication fromTemplate(AgentAppTemplate template) {
        AgentApplication app = new AgentApplication();
        app.setTemplateId(template.getId());
        app.setAppType(template.getAppType());
        app.setConfig(template.getConfig() != null ? template.getConfig().copy() : null);
        return app;
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

    @Schema(description = "Origin of the application (INSTALLED or DISCOVERED)")
    public AgentApplicationOrigin getOrigin() {
        return origin;
    }

    public static String generateProjectName() {
        return Long.toHexString(ThreadLocalRandom.current().nextLong(0x1000000000000L, 0xFFFFFFFFFFFFFL));
    }

}
