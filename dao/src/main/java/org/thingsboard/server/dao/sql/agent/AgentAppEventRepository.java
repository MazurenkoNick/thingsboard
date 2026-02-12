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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.agent.AgentAppEventDeliveryState;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.dao.model.sql.AgentAppEventEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentAppEventRepository extends JpaRepository<AgentAppEventEntity, UUID> {

    @Query("SELECT e FROM AgentAppEventEntity e WHERE e.applicationId = :appId AND e.deliveryState = 'PENDING' ORDER BY e.createdTime ASC LIMIT 1")
    Optional<AgentAppEventEntity> findOldestPendingByApplicationId(@Param("appId") UUID applicationId);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM AgentAppEventEntity e " +
            "WHERE e.applicationId = :appId AND e.deliveryState = 'DELIVERED' " +
            "AND (e.status IS NULL OR e.status NOT IN ('FINISHED', 'ERROR'))")
    boolean hasActiveEventForApplication(@Param("appId") UUID applicationId);

    @Query("SELECT e FROM AgentAppEventEntity e JOIN AgentApplicationEntity a ON e.applicationId = a.id " +
            "WHERE a.agentId = :agentId AND e.deliveryState = 'PENDING' ORDER BY e.createdTime ASC")
    List<AgentAppEventEntity> findPendingEventsByAgentId(@Param("agentId") UUID agentId);

    @Transactional
    @Modifying
    @Query("UPDATE AgentAppEventEntity e SET e.deliveryState = 'DELIVERED', e.updatedTime = :now " +
            "WHERE e.id = :eventId AND e.deliveryState = 'PENDING'")
    int markDelivered(@Param("eventId") UUID eventId, @Param("now") long now);

    @Transactional
    @Modifying
    @Query("UPDATE AgentAppEventEntity e SET e.status = :status, e.currentStepId = :stepId, e.updatedTime = :now " +
            "WHERE e.id = :eventId")
    void updateStatus(@Param("eventId") UUID eventId, @Param("status") AgentAppEventStatus status,
                      @Param("stepId") String currentStepId, @Param("now") long now);

    @Query("SELECT e FROM AgentAppEventEntity e WHERE e.deliveryState = 'DELIVERED' " +
            "AND (e.status IS NULL OR e.status NOT IN ('FINISHED', 'ERROR')) " +
            "AND e.updatedTime < :before")
    List<AgentAppEventEntity> findStaleDeliveredEvents(@Param("before") long updatedTimeBefore);

    @Transactional
    @Modifying
    @Query("UPDATE AgentAppEventEntity e SET e.deliveryState = 'PENDING', e.status = NULL, e.updatedTime = :now " +
            "WHERE e.id = :eventId")
    void revertToPending(@Param("eventId") UUID eventId, @Param("now") long now);

    @Transactional
    @Modifying
    @Query("DELETE FROM AgentAppEventEntity e WHERE e.applicationId = :appId AND e.deliveryState = 'PENDING'")
    void deleteAllPendingByApplicationId(@Param("appId") UUID applicationId);
}
