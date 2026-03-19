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
package org.thingsboard.server.service.agent.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.template.TemplateMergeCtx;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.service.agent.template.merge.MergeCredentialsMergeRule;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MergeCredentialsMergeRuleTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());

    @Mock
    private EdgeService edgeService;
    @Mock
    private DeviceCredentialsService deviceCredentialsService;

    private MergeCredentialsMergeRule rule;

    @BeforeEach
    void setUp() {
        rule = new MergeCredentialsMergeRule(edgeService, deviceCredentialsService);
        ReflectionTestUtils.setField(rule, "edgeRpcPort", 7070);
    }

    // ==================== supports() tests ====================

    @Test
    void supports_shouldReturnFalse_whenCtxIsNull() {
        AgentApplication app = createEdgeApp(null);
        assertFalse(rule.supports(app, new AgentAppTemplate(), null));
    }

    @Test
    void supports_shouldReturnFalse_whenRelatedEntityIdIsNull() {
        AgentApplication app = createEdgeApp(null);
        TemplateMergeCtx ctx = TemplateMergeCtx.builder().build();
        assertFalse(rule.supports(app, new AgentAppTemplate(), ctx));
    }

    @Test
    void supports_shouldReturnFalse_whenAppTypeIsGeneric() {
        AgentApplication app = createApp(AgentApplicationType.GENERIC, null);
        TemplateMergeCtx ctx = TemplateMergeCtx.builder().relatedEntityId(UUID.randomUUID()).build();
        assertFalse(rule.supports(app, new AgentAppTemplate(), ctx));
    }

    @Test
    void supports_shouldReturnFalse_whenAppTypeIsNull() {
        AgentApplication app = createApp(null, null);
        TemplateMergeCtx ctx = TemplateMergeCtx.builder().relatedEntityId(UUID.randomUUID()).build();
        assertFalse(rule.supports(app, new AgentAppTemplate(), ctx));
    }

    @Test
    void supports_shouldReturnTrue_whenEdgeAppWithRelatedEntityId() {
        AgentApplication app = createEdgeApp(null);
        TemplateMergeCtx ctx = TemplateMergeCtx.builder().relatedEntityId(UUID.randomUUID()).build();
        assertTrue(rule.supports(app, new AgentAppTemplate(), ctx));
    }

    @Test
    void supports_shouldReturnTrue_whenGatewayAppWithRelatedEntityId() {
        AgentApplication app = createApp(AgentApplicationType.GATEWAY, null);
        TemplateMergeCtx ctx = TemplateMergeCtx.builder().relatedEntityId(UUID.randomUUID()).build();
        assertTrue(rule.supports(app, new AgentAppTemplate(), ctx));
    }

    // ==================== apply() - Edge ====================

    @Test
    void apply_shouldInjectEdgeCredentials() {
        UUID edgeUuid = UUID.randomUUID();
        EdgeId edgeId = new EdgeId(edgeUuid);

        Edge edge = new Edge();
        edge.setId(edgeId);
        edge.setRoutingKey("test-routing-key");
        edge.setSecret("test-secret");
        when(edgeService.findEdgeById(TENANT_ID, edgeId)).thenReturn(edge);

        JsonNode compose = createEdgeCompose("placeholder-key", "placeholder-secret", "7070");
        AgentApplication app = createEdgeApp(compose);

        TemplateMergeCtx ctx = TemplateMergeCtx.builder().relatedEntityId(edgeUuid).build();
        rule.apply(app, new AgentAppTemplate(), ctx);

        JsonNode env = getServiceEnvironment(app, "mytbedge");
        assertEquals("test-routing-key", env.get("CLOUD_ROUTING_KEY").asText());
        assertEquals("test-secret", env.get("CLOUD_ROUTING_SECRET").asText());
        assertEquals("7070", env.get("CLOUD_RPC_PORT").asText());
    }

    @Test
    void apply_shouldSkip_whenEdgeNotFound() {
        UUID edgeUuid = UUID.randomUUID();
        EdgeId edgeId = new EdgeId(edgeUuid);
        when(edgeService.findEdgeById(TENANT_ID, edgeId)).thenReturn(null);

        JsonNode compose = createEdgeCompose("original-key", "original-secret", "7070");
        AgentApplication app = createEdgeApp(compose);

        TemplateMergeCtx ctx = TemplateMergeCtx.builder().relatedEntityId(edgeUuid).build();
        rule.apply(app, new AgentAppTemplate(), ctx);

        JsonNode env = getServiceEnvironment(app, "mytbedge");
        assertEquals("original-key", env.get("CLOUD_ROUTING_KEY").asText());
    }

    // ==================== apply() - Gateway (ACCESS_TOKEN) ====================

    @Test
    void apply_shouldInjectGatewayAccessTokenCredentials() {
        UUID deviceUuid = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(deviceUuid);

        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        credentials.setCredentialsId("my-access-token");
        when(deviceCredentialsService.findDeviceCredentialsByDeviceId(TENANT_ID, deviceId)).thenReturn(credentials);

        JsonNode compose = createGatewayAccessTokenCompose("placeholder-token", "placeholder-type");
        AgentApplication app = createGatewayApp(compose);

        TemplateMergeCtx ctx = TemplateMergeCtx.builder().relatedEntityId(deviceUuid).build();
        rule.apply(app, new AgentAppTemplate(), ctx);

        JsonNode env = getServiceEnvironment(app, "mygateway");
        assertEquals("accessToken", env.get("TB_GW_SECURITY_TYPE").asText());
        assertEquals("my-access-token", env.get("TB_GW_ACCESS_TOKEN").asText());
    }

    // ==================== apply() - Gateway (MQTT_BASIC) ====================

    @Test
    void apply_shouldInjectGatewayMqttBasicCredentials() {
        UUID deviceUuid = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(deviceUuid);

        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setCredentialsType(DeviceCredentialsType.MQTT_BASIC);
        credentials.setCredentialsValue("{\"clientId\":\"myClient\",\"userName\":\"myUser\",\"password\":\"myPass\"}");
        when(deviceCredentialsService.findDeviceCredentialsByDeviceId(TENANT_ID, deviceId)).thenReturn(credentials);

        JsonNode compose = createGatewayMqttBasicCompose();
        AgentApplication app = createGatewayApp(compose);

        TemplateMergeCtx ctx = TemplateMergeCtx.builder().relatedEntityId(deviceUuid).build();
        rule.apply(app, new AgentAppTemplate(), ctx);

        JsonNode env = getServiceEnvironment(app, "mygateway");
        assertEquals("usernamePassword", env.get("TB_GW_SECURITY_TYPE").asText());
        assertEquals("myClient", env.get("TB_GW_CLIENT_ID").asText());
        assertEquals("myUser", env.get("TB_GW_USERNAME").asText());
        assertEquals("myPass", env.get("TB_GW_PASSWORD").asText());
    }

    @Test
    void apply_shouldInjectGatewayMqttBasicCredentials_partialFields() {
        UUID deviceUuid = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(deviceUuid);

        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setCredentialsType(DeviceCredentialsType.MQTT_BASIC);
        credentials.setCredentialsValue("{\"userName\":\"onlyUser\"}");
        when(deviceCredentialsService.findDeviceCredentialsByDeviceId(TENANT_ID, deviceId)).thenReturn(credentials);

        JsonNode compose = createGatewayMqttBasicCompose();
        AgentApplication app = createGatewayApp(compose);

        TemplateMergeCtx ctx = TemplateMergeCtx.builder().relatedEntityId(deviceUuid).build();
        rule.apply(app, new AgentAppTemplate(), ctx);

        JsonNode env = getServiceEnvironment(app, "mygateway");
        assertEquals("usernamePassword", env.get("TB_GW_SECURITY_TYPE").asText());
        assertEquals("placeholder", env.get("TB_GW_CLIENT_ID").asText()); // not overwritten since field was empty
        assertEquals("onlyUser", env.get("TB_GW_USERNAME").asText());
        assertEquals("placeholder", env.get("TB_GW_PASSWORD").asText()); // not overwritten
    }

    @Test
    void apply_shouldSkip_whenDeviceCredentialsNotFound() {
        UUID deviceUuid = UUID.randomUUID();
        DeviceId deviceId = new DeviceId(deviceUuid);
        when(deviceCredentialsService.findDeviceCredentialsByDeviceId(TENANT_ID, deviceId)).thenReturn(null);

        JsonNode compose = createGatewayAccessTokenCompose("original-token", "accessToken");
        AgentApplication app = createGatewayApp(compose);

        TemplateMergeCtx ctx = TemplateMergeCtx.builder().relatedEntityId(deviceUuid).build();
        rule.apply(app, new AgentAppTemplate(), ctx);

        JsonNode env = getServiceEnvironment(app, "mygateway");
        assertEquals("original-token", env.get("TB_GW_ACCESS_TOKEN").asText());
    }

    // ==================== apply() - null/missing compose ====================

    @Test
    void apply_shouldSkip_whenComposeIsNull() {
        UUID edgeUuid = UUID.randomUUID();
        AgentApplication app = createEdgeApp(null);

        TemplateMergeCtx ctx = TemplateMergeCtx.builder().relatedEntityId(edgeUuid).build();
        // Should not throw
        rule.apply(app, new AgentAppTemplate(), ctx);
    }

    @Test
    void apply_shouldSkip_whenConfigIsNotDockerCompose() {
        UUID edgeUuid = UUID.randomUUID();
        AgentApplication app = new AgentApplication();
        app.setAppType(AgentApplicationType.EDGE);
        app.setTenantId(TENANT_ID);
        // config is null (not DockerComposeConfig)

        TemplateMergeCtx ctx = TemplateMergeCtx.builder().relatedEntityId(edgeUuid).build();
        rule.apply(app, new AgentAppTemplate(), ctx);
    }

    @Test
    void apply_shouldNotModify_whenEnvVarKeyNotPresent() {
        UUID edgeUuid = UUID.randomUUID();
        EdgeId edgeId = new EdgeId(edgeUuid);

        Edge edge = new Edge();
        edge.setId(edgeId);
        edge.setRoutingKey("test-key");
        edge.setSecret("test-secret");
        when(edgeService.findEdgeById(TENANT_ID, edgeId)).thenReturn(edge);

        // Compose with no CLOUD_ROUTING_KEY env var
        ObjectNode service = MAPPER.createObjectNode();
        service.put("image", "thingsboard/tb-edge:3.8.0");
        ObjectNode env = MAPPER.createObjectNode();
        env.put("SOME_OTHER_VAR", "value");
        service.set("environment", env);
        ObjectNode services = MAPPER.createObjectNode();
        services.set("mytbedge", service);
        ObjectNode compose = MAPPER.createObjectNode();
        compose.set("services", services);

        AgentApplication app = createEdgeApp(compose);
        TemplateMergeCtx ctx = TemplateMergeCtx.builder().relatedEntityId(edgeUuid).build();
        rule.apply(app, new AgentAppTemplate(), ctx);

        JsonNode resultEnv = getServiceEnvironment(app, "mytbedge");
        assertEquals("value", resultEnv.get("SOME_OTHER_VAR").asText());
        assertNull(resultEnv.get("CLOUD_ROUTING_KEY"));
    }

    // ==================== Helpers ====================

    private AgentApplication createEdgeApp(JsonNode compose) {
        return createAppWithCompose(AgentApplicationType.EDGE, compose);
    }

    private AgentApplication createGatewayApp(JsonNode compose) {
        return createAppWithCompose(AgentApplicationType.GATEWAY, compose);
    }

    private AgentApplication createApp(AgentApplicationType type, JsonNode compose) {
        return createAppWithCompose(type, compose);
    }

    private AgentApplication createAppWithCompose(AgentApplicationType type, JsonNode compose) {
        AgentApplication app = new AgentApplication();
        app.setAppType(type);
        app.setTenantId(TENANT_ID);
        if (compose != null) {
            DockerComposeConfig config = new DockerComposeConfig();
            config.setCompose(compose);
            app.setConfig(config);
        }
        return app;
    }

    private JsonNode createEdgeCompose(String routingKey, String secret, String rpcPort) {
        ObjectNode env = MAPPER.createObjectNode();
        env.put("CLOUD_ROUTING_KEY", routingKey);
        env.put("CLOUD_ROUTING_SECRET", secret);
        env.put("CLOUD_RPC_PORT", rpcPort);

        ObjectNode service = MAPPER.createObjectNode();
        service.put("image", "thingsboard/tb-edge:3.8.0");
        service.set("environment", env);

        ObjectNode services = MAPPER.createObjectNode();
        services.set("mytbedge", service);

        ObjectNode compose = MAPPER.createObjectNode();
        compose.set("services", services);
        return compose;
    }

    private JsonNode createGatewayAccessTokenCompose(String accessToken, String securityType) {
        ObjectNode env = MAPPER.createObjectNode();
        env.put("TB_GW_SECURITY_TYPE", securityType);
        env.put("TB_GW_ACCESS_TOKEN", accessToken);

        ObjectNode service = MAPPER.createObjectNode();
        service.put("image", "thingsboard/tb-gateway:3.7");
        service.set("environment", env);

        ObjectNode services = MAPPER.createObjectNode();
        services.set("mygateway", service);

        ObjectNode compose = MAPPER.createObjectNode();
        compose.set("services", services);
        return compose;
    }

    private JsonNode createGatewayMqttBasicCompose() {
        ObjectNode env = MAPPER.createObjectNode();
        env.put("TB_GW_SECURITY_TYPE", "placeholder");
        env.put("TB_GW_CLIENT_ID", "placeholder");
        env.put("TB_GW_USERNAME", "placeholder");
        env.put("TB_GW_PASSWORD", "placeholder");

        ObjectNode service = MAPPER.createObjectNode();
        service.put("image", "thingsboard/tb-gateway:3.7");
        service.set("environment", env);

        ObjectNode services = MAPPER.createObjectNode();
        services.set("mygateway", service);

        ObjectNode compose = MAPPER.createObjectNode();
        compose.set("services", services);
        return compose;
    }

    private JsonNode getServiceEnvironment(AgentApplication app, String serviceName) {
        DockerComposeConfig config = (DockerComposeConfig) app.getConfig();
        return config.getCompose().get("services").get(serviceName).get("environment");
    }
}
