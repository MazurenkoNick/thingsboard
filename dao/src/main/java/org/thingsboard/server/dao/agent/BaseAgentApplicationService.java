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

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.agent.AgentApplicationCacheEvictEvent;
import org.thingsboard.server.cache.agent.AgentApplicationCacheKey;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.agent.config.MergeCredentialsToConfigRule;
import org.thingsboard.server.dao.agent.config.ProfileConfigResolver;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.common.data.agent.AgentApplicationType.GATEWAY;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
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
    private AgentAppRelationService agentAppRelationService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private DataValidator<AgentApplication> agentApplicationValidator;

    @Autowired
    private ProfileConfigResolver profileConfigResolver;

    @Autowired
    private MergeCredentialsToConfigRule mergeCredentialsToConfigRule;

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
        setConfigFromProfile(tenantId, application, old);
        applyRelatedEntityCredsIfConfigChanged(tenantId, application, old);
        agentApplicationValidator.validate(application, app -> tenantId);
        boolean templateChanged = old == null || !application.getTemplateId().equals(old.getTemplateId());
        if (templateChanged) {
            application.setDesiredTemplateId(null);
        }
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
    @Transactional
    public AgentApplication saveWithRelatedEntity(TenantId tenantId, AgentApplication application, EntityId relatedEntityId) {
        AgentApplication saved = save(tenantId, application);
        if (relatedEntityId != null) {
            saved = assignRelatedEntity(tenantId, saved.getId(), relatedEntityId);
        }
        return saved;
    }

    @Override
    @Transactional
    public AgentApplication assignRelatedEntity(TenantId tenantId, AgentApplicationId agentApplicationId, EntityId relatedEntityId) {
        log.trace("Executing assignRelatedEntity, tenantId [{}], appId [{}], relatedEntityId [{}]", tenantId, agentApplicationId, relatedEntityId);
        validateId(agentApplicationId, id -> INCORRECT_AGENT_APPLICATION_ID + id);
        AgentApplication application = agentApplicationDao.findById(tenantId, agentApplicationId.getId());
        if (application == null) {
            throw new DataValidationException("Agent application not found: " + agentApplicationId);
        }
        AgentApplicationType appType = application.getAppType();
        if (appType == null || appType.getRelatedEntityType() == null) {
            throw new DataValidationException("Agent application of type '" + appType + "' cannot have a related entity");
        }
        if (!relatedEntityId.getEntityType().equals(appType.getRelatedEntityType())) {
            throw new DataValidationException("Entity type " + relatedEntityId.getEntityType()
                    + " can't be assigned to application of type " + appType);
        }
        agentAppRelationService.validateRelatedEntityNotManaged(tenantId, application, relatedEntityId);
        agentAppRelationService.createOrUpdateRelation(tenantId, agentApplicationId, relatedEntityId);
        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().relatedEntityId(relatedEntityId).build();
        if (mergeCredentialsToConfigRule.supports(application, ctx)) {
            mergeCredentialsToConfigRule.apply(application, ctx);
            application = agentApplicationDao.save(tenantId, application);
        }
        publishEvictEvent(new AgentApplicationCacheEvictEvent(agentApplicationId));
        eventPublisher.publishEvent(SaveEntityEvent.builder()
                .tenantId(tenantId)
                .entityId(agentApplicationId)
                .entity(application)
                .created(false)
                .build());
        return application;
    }

    @Override
    @Transactional
    public AgentApplication unassignRelatedEntity(TenantId tenantId, AgentApplicationId agentApplicationId) {
        log.trace("Executing unassignRelatedEntity, tenantId [{}], appId [{}]", tenantId, agentApplicationId);
        validateId(agentApplicationId, id -> INCORRECT_AGENT_APPLICATION_ID + id);
        AgentApplication application = agentApplicationDao.findById(tenantId, agentApplicationId.getId());
        if (application == null) {
            throw new DataValidationException("Agent application not found: " + agentApplicationId);
        }
        agentAppRelationService.deleteRelation(tenantId, agentApplicationId);
        publishEvictEvent(new AgentApplicationCacheEvictEvent(agentApplicationId));
        eventPublisher.publishEvent(SaveEntityEvent.builder()
                .tenantId(tenantId)
                .entityId(agentApplicationId)
                .entity(application)
                .created(false)
                .build());
        return application;
    }

    @Override
    public AgentApplication findById(TenantId tenantId, AgentApplicationId agentApplicationId) {
        log.trace("Executing findById [{}]", agentApplicationId);
        validateId(agentApplicationId, id -> INCORRECT_AGENT_APPLICATION_ID + id);
        return cache.getAndPutInTransaction(AgentApplicationCacheKey.from(agentApplicationId),
                () -> agentApplicationDao.findById(tenantId, agentApplicationId.getId()), true);
    }

    @Override
    public AgentApplication findByIdForUpdate(TenantId tenantId, AgentApplicationId agentApplicationId) {
        log.trace("Executing findByIdForUpdate [{}]", agentApplicationId);
        validateId(agentApplicationId, id -> INCORRECT_AGENT_APPLICATION_ID + id);
        return agentApplicationDao.findByIdForUpdate(tenantId, agentApplicationId.getId());
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
    public PageData<AgentApplication> findByEntityGroupId(EntityGroupId groupId, PageLink pageLink) {
        log.trace("Executing findAgentApplicationsByEntityGroupId, groupId [{}], pageLink [{}]", groupId, pageLink);
        validateId(groupId, id -> "Incorrect entityGroupId " + id);
        validatePageLink(pageLink);
        return agentApplicationDao.findByEntityGroupId(groupId.getId(), pageLink);
    }

    @Override
    public PageData<AgentApplication> findByEntityGroupIds(List<EntityGroupId> groupIds, PageLink pageLink) {
        log.trace("Executing findAgentApplicationsByEntityGroupIds, groupIds [{}], pageLink [{}]", groupIds, pageLink);
        validateIds(groupIds, ids -> "Incorrect groupIds " + ids);
        validatePageLink(pageLink);
        return agentApplicationDao.findByEntityGroupIds(DaoUtil.toUUIDs(groupIds), pageLink);
    }

    @Override
    public AgentApplication findByRelatedEntity(TenantId tenantId, EntityId entityId) {
        log.trace("Executing findByRelatedEntity, tenantId [{}], entityId [{}]", tenantId, entityId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(entityId.getId(), id -> "Incorrect entityId " + id);
        return agentApplicationDao.findByRelatedEntity(tenantId, entityId.getId());
    }

    @Override
    public List<EntityId> findManagedRelatedEntityIds(TenantId tenantId, EntityType relatedEntityType) {
        log.trace("Executing findManagedRelatedEntityIds, tenantId [{}], type [{}]", tenantId, relatedEntityType);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return agentApplicationDao.findManagedRelatedEntityIds(tenantId, relatedEntityType.name()).stream()
                .map(id -> EntityIdFactory.getByTypeAndUuid(relatedEntityType, id))
                .toList();
    }

    @Override
    @Transactional
    public void delete(TenantId tenantId, AgentApplicationId agentApplicationId) {
        log.trace("Executing deleteAgentApplication [{}]", agentApplicationId);
        validateId(agentApplicationId, id -> INCORRECT_AGENT_APPLICATION_ID + id);
        AgentApplication application = agentApplicationDao.findById(tenantId, agentApplicationId.getId());
        if (application != null) {
            cleanupApplicationOnDelete(tenantId, application);
            agentApplicationDao.removeById(tenantId, agentApplicationId.getId());
        }
    }

    private void cleanupApplicationOnDelete(TenantId tenantId, AgentApplication application) {
        AgentApplicationId agentApplicationId = application.getId();
        agentAppUnitService.deleteByAgentApplicationId(tenantId, agentApplicationId);
        relationService.deleteEntityCommonRelations(tenantId, agentApplicationId);
        agentAppRelationService.deleteRelation(tenantId, agentApplicationId);
        publishCacheEvictAndDeleteEvent(tenantId, application);
    }

    @Override
    @Transactional
    public void promoteDesiredTemplate(TenantId tenantId, AgentApplicationId agentApplicationId) {
        log.trace("Executing promoteDesiredTemplate, tenantId [{}], applicationId [{}]", tenantId, agentApplicationId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(agentApplicationId, id -> INCORRECT_AGENT_APPLICATION_ID + id);
        AgentApplication old = agentApplicationDao.findById(tenantId, agentApplicationId.getId());
        if (old == null || old.getDesiredTemplateId() == null) {
            return;
        }
        AgentAppTemplateId promotedTemplateId = old.getDesiredTemplateId();
        int updated = agentApplicationDao.promoteDesiredTemplate(tenantId, agentApplicationId, promotedTemplateId);
        if (updated == 0) {
            return;
        }
        AgentApplication promoted = new AgentApplication(old);
        promoted.setTemplateId(promotedTemplateId);
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
                .forEach(a -> cleanupApplicationOnDelete(tenantId, a));
        agentApplicationDao.removeByAgentId(tenantId, agentId.getId());
    }

    private void setConfigFromProfile(TenantId tenantId, AgentApplication application, AgentApplication old) {
        if (isApplicationProfileChangedOrAdded(application, old)) {
            AgentAppProfile profile = profileConfigResolver.resolve(tenantId, application);
            if (old == null && profile != null) {
                application.setTemplateId(profile.getTemplateId());
            }
        }
    }

    private void applyRelatedEntityCredsIfConfigChanged(TenantId tenantId, AgentApplication application, AgentApplication old) {
        if (application.getId() == null || application.getAppType() == null || !application.getAppType().hasRelatedEntityType()) {
            return;
        }
        // always sync GW creds as they are dynamic and can be changed by the user
        if (application.getAppType() != GATEWAY && (old != null && Objects.equals(application.getConfig(), old.getConfig()))) {
            return;
        }
        EntityId relatedEntityId = findRelatedEntityId(tenantId, application.getId());
        if (relatedEntityId == null) {
            return;
        }
        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().relatedEntityId(relatedEntityId).build();
        if (mergeCredentialsToConfigRule.supports(application, ctx)) {
            mergeCredentialsToConfigRule.apply(application, ctx);
        }
    }

    private EntityId findRelatedEntityId(TenantId tenantId, AgentApplicationId appId) {
        List<EntityRelation> relations = relationService.findByTo(tenantId, appId, RelationTypeGroup.AGENT);
        return relations.isEmpty() ? null : relations.get(0).getFrom();
    }

    private boolean isApplicationProfileChangedOrAdded(AgentApplication current, AgentApplication old) {
        return current.getApplicationProfileId() != null &&
                (old == null || !current.getApplicationProfileId().equals(old.getApplicationProfileId()));
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

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findById(tenantId, new AgentApplicationId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        ListenableFuture<AgentApplication> future = agentApplicationDao.findByIdAsync(tenantId, entityId.getId());
        return FluentFuture.from(future).transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_APPLICATION;
    }

}
