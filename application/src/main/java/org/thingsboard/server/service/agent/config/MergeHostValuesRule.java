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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.agent.HasAgentAppConfig;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeUtils;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.dao.agent.config.AppConfigMergeRule;
import org.thingsboard.server.dao.device.DeviceConnectivityService;
import org.thingsboard.server.dao.util.DeviceConnectivityUtil;

import java.net.URI;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class MergeHostValuesRule implements AppConfigMergeRule {

    private static final String CLOUD_RPC_HOST = "CLOUD_RPC_HOST";
    private static final String CLOUD_RPC_PORT = "CLOUD_RPC_PORT";
    private static final String TB_GW_HOST = "TB_GW_HOST";
    private static final String TB_GW_PORT = "TB_GW_PORT";
    private static final String GATEWAY_DEFAULT_PORT = "1883";

    private final DeviceConnectivityService deviceConnectivityService;

    @Value("${edges.rpc.port:7070}")
    private int edgeRpcPort;

    @Override
    public boolean supports(HasAgentAppConfig data, AppConfigMergeCtx ctx) {
        if (ctx == null || !ctx.isSetHostValues()) {
            return false;
        }
        AgentApplicationType appType = resolveAppType(data);
        return appType == AgentApplicationType.EDGE || appType == AgentApplicationType.GATEWAY;
    }

    @Override
    public void apply(HasAgentAppConfig data, AppConfigMergeCtx ctx) {
        AgentAppConfig config = data.getConfig();
        if (!(config instanceof DockerComposeConfig composeConfig)) {
            log.trace("Skipping host values merge: config is not DockerComposeConfig");
            return;
        }
        JsonNode compose = composeConfig.getCompose();
        if (compose == null || compose.isNull()) {
            log.trace("Skipping host values merge: compose is null");
            return;
        }
        mergeHostValues(data, ctx, compose);
    }

    private void mergeHostValues(HasAgentAppConfig data, AppConfigMergeCtx ctx, JsonNode compose) {
        String baseUrl = ctx.getBaseUrl();
        if (StringUtils.isBlank(baseUrl)) {
            log.warn("Skipping host values merge: baseUrl is blank (general.baseUrl unset and no request)");
            return;
        }

        AgentApplicationType appType = resolveAppType(data);
        if (appType == AgentApplicationType.EDGE) {
            applyEdgeHost(compose, baseUrl);
        } else if (appType == AgentApplicationType.GATEWAY) {
            applyGatewayHost(compose, baseUrl);
        }
    }

    private static AgentApplicationType resolveAppType(HasAgentAppConfig data) {
        if (data instanceof AgentApplication app) {
            return app.getAppType();
        } else if (data instanceof AgentAppProfile profile) {
            return profile.getAppType();
        } else if (data instanceof AgentAppTemplate template) {
            return template.getAppType();
        }
        return null;
    }

    private void applyEdgeHost(JsonNode compose, String baseUrl) {
        String host = extractHost(baseUrl);
        String resolvedHost = DeviceConnectivityUtil.isLocalhost(host) ? DeviceConnectivityUtil.HOST_DOCKER_INTERNAL : host;
        Map<String, String> envVars = Map.of(
                CLOUD_RPC_HOST, resolvedHost,
                CLOUD_RPC_PORT, String.valueOf(edgeRpcPort)
        );
        DockerComposeUtils.setEnvVariables(compose, AgentApplicationType.EDGE.getMainImagePattern(), envVars);
    }

    private void applyGatewayHost(JsonNode compose, String baseUrl) {
        String host = deviceConnectivityService.resolveGatewayHost(baseUrl);
        if (StringUtils.isBlank(host)) {
            log.warn("Skipping gateway host merge: resolveGatewayHost returned blank for baseUrl [{}]", baseUrl);
            return;
        }
        Map<String, String> envVars = Map.of(
                TB_GW_HOST, host,
                TB_GW_PORT, GATEWAY_DEFAULT_PORT
        );
        DockerComposeUtils.setEnvVariables(compose, AgentApplicationType.GATEWAY.getMainImagePattern(), envVars);
    }

    private static String extractHost(String baseUrl) {
        try {
            String host = URI.create(baseUrl).getHost();
            return StringUtils.isNotBlank(host) ? host : baseUrl;
        } catch (IllegalArgumentException e) {
            log.warn("Failed to parse baseUrl [{}] as URI, falling back to raw value", baseUrl, e);
            return baseUrl;
        }
    }
}
