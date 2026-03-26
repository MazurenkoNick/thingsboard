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
import org.thingsboard.server.common.data.agent.AgentGroup;
import org.thingsboard.server.common.data.agent.AgentGroupInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.agent.AgentGroupDao;
import org.thingsboard.server.dao.model.sql.AgentGroupEntity;
import org.thingsboard.server.dao.model.sql.AgentGroupInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Component
@SqlDao
@Slf4j
public class JpaAgentGroupDao extends JpaAbstractDao<AgentGroupEntity, AgentGroup> implements AgentGroupDao {

    @Autowired
    private AgentGroupRepository groupRepository;

    @Override
    protected Class<AgentGroupEntity> getEntityClass() {
        return AgentGroupEntity.class;
    }

    @Override
    protected JpaRepository<AgentGroupEntity, UUID> getRepository() {
        return groupRepository;
    }

    @Override
    public AgentGroupInfo findAgentGroupInfoById(UUID groupId) {
        return DaoUtil.getData(groupRepository.findAgentGroupInfoById(groupId));
    }

    @Override
    public PageData<AgentGroup> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(groupRepository.findByTenantId(
                tenantId,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AgentGroupInfo> findAgentGroupInfosByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(groupRepository.findAgentGroupInfosByTenantId(
                tenantId,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink, AgentGroupInfoEntity.agentGroupInfoColumnMap)));
    }

    @Override
    public PageData<AgentGroup> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(groupRepository.findByTenantIdAndCustomerId(
                tenantId,
                customerId,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return groupRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_GROUP;
    }
}
