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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DockerComposeUtils {

    /**
     * Finds the environment node of the first service whose image matches the given pattern.
     *
     * @return the environment {@link ObjectNode}, or {@code null} if not found
     */
    public static ObjectNode findServiceEnvironment(JsonNode compose, Pattern imagePattern) {
        if (compose == null || compose.isNull() || imagePattern == null) {
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
            if (environment != null && environment.isObject()) {
                return (ObjectNode) environment;
            }
        }
        return null;
    }

    /**
     * Reads a single environment variable value from the first matching service.
     *
     * @return the variable value, or {@code null} if the service or variable is not found
     */
    public static String getEnvVariable(JsonNode compose, Pattern imagePattern, String envVarName) {
        ObjectNode env = findServiceEnvironment(compose, imagePattern);
        if (env != null && env.has(envVarName)) {
            return env.get(envVarName).asText();
        }
        return null;
    }

    /**
     * Sets environment variables on the first matching service.
     * Only overwrites keys that already exist in the environment node.
     */
    public static void setEnvVariables(JsonNode compose, Pattern imagePattern, Map<String, String> envVars) {
        if (envVars.isEmpty()) {
            return;
        }
        ObjectNode env = findServiceEnvironment(compose, imagePattern);
        if (env == null) {
            return;
        }
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            if (env.has(entry.getKey())) {
                env.set(entry.getKey(), new TextNode(entry.getValue()));
            }
        }
    }

}
