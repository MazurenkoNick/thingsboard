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
import org.thingsboard.server.common.data.agent.BulkOperationResult.SkipReason;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;
import java.util.UUID;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
@Setter
public class AgentBulkAction extends BaseData<AgentBulkActionId> implements HasId<AgentBulkActionId>, HasTenantId {

    private TenantId tenantId;
    private UUID groupId;
    private UUID profileId;
    private AgentAppEventActionType actionType;
    private int total;
    private int submitted;
    private Map<SkipReason, Integer> skipCounts;

    public AgentBulkAction() {
        super();
    }

    public AgentBulkAction(AgentBulkActionId id) {
        super(id);
    }

    public AgentBulkAction(AgentBulkAction action) {
        super(action);
        this.tenantId = action.getTenantId();
        this.groupId = action.getGroupId();
        this.profileId = action.getProfileId();
        this.actionType = action.getActionType();
        this.total = action.getTotal();
        this.submitted = action.getSubmitted();
        this.skipCounts = action.getSkipCounts();
    }

    @Schema(description = "JSON object with the Agent Bulk Action Id.")
    @Override
    public AgentBulkActionId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the bulk action creation, in milliseconds", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }
}
