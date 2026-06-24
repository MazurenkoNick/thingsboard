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
package org.thingsboard.server.service.agent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.dao.device.DeviceConnectivityService;
import org.thingsboard.server.dao.util.DeviceConnectivityUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MergeHostValuesRuleTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String EDGE_IMAGE = "thingsboard/tb-edge-pe:4.0.0";
    private static final String GATEWAY_IMAGE = "thingsboard/tb-gateway:3.7";
    private static final int EDGE_RPC_PORT = 7070;

    private final DeviceConnectivityService deviceConnectivityService = mock(DeviceConnectivityService.class);
    private MergeHostValuesRule rule;

    @BeforeEach
    void setUp() {
        rule = new MergeHostValuesRule(deviceConnectivityService);
        ReflectionTestUtils.setField(rule, "edgeRpcPort", EDGE_RPC_PORT);
    }

    // --- supports ---

    @Test
    void supports_true_forEdgeWhenHostValuesRequested() {
        assertTrue(rule.supports(edgeApp(), ctx(true, "http://host:8080")));
    }

    @Test
    void supports_true_forGatewayWhenHostValuesRequested() {
        assertTrue(rule.supports(gatewayApp(), ctx(true, "http://host:8080")));
    }

    @Test
    void supports_false_whenCtxNull() {
        assertFalse(rule.supports(edgeApp(), null));
    }

    @Test
    void supports_false_whenSetHostValuesFalse() {
        assertFalse(rule.supports(edgeApp(), ctx(false, "http://host:8080")));
    }

    @Test
    void supports_false_forNonEdgeNonGatewayType() {
        AgentApplication app = app(AgentApplicationType.GENERIC, "postgres:15");
        assertFalse(rule.supports(app, ctx(true, "http://host:8080")));
    }

    // --- apply: EDGE ---

    @Test
    void apply_setsEdgeCloudRpcHostAndPort_forRemoteHost() {
        AgentApplication app = edgeApp();
        rule.apply(app, ctx(true, "http://192.168.1.50:8080"));
        assertEquals("192.168.1.50", env(app, "CLOUD_RPC_HOST"));
        assertEquals(String.valueOf(EDGE_RPC_PORT), env(app, "CLOUD_RPC_PORT"));
    }

    @Test
    void apply_rewritesLocalhostToDockerInternal_forEdge() {
        AgentApplication app = edgeApp();
        rule.apply(app, ctx(true, "http://localhost:8080"));
        assertEquals(DeviceConnectivityUtil.HOST_DOCKER_INTERNAL, env(app, "CLOUD_RPC_HOST"));
    }

    // --- apply: GATEWAY ---

    @Test
    void apply_setsGatewayHostAndPort() {
        when(deviceConnectivityService.resolveGatewayHost("http://demo.example.com:8080")).thenReturn("demo.example.com");
        AgentApplication app = gatewayApp();
        rule.apply(app, ctx(true, "http://demo.example.com:8080"));
        assertEquals("demo.example.com", env(app, "TB_GW_HOST"));
        assertEquals("1883", env(app, "TB_GW_PORT"));
    }

    @Test
    void apply_skipsGateway_whenResolvedHostBlank() {
        when(deviceConnectivityService.resolveGatewayHost("http://demo.example.com:8080")).thenReturn("");
        AgentApplication app = gatewayApp();
        rule.apply(app, ctx(true, "http://demo.example.com:8080"));
        assertEquals("PLACEHOLDER", env(app, "TB_GW_HOST"));
    }

    // --- apply: no-op cases ---

    @Test
    void apply_skips_whenBaseUrlBlank() {
        AgentApplication app = edgeApp();
        rule.apply(app, ctx(true, null));
        assertEquals("PLACEHOLDER", env(app, "CLOUD_RPC_HOST"));
    }

    @Test
    void apply_doesNotThrow_whenComposeNull() {
        AgentApplication app = new AgentApplication();
        app.setAppType(AgentApplicationType.EDGE);
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(null);
        app.setConfig(config);
        rule.apply(app, ctx(true, "http://host:8080"));
    }

    // --- fixtures ---

    private static AgentApplication edgeApp() {
        AgentApplication app = app(AgentApplicationType.EDGE, EDGE_IMAGE);
        addEnv(app, "CLOUD_RPC_HOST", "CLOUD_RPC_PORT");
        return app;
    }

    private static AgentApplication gatewayApp() {
        AgentApplication app = app(AgentApplicationType.GATEWAY, GATEWAY_IMAGE);
        addEnv(app, "TB_GW_HOST", "TB_GW_PORT");
        return app;
    }

    private static AgentApplication app(AgentApplicationType appType, String image) {
        ObjectNode service = MAPPER.createObjectNode().put("image", image);
        ObjectNode services = MAPPER.createObjectNode();
        services.set("main", service);
        ObjectNode compose = MAPPER.createObjectNode();
        compose.set("services", services);
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);
        AgentApplication app = new AgentApplication();
        app.setAppType(appType);
        app.setConfig(config);
        return app;
    }

    private static void addEnv(AgentApplication app, String... keys) {
        ObjectNode environment = MAPPER.createObjectNode();
        for (String key : keys) {
            environment.put(key, "PLACEHOLDER");
        }
        ObjectNode main = (ObjectNode) compose(app).get("services").get("main");
        main.set("environment", environment);
    }

    private static JsonNode compose(AgentApplication app) {
        return ((DockerComposeConfig) app.getConfig()).getCompose();
    }

    private static String env(AgentApplication app, String key) {
        return compose(app).get("services").get("main").get("environment").get(key).asText();
    }

    private static AppConfigMergeCtx ctx(boolean setHostValues, String baseUrl) {
        return AppConfigMergeCtx.builder()
                .setHostValues(setHostValues)
                .baseUrl(baseUrl)
                .build();
    }
}
