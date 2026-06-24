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
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
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

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.AGENT_APP_TEMPLATE_CONFIG_TYPE_PROPERTY)
    private AgentAppConfigType configType;

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
        this.configType = template.getConfigType();
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
        template.setConfigType(configType);
        template.setStartSteps(startSteps != null ? JacksonUtil.convertValue(startSteps, new TypeReference<List<AgentAppStep>>() {}) : null);
        template.setUpgradeSteps(upgradeSteps != null ? JacksonUtil.convertValue(upgradeSteps, new TypeReference<List<AgentAppStep>>() {}) : null);
        template.setDeleteSteps(deleteSteps != null ? JacksonUtil.convertValue(deleteSteps, new TypeReference<List<AgentAppStep>>() {}) : null);
        template.setRollbackSteps(rollbackSteps != null ? JacksonUtil.convertValue(rollbackSteps, new TypeReference<List<AgentAppStep>>() {}) : null);
        template.setRestartSteps(restartSteps != null ? JacksonUtil.convertValue(restartSteps, new TypeReference<List<AgentAppStep>>() {}) : null);
        return template;
    }
}
