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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.AgentAppProfileEntity;

import java.util.List;
import java.util.UUID;

public interface AgentAppProfileRepository extends JpaRepository<AgentAppProfileEntity, UUID> {

    @Query("SELECT p FROM AgentAppProfileEntity p WHERE p.tenantId = :tenantId AND p.appType = :appType ORDER BY p.name ASC")
    List<AgentAppProfileEntity> findByTenantIdAndAppType(@Param("tenantId") UUID tenantId,
                                                          @Param("appType") org.thingsboard.server.common.data.agent.AgentApplicationType appType);

    @Query("SELECT p FROM AgentAppProfileEntity p WHERE p.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(p.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AgentAppProfileEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                                        @Param("textSearch") String textSearch,
                                                        Pageable pageable);

    @Query("SELECT count(*) FROM AgentAppProfileEntity p WHERE p.tenantId = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT p FROM AgentAppProfileEntity p " +
            "JOIN RelationEntity r ON r.toId = p.id " +
            "WHERE r.fromId = :groupId " +
            "AND r.fromType = 'AGENT_GROUP' " +
            "AND r.relationTypeGroup = 'AGENT' " +
            "AND r.relationType = 'HasProfile' " +
            "AND NOT EXISTS (SELECT 1 FROM AgentApplicationEntity a " +
            "                WHERE a.agentId = :agentId AND a.applicationProfileId = p.id)")
    java.util.List<AgentAppProfileEntity> findUninstalledProfilesForAgentInGroup(@Param("groupId") UUID groupId,
                                                                                 @Param("agentId") UUID agentId);
}
