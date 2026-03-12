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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DockerComposeConfigTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ObjectNode newObjectNode() {
        return MAPPER.createObjectNode();
    }

    @Test
    void testGetEdgeRoutingKey() {
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(createEdgeCompose("my-routing-key"));

        assertEquals("my-routing-key", config.getEdgeRoutingKey());
    }

    @Test
    void testGetEdgeRoutingKeyNoEdgeService() {
        ObjectNode env = newObjectNode();
        env.put("SOME_VAR", "value");
        ObjectNode service = newObjectNode();
        service.put("image", "postgres:16");
        service.set("environment", env);
        ObjectNode services = newObjectNode();
        services.set("db", service);
        ObjectNode compose = newObjectNode();
        compose.set("services", services);

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);

        assertNull(config.getEdgeRoutingKey());
    }

    @Test
    void testGetEdgeRoutingKeyNoEnvironment() {
        ObjectNode service = newObjectNode();
        service.put("image", "thingsboard/tb-edge:3.8.0");
        ObjectNode services = newObjectNode();
        services.set("mytbedge", service);
        ObjectNode compose = newObjectNode();
        compose.set("services", services);

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);

        assertNull(config.getEdgeRoutingKey());
    }

    @Test
    void testGetEdgeRoutingKeyNoRoutingKeyVar() {
        ObjectNode env = newObjectNode();
        env.put("CLOUD_ROUTING_SECRET", "secret");
        ObjectNode service = newObjectNode();
        service.put("image", "thingsboard/tb-edge:3.8.0");
        service.set("environment", env);
        ObjectNode services = newObjectNode();
        services.set("mytbedge", service);
        ObjectNode compose = newObjectNode();
        compose.set("services", services);

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);

        assertNull(config.getEdgeRoutingKey());
    }

    @Test
    void testGetEdgeRoutingKeyNullCompose() {
        DockerComposeConfig config = new DockerComposeConfig();

        assertNull(config.getEdgeRoutingKey());
    }

    @Test
    void testGetEdgeRoutingKeyMultipleServices() {
        ObjectNode dbService = newObjectNode();
        dbService.put("image", "postgres:16");

        ObjectNode edgeEnv = newObjectNode();
        edgeEnv.put("CLOUD_ROUTING_KEY", "edge-key");
        ObjectNode edgeService = newObjectNode();
        edgeService.put("image", "thingsboard/tb-edge:3.8.0");
        edgeService.set("environment", edgeEnv);

        ObjectNode services = newObjectNode();
        services.set("db", dbService);
        services.set("mytbedge", edgeService);
        ObjectNode compose = newObjectNode();
        compose.set("services", services);

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);

        assertEquals("edge-key", config.getEdgeRoutingKey());
    }

    @Test
    void testGetEdgeRoutingKeyFirstEdgeServiceMissingEnvVar() {
        ObjectNode edge1 = newObjectNode();
        edge1.put("image", "thingsboard/tb-edge:3.8.0");
        // no environment block

        ObjectNode edge2Env = newObjectNode();
        edge2Env.put("CLOUD_ROUTING_KEY", "second-key");
        ObjectNode edge2 = newObjectNode();
        edge2.put("image", "thingsboard/tb-edge:3.9.0");
        edge2.set("environment", edge2Env);

        ObjectNode services = newObjectNode();
        services.set("edge1", edge1);
        services.set("edge2", edge2);
        ObjectNode compose = newObjectNode();
        compose.set("services", services);

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);

        assertEquals("second-key", config.getEdgeRoutingKey());
    }

    private ObjectNode createEdgeCompose(String routingKey) {
        ObjectNode env = newObjectNode();
        env.put("CLOUD_ROUTING_KEY", routingKey);
        ObjectNode service = newObjectNode();
        service.put("image", "thingsboard/tb-edge:3.8.0");
        service.set("environment", env);
        ObjectNode services = newObjectNode();
        services.set("mytbedge", service);
        ObjectNode compose = newObjectNode();
        compose.set("services", services);
        return compose;
    }
}
