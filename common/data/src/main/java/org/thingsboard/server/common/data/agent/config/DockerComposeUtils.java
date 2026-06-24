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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
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
     * Reads the {@code image} of the first service whose image matches the given pattern.
     *
     * @return the image string, or {@code null} if no matching service is found
     */
    public static String getMainImage(JsonNode compose, Pattern imagePattern) {
        JsonNode service = findServiceByImage(compose, imagePattern);
        if (service == null) {
            return null;
        }
        JsonNode image = service.get("image");
        return image != null ? image.asText() : null;
    }

    /**
     * Sets the {@code image} on the first service whose current image matches the given pattern.
     */
    public static void setMainImage(JsonNode compose, Pattern imagePattern, String image) {
        JsonNode service = findServiceByImage(compose, imagePattern);
        if (service instanceof ObjectNode serviceObj) {
            serviceObj.set("image", new TextNode(image));
        }
    }

    private static JsonNode findServiceByImage(JsonNode compose, Pattern imagePattern) {
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
            if (service.isObject() && service.has("image")
                    && imagePattern.matcher(service.get("image").asText()).matches()) {
                return service;
            }
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
     * Sets environment variables on the first matching service, inserting missing keys.
     */
    public static void upsertEnvVariables(JsonNode compose, Pattern imagePattern, Map<String, String> envVars) {
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
                obj.set(entry.getKey(), new TextNode(entry.getValue()));
            }
            return;
        }
        ArrayNode arr = (ArrayNode) env;
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            int idx = findArrayEntryIndex(arr, entry.getKey());
            String value = entry.getKey() + "=" + entry.getValue();
            if (idx >= 0) {
                arr.set(idx, new TextNode(value));
            } else {
                arr.add(new TextNode(value));
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
            removeEnvVariables(aCopy, imagePattern, envKeysToIgnore);
            removeEnvVariables(bCopy, imagePattern, envKeysToIgnore);
        }
        return Objects.equals(aCopy, bCopy);
    }

    /** Removes the listed keys from the first matching service's environment. */
    public static void removeEnvVariables(JsonNode compose, Pattern imagePattern, List<String> keys) {
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

    /**
     * Collects host bind-mount sources that are declared with a relative path across all services' volumes.
     * Covers both the short syntax ({@code "source:target[:mode]"}) and the long syntax
     * ({@code {type: bind, source: ..., target: ...}}). Named volumes, absolute host paths, anonymous
     * volumes (container-only) and variable references ({@code $...}) are not reported.
     *
     * @return the list of offending relative source paths (empty when none)
     */
    public static List<String> findRelativeVolumeSources(JsonNode compose) {
        List<String> relative = new ArrayList<>();
        if (compose == null || compose.isNull()) {
            return relative;
        }
        JsonNode services = compose.get("services");
        if (services == null || !services.isObject()) {
            return relative;
        }
        Iterator<Map.Entry<String, JsonNode>> it = services.fields();
        while (it.hasNext()) {
            JsonNode service = it.next().getValue();
            if (!service.isObject()) {
                continue;
            }
            JsonNode volumes = service.get("volumes");
            if (volumes == null || !volumes.isArray()) {
                continue;
            }
            for (JsonNode volume : volumes) {
                String source = extractVolumeSource(volume);
                if (isRelativeHostPath(source)) {
                    relative.add(source);
                }
            }
        }
        return relative;
    }

    private static String extractVolumeSource(JsonNode volume) {
        if (volume == null) {
            return null;
        }
        if (volume.isTextual()) {
            String text = volume.asText();
            int colon = text.indexOf(':');
            // no colon -> anonymous volume (container path only), nothing to bind from the host
            return colon > 0 ? text.substring(0, colon) : null;
        }
        if (volume.isObject()) {
            JsonNode type = volume.get("type");
            // only bind mounts reference a host path; volume/tmpfs/npipe never do
            if (type != null && !"bind".equals(type.asText())) {
                return null;
            }
            JsonNode source = volume.get("source");
            return source != null ? source.asText() : null;
        }
        return null;
    }

    static boolean isRelativeHostPath(String source) {
        if (source == null || source.isBlank()) {
            return false;
        }
        // Allowed: variable references ('$...', resolved by the engine), absolute paths and named
        // volumes (a bare token with no path separator). Everything else non-absolute is a relative
        // host path: a leading '.' ('./', '../') or '~', or any source that carries a path separator
        // (e.g. 'data/logs', 'sub/../../etc') without being absolute.
        if (source.startsWith("$")) {
            return false;
        }
        if (isAbsoluteHostPath(source)) {
            return false;
        }
        return source.startsWith(".") || source.startsWith("~")
                || source.indexOf('/') >= 0 || source.indexOf('\\') >= 0;
    }

    private static boolean isAbsoluteHostPath(String source) {
        if (source.startsWith("/")) {
            return true;
        }
        // Windows absolute path, e.g. 'C:\...' or 'C:/...'.
        return source.length() >= 3 && Character.isLetter(source.charAt(0))
                && source.charAt(1) == ':' && (source.charAt(2) == '\\' || source.charAt(2) == '/');
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
