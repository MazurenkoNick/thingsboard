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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.agent.AgentAppUnitCacheEvictEvent;
import org.thingsboard.server.cache.agent.AgentAppUnitCacheKey;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.Collections;
import java.util.List;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service("AgentAppUnitDaoService")
@Slf4j
public class BaseAgentAppUnitService extends AbstractCachedEntityService<AgentAppUnitCacheKey, AgentAppUnit, AgentAppUnitCacheEvictEvent> implements AgentAppUnitService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_AGENT_APP_UNIT_ID = "Incorrect agentAppUnitId ";
    public static final String INCORRECT_AGENT_APPLICATION_ID = "Incorrect agentApplicationId ";

    @Autowired
    private AgentAppUnitDao agentAppUnitDao;

    @Autowired
    private DataValidator<AgentAppUnit> agentAppUnitValidator;

    @Override
    @TransactionalEventListener
    public void handleEvictEvent(AgentAppUnitCacheEvictEvent event) {
        cache.evict(Collections.singletonList(new AgentAppUnitCacheKey(event.getAgentAppUnitId())));
    }

    @Override
    @Transactional
    public AgentAppUnit saveAgentAppUnit(TenantId tenantId, AgentAppUnit agentAppUnit) {
        log.trace("Executing saveAgentAppUnit [{}]", agentAppUnit);
        AgentAppUnit old = agentAppUnitValidator.validate(agentAppUnit, unit -> tenantId);
        AgentAppUnit saved = agentAppUnitDao.save(tenantId, agentAppUnit);
        publishEvictEvent(new AgentAppUnitCacheEvictEvent(saved.getId()));
        eventPublisher.publishEvent(SaveEntityEvent.builder()
                .tenantId(tenantId)
                .entityId(saved.getId())
                .entity(saved)
                .oldEntity(old)
                .created(agentAppUnit.getId() == null)
                .build());
        return saved;
    }

    @Override
    public AgentAppUnit findAgentAppUnitById(TenantId tenantId, AgentAppUnitId agentAppUnitId) {
        log.trace("Executing findAgentAppUnitById [{}]", agentAppUnitId);
        validateId(agentAppUnitId, id -> INCORRECT_AGENT_APP_UNIT_ID + id);
        return cache.getAndPutInTransaction(AgentAppUnitCacheKey.from(agentAppUnitId),
                () -> agentAppUnitDao.findById(tenantId, agentAppUnitId.getId()), true);
    }

    @Override
    public List<AgentAppUnit> findAgentAppUnitsByAgentAppId(TenantId tenantId, AgentApplicationId agentAppId) {
        log.trace("Executing findAgentAppUnitsByAgentApplicationId, tenantId [{}], agentApplicationId [{}]", tenantId, agentAppId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(agentAppId, id -> INCORRECT_AGENT_APPLICATION_ID + id);
        return agentAppUnitDao.findByAgentApplicationId(tenantId, agentAppId.getId());
    }

    @Override
    @Transactional
    public void deleteAgentAppUnit(TenantId tenantId, AgentAppUnitId agentAppUnitId) {
        log.trace("Executing deleteAgentAppUnit [{}]", agentAppUnitId);
        validateId(agentAppUnitId, id -> INCORRECT_AGENT_APP_UNIT_ID + id);
        AgentAppUnit unit = agentAppUnitDao.findById(tenantId, agentAppUnitId.getId());
        if (unit == null) {
            return;
        }
        agentAppUnitDao.removeById(tenantId, agentAppUnitId.getId());
        publishCacheEvictAndDeleteEvent(tenantId, unit);
    }

    @Override
    @Transactional
    public void deleteByAgentApplicationId(TenantId tenantId, AgentApplicationId agentAppId) {
        log.trace("Executing deleteByAgentApplicationId, tenantId [{}], agentApplicationId [{}]", tenantId, agentAppId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(agentAppId, id -> INCORRECT_AGENT_APPLICATION_ID + id);
        agentAppUnitDao.findByAgentApplicationId(tenantId, agentAppId.getId())
                .forEach(u -> publishCacheEvictAndDeleteEvent(tenantId, u));
        agentAppUnitDao.removeByAgentApplicationId(tenantId, agentAppId.getId());
    }

    private void publishCacheEvictAndDeleteEvent(TenantId tenantId, AgentAppUnit unit) {
        AgentAppUnitId agentAppUnitId = unit.getId();
        publishEvictEvent(new AgentAppUnitCacheEvictEvent(agentAppUnitId));
        eventPublisher.publishEvent(DeleteEntityEvent.builder()
                .tenantId(tenantId)
                .entityId(agentAppUnitId)
                .entity(unit)
                .build());
    }
}
