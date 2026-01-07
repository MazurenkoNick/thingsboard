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
package org.thingsboard.server.dao.sql.ai;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.AiModelEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

interface AiModelRepository extends JpaRepository<AiModelEntity, UUID>, ExportableEntityRepository<AiModelEntity> {

    Optional<AiModelEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<AiModelEntity> findByTenantIdAndName(UUID tenantId, String name);

    @Query(
            value = """
                    SELECT *
                    FROM ai_model model
                    WHERE model.tenant_id = :tenantId
                      AND (:textSearch IS NULL
                        OR model.name ILIKE '%' || :textSearch || '%'
                        OR REPLACE(model.configuration ->> 'provider', '_', ' ') ILIKE '%' || :textSearch || '%'
                        OR model.configuration ->> 'modelId' ILIKE '%' || :textSearch || '%')
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM ai_model model
                    WHERE model.tenant_id = :tenantId
                      AND (:textSearch IS NULL
                        OR model.name ILIKE '%' || :textSearch || '%'
                        OR REPLACE(model.configuration ->> 'provider', '_', ' ') ILIKE '%' || :textSearch || '%'
                        OR (model.configuration ->> 'modelId') ILIKE '%' || :textSearch || '%')
                    """,
            nativeQuery = true
    )
    Page<AiModelEntity> findByTenantId(@Param("tenantId") UUID tenantId, @Param("textSearch") String textSearch, Pageable pageable);

    @Query("SELECT ai_model.id FROM AiModelEntity ai_model WHERE ai_model.tenantId = :tenantId")
    Page<UUID> findIdsByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT externalId FROM AiModelEntity WHERE id = :id")
    Optional<UUID> getExternalIdById(@Param("id") UUID id);

    long countByTenantId(UUID tenantId);

    @Transactional
    @Modifying
    @Query("DELETE FROM AiModelEntity ai_model WHERE ai_model.id IN (:ids)")
    int deleteByIdIn(@Param("ids") Set<UUID> ids);

    @Transactional
    @Modifying
    @Query(value = """
                DELETE FROM ai_model
                WHERE tenant_id = :tenantId
                RETURNING id
            """, nativeQuery = true
    )
    Set<UUID> deleteByTenantId(@Param("tenantId") UUID tenantId);

    @Transactional
    @Modifying
    @Query("DELETE FROM AiModelEntity ai_model WHERE ai_model.tenantId = :tenantId AND ai_model.id IN (:ids)")
    int deleteByTenantIdAndIdIn(@Param("tenantId") UUID tenantId, @Param("ids") Set<UUID> ids);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(m.id, 'AI_MODEL', m.name) " +
           "FROM AiModelEntity m WHERE m.tenantId = :tenantId AND ilike(m.configuration, CONCAT('%', :placeholder, '%'))")
    List<EntityInfo> findByTenantIdAndSecretPlaceholder(@Param("tenantId") UUID tenantId,
                                                        @Param("placeholder") String placeholder);

}
