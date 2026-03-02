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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;
import java.util.UUID;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
@Setter
public class AgentAppEvent extends BaseData<AgentAppEventId> implements HasId<AgentAppEventId>, HasTenantId {

    private TenantId tenantId;
    private AgentApplicationId applicationId;
    private AgentAppEventActionType actionType;
    private AgentAppEventDeliveryState deliveryState;
    private AgentAppEventStatus status;
    private UUID currentStepId;
    private long updatedTime;
    private Map<UUID, AgentAppStepState> stepStates;

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
        this.actionType = event.getActionType();
        this.deliveryState = event.getDeliveryState();
        this.status = event.getStatus();
        this.currentStepId = event.getCurrentStepId();
        this.updatedTime = event.getUpdatedTime();
        this.stepStates = event.getStepStates();
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
}
