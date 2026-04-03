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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.dao.model.sql.AgentAppEventEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface AgentAppEventRepository extends JpaRepository<AgentAppEventEntity, UUID> {

    @Query("""
           SELECT e FROM AgentAppEventEntity e
           WHERE e.applicationId = :appId AND e.deliveryState = 'PENDING'
           ORDER BY e.createdTime ASC LIMIT 1
           """)
    Optional<AgentAppEventEntity> findOldestPendingByApplicationId(@Param("appId") UUID applicationId);

    @Query("""
           SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM AgentAppEventEntity e
           WHERE e.applicationId = :appId AND e.deliveryState = 'DELIVERED'
           AND (e.status IS NULL OR e.status NOT IN ('FINISHED', 'ERROR'))
           """)
    boolean hasActiveEventForApplication(@Param("appId") UUID applicationId);

    @Query("""
           SELECT e FROM AgentAppEventEntity e
           WHERE e.applicationId = :appId AND e.deliveryState = 'DELIVERED'
           AND (e.status IS NULL OR e.status NOT IN ('FINISHED', 'ERROR'))
           ORDER BY e.createdTime ASC LIMIT 1
           """)
    Optional<AgentAppEventEntity> findActiveDeliveredByApplicationId(@Param("appId") UUID applicationId);

    @Transactional
    @Modifying
    @Query("""
           UPDATE AgentAppEventEntity e SET e.deliveryState = 'DELIVERED', e.updatedTime = :now
           WHERE e.id = :eventId AND e.deliveryState = 'PENDING'
           """)
    int markDelivered(@Param("eventId") UUID eventId, @Param("now") long now);

    @Transactional
    @Modifying
    @Query("""
           UPDATE AgentAppEventEntity e SET
                       e.status = COALESCE(:status, e.status),
                       e.currentStepId = COALESCE(:stepId, e.currentStepId),
                       e.updatedTime = :now
           WHERE e.id = :eventId
           AND (e.status IS NULL OR e.status NOT IN ('FINISHED', 'ERROR'))
           """)
    void updateStatus(@Param("eventId") UUID eventId, @Param("status") AgentAppEventStatus status,
                      @Param("stepId") UUID currentStepId, @Param("now") long now);

    @Transactional
    @Modifying
    @Query("DELETE FROM AgentAppEventEntity e WHERE e.applicationId = :appId AND e.deliveryState = 'PENDING'")
    void deleteAllPendingByApplicationId(@Param("appId") UUID applicationId);

    @Query("""
           SELECT e FROM AgentAppEventEntity e
           WHERE e.bulkActionId = :bulkActionId
           AND (:status IS NULL OR e.status = :status)
           """)
    Page<AgentAppEventEntity> findByBulkActionId(@Param("bulkActionId") UUID bulkActionId,
                                                 @Param("status") AgentAppEventStatus status,
                                                 Pageable pageable);

    Page<AgentAppEventEntity> findByTenantIdAndApplicationId(UUID tenantId, UUID applicationId, Pageable pageable);

    @Query("""
           SELECT e FROM AgentAppEventEntity e
           JOIN AgentApplicationEntity a ON e.applicationId = a.id
           WHERE e.tenantId = :tenantId AND a.agentId = :agentId
           """)
    Page<AgentAppEventEntity> findByTenantIdAndAgentId(@Param("tenantId") UUID tenantId,
                                                       @Param("agentId") UUID agentId,
                                                       Pageable pageable);
}
