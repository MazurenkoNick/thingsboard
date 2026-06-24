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
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.AgentProfileInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.agent.AgentProfileDao;
import org.thingsboard.server.dao.model.sql.AgentProfileEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Component
@SqlDao
@Slf4j
public class JpaAgentProfileDao extends JpaAbstractDao<AgentProfileEntity, AgentProfile> implements AgentProfileDao {

    @Autowired
    private AgentProfileRepository profileRepository;

    @Override
    protected Class<AgentProfileEntity> getEntityClass() {
        return AgentProfileEntity.class;
    }

    @Override
    protected JpaRepository<AgentProfileEntity, UUID> getRepository() {
        return profileRepository;
    }

    @Override
    public AgentProfileInfo findAgentProfileInfoById(UUID agentProfileId) {
        return DaoUtil.getData(profileRepository.findAgentProfileInfoById(agentProfileId));
    }

    @Override
    public PageData<AgentProfile> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(profileRepository.findByTenantId(
                tenantId,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AgentProfileInfo> findAgentProfileInfosByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(profileRepository.findAgentProfileInfosByTenantId(
                tenantId,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return profileRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public AgentProfile findByProvisionKey(String provisionKey) {
        return profileRepository.findByProvisionKey(provisionKey).map(AgentProfileEntity::toData).orElse(null);
    }

    @Override
    public AgentProfile findByTenantIdAndName(UUID tenantId, String name) {
        return profileRepository.findByTenantIdAndName(tenantId, name).map(AgentProfileEntity::toData).orElse(null);
    }

    @Override
    public AgentProfile findDefaultAgentProfile(TenantId tenantId) {
        AgentProfileEntity entity = profileRepository.findByTenantIdAndDefaultTrue(tenantId.getId());
        return entity != null ? entity.toData() : null;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_PROFILE;
    }
}
