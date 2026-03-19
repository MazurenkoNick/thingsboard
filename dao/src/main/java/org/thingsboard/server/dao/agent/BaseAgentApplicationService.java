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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.agent.AgentApplicationCacheEvictEvent;
import org.thingsboard.server.cache.agent.AgentApplicationCacheKey;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service("AgentApplicationDaoService")
@Slf4j
public class BaseAgentApplicationService extends AbstractCachedEntityService<AgentApplicationCacheKey, AgentApplication, AgentApplicationCacheEvictEvent> implements AgentApplicationService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_AGENT_APPLICATION_ID = "Incorrect agentApplicationId ";
    public static final String INCORRECT_AGENT_ID = "Incorrect agentId ";

    @Autowired
    private AgentApplicationDao agentApplicationDao;

    @Autowired
    @Lazy
    private AgentAppUnitService agentAppUnitService;

    @Autowired
    private AgentAppTemplateService agentAppTemplateService;

    @Autowired
    @Lazy
    private EdgeService edgeService;

    @Autowired
    @Lazy
    private DeviceCredentialsService deviceCredentialsService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private DataValidator<AgentApplication> agentApplicationValidator;

    @Override
    @TransactionalEventListener
    public void handleEvictEvent(AgentApplicationCacheEvictEvent event) {
        cache.evict(Collections.singletonList(new AgentApplicationCacheKey(event.getAgentApplicationId())));
    }

    @Override
    @Transactional
    public AgentApplication save(TenantId tenantId, AgentApplication agentApplication) {
        log.trace("Executing saveAgentApplication [{}]", agentApplication);
        AgentApplication old = agentApplication.getId() != null
                ? agentApplicationDao.findById(tenantId, agentApplication.getUuidId())
                : null;
        resolveTemplateId(agentApplication, old);
        resolveProjectName(agentApplication, old);
        agentApplicationValidator.validate(agentApplication, app -> tenantId);
        AgentApplication saved = agentApplicationDao.save(tenantId, agentApplication);
        saveRelatedEntityRelation(tenantId, saved);
        publishEvictEvent(new AgentApplicationCacheEvictEvent(saved.getId()));
        eventPublisher.publishEvent(SaveEntityEvent.builder()
                .tenantId(tenantId)
                .entityId(saved.getId())
                .entity(saved)
                .oldEntity(old)
                .created(agentApplication.getId() == null)
                .build());
        return saved;
    }

    @Override
    public AgentApplication findById(TenantId tenantId, AgentApplicationId agentApplicationId) {
        log.trace("Executing findById [{}]", agentApplicationId);
        validateId(agentApplicationId, id -> INCORRECT_AGENT_APPLICATION_ID + id);
        return cache.getAndPutInTransaction(AgentApplicationCacheKey.from(agentApplicationId),
                () -> agentApplicationDao.findById(tenantId, agentApplicationId.getId()), true);
    }

    @Override
    public AgentApplicationInfo findInfoById(TenantId tenantId, AgentApplicationId agentApplicationId) {
        log.trace("Executing findAgentApplicationInfoById [{}]", agentApplicationId);
        validateId(agentApplicationId, id -> INCORRECT_AGENT_APPLICATION_ID + id);
        return agentApplicationDao.findInfoById(tenantId, agentApplicationId.getId());
    }

    @Override
    public AgentApplication findByProjectName(TenantId tenantId, String projectName) {
        log.trace("Executing findAgentApplicationByProjectName [{}]", projectName);
        return agentApplicationDao.findByProjectName(tenantId, projectName);
    }

    @Override
    public AgentApplication findByEventId(TenantId tenantId, AgentAppEventId agentAppEventId) {
        log.trace("Executing findAgentApplicationByEventId [{}]", agentAppEventId);
        validateId(agentAppEventId, id -> "Incorrect agentAppEventId " + id);
        return agentApplicationDao.findByEventId(tenantId, agentAppEventId.getId());
    }

    @Override
    public PageData<AgentApplication> findByAgentId(TenantId tenantId, AgentId agentId, PageLink pageLink) {
        log.trace("Executing findAgentApplicationsByAgentId, tenantId [{}], agentId [{}]", tenantId, agentId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(agentId, id -> INCORRECT_AGENT_ID + id);
        validatePageLink(pageLink);
        return agentApplicationDao.findByAgentId(tenantId, agentId.getId(), pageLink);
    }

    @Override
    public PageData<AgentApplicationInfo> findInfosByAgentId(TenantId tenantId, AgentId agentId, PageLink pageLink) {
        log.trace("Executing findAgentApplicationInfosByAgentId, tenantId [{}], agentId [{}]", tenantId, agentId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(agentId, id -> INCORRECT_AGENT_ID + id);
        validatePageLink(pageLink);
        return agentApplicationDao.findInfosByAgentId(tenantId, agentId.getId(), pageLink);
    }

    @Override
    public AgentApplication findByRelatedEntity(TenantId tenantId, EntityId entityId) {
        log.trace("Executing findByRelatedEntity, tenantId [{}], entityId [{}]", tenantId, entityId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(entityId.getId(), id -> "Incorrect entityId " + id);
        return relationService.findByToAndType(tenantId, entityId, EntityRelation.MANAGED_BY_AGENT_APP_TYPE, RelationTypeGroup.COMMON)
                .stream()
                .map(EntityRelation::getFrom)
                .filter(BaseAgentApplicationService::isAgentAppId)
                .map(id -> new AgentApplicationId(id.getId()))
                .findFirst()
                .map(id -> findById(tenantId, id))
                .orElse(null);
    }

    @Override
    @Transactional
    public void delete(TenantId tenantId, AgentApplicationId agentApplicationId) {
        log.trace("Executing deleteAgentApplication [{}]", agentApplicationId);
        validateId(agentApplicationId, id -> INCORRECT_AGENT_APPLICATION_ID + id);
        AgentApplication application = agentApplicationDao.findById(tenantId, agentApplicationId.getId());
        if (application != null) {
            agentAppUnitService.deleteByAgentApplicationId(tenantId, agentApplicationId);
            relationService.deleteEntityCommonRelations(tenantId, agentApplicationId);
            agentApplicationDao.removeById(tenantId, agentApplicationId.getId());
            publishCacheEvictAndDeleteEvent(tenantId, application);
        }
    }

    @Override
    @Transactional
    public void deleteByAgentId(TenantId tenantId, AgentId agentId) {
        log.trace("Executing deleteByAgentId, tenantId [{}], agentId [{}]", tenantId, agentId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(agentId, id -> INCORRECT_AGENT_ID + id);
        agentApplicationDao.findByAgentId(tenantId, agentId.getId())
                .forEach(a -> {
                    agentAppUnitService.deleteByAgentApplicationId(tenantId, a.getId());
                    relationService.deleteEntityCommonRelations(tenantId, a.getId());
                    publishCacheEvictAndDeleteEvent(tenantId, a);
                });
        agentApplicationDao.removeByAgentId(tenantId, agentId.getId());
    }

    private void publishCacheEvictAndDeleteEvent(TenantId tenantId, AgentApplication application) {
        AgentApplicationId agentApplicationId = application.getId();
        publishEvictEvent(new AgentApplicationCacheEvictEvent(agentApplicationId));
        eventPublisher.publishEvent(DeleteEntityEvent.builder()
                .tenantId(tenantId)
                .entityId(agentApplicationId)
                .entity(application)
                .build());
    }

    private void resolveTemplateId(AgentApplication agentApplication, AgentApplication old) {
        if (agentApplication.getAppType() != AgentApplicationType.GENERIC) {
            return;
        }
        if (old != null) {
            agentApplication.setTemplateId(old.getTemplateId());
        } else {
            String defaultVersion = agentApplication.getAppType().getDefaultVersion();
            if (defaultVersion != null) {
                AgentAppTemplate template = agentAppTemplateService.findByAppTypeAndConfigTypeAndVersion(
                        agentApplication.getAppType(), AgentAppConfigType.DOCKER_COMPOSE, defaultVersion);
                if (template != null) {
                    agentApplication.setTemplateId(template.getId());
                }
            }
        }
    }

    private void resolveProjectName(AgentApplication agentApplication, AgentApplication old) {
        if (old != null && old.getProjectName() != null) {
            agentApplication.setProjectName(old.getProjectName());
        } else if (agentApplication.getProjectName() == null) {
            agentApplication.setProjectName(AgentApplication.generateProjectName());
        }
    }

    private void saveRelatedEntityRelation(TenantId tenantId, AgentApplication app) {
        if (app.getAppType() == null) {
            return;
        }
        EntityType relatedType = app.getAppType().getRelatedEntityType();
        if (relatedType == null || app.getConfig() == null) {
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
