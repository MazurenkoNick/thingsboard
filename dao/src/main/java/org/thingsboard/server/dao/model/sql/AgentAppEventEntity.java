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
import com.fasterxml.jackson.core.type.TypeReference;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;
import org.thingsboard.server.common.data.agent.AgentAppEventDeliveryState;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.Map;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.AGENT_APP_EVENT_TABLE_NAME)
public class AgentAppEventEntity extends BaseSqlEntity<AgentAppEvent> {

    @Column(name = ModelConstants.AGENT_APP_EVENT_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.AGENT_APP_EVENT_APPLICATION_ID_PROPERTY)
    private UUID applicationId;

    @Column(name = ModelConstants.AGENT_APP_EVENT_AGENT_ID_PROPERTY)
    private UUID agentId;

    @Column(name = ModelConstants.AGENT_APP_EVENT_APPLICATION_NAME_PROPERTY)
    private String applicationName;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.AGENT_APP_EVENT_ACTION_TYPE_PROPERTY)
    private AgentAppEventActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.AGENT_APP_EVENT_DELIVERY_STATE_PROPERTY)
    private AgentAppEventDeliveryState deliveryState;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.AGENT_APP_EVENT_STATUS_PROPERTY)
    private AgentAppEventStatus status;

    @Column(name = ModelConstants.AGENT_APP_EVENT_CURRENT_STEP_ID_PROPERTY)
    private UUID currentStepId;

    @Column(name = ModelConstants.AGENT_APP_EVENT_CURRENT_ACTIVITY_PROPERTY)
    private String currentActivity;

    @Column(name = ModelConstants.AGENT_APP_EVENT_ERROR_MESSAGE_PROPERTY)
    private String errorMessage;

    @Column(name = ModelConstants.AGENT_APP_EVENT_UPDATED_TIME_PROPERTY)
    private long updatedTime;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.AGENT_APP_EVENT_STEP_STATES_PROPERTY, columnDefinition = "jsonb")
    private JsonNode stepStates;

    @Column(name = ModelConstants.AGENT_APP_EVENT_BULK_ACTION_ID_PROPERTY)
    private UUID bulkActionId;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.AGENT_APP_EVENT_RESOLVED_ARGUMENTS_PROPERTY, columnDefinition = "jsonb")
    private JsonNode resolvedArguments;

    public AgentAppEventEntity() {
        super();
    }

    public AgentAppEventEntity(AgentAppEvent event) {
        super(event);
        if (event.getTenantId() != null) {
            this.tenantId = event.getTenantId().getId();
        }
        if (event.getApplicationId() != null) {
            this.applicationId = event.getApplicationId().getId();
        }
        if (event.getAgentId() != null) {
            this.agentId = event.getAgentId().getId();
        }
        this.applicationName = event.getApplicationName();
        this.actionType = event.getActionType();
        this.deliveryState = event.getDeliveryState();
        this.status = event.getStatus();
        this.currentStepId = event.getCurrentStepId();
        this.currentActivity = event.getCurrentActivity();
        this.errorMessage = event.getErrorMessage();
        this.updatedTime = event.getUpdatedTime();
        this.stepStates = JacksonUtil.convertValue(event.getStepStates(), JsonNode.class);
        this.bulkActionId = event.getBulkActionId();
        this.resolvedArguments = JacksonUtil.convertValue(event.getResolvedArguments(), JsonNode.class);
    }

    public AgentAppEventEntity(AgentAppEventEntity entity) {
        super(entity);
        this.tenantId = entity.tenantId;
        this.applicationId = entity.applicationId;
        this.agentId = entity.agentId;
        this.applicationName = entity.applicationName;
        this.actionType = entity.actionType;
        this.deliveryState = entity.deliveryState;
        this.status = entity.status;
        this.currentStepId = entity.currentStepId;
        this.currentActivity = entity.currentActivity;
        this.errorMessage = entity.errorMessage;
        this.updatedTime = entity.updatedTime;
        this.stepStates = entity.stepStates;
        this.bulkActionId = entity.bulkActionId;
        this.resolvedArguments = entity.resolvedArguments;
    }

    @Override
    public AgentAppEvent toData() {
        AgentAppEvent event = new AgentAppEvent(new AgentAppEventId(id));
        event.setCreatedTime(createdTime);
        if (tenantId != null) {
            event.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (applicationId != null) {
            event.setApplicationId(new AgentApplicationId(applicationId));
        }
        if (agentId != null) {
            event.setAgentId(new AgentId(agentId));
        }
        event.setApplicationName(applicationName);
        event.setActionType(actionType);
        event.setDeliveryState(deliveryState);
        event.setStatus(status);
        event.setCurrentStepId(currentStepId);
        event.setCurrentActivity(currentActivity);
        event.setErrorMessage(errorMessage);
        event.setUpdatedTime(updatedTime);
        event.setStepStates(stepStates != null ? JacksonUtil.convertValue(stepStates, new TypeReference<Map<UUID, AgentAppStepState>>() {}) : null);
        event.setBulkActionId(bulkActionId);
        event.setResolvedArguments(resolvedArguments != null ? JacksonUtil.convertValue(resolvedArguments, new TypeReference<Map<String, String>>() {}) : null);
        return event;
    }
}
