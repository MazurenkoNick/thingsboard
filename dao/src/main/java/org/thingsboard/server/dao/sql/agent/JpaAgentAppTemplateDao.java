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
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.agent.AgentAppTemplateDao;
import org.thingsboard.server.dao.model.sql.AgentAppTemplateEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Component
@SqlDao
@Slf4j
public class JpaAgentAppTemplateDao extends JpaAbstractDao<AgentAppTemplateEntity, AgentAppTemplate> implements AgentAppTemplateDao {

    @Autowired
    private AgentAppTemplateRepository agentAppTemplateRepository;

    @Override
    protected Class<AgentAppTemplateEntity> getEntityClass() {
        return AgentAppTemplateEntity.class;
    }

    @Override
    protected JpaRepository<AgentAppTemplateEntity, UUID> getRepository() {
        return agentAppTemplateRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_APP_TEMPLATE;
    }

    @Override
    public AgentAppTemplate findLatestByAppTypeAndConfigType(TenantId tenantId, AgentApplicationType appType, AgentAppConfigType configType) {
        return DaoUtil.getData(agentAppTemplateRepository.findLatestByAppTypeAndConfigType(
                appType.name(), configType.name()));
    }

    @Override
    public AgentAppTemplate findByAppTypeAndConfigTypeAndVersion(TenantId tenantId, AgentApplicationType appType,
                                                                  AgentAppConfigType configType, String currentVersion) {
        return DaoUtil.getData(agentAppTemplateRepository.findFirstByAppTypeAndConfigTypeAndCurrentVersion(
                appType.name(), configType.name(), currentVersion));
    }

    @Override
    public List<AgentAppTemplate> findAll(TenantId tenantId) {
        return DaoUtil.convertDataList(agentAppTemplateRepository.findAll());
    }

    @Override
    public void removeById(TenantId tenantId, UUID id) {
        agentAppTemplateRepository.deleteById(id);
    }
}
