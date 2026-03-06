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
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
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
        resolveProjectName(agentApplication, old);
        agentApplicationValidator.validate(agentApplication, app -> tenantId);
        AgentApplication saved = agentApplicationDao.save(tenantId, agentApplication);
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
        log.trace("Executing findAgentApplicationById [{}]", agentApplicationId);
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
    @Transactional
    public void delete(TenantId tenantId, AgentApplicationId agentApplicationId) {
        log.trace("Executing deleteAgentApplication [{}]", agentApplicationId);
        validateId(agentApplicationId, id -> INCORRECT_AGENT_APPLICATION_ID + id);
        AgentApplication application = agentApplicationDao.findById(tenantId, agentApplicationId.getId());
        if (application != null) {
            agentAppUnitService.deleteByAgentApplicationId(tenantId, agentApplicationId);
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

    private void resolveProjectName(AgentApplication agentApplication, AgentApplication old) {
        if (old != null && old.getProjectName() != null) {
            agentApplication.setProjectName(old.getProjectName());
        } else if (agentApplication.getProjectName() == null) {
            agentApplication.setProjectName(AgentApplication.generateProjectName());
        }
    }

}
