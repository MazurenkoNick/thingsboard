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
package org.thingsboard.server.common.data.agent.template;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.InstallationAppType;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString
@Setter
public class AgentAppTemplate extends BaseData<AgentAppTemplateId> implements HasId<AgentAppTemplateId>, HasTenantId, HasVersion {

    private TenantId tenantId;
    private AgentApplicationType appType;
    private InstallationAppType type;
    private String currentVersion;
    private String previousVersion;
    private String nextVersion;
    private List<AgentAppStep> installSteps;
    private List<AgentAppStep> upgradeSteps;
    @Getter
    private Long version;

    public AgentAppTemplate() {
        super();
    }

    public AgentAppTemplate(AgentAppTemplateId id) {
        super(id);
    }

    public AgentAppTemplate(AgentAppTemplate template) {
        super(template);
        this.tenantId = template.getTenantId();
        this.appType = template.getAppType();
        this.type = template.getType();
        this.currentVersion = template.getCurrentVersion();
        this.previousVersion = template.getPreviousVersion();
        this.nextVersion = template.getNextVersion();
        this.installSteps = template.getInstallSteps();
        this.upgradeSteps = template.getUpgradeSteps();
        this.version = template.getVersion();
    }

    @Schema(description = "JSON object with the Agent App Template Id.")
    @Override
    public AgentAppTemplateId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the template creation, in milliseconds", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "JSON object with Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public TenantId getTenantId() {
        return tenantId;
    }

    @Schema(description = "Application type", requiredMode = Schema.RequiredMode.REQUIRED)
    public AgentApplicationType getAppType() {
        return appType;
    }

    @Schema(description = "Installation type", requiredMode = Schema.RequiredMode.REQUIRED)
    public InstallationAppType getType() {
        return type;
    }

    @Schema(description = "Current template version", requiredMode = Schema.RequiredMode.REQUIRED)
    public String getCurrentVersion() {
        return currentVersion;
    }

    @Schema(description = "Previous template version", requiredMode = Schema.RequiredMode.REQUIRED)
    public String getPreviousVersion() {
        return previousVersion;
    }

    @Schema(description = "Next template version")
    public String getNextVersion() {
        return nextVersion;
    }

    @Schema(description = "Install steps")
    public List<AgentAppStep> getInstallSteps() {
        return installSteps;
    }

    @Schema(description = "Upgrade steps")
    public List<AgentAppStep> getUpgradeSteps() {
        return upgradeSteps;
    }
}
