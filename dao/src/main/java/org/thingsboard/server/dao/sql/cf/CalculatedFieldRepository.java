/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.cf;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.dao.model.sql.CalculatedFieldEntity;

import java.util.List;
import java.util.UUID;

public interface CalculatedFieldRepository extends JpaRepository<CalculatedFieldEntity, UUID> {

    boolean existsByTenantIdAndEntityId(UUID tenantId, UUID entityId);

    CalculatedFieldEntity findByEntityIdAndTypeAndName(UUID entityId, String type, String name);

    List<CalculatedFieldId> findCalculatedFieldIdsByTenantIdAndEntityId(UUID tenantId, UUID entityId);

    List<CalculatedFieldEntity> findAllByTenantIdAndEntityId(UUID tenantId, UUID entityId);

    Page<CalculatedFieldEntity> findAllByTenantId(UUID tenantId, Pageable pageable);

    @Query("SELECT cf FROM CalculatedFieldEntity cf WHERE cf.tenantId = :tenantId " +
           "AND cf.entityId = :entityId AND cf.type IN :types " +
           "AND (:textSearch IS NULL OR ilike(cf.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<CalculatedFieldEntity> findByTenantIdAndEntityIdAndTypes(UUID tenantId, UUID entityId, List<String> types, String textSearch, Pageable pageable);

    @Query("SELECT cf FROM CalculatedFieldEntity cf WHERE cf.tenantId = :tenantId " +
           "AND cf.type = :type " +
           "AND cf.entityType IN :entityTypes " +
           "AND (:entityIds IS NULL OR cf.entityId IN :entityIds) " +
           "AND (:names IS NULL OR cf.name IN :names) " +
           "AND (:textSearch IS NULL OR ilike(cf.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<CalculatedFieldEntity> findByTenantIdAndFilter(UUID tenantId, String type, List<String> entityTypes,
                                                        List<UUID> entityIds, List<String> names, String textSearch, Pageable pageable);

    List<CalculatedFieldEntity> findAllByTenantId(UUID tenantId);

    List<CalculatedFieldEntity> removeAllByTenantIdAndEntityId(UUID tenantId, UUID entityId);

    long countByTenantIdAndEntityIdAndTypeNot(UUID tenantId, UUID entityId, String type);

}
