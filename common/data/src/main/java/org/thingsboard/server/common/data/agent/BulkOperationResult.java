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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Internal accumulator used while a bulk operation is filtered and executed (single-threaded).
 * It is not serialized in any REST response — {@link BulkOperationPreview} and
 * {@link AgentBulkAction} expose the totals/skip-counts to clients.
 */
@Data
@NoArgsConstructor
public class BulkOperationResult {

    private int total;
    private int submitted;
    private Collection<SkippedApp> skipped = new ArrayList<>();

    public void incrementTotal() {
        total++;
    }

    public void incrementSubmitted() {
        submitted++;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema
    public static class SkippedApp {
        @Schema(description = "Agent Id owning the application")
        private AgentId agentId;
        @Schema(description = "Agent name")
        private String agentName;
        @Schema(description = "Application Id")
        private AgentApplicationId applicationId;
        @Schema(description = "Application name")
        private String applicationName;
        @Schema(description = "Reason for skipping")
        private SkipReason reason;
        @Schema(description = "Optional message in case of a failure")
        private String msg;

        public SkippedApp(AgentId agentId, String agentName, AgentApplicationId applicationId, String applicationName, SkipReason reason) {
            this(agentId, agentName, applicationId, applicationName, reason, null);
        }
    }

    public enum SkipReason {
        VERSION_MISMATCH,
        ACTIVE_EVENT,
        RATE_LIMIT_EXCEEDED,
        ERROR
    }
}
