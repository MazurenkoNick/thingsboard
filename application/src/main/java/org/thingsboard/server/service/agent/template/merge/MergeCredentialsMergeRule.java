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
package org.thingsboard.server.service.agent.template.merge;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeUtils;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.template.TemplateMergeCtx;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.edge.EdgeService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Merge rule that injects entity credentials into the compose configuration.
 * Must run after {@link MergeComposeStepRule} (which has {@code @Order(Ordered.HIGHEST_PRECEDENCE)})
 * because it operates on the already-populated compose config.
 * <p>
 * For EDGE applications: injects CLOUD_ROUTING_KEY, CLOUD_ROUTING_SECRET, and CLOUD_RPC_PORT from the selected Edge entity.
 * For GATEWAY applications: injects credentials based on type — ACCESS_TOKEN for token auth,
 * or TB_GW_CLIENT_ID/TB_GW_USERNAME/TB_GW_PASSWORD for MQTT_BASIC auth.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MergeCredentialsMergeRule implements AppTemplateMergeRule {

    private static final String CLOUD_ROUTING_KEY = "CLOUD_ROUTING_KEY";
    private static final String CLOUD_ROUTING_SECRET = "CLOUD_ROUTING_SECRET";
    private static final String CLOUD_RPC_PORT = "CLOUD_RPC_PORT";

    private static final String TB_GW_SECURITY_TYPE = "TB_GW_SECURITY_TYPE";
    private static final String TB_GW_ACCESS_TOKEN = "TB_GW_ACCESS_TOKEN";
    private static final String TB_GW_CLIENT_ID = "TB_GW_CLIENT_ID";
    private static final String TB_GW_USERNAME = "TB_GW_USERNAME";
    private static final String TB_GW_PASSWORD = "TB_GW_PASSWORD";

    private final EdgeService edgeService;
    private final DeviceCredentialsService deviceCredentialsService;

    @Value("${edges.rpc.port:7070}")
    private int edgeRpcPort;

    @Override
    public boolean supports(AgentApplication agentApp, AgentAppTemplate template, TemplateMergeCtx ctx) {
        return ctx != null
                && ctx.getRelatedEntityId() != null
                && agentApp.getAppType() != null
                && (agentApp.getAppType() == AgentApplicationType.EDGE || agentApp.getAppType() == AgentApplicationType.GATEWAY);
    }

    @Override
    public void apply(AgentApplication agentApp, AgentAppTemplate template, TemplateMergeCtx ctx) {
        AgentAppConfig config = agentApp.getConfig();
        if (!(config instanceof DockerComposeConfig composeConfig)) {
            log.trace("Skipping credentials merge: config is not DockerComposeConfig");
            return;
        }

        JsonNode compose = composeConfig.getCompose();
        if (compose == null || compose.isNull()) {
            log.trace("Skipping credentials merge: compose is null");
            return;
        }

        AgentApplicationType appType = agentApp.getAppType();
        TenantId tenantId = agentApp.getTenantId();

        if (appType == AgentApplicationType.EDGE) {
            applyEdgeCredentials(compose, tenantId, ctx);
        } else if (appType == AgentApplicationType.GATEWAY) {
            applyGatewayCredentials(compose, tenantId, ctx);
        }
    }

    private void applyEdgeCredentials(JsonNode compose, TenantId tenantId, TemplateMergeCtx ctx) {
        EdgeId edgeId = new EdgeId(ctx.getRelatedEntityId());
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            log.warn("Edge not found for id [{}], skipping credentials merge", edgeId);
            return;
        }
        var envVars = Map.of(
                CLOUD_ROUTING_KEY, edge.getRoutingKey(),
                CLOUD_ROUTING_SECRET, edge.getSecret(),
                CLOUD_RPC_PORT, String.valueOf(edgeRpcPort)
        );
        DockerComposeUtils.setEnvVariables(compose, AgentApplicationType.EDGE.getMainImagePattern(), envVars);
    }

    private void applyGatewayCredentials(JsonNode compose, TenantId tenantId, TemplateMergeCtx ctx) {
        DeviceId deviceId = new DeviceId(ctx.getRelatedEntityId());
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceId);
        if (credentials == null) {
            log.warn("Device credentials not found for device [{}], skipping credentials merge", deviceId);
            return;
        }
        Map<String, String> envVars = new LinkedHashMap<>();
        switch (credentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                envVars.put(TB_GW_SECURITY_TYPE, "accessToken");
                envVars.put(TB_GW_ACCESS_TOKEN, credentials.getCredentialsId());
                break;
            case MQTT_BASIC:
                envVars.put(TB_GW_SECURITY_TYPE, "usernamePassword");
                BasicMqttCredentials mqttCredentials = JacksonUtil.fromString(
                        credentials.getCredentialsValue(), BasicMqttCredentials.class);
                if (mqttCredentials != null) {
                    if (StringUtils.isNotEmpty(mqttCredentials.getClientId())) {
                        envVars.put(TB_GW_CLIENT_ID, mqttCredentials.getClientId());
                    }
                    if (StringUtils.isNotEmpty(mqttCredentials.getUserName())) {
                        envVars.put(TB_GW_USERNAME, mqttCredentials.getUserName());
                    }
                    if (StringUtils.isNotEmpty(mqttCredentials.getPassword())) {
                        envVars.put(TB_GW_PASSWORD, mqttCredentials.getPassword());
                    }
                }
                break;
            default:
                log.warn("Unsupported credentials type [{}] for gateway auto-fill", credentials.getCredentialsType());
                return;
        }
        DockerComposeUtils.setEnvVariables(compose, AgentApplicationType.GATEWAY.getMainImagePattern(), envVars);
    }
}
