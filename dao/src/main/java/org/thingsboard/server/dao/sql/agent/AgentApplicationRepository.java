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

    @Query(value = "SELECT * FROM agent_application a WHERE a.id = :id FOR UPDATE NOWAIT", nativeQuery = true)
    AgentApplicationEntity findByIdForUpdate(@Param("id") UUID id);

    List<AgentApplicationEntity> findByTenantIdAndAgentId(UUID tenantId, UUID agentId);

    @Query("SELECT e FROM AgentApplicationEntity e WHERE e.tenantId = :tenantId AND e.agentId = :agentId " +
            "AND (:textSearch IS NULL OR ilike(e.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AgentApplicationEntity> findByAgentId(@Param("tenantId") UUID tenantId,
                                               @Param("agentId") UUID agentId,
                                               @Param("textSearch") String textSearch,
                                               Pageable pageable);

    List<AgentApplicationEntity> findByTemplateId(UUID templateId);

    @Transactional
    @Modifying
    @Query("DELETE FROM AgentApplicationEntity e WHERE e.tenantId = :tenantId AND e.agentId = :agentId")
    void deleteByAgentId(@Param("tenantId") UUID tenantId, @Param("agentId") UUID agentId);

    @Transactional
    @Modifying
    @Query("DELETE FROM AgentApplicationEntity e WHERE e.templateId = :templateId")
    void deleteByTemplateId(@Param("templateId") UUID templateId);

    @Transactional
    @Modifying
    @Query("UPDATE AgentApplicationEntity e SET e.templateId = :templateId, e.desiredTemplateId = null WHERE e.id = :id AND e.tenantId = :tenantId")
    int promoteDesiredTemplate(@Param("tenantId") UUID tenantId, @Param("id") UUID id, @Param("templateId") UUID templateId);

    AgentApplicationEntity findByTenantIdAndAgentIdAndProjectName(UUID tenantId, UUID agentId, String projectName);

    @Query("SELECT app FROM AgentApplicationEntity app JOIN AgentAppEventEntity evt ON app.id = evt.applicationId WHERE evt.id = :eventId AND app.tenantId = :tenantId")
    AgentApplicationEntity findByEventId(@Param("tenantId") UUID tenantId, @Param("eventId") UUID eventId);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.AgentApplicationInfoEntity(a, t.currentVersion, t.nextVersion, p.version, p.name, re.fromId, re.fromType) " +
            "FROM AgentApplicationEntity a " +
            "LEFT JOIN AgentAppTemplateEntity t ON a.templateId = t.id " +
            "LEFT JOIN AgentAppProfileEntity p ON a.applicationProfileId = p.id " +
            "LEFT JOIN RelationEntity re ON re.toId = a.id AND re.toType = 'AGENT_APPLICATION' " +
            "    AND re.relationTypeGroup = 'AGENT' AND re.relationType = 'ManagedByAgentApp' " +
            "WHERE a.id = :id AND a.tenantId = :tenantId")
    AgentApplicationInfoEntity findInfoById(@Param("tenantId") UUID tenantId, @Param("id") UUID id);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.AgentApplicationInfoEntity(a, t.currentVersion, t.nextVersion, p.version, p.name, re.fromId, re.fromType) " +
            "FROM AgentApplicationEntity a " +
            "LEFT JOIN AgentAppTemplateEntity t ON a.templateId = t.id " +
            "LEFT JOIN AgentAppProfileEntity p ON a.applicationProfileId = p.id " +
            "LEFT JOIN RelationEntity re ON re.toId = a.id AND re.toType = 'AGENT_APPLICATION' " +
            "    AND re.relationTypeGroup = 'AGENT' AND re.relationType = 'ManagedByAgentApp' " +
            "WHERE a.tenantId = :tenantId AND a.agentId = :agentId " +
            "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AgentApplicationInfoEntity> findInfosByAgentId(@Param("tenantId") UUID tenantId,
                                                        @Param("agentId") UUID agentId,
                                                        @Param("textSearch") String textSearch,
                                                        Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.AgentApplicationInfoEntity(a, t.currentVersion, t.nextVersion, p.version, p.name, ag.name, re.fromId, re.fromType) " +
            "FROM AgentApplicationEntity a " +
            "JOIN AgentEntity ag ON a.agentId = ag.id " +
            "LEFT JOIN AgentAppTemplateEntity t ON a.templateId = t.id " +
            "LEFT JOIN AgentAppProfileEntity p ON a.applicationProfileId = p.id " +
            "LEFT JOIN RelationEntity re ON re.toId = a.id AND re.toType = 'AGENT_APPLICATION' " +
            "    AND re.relationTypeGroup = 'AGENT' AND re.relationType = 'ManagedByAgentApp' " +
            "WHERE a.tenantId = :tenantId AND a.applicationProfileId = :applicationProfileId AND ag.agentProfileId = :agentProfileId")
    Page<AgentApplicationInfoEntity> findByApplicationProfileIdAndAgentProfileId(@Param("tenantId") UUID tenantId,
                                                                                @Param("applicationProfileId") UUID applicationProfileId,
                                                                                @Param("agentProfileId") UUID agentProfileId,
                                                                                Pageable pageable);


    @Query("SELECT a FROM AgentApplicationEntity a, RelationEntity re " +
            "WHERE a.id = re.toId AND re.toType = 'AGENT_APPLICATION' " +
            "AND re.relationTypeGroup = 'AGENT' " +
            "AND re.relationType = 'ManagedByAgentApp' " +
            "AND re.fromId = :relatedEntityId AND a.tenantId = :tenantId")
    AgentApplicationEntity findByRelatedEntityId(@Param("tenantId") UUID tenantId, @Param("relatedEntityId") UUID relatedEntityId);

    @Query("SELECT re.fromId FROM RelationEntity re, AgentApplicationEntity a " +
            "WHERE a.id = re.toId AND re.toType = 'AGENT_APPLICATION' " +
            "AND re.relationTypeGroup = 'AGENT' AND re.relationType = 'ManagedByAgentApp' " +
            "AND re.fromType = :relatedEntityType AND a.tenantId = :tenantId")
    List<UUID> findManagedRelatedEntityIds(@Param("tenantId") UUID tenantId,
                                           @Param("relatedEntityType") String relatedEntityType);

    @Query("SELECT a FROM AgentApplicationEntity a, RelationEntity re " +
            "WHERE a.id = re.toId AND re.toType = 'AGENT_APPLICATION' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
            "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AgentApplicationEntity> findByEntityGroupId(@Param("groupId") UUID groupId,
                                                     @Param("textSearch") String textSearch,
                                                     Pageable pageable);

    @Query("SELECT a FROM AgentApplicationEntity a, RelationEntity re " +
            "WHERE a.id = re.toId AND re.toType = 'AGENT_APPLICATION' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId IN :groupIds AND re.fromType = 'ENTITY_GROUP' " +
            "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AgentApplicationEntity> findByEntityGroupIds(@Param("groupIds") List<UUID> groupIds,
                                                      @Param("textSearch") String textSearch,
                                                      Pageable pageable);

}
