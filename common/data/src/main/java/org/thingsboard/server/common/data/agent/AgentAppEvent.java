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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;
import java.util.UUID;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
@Setter
public class AgentAppEvent extends BaseData<AgentAppEventId> implements HasId<AgentAppEventId>, HasTenantId, TenantEntity {

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_APP_EVENT;
    }

    private TenantId tenantId;
    private AgentApplicationId applicationId;
    private AgentId agentId;
    private String applicationName;
    private AgentAppEventActionType actionType;
    private AgentAppEventDeliveryState deliveryState;
    private AgentAppEventStatus status;
    private UUID currentStepId;
    private String currentActivity;
    private String errorMessage;
    private long updatedTime;
    private Map<UUID, AgentAppStepState> stepStates;
    private UUID bulkActionId;
    private Map<String, String> resolvedArguments;

    public AgentAppEvent() {
        super();
    }

    public AgentAppEvent(AgentAppEventId id) {
        super(id);
    }

    public AgentAppEvent(AgentAppEvent event) {
        super(event);
        this.tenantId = event.getTenantId();
        this.applicationId = event.getApplicationId();
        this.agentId = event.getAgentId();
        this.applicationName = event.getApplicationName();
        this.actionType = event.getActionType();
        this.deliveryState = event.getDeliveryState();
        this.status = event.getStatus();
        this.currentStepId = event.getCurrentStepId();
        this.currentActivity = event.getCurrentActivity();
        this.errorMessage = event.getErrorMessage();
        this.updatedTime = event.getUpdatedTime();
        this.stepStates = event.getStepStates();
        this.bulkActionId = event.getBulkActionId();
        this.resolvedArguments = event.getResolvedArguments();
    }

    @Schema(description = "JSON object with the Agent App Event Id.")
    @Override
    public AgentAppEventId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the event creation, in milliseconds", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    public boolean hasActionType(AgentAppEventActionType type) {
        return actionType != null && actionType == type;
    }
}
