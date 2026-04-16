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
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.agent.config.ProfileConfigResolver;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.Collections;

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
    private AgentAppRelationService agentAppRelationService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private DataValidator<AgentApplication> agentApplicationValidator;

    @Autowired
    private ProfileConfigResolver profileConfigResolver;

    @Override
    @TransactionalEventListener
    public void handleEvictEvent(AgentApplicationCacheEvictEvent event) {
        cache.evict(Collections.singletonList(new AgentApplicationCacheKey(event.getAgentApplicationId())));
    }

    @Override
    @Transactional
    public AgentApplication save(TenantId tenantId, AgentApplication application) {
        log.trace("Executing saveAgentApplication [{}]", application);
        AgentApplication old = application.getId() != null
                ? agentApplicationDao.findById(tenantId, application.getUuidId())
                : null;
        if (old == null) {
            AgentAppProfile profile = profileConfigResolver.resolve(tenantId, application);
            if (profile != null) {
                application.setTemplateId(profile.getTemplateId());
            }
        } else {
            if (application.getAppType() != old.getAppType()) {
                application.setAppType(old.getAppType());
            }
            // Template id is set explicitly only at creation; on update we lock it to the persisted value,
            // so profile reassignments and incoming payloads can't silently rewrite it.
            // Upgrades flow through desiredTemplateId, not templateId.
            application.setTemplateId(old.getTemplateId());
            if (isApplicationProfileChangedOrAdded(application, old)) {
                log.trace("[{}] Agent profile is added or changed for application {}", application.getTenantId(), application.getTenantId());
                profileConfigResolver.resolve(tenantId, application);
            }
        }
        resolveOrigin(application, old);
        resolveTemplateId(application, old);
        resolveProjectName(application, old);
        agentAppRelationService.resolveRelatedEntityFromConfig(tenantId, application);
        agentApplicationValidator.validate(application, app -> tenantId);
        AgentApplication saved = agentApplicationDao.save(tenantId, application);
        publishEvictEvent(new AgentApplicationCacheEvictEvent(saved.getId()));
        eventPublisher.publishEvent(SaveEntityEvent.builder()
                .tenantId(tenantId)
                .entityId(saved.getId())
                .entity(saved)
                .oldEntity(old)
                .created(application.getId() == null)
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
    public AgentApplication findByProjectName(TenantId tenantId, AgentId agentId, String projectName) {
        log.trace("Executing findAgentApplicationByProjectName [{}]", projectName);
        return agentApplicationDao.findByProjectName(tenantId, agentId, projectName);
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
        return agentApplicationDao.findByRelatedEntity(tenantId, entityId.getId(), entityId.getEntityType().name());
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
    public void promoteDesiredTemplate(TenantId tenantId, AgentApplicationId agentApplicationId, AgentAppTemplateId templateId) {
        log.trace("Executing promoteDesiredTemplate, tenantId [{}], applicationId [{}], templateId [{}]",
                tenantId, agentApplicationId, templateId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(agentApplicationId, id -> INCORRECT_AGENT_APPLICATION_ID + id);
        if (templateId == null) {
            return;
        }
        AgentApplication old = agentApplicationDao.findById(tenantId, agentApplicationId.getId());
        if (old == null) {
            return;
        }
        int updated = agentApplicationDao.promoteDesiredTemplate(tenantId, agentApplicationId, templateId);
        if (updated == 0) {
            return;
        }
        AgentApplication promoted = new AgentApplication(old);
        promoted.setTemplateId(templateId);
        promoted.setDesiredTemplateId(null);
        publishEvictEvent(new AgentApplicationCacheEvictEvent(agentApplicationId));
        eventPublisher.publishEvent(SaveEntityEvent.builder()
                .tenantId(tenantId)
                .entityId(agentApplicationId)
                .entity(promoted)
                .oldEntity(old)
                .created(false)
                .build());
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

    private void resolveOrigin(AgentApplication agentApplication, AgentApplication old) {
        if (old != null && old.getOrigin() != null) {
            agentApplication.setOrigin(old.getOrigin());
        }
    }

    private void resolveProjectName(AgentApplication agentApplication, AgentApplication old) {
        if (old != null && old.getProjectName() != null) {
            agentApplication.setProjectName(old.getProjectName());
        } else if (agentApplication.getProjectName() == null) {
            agentApplication.setProjectName(AgentApplication.generateProjectName());
        }
    }

    private boolean isApplicationProfileChangedOrAdded(AgentApplication application, AgentApplication old) {
        return application.getApplicationProfileId() != null && !application.getApplicationProfileId().equals(old.getApplicationProfileId());
    }

}
