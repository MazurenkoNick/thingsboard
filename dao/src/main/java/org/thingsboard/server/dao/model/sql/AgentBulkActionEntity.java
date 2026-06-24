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

    @Column(name = ModelConstants.AGENT_BULK_ACTION_AGENT_PROFILE_ID_PROPERTY)
    private UUID agentProfileId;

    @Column(name = ModelConstants.AGENT_BULK_ACTION_APPLICATION_PROFILE_ID_PROPERTY)
    private UUID applicationProfileId;

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
        this.agentProfileId = action.getAgentProfileId();
        this.applicationProfileId = action.getApplicationProfileId();
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
        action.setAgentProfileId(agentProfileId);
        action.setApplicationProfileId(applicationProfileId);
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
