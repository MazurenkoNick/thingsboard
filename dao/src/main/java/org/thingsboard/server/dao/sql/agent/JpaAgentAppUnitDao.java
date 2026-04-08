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
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitFilter;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.agent.AgentAppUnitDao;
import org.thingsboard.server.dao.model.sql.AgentAppUnitEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@SqlDao
@Slf4j
public class JpaAgentAppUnitDao extends JpaAbstractDao<AgentAppUnitEntity, AgentAppUnit> implements AgentAppUnitDao {

    @Autowired
    private AgentAppUnitRepository agentAppUnitRepository;

    @Override
    protected Class<AgentAppUnitEntity> getEntityClass() {
        return AgentAppUnitEntity.class;
    }

    @Override
    protected JpaRepository<AgentAppUnitEntity, UUID> getRepository() {
        return agentAppUnitRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_APP_UNIT;
    }

    private static final Map<String, String> UNIT_COLUMN_MAP = Map.of(
            "createdTime", "created_time",
            "identifier", "identifier",
            "type", "type"
    );

    @Override
    public List<AgentAppUnit> findByAgentApplicationId(TenantId tenantId, UUID agentApplicationId) {
        return DaoUtil.convertDataList(agentAppUnitRepository.findByAgentApplicationId(agentApplicationId));
    }

    @Override
    public PageData<AgentAppUnit> findByFilter(AgentAppUnitFilter filter, PageLink pageLink) {
        String ts = pageLink.getTextSearch();
        String textSearch = (ts == null || ts.isBlank()) ? null : ts.trim();
        return DaoUtil.pageToPageData(
                agentAppUnitRepository.findByFilter(
                                filter.getApplicationId().getId(),
                                filter.getType() != null ? filter.getType().name() : null,
                                textSearch,
                                DaoUtil.toPageable(pageLink, UNIT_COLUMN_MAP))
                        .map(AgentAppUnitEntity::toData)
        );
    }

    @Override
    public void removeByAgentApplicationId(TenantId tenantId, UUID agentApplicationId) {
        agentAppUnitRepository.deleteByAgentApplicationId(agentApplicationId);
    }

}
