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
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.model.sql.AgentApplicationEntity;
import org.thingsboard.server.dao.model.sql.AgentApplicationInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Component
@SqlDao
@Slf4j
public class JpaAgentApplicationDao extends JpaAbstractDao<AgentApplicationEntity, AgentApplication> implements AgentApplicationDao {

    @Autowired
    private AgentApplicationRepository agentApplicationRepository;

    @Override
    protected Class<AgentApplicationEntity> getEntityClass() {
        return AgentApplicationEntity.class;
    }

    @Override
    protected JpaRepository<AgentApplicationEntity, UUID> getRepository() {
        return agentApplicationRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_APPLICATION;
    }

    @Override
    public void removeByAgentId(TenantId tenantId, UUID agentId) {
        agentApplicationRepository.deleteByAgentId(agentId);
    }

    @Override
    public void removeByTemplateId(TenantId tenantId, UUID templateId) {
        agentApplicationRepository.deleteByTemplateId(templateId);
    }

    @Override
    public List<AgentApplication> findByAgentId(TenantId tenantId, UUID agentId) {
        return DaoUtil.convertDataList(agentApplicationRepository.findByAgentId(agentId));
    }

    @Override
    public PageData<AgentApplication> findByAgentId(TenantId tenantId, UUID agentId, PageLink pageLink) {
        return DaoUtil.toPageData(agentApplicationRepository.findByAgentId(
                agentId,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<AgentApplication> findByTemplateId(TenantId tenantId, UUID templateId) {
        return DaoUtil.convertDataList(agentApplicationRepository.findByTemplateId(templateId));
    }

    @Override
    public AgentApplication findByProjectName(TenantId tenantId, String projectName) {
        return DaoUtil.getData(agentApplicationRepository.findByProjectName(projectName));
    }

    @Override
    public AgentApplication findByEventId(TenantId tenantId, UUID eventId) {
        return DaoUtil.getData(agentApplicationRepository.findByEventId(eventId));
    }

    @Override
    public AgentApplicationInfo findInfoById(TenantId tenantId, UUID id) {
        AgentApplicationInfoEntity entity = agentApplicationRepository.findInfoById(id);
        return entity != null ? entity.toData() : null;
    }

    @Override
    public PageData<AgentApplicationInfo> findInfosByAgentId(TenantId tenantId, UUID agentId, PageLink pageLink) {
        return DaoUtil.pageToPageData(agentApplicationRepository.findInfosByAgentId(
                agentId, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink))
                .map(AgentApplicationInfoEntity::toData));
    }

    @Override
    public PageData<AgentApplication> findByApplicationProfileIdAndAgentGroupId(UUID profileId, UUID groupId, PageLink pageLink) {
        return DaoUtil.toPageData(agentApplicationRepository.findByApplicationProfileIdAndAgentGroupId(
                profileId, groupId, DaoUtil.toPageable(pageLink)));
    }

    @Override
    public AgentApplication findByRelatedEntity(TenantId tenantId, UUID relatedEntityId, String relatedEntityType) {
        return DaoUtil.getData(agentApplicationRepository.findByRelatedEntityIdAndRelatedEntityType(
                relatedEntityId, EntityType.valueOf(relatedEntityType)));
    }
}
