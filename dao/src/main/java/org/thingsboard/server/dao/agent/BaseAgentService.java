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
import org.thingsboard.server.cache.agent.AgentCacheEvictEvent;
import org.thingsboard.server.cache.agent.AgentCacheKey;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentInfo;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service("AgentDaoService")
@Slf4j
public class BaseAgentService extends AbstractCachedEntityService<AgentCacheKey, Agent, AgentCacheEvictEvent> implements AgentService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_AGENT_ID = "Incorrect agentId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";

    @Autowired
    private AgentDao agentDao;

    @Autowired
    private DataValidator<Agent> agentValidator;

    @Override
    @TransactionalEventListener
    public void handleEvictEvent(AgentCacheEvictEvent event) {
        List<AgentCacheKey> keys = new ArrayList<>(2);
        keys.add(new AgentCacheKey(event.getTenantId(), event.getNewName()));
        if (StringUtils.isNotEmpty(event.getOldName()) && !event.getOldName().equals(event.getNewName())) {
            keys.add(new AgentCacheKey(event.getTenantId(), event.getOldName()));
        }
        cache.evict(keys);
    }

    @Override
    @Transactional
    public Agent saveAgent(Agent agent) {
        return saveEntity(agent, () -> doSaveAgent(agent));
    }

    private Agent doSaveAgent(Agent agent) {
        log.trace("Executing saveAgent [{}]", agent);
        Agent oldAgent = agentValidator.validate(agent, Agent::getTenantId);
        String oldName = oldAgent != null ? oldAgent.getName() : null;
        AgentCacheEvictEvent evictEvent = new AgentCacheEvictEvent(agent.getTenantId(), agent.getName(), oldName);
        try {
            Agent savedAgent = agentDao.save(agent.getTenantId(), agent);
            publishEvictEvent(evictEvent);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(savedAgent.getTenantId())
                    .entityId(savedAgent.getId()).entity(savedAgent).oldEntity(oldAgent).created(agent.getId() == null).build());
            return savedAgent;
        } catch (Exception t) {
            handleEvictEvent(evictEvent);
            checkConstraintViolation(t, "agent_name_unq_key", "Agent with such name already exists!");
            throw t;
        }
    }

    @Override
    public Agent findAgentById(TenantId tenantId, AgentId agentId) {
        log.trace("Executing findAgentById [{}]", agentId);
        validateId(agentId, id -> INCORRECT_AGENT_ID + id);
        return agentDao.findById(tenantId, agentId.getId());
    }

    @Override
    public AgentInfo findAgentInfoById(TenantId tenantId, AgentId agentId) {
        log.trace("Executing findAgentInfoById [{}]", agentId);
        validateId(agentId, id -> INCORRECT_AGENT_ID + id);
        return agentDao.findAgentInfoById(tenantId, agentId.getId());
    }

    @Override
    public PageData<Agent> findAgentsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findAgentsByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validatePageLink(pageLink);
        return agentDao.findAgentsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<AgentInfo> findAgentInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findAgentInfosByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validatePageLink(pageLink);
        return agentDao.findAgentInfosByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<Agent> findAgentsByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAgentsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return agentDao.findAgentsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<AgentInfo> findAgentInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAgentInfosByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return agentDao.findAgentInfosByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findAgentById(tenantId, new AgentId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        ListenableFuture<Agent> future = agentDao.findByIdAsync(tenantId, entityId.getId());
        return FluentFuture.from(future)
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return agentDao.countByTenantId(tenantId);
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteAgent(tenantId, new AgentId(id.getId()));
    }

    @Override
    @Transactional
    public void deleteByTenantId(TenantId tenantId) {
        log.trace("Executing deleteByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        tenantAgentsRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    @Transactional
    public void deleteAgent(TenantId tenantId, AgentId agentId) {
        log.trace("Executing deleteAgent [{}]", agentId);
        validateId(agentId, id -> INCORRECT_AGENT_ID + id);

        Agent agent = agentDao.findById(tenantId, agentId.getId());
        if (agent == null) {
            return;
        }
        agentDao.removeById(tenantId, agentId.getId());

        publishEvictEvent(new AgentCacheEvictEvent(agent.getTenantId(), agent.getName(), null));
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(agentId).entity(agent).build());
    }

    @Override
    @Transactional
    public void unassignCustomerAgents(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing unassignCustomerAgents, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        customerAgentsUnassigner.removeEntities(tenantId, customerId);
    }

    @Override
    public Agent unassignAgentFromCustomer(TenantId tenantId, AgentId agentId) {
        Agent agent = findAgentById(tenantId, agentId);
        if (agent.getCustomerId() == null) {
            return agent;
        }
        agent.setCustomerId(null);
        return saveAgent(agent);
    }

    @Override
    public Agent assignAgentToCustomer(TenantId tenantId, AgentId agentId, CustomerId customerId) {
        Agent agent = findAgentById(tenantId, agentId);
        if (customerId.equals(agent.getCustomerId())) {
            return agent;
        }
        agent.setCustomerId(customerId);
        return saveAgent(agent);

    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT;
    }

    private final PaginatedRemover<TenantId, Agent> tenantAgentsRemover = new PaginatedRemover<>() {
        @Override
        protected PageData<Agent> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return agentDao.findAgentsByTenantId(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Agent entity) {
            deleteAgent(tenantId, new AgentId(entity.getUuidId()));
        }
    };

    private final PaginatedRemover<CustomerId, Agent> customerAgentsUnassigner = new PaginatedRemover<>() {

        @Override
        protected PageData<Agent> findEntities(TenantId tenantId, CustomerId id, PageLink pageLink) {
            return agentDao.findAgentsByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Agent entity) {
            unassignAgentFromCustomer(tenantId, entity.getId());
        }
    };

}
