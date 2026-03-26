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

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.agent.AgentGroupCacheEvictEvent;
import org.thingsboard.server.cache.agent.AgentGroupCacheKey;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.AgentGroup;
import org.thingsboard.server.common.data.agent.AgentGroupInfo;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentGroupId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service("AgentGroupDaoService")
@Slf4j
public class BaseAgentGroupService extends AbstractCachedEntityService<AgentGroupCacheKey, AgentGroup, AgentGroupCacheEvictEvent> implements AgentGroupService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_GROUP_ID = "Incorrect groupId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";

    public static final String HAS_PROFILE_RELATION_TYPE = "HasProfile";

    @Autowired
    private AgentGroupDao groupDao;

    @Autowired
    private RelationService relationService;

    @Autowired
    private DataValidator<AgentGroup> groupValidator;

    @Override
    @TransactionalEventListener
    public void handleEvictEvent(AgentGroupCacheEvictEvent event) {
        List<AgentGroupCacheKey> keys = new ArrayList<>(2);
        keys.add(new AgentGroupCacheKey(event.getTenantId(), event.getNewName()));
        if (StringUtils.isNotEmpty(event.getOldName()) && !event.getOldName().equals(event.getNewName())) {
            keys.add(new AgentGroupCacheKey(event.getTenantId(), event.getOldName()));
        }
        cache.evict(keys);
    }

    @Override
    @Transactional
    public AgentGroup saveGroup(AgentGroup group) {
        return saveEntity(group, () -> doSaveGroup(group));
    }

    private AgentGroup doSaveGroup(AgentGroup group) {
        log.trace("Executing saveGroup [{}]", group);
        AgentGroup oldGroup = groupValidator.validate(group, AgentGroup::getTenantId);
        String oldName = oldGroup != null ? oldGroup.getName() : null;
        AgentGroupCacheEvictEvent evictEvent = new AgentGroupCacheEvictEvent(group.getTenantId(), group.getName(), oldName);
        try {
            AgentGroup saved = groupDao.save(group.getTenantId(), group);
            publishEvictEvent(evictEvent);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(saved.getTenantId())
                    .entityId(saved.getId()).entity(saved).oldEntity(oldGroup).created(group.getId() == null).build());
            return saved;
        } catch (Exception t) {
            handleEvictEvent(evictEvent);
            checkConstraintViolation(t, "agent_group_name_unq_key", "Agent group with such name already exists!");
            checkConstraintViolation(t, "agent_group_provision_key_unq_key", "Agent group with such provision key already exists!");
            throw t;
        }
    }

    @Override
    public AgentGroup findGroupById(TenantId tenantId, AgentGroupId groupId) {
        log.trace("Executing findGroupById [{}]", groupId);
        validateId(groupId, id -> INCORRECT_GROUP_ID + id);
        return groupDao.findById(tenantId, groupId.getId());
    }

    @Override
    public AgentGroupInfo findGroupInfoById(TenantId tenantId, AgentGroupId groupId) {
        log.trace("Executing findGroupInfoById [{}]", groupId);
        validateId(groupId, id -> INCORRECT_GROUP_ID + id);
        return groupDao.findAgentGroupInfoById(groupId.getId());
    }

    @Override
    public PageData<AgentGroup> findGroupsByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findGroupsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return groupDao.findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<AgentGroupInfo> findGroupInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findGroupInfosByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return groupDao.findAgentGroupInfosByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<AgentGroup> findGroupsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findGroupsByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validatePageLink(pageLink);
        return groupDao.findByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    @Transactional
    public void deleteGroup(TenantId tenantId, AgentGroupId groupId) {
        log.trace("Executing deleteGroup [{}]", groupId);
        validateId(groupId, id -> INCORRECT_GROUP_ID + id);

        AgentGroup group = groupDao.findById(tenantId, groupId.getId());
        if (group == null) {
            return;
        }
        groupDao.removeById(tenantId, groupId.getId());
        publishEvictEvent(new AgentGroupCacheEvictEvent(group.getTenantId(), group.getName(), null));
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(groupId).entity(group).build());
    }

    @Override
    public AgentGroup assignGroupToCustomer(TenantId tenantId, AgentGroupId groupId, CustomerId customerId) {
        AgentGroup group = findGroupById(tenantId, groupId);
        if (customerId.equals(group.getCustomerId())) {
            return group;
        }
        group.setCustomerId(customerId);
        return saveGroup(group);
    }

    @Override
    public AgentGroup unassignGroupFromCustomer(TenantId tenantId, AgentGroupId groupId) {
        AgentGroup group = findGroupById(tenantId, groupId);
        if (group.getCustomerId() == null) {
            return group;
        }
        group.setCustomerId(null);
        return saveGroup(group);
    }

    @Override
    public void assignProfileToGroup(TenantId tenantId, AgentGroupId groupId, AgentAppProfileId profileId) {
        if (relationService.checkRelation(tenantId, groupId, profileId, HAS_PROFILE_RELATION_TYPE, RelationTypeGroup.COMMON)) {
            throw new DataValidationException("Profile is already assigned to this group!");
        }
        relationService.saveRelation(tenantId, new EntityRelation(groupId, profileId, HAS_PROFILE_RELATION_TYPE, RelationTypeGroup.COMMON));
    }

    @Override
    public void unassignProfileFromGroup(TenantId tenantId, AgentGroupId groupId, AgentAppProfileId profileId) {
        relationService.deleteRelation(tenantId, groupId, profileId, HAS_PROFILE_RELATION_TYPE, RelationTypeGroup.COMMON);
    }

    @Override
    public List<EntityRelation> findProfileRelations(TenantId tenantId, AgentGroupId groupId) {
        return relationService.findByFromAndType(tenantId, groupId, HAS_PROFILE_RELATION_TYPE, RelationTypeGroup.COMMON);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findGroupById(tenantId, new AgentGroupId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        ListenableFuture<AgentGroup> future = groupDao.findByIdAsync(tenantId, entityId.getId());
        return FluentFuture.from(future)
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return groupDao.countByTenantId(tenantId);
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteGroup(tenantId, new AgentGroupId(id.getId()));
    }

    @Override
    @Transactional
    public void deleteByTenantId(TenantId tenantId) {
        log.trace("Executing deleteByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        tenantGroupsRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_GROUP;
    }

    private final PaginatedRemover<TenantId, AgentGroup> tenantGroupsRemover = new PaginatedRemover<>() {
        @Override
        protected PageData<AgentGroup> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return groupDao.findByTenantId(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, AgentGroup entity) {
            deleteGroup(tenantId, entity.getId());
        }
    };
}
