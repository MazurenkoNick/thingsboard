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
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BaseAgentAppRelationService implements AgentAppRelationService {

    private final EdgeService edgeService;
    private final RelationService relationService;
    private final DeviceCredentialsService deviceCredentialsService;

    @Override
    public void relateToParentEntityByConfig(TenantId tenantId, AgentApplication app) {
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

        List<EntityRelation> existingRelated = relationService.findByFromAndType(
                tenantId, app.getId(), EntityRelation.MANAGED_BY_AGENT_APP_TYPE, RelationTypeGroup.COMMON);

        if (sameRelatedEntityId(existingRelated, newRelatedEntityId)) return;

        for (EntityRelation rel : existingRelated) {
            relationService.deleteRelation(tenantId, rel);
        }
        if (newRelatedEntityId != null) {
            validateRelatedEntityNotManaged(tenantId, app, newRelatedEntityId);
            relationService.saveRelation(tenantId, new EntityRelation(
                    app.getId(), newRelatedEntityId, EntityRelation.MANAGED_BY_AGENT_APP_TYPE, RelationTypeGroup.COMMON));
        }
    }

    @Override
    public UUID findRelatedEntityId(TenantId tenantId, AgentApplicationId applicationId) {
        return relationService.findByFromAndType(
                        tenantId, applicationId, EntityRelation.MANAGED_BY_AGENT_APP_TYPE, RelationTypeGroup.COMMON)
                .stream()
                .findFirst()
                .map(rel -> rel.getTo().getId())
                .orElse(null);
    }

    private boolean sameRelatedEntityId(List<EntityRelation> existing, EntityId newRelatedEntityId) {
        UUID currentId = existing.stream()
                .findFirst()
                .map(r -> r.getTo().getId())
                .orElse(null);

        UUID newId = newRelatedEntityId != null ? newRelatedEntityId.getId() : null;

        return Objects.equals(currentId, newId);
    }

    private void validateRelatedEntityNotManaged(TenantId tenantId, AgentApplication app, EntityId relatedEntityId) {
        boolean alreadyManaged = relationService.findByToAndType(
                        tenantId, relatedEntityId, EntityRelation.MANAGED_BY_AGENT_APP_TYPE, RelationTypeGroup.COMMON)
                .stream()
                .anyMatch(r -> isAgentAppId(r.getFrom()) && !r.getFrom().getId().equals(app.getId().getId()));
        if (alreadyManaged) {
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

    private static boolean isAgentAppId(EntityId appCandidateId) {
        return appCandidateId.getEntityType() == EntityType.AGENT_APPLICATION;
    }

}
