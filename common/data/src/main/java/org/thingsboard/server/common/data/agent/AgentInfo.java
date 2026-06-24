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
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.id.AgentId;

import java.io.Serial;
import java.util.List;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentInfo extends Agent {

    @Serial
    private static final long serialVersionUID = -5509870435345957907L;

    @Schema(description = "Title of the Customer that owns the agent.", accessMode = Schema.AccessMode.READ_ONLY)
    private String customerTitle;
    @Schema(description = "Indicates special 'Public' Customer that is auto-generated to use the agents on public dashboards.", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean customerIsPublic;
    @Schema(description = "Name of the Agent Profile the agent belongs to.", accessMode = Schema.AccessMode.READ_ONLY)
    private String agentProfileName;
    @Schema(description = "Owner name — tenant title if the agent is owned by a tenant, or customer title otherwise.", accessMode = Schema.AccessMode.READ_ONLY)
    private String ownerName;
    @Schema(description = "Entity groups that contain this agent (excluding the implicit 'All' group).", accessMode = Schema.AccessMode.READ_ONLY)
    private List<EntityInfo> groups;
    @Schema(description = "Whether the agent currently has an active connection (derived from server-scope 'active' attribute).", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean active;

    public AgentInfo() {
        super();
    }

    public AgentInfo(AgentId agentId) {
        super(agentId);
    }

    public AgentInfo(Agent agent, String customerTitle, boolean customerIsPublic, String agentProfileName) {
        super(agent);
        this.customerTitle = customerTitle;
        this.customerIsPublic = customerIsPublic;
        this.agentProfileName = agentProfileName;
    }
}
