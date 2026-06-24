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
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.AGENT_APP_PROFILE_TABLE_NAME)
public class AgentAppProfileEntity extends BaseVersionedEntity<AgentAppProfile> {

    @Column(name = ModelConstants.AGENT_APP_PROFILE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.AGENT_APP_PROFILE_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.AGENT_APP_PROFILE_DESCRIPTION_PROPERTY)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.AGENT_APP_PROFILE_APP_TYPE_PROPERTY)
    private AgentApplicationType appType;

    @Column(name = ModelConstants.AGENT_APP_PROFILE_TEMPLATE_ID_PROPERTY)
    private UUID templateId;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.AGENT_APP_PROFILE_CONFIG_PROPERTY, columnDefinition = "jsonb")
    private JsonNode config;

    public AgentAppProfileEntity() {
        super();
    }

    public AgentAppProfileEntity(AgentAppProfile profile) {
        super(profile);
        if (profile.getTenantId() != null) {
            this.tenantId = profile.getTenantId().getId();
        }
        this.name = profile.getName();
        this.description = profile.getDescription();
        this.appType = profile.getAppType();
        if (profile.getTemplateId() != null) {
            this.templateId = profile.getTemplateId().getId();
        }
        this.config = profile.getConfig() != null ? JacksonUtil.valueToTree(profile.getConfig()) : null;
    }

    public AgentAppProfileEntity(AgentAppProfileEntity entity) {
        super(entity);
        this.tenantId = entity.tenantId;
        this.name = entity.name;
        this.description = entity.description;
        this.appType = entity.appType;
        this.templateId = entity.templateId;
        this.config = entity.config;
    }

    @Override
    public AgentAppProfile toData() {
        AgentAppProfile profile = new AgentAppProfile(new AgentAppProfileId(id));
        profile.setCreatedTime(createdTime);
        profile.setVersion(version);
        if (tenantId != null) {
            profile.setTenantId(TenantId.fromUUID(tenantId));
        }
        profile.setName(name);
        profile.setDescription(description);
        profile.setAppType(appType);
        if (templateId != null) {
            profile.setTemplateId(new AgentAppTemplateId(templateId));
        }
        profile.setConfig(config != null ? JacksonUtil.treeToValue(config, AgentAppConfig.class) : null);
        return profile;
    }
}
