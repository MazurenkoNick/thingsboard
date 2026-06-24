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
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentAppProfileInfo;
import org.thingsboard.server.common.data.agent.AgentAppProfileRelationInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.agent.AgentAppProfileDao;
import org.thingsboard.server.dao.model.sql.AgentAppProfileEntity;
import org.thingsboard.server.dao.model.sql.AgentAppProfileInfoEntity;
import org.thingsboard.server.dao.model.sql.AgentAppProfileRelationInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Component
@SqlDao
@Slf4j
public class JpaAgentAppProfileDao extends JpaAbstractDao<AgentAppProfileEntity, AgentAppProfile> implements AgentAppProfileDao {

    @Autowired
    private AgentAppProfileRepository profileRepository;

    @Override
    protected Class<AgentAppProfileEntity> getEntityClass() {
        return AgentAppProfileEntity.class;
    }

    @Override
    protected JpaRepository<AgentAppProfileEntity, UUID> getRepository() {
        return profileRepository;
    }

    @Override
    public PageData<AgentAppProfile> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(profileRepository.findByTenantId(
                tenantId,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public AgentAppProfileInfo findInfoById(UUID profileId) {
        AgentAppProfileInfoEntity entity = profileRepository.findInfoById(profileId);
        return entity != null ? entity.toData() : null;
    }

    @Override
    public List<AgentAppProfileInfo> findInfosByTenantIdAndAppType(UUID tenantId, AgentApplicationType appType) {
        return profileRepository.findInfosByTenantIdAndAppType(tenantId, appType).stream()
                .map(AgentAppProfileInfoEntity::toData)
                .toList();
    }

    @Override
    public List<AgentAppProfileRelationInfo> findRelationInfosByAgentProfileId(UUID agentProfileId, String relationType) {
        return profileRepository.findRelationInfosByAgentProfileId(agentProfileId, relationType).stream()
                .map(AgentAppProfileRelationInfoEntity::toData)
                .toList();
    }

    @Override
    public List<AgentAppProfileRelationInfo> findRelationInfosByAgentProfileIdAndAppTypeAndTemplateId(UUID agentProfileId, AgentApplicationType appType, UUID templateId, String relationType) {
        return profileRepository.findRelationInfosByAgentProfileIdAndAppTypeAndTemplateId(agentProfileId, appType, templateId, relationType).stream()
                .map(AgentAppProfileRelationInfoEntity::toData)
                .toList();
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return profileRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public List<AgentAppProfile> findUninstalledAppProfilesForAgentProfile(UUID agentProfileId, UUID agentId) {
        return DaoUtil.convertDataList(profileRepository.findUninstalledAppProfilesForAgentProfile(agentProfileId, agentId));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_APP_PROFILE;
    }
}
