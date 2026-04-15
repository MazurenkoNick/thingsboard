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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DockerComposeUtils {

    /**
     * Finds the environment node of the first service whose image matches the given pattern.
     * The returned node may be an {@link ObjectNode} (map form) or {@link ArrayNode} (list form).
     *
     * @return the environment node, or {@code null} if no matching service has an env block
     */
    public static JsonNode findServiceEnvironment(JsonNode compose, Pattern imagePattern) {
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
            if (environment != null && (environment.isObject() || environment.isArray())) {
                return environment;
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
        return envGet(findServiceEnvironment(compose, imagePattern), envVarName);
    }

    /**
     * Sets environment variables on the first matching service.
     * Only overwrites keys that already exist in the environment node.
     */
    public static void setEnvVariables(JsonNode compose, Pattern imagePattern, Map<String, String> envVars) {
        if (envVars.isEmpty()) {
            return;
        }
        JsonNode env = findServiceEnvironment(compose, imagePattern);
        if (env == null) {
            return;
        }
        if (env.isObject()) {
            ObjectNode obj = (ObjectNode) env;
            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                if (obj.has(entry.getKey())) {
                    obj.set(entry.getKey(), new TextNode(entry.getValue()));
                }
            }
            return;
        }
        ArrayNode arr = (ArrayNode) env;
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            int idx = findArrayEntryIndex(arr, entry.getKey());
            if (idx >= 0) {
                arr.set(idx, new TextNode(entry.getKey() + "=" + entry.getValue()));
            }
        }
    }

    /**
     * Compare two compose documents for semantic equality while ignoring the
     * given env keys on the main service (identified by {@code imagePattern}).
     * Both inputs are deep-copied, so the originals are not modified.
     */
    public static boolean equalsIgnoringEnvKeys(JsonNode a, JsonNode b, Pattern imagePattern, List<String> envKeysToIgnore) {
        if (a == b) {
            return true;
        }
        JsonNode aCopy = a != null ? a.deepCopy() : null;
        JsonNode bCopy = b != null ? b.deepCopy() : null;
        if (imagePattern != null && envKeysToIgnore != null && !envKeysToIgnore.isEmpty()) {
            removeEnvKeys(aCopy, imagePattern, envKeysToIgnore);
            removeEnvKeys(bCopy, imagePattern, envKeysToIgnore);
        }
        return Objects.equals(aCopy, bCopy);
    }

    private static void removeEnvKeys(JsonNode compose, Pattern imagePattern, List<String> keys) {
        JsonNode env = findServiceEnvironment(compose, imagePattern);
        if (env == null) {
            return;
        }
        if (env.isObject()) {
            ObjectNode obj = (ObjectNode) env;
            for (String key : keys) {
                obj.remove(key);
            }
            return;
        }
        ArrayNode arr = (ArrayNode) env;
        for (String key : keys) {
            int idx = findArrayEntryIndex(arr, key);
            if (idx >= 0) {
                arr.remove(idx);
            }
        }
    }

    /** True when the env node (map or list) declares the given key. */
    public static boolean envHasKey(JsonNode env, String key) {
        if (env == null) {
            return false;
        }
        if (env.isObject()) {
            return env.has(key);
        }
        if (env.isArray()) {
            return findArrayEntryIndex((ArrayNode) env, key) >= 0;
        }
        return false;
    }

    /** Reads a value from either a map-form or list-form env node. */
    public static String envGet(JsonNode env, String key) {
        if (env == null) {
            return null;
        }
        if (env.isObject()) {
            return env.has(key) ? env.get(key).asText() : null;
        }
        if (env.isArray()) {
            int idx = findArrayEntryIndex((ArrayNode) env, key);
            if (idx < 0) {
                return null;
            }
            String entry = env.get(idx).asText();
            int eq = entry.indexOf('=');
            return eq >= 0 ? entry.substring(eq + 1) : "";
        }
        return null;
    }

    private static int findArrayEntryIndex(ArrayNode arr, String key) {
        String prefix = key + "=";
        for (int i = 0; i < arr.size(); i++) {
            JsonNode item = arr.get(i);
            if (item != null && item.isTextual()) {
                String v = item.asText();
                if (v.equals(key) || v.startsWith(prefix)) {
                    return i;
                }
            }
        }
        return -1;
    }

}
