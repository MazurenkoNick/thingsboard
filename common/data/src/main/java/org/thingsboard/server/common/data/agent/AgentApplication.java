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
package org.thingsboard.server.common.data.agent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.concurrent.ThreadLocalRandom;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString
@Setter
public class AgentApplication extends BaseData<AgentApplicationId> implements HasId<AgentApplicationId>, HasTenantId, HasVersion, HasName, HasAgentAppConfig, HasOwnerId, TenantEntity {

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_APPLICATION;
    }

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
    private AgentAppProfileId applicationProfileId;
    @Getter
    private Long profileConfigVersion;

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
        this.config = application.getConfig();
        this.projectName = application.getProjectName();
        this.version = application.getVersion();
        this.pendingDeletion = application.isPendingDeletion();
        this.origin = application.getOrigin();
        this.applicationProfileId = application.getApplicationProfileId();
        this.profileConfigVersion = application.getProfileConfigVersion();
    }

    public static AgentApplication fromTemplate(AgentAppTemplate template) {
        AgentApplication app = new AgentApplication();
        app.setTemplateId(template.getId());
        app.setAppType(template.getAppType());
        app.setConfig(AgentAppConfig.forType(template.getConfigType()));
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

    // AgentApplication has no direct customer association — ownership cascades
    // from the parent Agent via entity-group membership, not a direct field.
    @Override
    public EntityId getOwnerId() {
        return tenantId;
    }

    @Override
    public void setOwnerId(EntityId entityId) {
        // no-op: AgentApplication does not store its own owner
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

    @Schema(description = "Application Profile Id. When set, config is read-only and inherited from the profile.")
    public AgentAppProfileId getApplicationProfileId() {
        return applicationProfileId;
    }

    public static String generateProjectName() {
        return Long.toHexString(ThreadLocalRandom.current().nextLong(0x1000000000000L, 0xFFFFFFFFFFFFFL));
    }

}
