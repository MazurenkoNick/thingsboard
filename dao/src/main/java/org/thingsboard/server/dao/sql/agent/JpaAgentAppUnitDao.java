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
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitFilter;
import org.thingsboard.server.common.data.agent.AgentAppUnitInfo;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.agent.AgentAppUnitDao;
import org.thingsboard.server.dao.model.sql.AgentAppUnitEntity;
import org.thingsboard.server.dao.model.sql.AgentAppUnitInfoEntity;
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
    public AgentAppUnit findByAgentAndProjectAndIdentifier(TenantId tenantId, AgentId agentId, String projectName, String identifier) {
        AgentAppUnitEntity entity = agentAppUnitRepository.findByAgentAndProjectAndIdentifier(
                tenantId.getId(), agentId.getId(), projectName, identifier);
        return entity == null ? null : entity.toData();
    }

    @Override
    public AgentAppUnitInfo findAgentAppUnitInfoById(TenantId tenantId, AgentAppUnitId agentAppUnitId) {
        AgentAppUnitInfoEntity entity = agentAppUnitRepository.findInfoById(tenantId.getId(), agentAppUnitId.getId());
        return entity == null ? null : entity.toData();
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
