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
import lombok.Data;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Map;
import java.util.UUID;

@Schema(description = "Request payload for creating an agent application event (install, update, upgrade, restart, delete, etc.).")
@Data
public class AgentAppEventRequest {

    @Schema(description = "Action to perform against the agent application.")
    private AgentAppEventActionType actionType;

    @Schema(description = "Agent application payload supplied by the caller. "
            + "Used to carry name/config changes for UPDATE and the target state for INSTALL/UPGRADE.")
    private AgentApplication application;

    @Schema(description = "Per-step input overrides keyed by step id (e.g. pullImages flag, backup volume selection).")
    private Map<UUID, AgentAppStepState> stepInputs;

    @Schema(description = "INSTALL-action optional related entity (Edge or Gateway Device) to link to the application in the "
            + "same operation. When set, the application is created and the relation is assigned atomically. "
            + "Ignored for non-INSTALL actions.")
    private EntityId relatedEntityId;

    @Schema(description = "Optional bulk-action correlation id. "
            + "When the same value is used across multiple application events, duplicates are deduplicated server-side.")
    private UUID bulkActionId;

    @Schema(description = "UPDATE-action flag for profile-managed apps. When true, the compose is not re-resolved from the "
            + "(possibly upgraded) profile — only the credentials carried by `application.config` are applied. "
            + "Ignored for non-UPDATE actions and for non-profile-managed apps.")
    private boolean skipProfileRefetch;
}
