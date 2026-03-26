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
import org.thingsboard.server.dao.model.sql.AgentApplicationEntity;
import org.thingsboard.server.dao.model.sql.AgentApplicationInfoEntity;

import java.util.List;
import java.util.UUID;

public interface AgentApplicationRepository extends JpaRepository<AgentApplicationEntity, UUID> {

    List<AgentApplicationEntity> findByAgentId(UUID agentId);

    @Query("SELECT e FROM AgentApplicationEntity e WHERE e.agentId = :agentId " +
            "AND (:textSearch IS NULL OR ilike(e.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AgentApplicationEntity> findByAgentId(@Param("agentId") UUID agentId,
                                               @Param("textSearch") String textSearch,
                                               Pageable pageable);

    List<AgentApplicationEntity> findByTemplateId(UUID templateId);

    @Transactional
    @Modifying
    @Query("DELETE FROM AgentApplicationEntity e WHERE e.agentId = :agentId")
    void deleteByAgentId(@Param("agentId") UUID agentId);

    @Transactional
    @Modifying
    @Query("DELETE FROM AgentApplicationEntity e WHERE e.templateId = :templateId")
    void deleteByTemplateId(@Param("templateId") UUID templateId);

    AgentApplicationEntity findByProjectName(String projectName);

    @Query("SELECT app FROM AgentApplicationEntity app JOIN AgentAppEventEntity evt ON app.id = evt.applicationId WHERE evt.id = :eventId")
    AgentApplicationEntity findByEventId(@Param("eventId") UUID eventId);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.AgentApplicationInfoEntity(a, t.currentVersion, t.nextVersion) " +
            "FROM AgentApplicationEntity a " +
            "LEFT JOIN AgentAppTemplateEntity t ON a.templateId = t.id " +
            "WHERE a.id = :id")
    AgentApplicationInfoEntity findInfoById(@Param("id") UUID id);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.AgentApplicationInfoEntity(a, t.currentVersion, t.nextVersion) " +
            "FROM AgentApplicationEntity a " +
            "LEFT JOIN AgentAppTemplateEntity t ON a.templateId = t.id " +
            "WHERE a.agentId = :agentId " +
            "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AgentApplicationInfoEntity> findInfosByAgentId(@Param("agentId") UUID agentId,
                                                        @Param("textSearch") String textSearch,
                                                        Pageable pageable);

    @Query("SELECT a FROM AgentApplicationEntity a " +
            "JOIN AgentEntity ag ON a.agentId = ag.id " +
            "WHERE a.applicationProfileId = :profileId AND ag.agentGroupId = :groupId")
    Page<AgentApplicationEntity> findByApplicationProfileIdAndAgentGroupId(@Param("profileId") UUID profileId,
                                                                           @Param("groupId") UUID groupId,
                                                                           Pageable pageable);

}
