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
import org.thingsboard.server.exception.DataValidationException;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DockerComposeConfig extends AgentAppConfig {

    private JsonNode compose;
    private String imageDigest;

    @Override
    public AgentAppConfigType getType() {
        return AgentAppConfigType.DOCKER_COMPOSE;
    }

    @Override
    public AgentAppConfig copy() {
        DockerComposeConfig copy = new DockerComposeConfig();
        copy.setCompose(this.compose != null ? this.compose.deepCopy() : null);
        copy.setImageDigest(this.imageDigest);
        return copy;
    }

    @Override
    public void validate() {
        if (compose == null || compose.isNull()) {
            throw new DataValidationException("Docker compose config compose content must be specified!");
        }
    }

    @Override
    public boolean isDeployFieldsChanged(AgentAppConfig other) {
        if (!(other instanceof DockerComposeConfig that)) {
            return true;
        }
        return !Objects.equals(this.compose, that.compose);
    }

    @Override
    @JsonIgnore
    public String getEdgeRoutingKey() {
        return getServiceEnvVariable(AgentApplicationType.EDGE.getMainImagePattern(), "CLOUD_ROUTING_KEY");
    }

    private String getServiceEnvVariable(Pattern imagePattern, String envVarName) {
        if (compose == null || imagePattern == null) {
            return null;
        }
        JsonNode services = compose.get("services");
        if (services == null || !services.isObject()) {
            return null;
        }
        Iterator<Map.Entry<String, JsonNode>> it = services.fields();
        while (it.hasNext()) {
            JsonNode service = it.next().getValue();
            if (!service.isObject() || !service.has("image")) {
                continue;
            }
            String image = service.get("image").asText();
            if (!imagePattern.matcher(image).matches()) {
                continue;
            }
            JsonNode environment = service.get("environment");
            if (environment != null && environment.isObject() && environment.has(envVarName)) {
                return environment.get(envVarName).asText();
            }
        }
        return null;
    }
}

