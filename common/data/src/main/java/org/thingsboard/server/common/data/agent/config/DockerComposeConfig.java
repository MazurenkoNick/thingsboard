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
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.validation.NoXss;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DockerComposeConfig extends AgentAppConfig {

    @NoXss
    private JsonNode compose;

    @NoXss
    private String composeType;

    @Override
    public AgentAppConfigType getType() {
        return AgentAppConfigType.DOCKER_COMPOSE;
    }

    @Override
    public AgentAppConfig copy() {
        DockerComposeConfig copy = new DockerComposeConfig();
        copy.setCompose(this.compose != null ? this.compose.deepCopy() : null);
        copy.setComposeType(this.composeType);
        copy.setArguments(copyArguments());
        return copy;
    }

    @Override
    public void validate() {
        if (compose == null || compose.isNull()) {
            throw new DataValidationException("Docker compose config compose content must be specified!");
        }
        List<String> relativeVolumes = DockerComposeUtils.findRelativeVolumeSources(compose);
        if (!relativeVolumes.isEmpty()) {
            throw new DataValidationException("Docker compose config must not use relative paths in volumes: "
                    + relativeVolumes + ". Use an absolute host path or a named volume.");
        }
        validateArguments();
    }

    @Override
    public void validateForProfile(AgentApplicationType appType) {
        Pattern imagePattern = appType.getMainImagePattern();
        if (imagePattern == null) {
            return;
        }
        JsonNode env = DockerComposeUtils.findServiceEnvironment(compose, imagePattern);
        if (env == null) {
            throw new DataValidationException("Compose config must contain a service matching image pattern: " + imagePattern);
        }
        switch (appType) {
            case EDGE -> requireEnvKeys(env, "CLOUD_ROUTING_KEY", "CLOUD_ROUTING_SECRET", "CLOUD_RPC_HOST");
            case GATEWAY -> validateGatewayCredentialKeys(env);
        }
    }

    private void validateGatewayCredentialKeys(JsonNode env) {
        requireEnvKeys(env, "TB_GW_SECURITY_TYPE");
        String securityType = DockerComposeUtils.envGet(env, "TB_GW_SECURITY_TYPE");
        if (securityType == null || securityType.isBlank()) {
            throw new DataValidationException("Gateway security type (TB_GW_SECURITY_TYPE) must be specified");
        }
        switch (securityType) {
            case "accessToken" -> requireEnvKeys(env, "TB_GW_ACCESS_TOKEN");
            case "usernamePassword" -> requireEnvKeys(env, "TB_GW_CLIENT_ID", "TB_GW_USERNAME", "TB_GW_PASSWORD");
            default -> throw new DataValidationException("Unsupported gateway security type: " + securityType
                    + ". Supported types: accessToken, usernamePassword");
        }
    }

    private void requireEnvKeys(JsonNode env, String... keys) {
        List<String> missing = new ArrayList<>();
        for (String key : keys) {
            if (!DockerComposeUtils.envHasKey(env, key)) {
                missing.add(key);
            }
        }
        if (!missing.isEmpty()) {
            throw new DataValidationException("Compose config is missing required credential environment variables: " + missing);
        }
    }

    @Override
    @JsonIgnore
    public String getEdgeRoutingKey() {
        return DockerComposeUtils.getEnvVariable(
                compose, AgentApplicationType.EDGE.getMainImagePattern(), "CLOUD_ROUTING_KEY");
    }
}

