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
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.dao.model.sql.AgentAppEventEntity;
import org.thingsboard.server.dao.model.sql.AgentAppEventInfoEntity;

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
           SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM AgentAppEventEntity e
           WHERE e.applicationId = :appId AND e.bulkActionId = :bulkActionId
           """)
    boolean existsByApplicationIdAndBulkActionId(@Param("appId") UUID applicationId, @Param("bulkActionId") UUID bulkActionId);

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
                       e.currentActivity = COALESCE(:activity, e.currentActivity),
                       e.errorMessage = COALESCE(:errorMessage, e.errorMessage),
                       e.updatedTime = :now
           WHERE e.id = :eventId
           AND (e.status IS NULL OR e.status NOT IN ('FINISHED', 'ERROR'))
           """)
    void updateStatus(@Param("eventId") UUID eventId, @Param("status") AgentAppEventStatus status,
                      @Param("stepId") UUID currentStepId, @Param("activity") String currentActivity,
                      @Param("errorMessage") String errorMessage, @Param("now") long now);

    @Transactional
    @Modifying
    @Query("DELETE FROM AgentAppEventEntity e WHERE e.applicationId = :appId AND e.deliveryState = 'PENDING'")
    void deleteAllPendingByApplicationId(@Param("appId") UUID applicationId);

    @Transactional
    @Modifying
    @Query("DELETE FROM AgentAppEventEntity e WHERE e.updatedTime < :expirationTs")
    int deleteEventsUpdatedBefore(@Param("expirationTs") long expirationTs);

    @Query(value = """
           SELECT * FROM agent_app_event e
           WHERE e.bulk_action_id = :bulkActionId
           AND (CAST(:actionType AS varchar) IS NULL OR e.action_type = CAST(:actionType AS varchar))
           AND (CAST(:status AS varchar) IS NULL OR e.status = CAST(:status AS varchar))
           AND (CAST(:textSearch AS varchar) IS NULL
                OR LOWER(e.action_type) LIKE LOWER(CONCAT('%', CAST(:textSearch AS varchar), '%'))
                OR LOWER(e.status) LIKE LOWER(CONCAT('%', CAST(:textSearch AS varchar), '%')))
           """,
           countQuery = """
           SELECT count(*) FROM agent_app_event e
           WHERE e.bulk_action_id = :bulkActionId
           AND (CAST(:actionType AS varchar) IS NULL OR e.action_type = CAST(:actionType AS varchar))
           AND (CAST(:status AS varchar) IS NULL OR e.status = CAST(:status AS varchar))
           AND (CAST(:textSearch AS varchar) IS NULL
                OR LOWER(e.action_type) LIKE LOWER(CONCAT('%', CAST(:textSearch AS varchar), '%'))
                OR LOWER(e.status) LIKE LOWER(CONCAT('%', CAST(:textSearch AS varchar), '%')))
           """,
           nativeQuery = true)
    Page<AgentAppEventEntity> findByBulkActionId(@Param("bulkActionId") UUID bulkActionId,
                                                 @Param("actionType") String actionType,
                                                 @Param("status") String status,
                                                 @Param("textSearch") String textSearch,
                                                 Pageable pageable);

    @Query(value = """
           SELECT * FROM agent_app_event e
           WHERE e.tenant_id = :tenantId AND e.application_id = :applicationId
           AND (CAST(:actionType AS varchar) IS NULL OR e.action_type = CAST(:actionType AS varchar))
           AND (CAST(:status AS varchar) IS NULL OR e.status = CAST(:status AS varchar))
           AND (CAST(:textSearch AS varchar) IS NULL
                OR LOWER(e.action_type) LIKE LOWER(CONCAT('%', CAST(:textSearch AS varchar), '%'))
                OR LOWER(e.status) LIKE LOWER(CONCAT('%', CAST(:textSearch AS varchar), '%')))
           """,
           countQuery = """
           SELECT count(*) FROM agent_app_event e
           WHERE e.tenant_id = :tenantId AND e.application_id = :applicationId
           AND (CAST(:actionType AS varchar) IS NULL OR e.action_type = CAST(:actionType AS varchar))
           AND (CAST(:status AS varchar) IS NULL OR e.status = CAST(:status AS varchar))
           AND (CAST(:textSearch AS varchar) IS NULL
                OR LOWER(e.action_type) LIKE LOWER(CONCAT('%', CAST(:textSearch AS varchar), '%'))
                OR LOWER(e.status) LIKE LOWER(CONCAT('%', CAST(:textSearch AS varchar), '%')))
           """,
           nativeQuery = true)
    Page<AgentAppEventEntity> findByFilter(@Param("tenantId") UUID tenantId,
                                           @Param("applicationId") UUID applicationId,
                                           @Param("actionType") String actionType,
                                           @Param("status") String status,
                                           @Param("textSearch") String textSearch,
                                           Pageable pageable);

    @Query("""
           SELECT e FROM AgentAppEventEntity e
           WHERE e.tenantId = :tenantId AND e.agentId = :agentId
           """)
    Page<AgentAppEventEntity> findByTenantIdAndAgentId(@Param("tenantId") UUID tenantId,
                                                       @Param("agentId") UUID agentId,
                                                       Pageable pageable);

    @Query(value = """
           SELECT new org.thingsboard.server.dao.model.sql.AgentAppEventInfoEntity(e, COALESCE(a.name, e.applicationName), ag.name)
           FROM AgentAppEventEntity e
           LEFT JOIN AgentApplicationEntity a ON e.applicationId = a.id
           LEFT JOIN AgentEntity ag ON e.agentId = ag.id
           WHERE e.tenantId = :tenantId AND e.agentId = :agentId
           AND (:actionType IS NULL OR e.actionType = :actionType)
           AND (:status IS NULL OR e.status = :status)
           AND (:textSearch IS NULL OR ilike(COALESCE(a.name, e.applicationName), CONCAT('%', :textSearch, '%')) = true)
           """,
           countQuery = """
           SELECT COUNT(e)
           FROM AgentAppEventEntity e
           LEFT JOIN AgentApplicationEntity a ON e.applicationId = a.id
           WHERE e.tenantId = :tenantId AND e.agentId = :agentId
           AND (:actionType IS NULL OR e.actionType = :actionType)
           AND (:status IS NULL OR e.status = :status)
           AND (:textSearch IS NULL OR ilike(COALESCE(a.name, e.applicationName), CONCAT('%', :textSearch, '%')) = true)
           """)
    Page<AgentAppEventInfoEntity> findInfosByTenantIdAndAgentId(@Param("tenantId") UUID tenantId,
                                                                @Param("agentId") UUID agentId,
                                                                @Param("actionType") AgentAppEventActionType actionType,
                                                                @Param("status") AgentAppEventStatus status,
                                                                @Param("textSearch") String textSearch,
                                                                Pageable pageable);

    @Query("""
           SELECT new org.thingsboard.server.dao.model.sql.AgentAppEventInfoEntity(e, COALESCE(a.name, e.applicationName), ag.name)
           FROM AgentAppEventEntity e
           LEFT JOIN AgentApplicationEntity a ON e.applicationId = a.id
           LEFT JOIN AgentEntity ag ON e.agentId = ag.id
           WHERE e.bulkActionId = :bulkActionId
           AND (:actionType IS NULL OR e.actionType = :actionType)
           AND (:status IS NULL OR e.status = :status)
           AND (:textSearch IS NULL
                OR ilike(COALESCE(a.name, e.applicationName), CONCAT('%', :textSearch, '%')) = true
                OR ilike(ag.name, CONCAT('%', :textSearch, '%')) = true)
           """)
    Page<AgentAppEventInfoEntity> findInfosByBulkActionId(@Param("bulkActionId") UUID bulkActionId,
                                                          @Param("actionType") AgentAppEventActionType actionType,
                                                          @Param("status") AgentAppEventStatus status,
                                                          @Param("textSearch") String textSearch,
                                                          Pageable pageable);
}
