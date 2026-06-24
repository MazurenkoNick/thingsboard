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
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.AGENT_APPLICATION_TABLE_NAME)
public class AgentApplicationEntity extends BaseVersionedEntity<AgentApplication> {

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

    @Column(name = ModelConstants.AGENT_APPLICATION_DESIRED_TEMPLATE_ID_PROPERTY)
    private UUID desiredTemplateId;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.AGENT_APPLICATION_CONFIG_PROPERTY, columnDefinition = "jsonb")
    private JsonNode config;

    @Column(name = ModelConstants.AGENT_APPLICATION_PROJECT_NAME_PROPERTY)
    private String projectName;

    @Column(name = ModelConstants.AGENT_APPLICATION_PENDING_DELETION_PROPERTY)
    private boolean pendingDeletion;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.AGENT_APPLICATION_ORIGIN_PROPERTY)
    private AgentApplicationOrigin origin;

    @Column(name = ModelConstants.AGENT_APP_PROFILE_ID_PROPERTY)
    private UUID applicationProfileId;

    @Column(name = ModelConstants.AGENT_APPLICATION_PROFILE_CONFIG_VERSION_PROPERTY)
    private Long profileConfigVersion;

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
        if (application.getDesiredTemplateId() != null) {
            this.desiredTemplateId = application.getDesiredTemplateId().getId();
        }
        this.config = application.getConfig() != null ? JacksonUtil.valueToTree(application.getConfig()) : null;
        this.projectName = application.getProjectName();
        this.pendingDeletion = application.isPendingDeletion();
        this.origin = application.getOrigin();
        if (application.getApplicationProfileId() != null) {
            this.applicationProfileId = application.getApplicationProfileId().getId();
        }
        this.profileConfigVersion = application.getProfileConfigVersion();
    }

    public AgentApplicationEntity(AgentApplicationEntity entity) {
        super(entity);
        this.tenantId = entity.tenantId;
        this.agentId = entity.agentId;
        this.appType = entity.appType;
        this.name = entity.name;
        this.templateId = entity.templateId;
        this.desiredTemplateId = entity.desiredTemplateId;
        this.config = entity.config;
        this.projectName = entity.projectName;
        this.pendingDeletion = entity.pendingDeletion;
        this.origin = entity.origin;
        this.applicationProfileId = entity.applicationProfileId;
        this.profileConfigVersion = entity.profileConfigVersion;
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
        if (desiredTemplateId != null) {
            application.setDesiredTemplateId(new AgentAppTemplateId(desiredTemplateId));
        }
        application.setConfig(config != null ? JacksonUtil.treeToValue(config, AgentAppConfig.class) : null);
        application.setProjectName(projectName);
        application.setPendingDeletion(pendingDeletion);
        application.setOrigin(origin);
        if (applicationProfileId != null) {
            application.setApplicationProfileId(new AgentAppProfileId(applicationProfileId));
        }
        application.setProfileConfigVersion(profileConfigVersion);
        return application;
    }
}
