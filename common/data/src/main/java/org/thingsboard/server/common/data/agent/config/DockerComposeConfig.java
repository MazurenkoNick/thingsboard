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

    @Override
    public AgentAppConfigType getType() {
        return AgentAppConfigType.DOCKER_COMPOSE;
    }

    @Override
    public AgentAppConfig copy() {
        DockerComposeConfig copy = new DockerComposeConfig();
        copy.setCompose(this.compose != null ? this.compose.deepCopy() : null);
        return copy;
    }

    @Override
    public void validate() {
        if (compose == null || compose.isNull()) {
            throw new DataValidationException("Docker compose config compose content must be specified!");
        }
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

