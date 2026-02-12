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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventDeliveryState;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.AGENT_APP_EVENT_TABLE_NAME)
public final class AgentAppEventEntity extends BaseSqlEntity<AgentAppEvent> {

    @Column(name = ModelConstants.AGENT_APP_EVENT_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.AGENT_APP_EVENT_APPLICATION_ID_PROPERTY)
    private UUID applicationId;

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
    private String currentStepId;

    @Column(name = ModelConstants.AGENT_APP_EVENT_TOTAL_STEPS_PROPERTY)
    private int totalSteps;

    @Column(name = ModelConstants.AGENT_APP_EVENT_UPDATED_TIME_PROPERTY)
    private long updatedTime;

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
        this.actionType = event.getActionType();
        this.deliveryState = event.getDeliveryState();
        this.status = event.getStatus();
        this.currentStepId = event.getCurrentStepId();
        this.totalSteps = event.getTotalSteps();
        this.updatedTime = event.getUpdatedTime();
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
        event.setActionType(actionType);
        event.setDeliveryState(deliveryState);
        event.setStatus(status);
        event.setCurrentStepId(currentStepId);
        event.setTotalSteps(totalSteps);
        event.setUpdatedTime(updatedTime);
        return event;
    }
}
