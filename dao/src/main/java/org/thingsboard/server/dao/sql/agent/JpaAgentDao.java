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
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.agent.AgentDao;
import org.thingsboard.server.dao.model.sql.AgentEntity;
import org.thingsboard.server.dao.model.sql.AgentInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Component
@SqlDao
@Slf4j
public class JpaAgentDao extends JpaAbstractDao<AgentEntity, Agent> implements AgentDao {

    @Autowired
    private AgentRepository agentRepository;

    @Override
    protected Class<AgentEntity> getEntityClass() {
        return AgentEntity.class;
    }

    @Override
    protected JpaRepository<AgentEntity, UUID> getRepository() {
        return agentRepository;
    }

    @Override
    public AgentInfo findAgentInfoById(TenantId tenantId, UUID agentId) {
        return DaoUtil.getData(agentRepository.findAgentInfoById(agentId));
    }

    @Override
    public PageData<Agent> findAgentsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(agentRepository
                .findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AgentInfo> findAgentInfosByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(
                agentRepository.findAgentInfosByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, AgentInfoEntity.agentInfoColumnMap)));
    }

    @Override
    public PageData<Agent> findAgentsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(agentRepository
                .findByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AgentInfo> findAgentInfosByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(agentRepository
                .findAgentInfosByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, AgentInfoEntity.agentInfoColumnMap)));
    }

    @Override
    public Agent findByRoutingKey(UUID tenantId, String routingKey) {
        return DaoUtil.getData(agentRepository.findByRoutingKey(routingKey));
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return agentRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT;
    }
}
