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
package org.thingsboard.server.dao.sql.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.agent.AgentAppEventDao;
import org.thingsboard.server.dao.model.sql.AgentAppEventEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Optional;
import java.util.UUID;

@Component
@SqlDao
@Slf4j
public class JpaAgentAppEventDao extends JpaAbstractDao<AgentAppEventEntity, AgentAppEvent> implements AgentAppEventDao {

    @Autowired
    private AgentAppEventRepository repository;

    @Override
    protected Class<AgentAppEventEntity> getEntityClass() {
        return AgentAppEventEntity.class;
    }

    @Override
    protected JpaRepository<AgentAppEventEntity, UUID> getRepository() {
        return repository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_APP_EVENT;
    }

    @Override
    public Optional<AgentAppEvent> findOldestPendingByApplicationId(UUID applicationId) {
        return repository.findOldestPendingByApplicationId(applicationId).map(AgentAppEventEntity::toData);
    }

    @Override
    public boolean hasActiveEventForApplication(UUID applicationId) {
        return repository.hasActiveEventForApplication(applicationId);
    }

    @Override
    public Optional<AgentAppEvent> findActiveDeliveredByApplicationId(UUID applicationId) {
        return repository.findActiveDeliveredByApplicationId(applicationId).map(AgentAppEventEntity::toData);
    }

    @Override
    public boolean markDelivered(UUID eventId) {
        return repository.markDelivered(eventId, System.currentTimeMillis()) == 1;
    }

    @Override
    public void updateStatus(UUID eventId, AgentAppEventStatus status, UUID currentStepId) {
        repository.updateStatus(eventId, status, currentStepId, System.currentTimeMillis());
    }

    @Override
    public void deleteAllPendingByApplicationId(UUID applicationId) {
        repository.deleteAllPendingByApplicationId(applicationId);
    }

    @Override
    public PageData<AgentAppEvent> findByBulkActionId(UUID bulkActionId, AgentAppEventStatus status, PageLink pageLink) {
        return DaoUtil.pageToPageData(
                repository.findByBulkActionId(bulkActionId, status, DaoUtil.toPageable(pageLink))
                        .map(AgentAppEventEntity::toData)
        );
    }

    @Override
    public PageData<AgentAppEvent> findByTenantIdAndApplicationId(TenantId tenantId, AgentApplicationId applicationId, PageLink pageLink) {
        return DaoUtil.pageToPageData(
                repository.findByTenantIdAndApplicationId(
                                tenantId.getId(),
                                applicationId.getId(),
                                DaoUtil.toPageable(pageLink))
                        .map(AgentAppEventEntity::toData)
        );
    }

    @Override
    public PageData<AgentAppEvent> findByTenantIdAndAgentId(TenantId tenantId, AgentId agentId, PageLink pageLink) {
        return DaoUtil.pageToPageData(
                repository.findByTenantIdAndAgentId(
                                tenantId.getId(),
                                agentId.getId(),
                                DaoUtil.toPageable(pageLink))
                        .map(AgentAppEventEntity::toData)
        );
    }
}
