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

import lombok.Getter;
import org.thingsboard.server.common.data.EntityType;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public enum AgentApplicationType {
    GENERIC(null, "1.0.0", null, Collections.emptyList()),
    EDGE("thingsboard/tb-edge-pe:.+", null, EntityType.EDGE,
            List.of("CLOUD_ROUTING_KEY", "CLOUD_ROUTING_SECRET")),
    GATEWAY("thingsboard/tb-gateway:.+", null, EntityType.DEVICE,
            List.of("TB_GW_SECURITY_TYPE", "TB_GW_ACCESS_TOKEN", "TB_GW_CLIENT_ID", "TB_GW_USERNAME", "TB_GW_PASSWORD"));

    @Getter
    private final Pattern mainImagePattern;
    @Getter
    private final String defaultVersion;
    @Getter
    private final EntityType relatedEntityType;
    @Getter
    private final List<String> credentialEnvKeys;

    AgentApplicationType(String mainImageRegex, String defaultVersion, EntityType relatedEntityType,
                         List<String> credentialEnvKeys) {
        this.mainImagePattern = mainImageRegex != null ? Pattern.compile(mainImageRegex) : null;
        this.defaultVersion = defaultVersion;
        this.relatedEntityType = relatedEntityType;
        this.credentialEnvKeys = credentialEnvKeys;
    }

    public boolean hasRelatedEntityType() {
        return relatedEntityType != null;
    }
}
