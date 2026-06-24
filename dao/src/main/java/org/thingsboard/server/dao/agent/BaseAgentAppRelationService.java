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

@Service
@RequiredArgsConstructor
public class BaseAgentAppRelationService implements AgentAppRelationService {

    private final EdgeService edgeService;
    private final DeviceCredentialsService deviceCredentialsService;
    private final AgentApplicationDao agentApplicationDao;
    private final RelationService relationService;

    @Override
    public EntityId findRelatedEntityByConfig(TenantId tenantId, AgentApplication app) {
        if (app.getAppType() == null || app.getConfig() == null) {
            return null;
        }
        EntityType relatedType = app.getAppType().getRelatedEntityType();
        if (relatedType == null) {
            return null;
        }
        return switch (relatedType) {
            case EDGE -> resolveEdgeId(tenantId, app);
            case DEVICE -> resolveDeviceId(app);
            default -> null;
        };
    }

    @Override
    public EntityId findRelatedEntity(TenantId tenantId, AgentApplication app) {
        return relationService.findByTo(tenantId, app.getId(), RelationTypeGroup.AGENT)
                .stream().map(EntityRelation::getFrom).findFirst().orElse(null);
    }

    @Override
    public void createOrUpdateRelation(TenantId tenantId, AgentApplicationId appId, EntityId relatedEntityId) {
        deleteRelation(tenantId, appId);
        relationService.saveRelation(tenantId, new EntityRelation(relatedEntityId, appId, EntityRelation.MANAGED_BY_AGENT_APP_TYPE, RelationTypeGroup.AGENT));
    }

    @Override
    public void deleteRelation(TenantId tenantId, AgentApplicationId appId) {
        relationService.findByTo(tenantId, appId, RelationTypeGroup.AGENT)
                .forEach(relation -> relationService.deleteRelation(tenantId, relation));
    }

    @Override
    public void validateRelatedEntityNotManaged(TenantId tenantId, AgentApplication app, EntityId relatedEntityId) {
        AgentApplication existing = agentApplicationDao.findByRelatedEntity(tenantId, relatedEntityId.getId());
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
