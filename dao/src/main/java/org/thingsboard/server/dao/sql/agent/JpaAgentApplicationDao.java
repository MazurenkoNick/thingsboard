/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.dao.sql.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
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
    public AgentApplication findByIdForUpdate(TenantId tenantId, UUID id) {
        return DaoUtil.getData(agentApplicationRepository.findByIdForUpdate(id));
    }

    @Override
    public void removeByAgentId(TenantId tenantId, UUID agentId) {
        agentApplicationRepository.deleteByAgentId(tenantId.getId(), agentId);
    }

    @Override
    public void removeByTemplateId(TenantId tenantId, UUID templateId) {
        agentApplicationRepository.deleteByTemplateId(templateId);
    }

    @Override
    public List<AgentApplication> findByAgentId(TenantId tenantId, UUID agentId) {
        return DaoUtil.convertDataList(agentApplicationRepository.findByTenantIdAndAgentId(tenantId.getId(), agentId));
    }

    @Override
    public PageData<AgentApplication> findByAgentId(TenantId tenantId, UUID agentId, PageLink pageLink) {
        return DaoUtil.toPageData(agentApplicationRepository.findByAgentId(
                tenantId.getId(),
                agentId,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<AgentApplication> findByTemplateId(TenantId tenantId, UUID templateId) {
        return DaoUtil.convertDataList(agentApplicationRepository.findByTemplateId(templateId));
    }

    @Override
    public AgentApplication findByProjectName(TenantId tenantId, AgentId agentId, String projectName) {
        return DaoUtil.getData(agentApplicationRepository.findByTenantIdAndAgentIdAndProjectName(tenantId.getId(), agentId.getId(), projectName));
    }

    @Override
    public AgentApplication findByEventId(TenantId tenantId, UUID eventId) {
        return DaoUtil.getData(agentApplicationRepository.findByEventId(tenantId.getId(), eventId));
    }

    @Override
    public AgentApplicationInfo findInfoById(TenantId tenantId, UUID id) {
        AgentApplicationInfoEntity entity = agentApplicationRepository.findInfoById(tenantId.getId(), id);
        return entity != null ? entity.toData() : null;
    }

    @Override
    public PageData<AgentApplicationInfo> findInfosByAgentId(TenantId tenantId, UUID agentId, PageLink pageLink) {
        return DaoUtil.pageToPageData(agentApplicationRepository.findInfosByAgentId(
                tenantId.getId(), agentId, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink))
                .map(AgentApplicationInfoEntity::toData));
    }

    @Override
    public PageData<AgentApplicationInfo> findByApplicationProfileIdAndAgentProfileId(TenantId tenantId, UUID profileId, UUID agentProfileId, PageLink pageLink) {
        return DaoUtil.pageToPageData(agentApplicationRepository.findByApplicationProfileIdAndAgentProfileId(
                tenantId.getId(), profileId, agentProfileId, DaoUtil.toPageable(pageLink))
                .map(AgentApplicationInfoEntity::toData));
    }

    @Override
    public PageData<AgentApplication> findByEntityGroupId(UUID groupId, PageLink pageLink) {
        return DaoUtil.toPageData(agentApplicationRepository
                .findByEntityGroupId(groupId, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AgentApplication> findByEntityGroupIds(List<UUID> groupIds, PageLink pageLink) {
        return DaoUtil.toPageData(agentApplicationRepository
                .findByEntityGroupIds(groupIds, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public AgentApplication findByRelatedEntity(TenantId tenantId, UUID relatedEntityId) {
        return DaoUtil.getData(agentApplicationRepository.findByRelatedEntityId(tenantId.getId(), relatedEntityId));
    }

    @Override
    public List<UUID> findManagedRelatedEntityIds(TenantId tenantId, String relatedEntityType) {
        return agentApplicationRepository.findManagedRelatedEntityIds(tenantId.getId(), relatedEntityType);
    }

    @Override
    public int promoteDesiredTemplate(TenantId tenantId, AgentApplicationId applicationId, AgentAppTemplateId templateId) {
        return agentApplicationRepository.promoteDesiredTemplate(tenantId.getId(), applicationId.getId(), templateId.getId());
    }
}
