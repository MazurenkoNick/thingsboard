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
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.AgentBulkActionStatus;
import org.thingsboard.server.common.data.agent.BulkOperationResult;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.Map;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.AGENT_BULK_ACTION_TABLE_NAME)
public final class AgentBulkActionEntity extends BaseSqlEntity<AgentBulkAction> {

    @Column(name = ModelConstants.AGENT_BULK_ACTION_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.AGENT_BULK_ACTION_GROUP_ID_PROPERTY)
    private UUID groupId;

    @Column(name = ModelConstants.AGENT_BULK_ACTION_PROFILE_ID_PROPERTY)
    private UUID profileId;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.AGENT_BULK_ACTION_ACTION_TYPE_PROPERTY)
    private AgentAppEventActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.AGENT_BULK_ACTION_STATUS_PROPERTY)
    private AgentBulkActionStatus status;

    @Column(name = ModelConstants.AGENT_BULK_ACTION_ERROR_MSG_PROPERTY)
    private String errorMsg;

    @Column(name = ModelConstants.AGENT_BULK_ACTION_PROCESSING_STARTED_TIME_PROPERTY)
    private Long processingStartedTime;

    @Column(name = ModelConstants.AGENT_BULK_ACTION_TOTAL_PROPERTY)
    private int total;

    @Column(name = ModelConstants.AGENT_BULK_ACTION_SUBMITTED_PROPERTY)
    private int submitted;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.AGENT_BULK_ACTION_SKIP_COUNTS_PROPERTY, columnDefinition = "jsonb")
    private JsonNode skipCounts;

    public AgentBulkActionEntity() {
        super();
    }

    public AgentBulkActionEntity(AgentBulkAction action) {
        super(action);
        if (action.getTenantId() != null) {
            this.tenantId = action.getTenantId().getId();
        }
        this.groupId = action.getGroupId();
        this.profileId = action.getProfileId();
        this.actionType = action.getActionType();
        this.status = action.getStatus();
        this.errorMsg = action.getErrorMsg();
        this.processingStartedTime = action.getProcessingStartedTime();
        this.total = action.getTotal();
        this.submitted = action.getSubmitted();
        this.skipCounts = JacksonUtil.convertValue(action.getSkipCounts(), JsonNode.class);
    }

    @Override
    public AgentBulkAction toData() {
        AgentBulkAction action = new AgentBulkAction(new AgentBulkActionId(id));
        action.setCreatedTime(createdTime);
        if (tenantId != null) {
            action.setTenantId(TenantId.fromUUID(tenantId));
        }
        action.setGroupId(groupId);
        action.setProfileId(profileId);
        action.setActionType(actionType);
        action.setStatus(status);
        action.setErrorMsg(errorMsg);
        action.setProcessingStartedTime(processingStartedTime);
        action.setTotal(total);
        action.setSubmitted(submitted);
        action.setSkipCounts(skipCounts != null
                ? JacksonUtil.convertValue(skipCounts, new TypeReference<Map<BulkOperationResult.SkipReason, Integer>>() {})
                : null);
        return action;
    }
}
