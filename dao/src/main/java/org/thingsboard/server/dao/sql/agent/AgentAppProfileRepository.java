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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.dao.model.sql.AgentAppProfileEntity;
import org.thingsboard.server.dao.model.sql.AgentAppProfileInfoEntity;
import org.thingsboard.server.dao.model.sql.AgentAppProfileRelationInfoEntity;

import java.util.List;
import java.util.UUID;

public interface AgentAppProfileRepository extends JpaRepository<AgentAppProfileEntity, UUID> {

    @Query("SELECT new org.thingsboard.server.dao.model.sql.AgentAppProfileInfoEntity(p, t.currentVersion) " +
            "FROM AgentAppProfileEntity p " +
            "LEFT JOIN AgentAppTemplateEntity t ON p.templateId = t.id " +
            "WHERE p.id = :profileId")
    AgentAppProfileInfoEntity findInfoById(@Param("profileId") UUID profileId);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.AgentAppProfileInfoEntity(p, t.currentVersion) " +
            "FROM AgentAppProfileEntity p " +
            "LEFT JOIN AgentAppTemplateEntity t ON p.templateId = t.id " +
            "WHERE p.tenantId = :tenantId AND p.appType = :appType " +
            "ORDER BY p.name ASC")
    List<AgentAppProfileInfoEntity> findInfosByTenantIdAndAppType(@Param("tenantId") UUID tenantId,
                                                                  @Param("appType") AgentApplicationType appType);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.AgentAppProfileRelationInfoEntity(p, t.currentVersion, r.fromId, " +
            "(SELECT COUNT(a) FROM AgentApplicationEntity a " +
            " JOIN AgentEntity ag ON ag.id = a.agentId " +
            " WHERE a.applicationProfileId = p.id AND ag.agentProfileId = :agentProfileId), r.additionalInfo) " +
            "FROM AgentAppProfileEntity p " +
            "JOIN RelationEntity r ON r.toId = p.id " +
            "LEFT JOIN AgentAppTemplateEntity t ON p.templateId = t.id " +
            "WHERE r.fromId = :agentProfileId " +
            "AND r.fromType = 'AGENT_PROFILE' " +
            "AND r.relationTypeGroup = 'AGENT' " +
            "AND r.relationType = :relationType " +
            "ORDER BY p.name ASC")
    List<AgentAppProfileRelationInfoEntity> findRelationInfosByAgentProfileId(@Param("agentProfileId") UUID agentProfileId,
                                                                              @Param("relationType") String relationType);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.AgentAppProfileRelationInfoEntity(p, t.currentVersion, r.fromId, " +
            "(SELECT COUNT(a) FROM AgentApplicationEntity a " +
            " JOIN AgentEntity ag ON ag.id = a.agentId " +
            " WHERE a.applicationProfileId = p.id AND ag.agentProfileId = :agentProfileId), r.additionalInfo) " +
            "FROM AgentAppProfileEntity p " +
            "JOIN RelationEntity r ON r.toId = p.id " +
            "LEFT JOIN AgentAppTemplateEntity t ON p.templateId = t.id " +
            "WHERE r.fromId = :agentProfileId " +
            "AND r.fromType = 'AGENT_PROFILE' " +
            "AND r.relationTypeGroup = 'AGENT' " +
            "AND r.relationType = :relationType " +
            "AND p.appType = :appType " +
            "AND p.templateId = :templateId " +
            "ORDER BY p.name ASC")
    List<AgentAppProfileRelationInfoEntity> findRelationInfosByAgentProfileIdAndAppTypeAndTemplateId(@Param("agentProfileId") UUID agentProfileId,
                                                                                                     @Param("appType") AgentApplicationType appType,
                                                                                                     @Param("templateId") UUID templateId,
                                                                                                     @Param("relationType") String relationType);

    @Query("SELECT p FROM AgentAppProfileEntity p WHERE p.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(p.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AgentAppProfileEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                               @Param("textSearch") String textSearch,
                                               Pageable pageable);

    @Query("SELECT count(*) FROM AgentAppProfileEntity p WHERE p.tenantId = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT p FROM AgentAppProfileEntity p " +
            "JOIN RelationEntity r ON r.toId = p.id " +
            "WHERE r.fromId = :agentProfileId " +
            "AND r.fromType = 'AGENT_PROFILE' " +
            "AND r.relationTypeGroup = 'AGENT' " +
            "AND r.relationType = 'HasProfile' " +
            "AND NOT EXISTS (SELECT 1 FROM AgentApplicationEntity a " +
            "                WHERE a.agentId = :agentId " +
            "                AND ((p.appType = org.thingsboard.server.common.data.agent.AgentApplicationType.GENERIC " +
            "                      AND a.applicationProfileId = p.id) " +
            "                  OR (p.appType <> org.thingsboard.server.common.data.agent.AgentApplicationType.GENERIC " +
            "                      AND (a.applicationProfileId = p.id OR a.templateId = p.templateId))))")
    List<AgentAppProfileEntity> findUninstalledAppProfilesForAgentProfile(@Param("agentProfileId") UUID agentProfileId,
                                                                          @Param("agentId") UUID agentId);
}
