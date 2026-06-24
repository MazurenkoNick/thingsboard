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
import org.thingsboard.server.common.data.id.EntityId;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentApplicationInfo extends AgentApplication {

    @Schema(description = "Current version of the template this application is based on.", accessMode = Schema.AccessMode.READ_ONLY)
    private String currentVersion;

    @Schema(description = "Next version available for upgrade.", accessMode = Schema.AccessMode.READ_ONLY)
    private String nextVersion;

    @Schema(description = "True if the app's config is outdated relative to its profile.", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean profileConfigOutdated;

    @Schema(description = "Name of the application profile this app is based on.", accessMode = Schema.AccessMode.READ_ONLY)
    private String profileName;

    @Schema(description = "Name of the owning agent.", accessMode = Schema.AccessMode.READ_ONLY)
    private String agentName;

    @Schema(description = "Related entity id (Edge or Gateway device) currently assigned to this application.", accessMode = Schema.AccessMode.READ_ONLY)
    private EntityId relatedEntityId;

    public AgentApplicationInfo() {
        super();
    }

    public AgentApplicationInfo(AgentApplication application, String currentVersion, String nextVersion) {
        super(application);
        this.currentVersion = currentVersion;
        this.nextVersion = nextVersion;
    }

    public AgentApplicationInfo(AgentApplication application, String currentVersion, String nextVersion, boolean profileConfigOutdated) {
        super(application);
        this.currentVersion = currentVersion;
        this.nextVersion = nextVersion;
        this.profileConfigOutdated = profileConfigOutdated;
    }

}
