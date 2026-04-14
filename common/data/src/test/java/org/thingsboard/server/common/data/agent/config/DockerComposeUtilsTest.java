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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerComposeUtilsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern EDGE_PATTERN = Pattern.compile("thingsboard/tb-edge:.+");
    private static final List<String> EDGE_CRED_KEYS = List.of("CLOUD_ROUTING_KEY", "CLOUD_ROUTING_SECRET");

    @Test
    void equalsIgnoringEnvKeys_returnsTrue_whenOnlyIgnoredKeysDiffer() {
        ObjectNode a = createEdgeCompose("rk-1", "secret-1");
        ObjectNode b = createEdgeCompose("rk-2", "secret-2");
        assertTrue(DockerComposeUtils.equalsIgnoringEnvKeys(a, b, EDGE_PATTERN, EDGE_CRED_KEYS));
    }

    @Test
    void equalsIgnoringEnvKeys_returnsFalse_whenNonIgnoredEnvKeyDiffers() {
        ObjectNode a = createEdgeCompose("rk-1", "secret-1");
        ObjectNode b = createEdgeCompose("rk-1", "secret-1");
        ((ObjectNode) b.get("services").get("mytbedge").get("environment")).put("CLOUD_RPC_HOST", "other-host");
        assertFalse(DockerComposeUtils.equalsIgnoringEnvKeys(a, b, EDGE_PATTERN, EDGE_CRED_KEYS));
    }

    @Test
    void equalsIgnoringEnvKeys_returnsFalse_whenServiceImageDiffers() {
        ObjectNode a = createEdgeCompose("rk-1", "secret-1");
        ObjectNode b = createEdgeCompose("rk-1", "secret-1");
        ((ObjectNode) b.get("services").get("mytbedge")).put("image", "thingsboard/tb-edge:9.9.9");
        assertFalse(DockerComposeUtils.equalsIgnoringEnvKeys(a, b, EDGE_PATTERN, EDGE_CRED_KEYS));
    }

    @Test
    void equalsIgnoringEnvKeys_doesNotMutateInputs() {
        ObjectNode a = createEdgeCompose("rk-1", "secret-1");
        ObjectNode b = createEdgeCompose("rk-2", "secret-2");
        DockerComposeUtils.equalsIgnoringEnvKeys(a, b, EDGE_PATTERN, EDGE_CRED_KEYS);

        assertTrue(a.get("services").get("mytbedge").get("environment").has("CLOUD_ROUTING_KEY"));
        assertTrue(b.get("services").get("mytbedge").get("environment").has("CLOUD_ROUTING_SECRET"));
    }

    @Test
    void equalsIgnoringEnvKeys_fallsBackToPlainEquality_whenPatternIsNull() {
        ObjectNode a = createEdgeCompose("rk-1", "secret-1");
        ObjectNode b = createEdgeCompose("rk-1", "secret-1");
        assertTrue(DockerComposeUtils.equalsIgnoringEnvKeys(a, b, null, EDGE_CRED_KEYS));

        ObjectNode c = createEdgeCompose("rk-1", "secret-1");
        ObjectNode d = createEdgeCompose("rk-2", "secret-1");
        assertFalse(DockerComposeUtils.equalsIgnoringEnvKeys(c, d, null, EDGE_CRED_KEYS));
    }

    @Test
    void equalsIgnoringEnvKeys_fallsBackToPlainEquality_whenKeyListIsEmpty() {
        ObjectNode a = createEdgeCompose("rk-1", "secret-1");
        ObjectNode b = createEdgeCompose("rk-2", "secret-1");
        assertFalse(DockerComposeUtils.equalsIgnoringEnvKeys(a, b, EDGE_PATTERN, List.of()));
    }

    @Test
    void equalsIgnoringEnvKeys_bothNull_returnsTrue() {
        assertTrue(DockerComposeUtils.equalsIgnoringEnvKeys(null, null, EDGE_PATTERN, EDGE_CRED_KEYS));
    }

    @Test
    void equalsIgnoringEnvKeys_oneNull_returnsFalse() {
        ObjectNode a = createEdgeCompose("rk-1", "secret-1");
        assertFalse(DockerComposeUtils.equalsIgnoringEnvKeys(a, null, EDGE_PATTERN, EDGE_CRED_KEYS));
    }

    private ObjectNode createEdgeCompose(String routingKey, String secret) {
        ObjectNode env = MAPPER.createObjectNode();
        env.put("CLOUD_ROUTING_KEY", routingKey);
        env.put("CLOUD_ROUTING_SECRET", secret);
        env.put("CLOUD_RPC_HOST", "tb.cloud");
        env.put("CLOUD_RPC_PORT", "7070");

        ObjectNode service = MAPPER.createObjectNode();
        service.put("image", "thingsboard/tb-edge:3.8.0");
        service.set("environment", env);

        ObjectNode services = MAPPER.createObjectNode();
        services.set("mytbedge", service);

        ObjectNode compose = MAPPER.createObjectNode();
        compose.set("services", services);
        return compose;
    }
}
