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
public class AgentBulkAction extends BaseData<AgentBulkActionId> implements HasId<AgentBulkActionId>, HasTenantId, TenantEntity {

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_BULK_ACTION;
    }

    private TenantId tenantId;
    private UUID agentProfileId;
    private UUID applicationProfileId;
    private AgentAppEventActionType actionType;
    private AgentBulkActionStatus status;
    private String errorMsg;
    private Long processingStartedTime;
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
        this.agentProfileId = action.getAgentProfileId();
        this.applicationProfileId = action.getApplicationProfileId();
        this.actionType = action.getActionType();
        this.status = action.getStatus();
        this.errorMsg = action.getErrorMsg();
        this.processingStartedTime = action.getProcessingStartedTime();
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
