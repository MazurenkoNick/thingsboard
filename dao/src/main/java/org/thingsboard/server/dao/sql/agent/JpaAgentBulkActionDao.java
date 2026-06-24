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
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.agent.AgentBulkActionDao;
import org.thingsboard.server.dao.model.sql.AgentBulkActionEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Component
@SqlDao
@Slf4j
public class JpaAgentBulkActionDao extends JpaAbstractDao<AgentBulkActionEntity, AgentBulkAction> implements AgentBulkActionDao {

    @Autowired
    private AgentBulkActionRepository repository;

    @Override
    protected Class<AgentBulkActionEntity> getEntityClass() {
        return AgentBulkActionEntity.class;
    }

    @Override
    protected JpaRepository<AgentBulkActionEntity, UUID> getRepository() {
        return repository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_BULK_ACTION;
    }

    @Override
    public void cleanUpExpiredBulkActions(long expirationTs) {
        repository.deleteEventsByBulkActionCreatedTimeBefore(expirationTs);
        repository.deleteBulkActionsCreatedTimeBefore(expirationTs);
    }

    @Override
    public PageData<AgentBulkAction> findStuckBulkActions(long threshold, PageLink pageLink) {
        return DaoUtil.pageToPageData(
                repository.findStuckBulkActions(threshold, DaoUtil.toPageable(pageLink))
                        .map(AgentBulkActionEntity::toData));
    }

    @Override
    public PageData<AgentBulkAction> findByAgentProfileId(AgentProfileId agentProfileId, PageLink pageLink) {
        return DaoUtil.pageToPageData(
                repository.findByAgentProfileId(agentProfileId.getId(), DaoUtil.toPageable(pageLink))
                        .map(AgentBulkActionEntity::toData));
    }

    @Override
    public PageData<AgentBulkAction> findByAgentProfileIdAndApplicationProfileId(AgentProfileId agentProfileId,
                                                                                 AgentAppProfileId applicationProfileId,
                                                                                 PageLink pageLink) {
        return DaoUtil.pageToPageData(
                repository.findByAgentProfileIdAndApplicationProfileId(
                        agentProfileId.getId(),
                        applicationProfileId.getId(),
                        DaoUtil.toPageable(pageLink))
                        .map(AgentBulkActionEntity::toData));
    }
}
