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
package org.thingsboard.server.dao.agent;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;

import java.util.Optional;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class BaseAgentBulkActionService implements AgentBulkActionService {

    private static final String INCORRECT_AGENT_BULK_ACTION_ID = "Incorrect agentBulkActionId ";

    @Autowired
    private AgentBulkActionDao agentBulkActionDao;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public AgentBulkAction save(TenantId tenantId, AgentBulkAction bulkAction) {
        log.trace("Executing saveAgentBulkAction [{}]", bulkAction);
        AgentBulkAction saved = agentBulkActionDao.save(tenantId, bulkAction);
        eventPublisher.publishEvent(SaveEntityEvent.builder()
                .tenantId(saved.getTenantId())
                .entityId(saved.getId())
                .entity(saved)
                .created(bulkAction.getId() == null)
                .build());
        return saved;
    }

    @Override
    public AgentBulkAction findById(TenantId tenantId, AgentBulkActionId id) {
        log.trace("Executing findAgentBulkActionById [{}]", id);
        validateId(id, i -> INCORRECT_AGENT_BULK_ACTION_ID + i);
        return agentBulkActionDao.findById(tenantId, id.getId());
    }

    @Override
    public PageData<AgentBulkAction> findStuckBulkActions(long threshold, PageLink pageLink) {
        return agentBulkActionDao.findStuckBulkActions(threshold, pageLink);
    }

    @Override
    public PageData<AgentBulkAction> findByAgentProfileId(TenantId tenantId, AgentProfileId agentProfileId, PageLink pageLink) {
        log.trace("Executing findBulkActionsByProfileId [{}]", agentProfileId);
        validateId(agentProfileId, id -> "Incorrect agentProfileId " + id);
        return agentBulkActionDao.findByAgentProfileId(agentProfileId, pageLink);
    }

    @Override
    public PageData<AgentBulkAction> findByAgentProfileIdAndApplicationProfileId(TenantId tenantId,
                                                                                 AgentProfileId agentProfileId,
                                                                                 AgentAppProfileId applicationProfileId,
                                                                                 PageLink pageLink) {
        log.trace("Executing findBulkActionsByProfileIdAndAppProfileId [{}, {}]", agentProfileId, applicationProfileId);
        validateId(agentProfileId, id -> "Incorrect agentProfileId " + id);
        validateId(applicationProfileId, id -> "Incorrect applicationProfileId " + id);
        return agentBulkActionDao.findByAgentProfileIdAndApplicationProfileId(agentProfileId, applicationProfileId, pageLink);
    }

    @Override
    public void cleanUpExpiredBulkActions(long expirationTs) {
        log.trace("Executing cleanUpExpiredBulkActions before [{}]", expirationTs);
        agentBulkActionDao.cleanUpExpiredBulkActions(expirationTs);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findById(tenantId, new AgentBulkActionId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        ListenableFuture<AgentBulkAction> future = agentBulkActionDao.findByIdAsync(tenantId, entityId.getId());
        return FluentFuture.from(future).transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_BULK_ACTION;
    }
}
