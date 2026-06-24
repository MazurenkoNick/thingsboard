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
package org.thingsboard.server.common.data.agent.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.validation.NoXss;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentAppArgument {

    @Schema(description = "Argument name, referenced in the compose as ${tb.<name>}.", example = "device_uuid")
    @NoXss
    private String name;

    @Schema(description = "Source entity the value is resolved from.")
    private AgentAppArgumentSource sourceType;

    @Schema(description = "Concrete source entity id. Required when sourceType references a specific entity " +
            "(DEVICE, ASSET, CUSTOMER, EDGE); ignored for the context-derived sources.")
    private EntityId sourceEntityId;

    @Schema(description = "Whether the value is read from an attribute or the latest telemetry.")
    private AgentAppArgumentValueType valueType;

    @Schema(description = "Attribute scope. Applicable only when valueType is ATTRIBUTE. Defaults to SERVER_SCOPE.")
    private AttributeScope scope;

    @Schema(description = "Attribute or latest telemetry key to read.", example = "cloud_endpoint")
    @NoXss
    private String key;

    @Schema(description = "Optional fallback value used when the source has no value for the key.")
    @NoXss
    private String defaultValue;

    @Schema(description = "How the resolved value is injected into the compose: STRING (quoted) or JSON (raw, " +
            "for arrays/objects/numbers when the placeholder is the whole value). Defaults to STRING.")
    private AgentAppArgumentFormat format;

    public AgentAppArgument(AgentAppArgument other) {
        this.name = other.name;
        this.sourceType = other.sourceType;
        this.sourceEntityId = other.sourceEntityId;
        this.valueType = other.valueType;
        this.scope = other.scope;
        this.key = other.key;
        this.defaultValue = other.defaultValue;
        this.format = other.format;
    }

    @JsonIgnore
    public boolean isJsonFormat() {
        return format == AgentAppArgumentFormat.JSON;
    }

    @JsonIgnore
    public AttributeScope getScopeOrDefault() {
        return scope != null ? scope : AttributeScope.SERVER_SCOPE;
    }

}
