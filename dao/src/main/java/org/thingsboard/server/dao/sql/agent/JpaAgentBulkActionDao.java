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
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.id.AgentGroupId;
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
    public PageData<AgentBulkAction> findByGroupId(AgentGroupId groupId, PageLink pageLink) {
        return DaoUtil.pageToPageData(
                repository.findByGroupId(groupId.getId(), DaoUtil.toPageable(pageLink))
                        .map(AgentBulkActionEntity::toData));
    }
}
