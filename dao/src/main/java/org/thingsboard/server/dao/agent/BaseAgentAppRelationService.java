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
package org.thingsboard.server.dao.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.exception.DataValidationException;

@Service
@RequiredArgsConstructor
public class BaseAgentAppRelationService implements AgentAppRelationService {

    private final EdgeService edgeService;
    private final DeviceCredentialsService deviceCredentialsService;
    private final AgentApplicationDao agentApplicationDao;

    @Override
    public void resolveRelatedEntityFromConfig(TenantId tenantId, AgentApplication app) {
        if (app.getAppType() == null || app.getConfig() == null) {
            return;
        }
        EntityType relatedType = app.getAppType().getRelatedEntityType();
        if (relatedType == null) {
            return;
        }

        EntityId newRelatedEntityId = switch (relatedType) {
            case EDGE -> resolveEdgeId(tenantId, app);
            case DEVICE -> resolveDeviceId(app);
            default -> null;
        };

        if (newRelatedEntityId != null) {
            validateRelatedEntityNotManaged(tenantId, app, newRelatedEntityId);
        }
        app.setRelatedEntityId(newRelatedEntityId);
    }

    private void validateRelatedEntityNotManaged(TenantId tenantId, AgentApplication app, EntityId relatedEntityId) {
        AgentApplication existing = agentApplicationDao.findByRelatedEntity(
                tenantId, relatedEntityId.getId(), relatedEntityId.getEntityType().name());
        if (existing != null && (app.getId() == null || !existing.getUuidId().equals(app.getUuidId()))) {
            throw new DataValidationException("Entity is already managed by another agent application");
        }
    }

    private EntityId resolveEdgeId(TenantId tenantId, AgentApplication app) {
        String routingKey = app.getConfig().getEdgeRoutingKey();
        if (routingKey == null) {
            return null;
        }
        return edgeService.findEdgeByRoutingKey(tenantId, routingKey)
                .map(Edge::getId)
                .orElse(null);
    }

    private EntityId resolveDeviceId(AgentApplication app) {
        if (!(app.getConfig() instanceof DockerComposeConfig composeConfig)) {
            return null;
        }
        String credentialsId = resolveGatewayCredentialsId(composeConfig);
        if (credentialsId == null) {
            return null;
        }
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByCredentialsId(credentialsId);
        return credentials != null ? credentials.getDeviceId() : null;
    }

    private String resolveGatewayCredentialsId(DockerComposeConfig config) {
        var imagePattern = AgentApplicationType.GATEWAY.getMainImagePattern();
        String securityType = DockerComposeUtils.getEnvVariable(config.getCompose(), imagePattern, "TB_GW_SECURITY_TYPE");
        if ("accessToken".equals(securityType)) {
            return DockerComposeUtils.getEnvVariable(config.getCompose(), imagePattern, "TB_GW_ACCESS_TOKEN");
        } else if ("usernamePassword".equals(securityType)) {
            String clientId = DockerComposeUtils.getEnvVariable(config.getCompose(), imagePattern, "TB_GW_CLIENT_ID");
            String userName = DockerComposeUtils.getEnvVariable(config.getCompose(), imagePattern, "TB_GW_USERNAME");
            if (StringUtils.isEmpty(clientId) && StringUtils.isNotEmpty(userName)) {
                return userName;
            } else if (StringUtils.isNotEmpty(clientId) && StringUtils.isEmpty(userName)) {
                return EncryptionUtil.getSha3Hash(clientId);
            } else if (StringUtils.isNotEmpty(clientId) && StringUtils.isNotEmpty(userName)) {
                return EncryptionUtil.getSha3Hash("|", clientId, userName);
            }
        }
        return null;
    }

}
