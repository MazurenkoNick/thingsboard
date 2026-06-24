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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.exception.DataValidationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
        service.put("image", "thingsboard/tb-edge-pe:3.8.0");
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
        service.put("image", "thingsboard/tb-edge-pe:3.8.0");
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
        edgeService.put("image", "thingsboard/tb-edge-pe:3.8.0");
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
        edge1.put("image", "thingsboard/tb-edge-pe:3.8.0");
        // no environment block

        ObjectNode edge2Env = newObjectNode();
        edge2Env.put("CLOUD_ROUTING_KEY", "second-key");
        ObjectNode edge2 = newObjectNode();
        edge2.put("image", "thingsboard/tb-edge-pe:3.9.0");
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

    // ==================== validate(): relative paths in volumes ====================

    @Test
    void testValidateAbsoluteBindVolume_valid() {
        DockerComposeConfig config = configWithStringVolumes("/opt/data:/var/lib/postgresql/data");

        assertDoesNotThrow(config::validate);
    }

    @Test
    void testValidateNamedVolume_valid() {
        DockerComposeConfig config = configWithStringVolumes("pgdata:/var/lib/postgresql/data");

        assertDoesNotThrow(config::validate);
    }

    @Test
    void testValidateAnonymousVolume_valid() {
        DockerComposeConfig config = configWithStringVolumes("/var/lib/postgresql/data");

        assertDoesNotThrow(config::validate);
    }

    @Test
    void testValidateVariableVolume_valid() {
        DockerComposeConfig config = configWithStringVolumes("${DATA_DIR}:/var/lib/postgresql/data");

        assertDoesNotThrow(config::validate);
    }

    @Test
    void testValidateNoVolumes_valid() {
        ObjectNode service = newObjectNode();
        service.put("image", "postgres:16");
        DockerComposeConfig config = configWithService(service);

        assertDoesNotThrow(config::validate);
    }

    @Test
    void testValidateLongSyntaxBindAbsolute_valid() {
        DockerComposeConfig config = configWithVolumes(longVolume("bind", "/opt/data", "/var/lib/postgresql/data"));

        assertDoesNotThrow(config::validate);
    }

    @Test
    void testValidateLongSyntaxNamedVolume_valid() {
        // a 'volume' type source is a named volume, never a host path - even if it looks relative
        DockerComposeConfig config = configWithVolumes(longVolume("volume", "./pgdata", "/var/lib/postgresql/data"));

        assertDoesNotThrow(config::validate);
    }

    @Test
    void testValidateCurrentDirRelativeVolume_throws() {
        DockerComposeConfig config = configWithStringVolumes("./data:/var/lib/postgresql/data");

        assertThatThrownBy(config::validate)
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("relative paths in volumes")
                .hasMessageContaining("./data");
    }

    @Test
    void testValidateParentDirRelativeVolume_throws() {
        DockerComposeConfig config = configWithStringVolumes("../data:/var/lib/postgresql/data");

        assertThatThrownBy(config::validate)
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("../data");
    }

    @Test
    void testValidateHomeRelativeVolume_throws() {
        DockerComposeConfig config = configWithStringVolumes("~/data:/var/lib/postgresql/data");

        assertThatThrownBy(config::validate)
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("~/data");
    }

    @Test
    void testValidateLongSyntaxBindRelative_throws() {
        DockerComposeConfig config = configWithVolumes(longVolume("bind", "./data", "/var/lib/postgresql/data"));

        assertThatThrownBy(config::validate)
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("./data");
    }

    @Test
    void testValidateRelativeVolumeAmongValidOnes_throws() {
        DockerComposeConfig config = configWithStringVolumes(
                "/opt/data:/data", "pgdata:/var/lib", "./logs:/logs");

        assertThatThrownBy(config::validate)
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("./logs");
    }

    private DockerComposeConfig configWithStringVolumes(String... volumes) {
        ArrayNode volumeArray = MAPPER.createArrayNode();
        for (String volume : volumes) {
            volumeArray.add(volume);
        }
        return configWithVolumeArray(volumeArray);
    }

    private DockerComposeConfig configWithVolumes(ObjectNode... volumes) {
        ArrayNode volumeArray = MAPPER.createArrayNode();
        for (ObjectNode volume : volumes) {
            volumeArray.add(volume);
        }
        return configWithVolumeArray(volumeArray);
    }

    private DockerComposeConfig configWithVolumeArray(ArrayNode volumeArray) {
        ObjectNode service = newObjectNode();
        service.put("image", "postgres:16");
        service.set("volumes", volumeArray);
        return configWithService(service);
    }

    private DockerComposeConfig configWithService(ObjectNode service) {
        ObjectNode services = newObjectNode();
        services.set("db", service);
        ObjectNode compose = newObjectNode();
        compose.set("services", services);
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);
        return config;
    }

    private ObjectNode longVolume(String type, String source, String target) {
        ObjectNode volume = newObjectNode();
        volume.put("type", type);
        volume.put("source", source);
        volume.put("target", target);
        return volume;
    }

    private ObjectNode createEdgeCompose(String routingKey) {
        ObjectNode env = newObjectNode();
        env.put("CLOUD_ROUTING_KEY", routingKey);
        ObjectNode service = newObjectNode();
        service.put("image", "thingsboard/tb-edge-pe:3.8.0");
        service.set("environment", env);
        ObjectNode services = newObjectNode();
        services.set("mytbedge", service);
        ObjectNode compose = newObjectNode();
        compose.set("services", services);
        return compose;
    }
}
