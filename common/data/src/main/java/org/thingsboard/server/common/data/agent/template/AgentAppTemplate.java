/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.common.data.agent.template;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString
@Setter
public class AgentAppTemplate extends BaseData<AgentAppTemplateId> implements HasId<AgentAppTemplateId>, HasTenantId, HasVersion, TenantEntity {

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_APP_TEMPLATE;
    }

    private TenantId tenantId;
    private AgentApplicationType appType;
    private AgentAppConfigType configType;
    private String imageDigest;
    private String currentVersion;
    private String nextVersion;
    private List<AgentAppStep> startSteps;
    private List<AgentAppStep> upgradeSteps;
    private List<AgentAppStep> deleteSteps;
    private List<AgentAppStep> rollbackSteps;
    private List<AgentAppStep> restartSteps;
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
        this.configType = template.getConfigType();
        this.imageDigest = template.getImageDigest();
        this.currentVersion = template.getCurrentVersion();
        this.nextVersion = template.getNextVersion();
        this.startSteps = template.getStartSteps();
        this.upgradeSteps = template.getUpgradeSteps();
        this.deleteSteps = template.getDeleteSteps();
        this.rollbackSteps = template.getRollbackSteps();
        this.restartSteps = template.getRestartSteps();
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

    @Schema(description = "Config type (e.g. 'DOCKER_COMPOSE'); the compose body lives in the template's compose-template step, not here")
    public AgentAppConfigType getConfigType() {
        return configType;
    }

    @Schema(description = "Expected Docker image digest for the main service container (e.g. 'sha256:abc123...')")
    public String getImageDigest() {
        return imageDigest;
    }

    @Schema(description = "Current template version", requiredMode = Schema.RequiredMode.REQUIRED)
    public String getCurrentVersion() {
        return currentVersion;
    }

    @Schema(description = "Next template version")
    public String getNextVersion() {
        return nextVersion;
    }

    @Schema(description = "Start steps")
    public List<AgentAppStep> getStartSteps() {
        return startSteps;
    }

    @Schema(description = "Upgrade steps")
    public List<AgentAppStep> getUpgradeSteps() {
        return upgradeSteps;
    }

    @Schema(description = "Delete steps")
    public List<AgentAppStep> getDeleteSteps() {
        return deleteSteps;
    }

    @Schema(description = "Rollback steps")
    public List<AgentAppStep> getRollbackSteps() {
        return rollbackSteps;
    }

    @Schema(description = "Restart steps")
    public List<AgentAppStep> getRestartSteps() {
        return restartSteps;
    }
}
