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
package org.thingsboard.server.dao.sql.scheduler;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.SchedulerEventInfoEntity;
import org.thingsboard.server.dao.model.sql.SchedulerEventWithCustomerInfoEntity;

import java.util.List;
import java.util.UUID;

public interface SchedulerEventInfoRepository extends JpaRepository<SchedulerEventInfoEntity, UUID> {

    @Query("SELECT new org.thingsboard.server.dao.model.sql.SchedulerEventWithCustomerInfoEntity(s, c.title, c.additionalInfo) " +
            "FROM SchedulerEventInfoEntity s " +
            "LEFT JOIN CustomerEntity c on c.id = s.customerId " +
            "WHERE s.id = :schedulerEventId")
    SchedulerEventWithCustomerInfoEntity findSchedulerEventWithCustomerInfoById(@Param("schedulerEventId") UUID schedulerEventId);

    List<SchedulerEventInfoEntity> findSchedulerEventInfoEntitiesByTenantId(UUID tenantId);

    List<SchedulerEventInfoEntity> findSchedulerEventInfoEntitiesByTenantIdAndEnabled(UUID tenantId, boolean enabled);

    @Query("SELECT s.id FROM SchedulerEventInfoEntity s " +
            "WHERE s.tenantId = :tenantId " +
            "AND s.customerId = :customerId")
    List<UUID> findIdsByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                              @Param("customerId") UUID customerId);

    @Query("SELECT s.id FROM SchedulerEventInfoEntity s " +
            "WHERE s.tenantId = :tenantId")
    List<UUID> findIdsByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.SchedulerEventWithCustomerInfoEntity(s, c.title, c.additionalInfo) " +
            "FROM SchedulerEventInfoEntity s " +
            "LEFT JOIN CustomerEntity c on c.id = s.customerId " +
            "WHERE s.tenantId = :tenantId " +
            "AND (:customerId IS NULL OR s.customerId = :customerId) " +
            "AND (:type IS NULL OR s.type = :type) " +
            "AND (:searchText IS NULL OR ilike(s.name, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(s.type, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(c.title, CONCAT('%', :searchText, '%')) = true)")
    Page<SchedulerEventWithCustomerInfoEntity> findByTenantIdAndCustomerIdAndTypeAndSearchText(@Param("tenantId") UUID tenantId,
                                                                                               @Param("customerId") UUID customerId,
                                                                                               @Param("type") String type,
                                                                                               @Param("searchText") String searchText,
                                                                                               Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.SchedulerEventWithCustomerInfoEntity(s, c.title, c.additionalInfo) " +
            "FROM SchedulerEventInfoEntity s " +
            "LEFT JOIN CustomerEntity c on c.id = s.customerId " +
            "JOIN RelationEntity r ON r.toId = s.id AND r.toType = 'SCHEDULER_EVENT' " +
            "WHERE s.tenantId = :tenantId  AND r.relationTypeGroup = 'EDGE' " +
            "AND r.relationType = 'Contains' AND r.fromId = :edgeId AND r.fromType = 'EDGE' " +
            "AND (:customerId IS NULL OR s.customerId = :customerId) " +
            "AND (:type IS NULL OR s.type = :type) " +
            "AND (:searchText IS NULL OR ilike(s.name, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(s.type, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(c.title, CONCAT('%', :searchText, '%')) = true)")
    Page<SchedulerEventWithCustomerInfoEntity> findByTenantIdAndCustomerIdAndTypeAndEdgeIdAndSearchText(@Param("tenantId") UUID tenantId,
                                                                                                        @Param("customerId") UUID customerId,
                                                                                                        @Param("type") String type,
                                                                                                        @Param("edgeId") UUID edgeId,
                                                                                                        @Param("searchText") String searchText,
                                                                                                        Pageable pageable);

    List<SchedulerEventInfoEntity> findSchedulerEventsByTenantIdAndIdIn(UUID tenantId, List<UUID> schedulerEventIds);

    @Query("SELECT sei FROM SchedulerEventInfoEntity sei, RelationEntity re WHERE sei.tenantId = :tenantId " +
            "AND sei.id = re.toId AND re.toType = 'SCHEDULER_EVENT' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND (:searchText IS NULL OR ilike(sei.name, CONCAT('%', :searchText, '%')) = true)")
    Page<SchedulerEventInfoEntity> findByTenantIdAndEdgeId(@Param("tenantId") UUID tenantId,
                                                           @Param("edgeId") UUID edgeId,
                                                           @Param("searchText") String searchText,
                                                           Pageable pageable);

    @Query("SELECT sei FROM SchedulerEventInfoEntity sei, RelationEntity re WHERE sei.tenantId = :tenantId " +
            "AND sei.id = re.toId AND re.toType = 'SCHEDULER_EVENT' AND re.relationTypeGroup = 'EDGE' " +
            "AND sei.customerId = :customerId AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND (:searchText IS NULL OR ilike(sei.name, CONCAT('%', :searchText, '%')) = true)")
    Page<SchedulerEventInfoEntity> findByTenantIdAndEdgeIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                                        @Param("edgeId") UUID edgeId,
                                                                        @Param("customerId") UUID customerId,
                                                                        @Param("searchText") String searchText,
                                                                        Pageable pageable);

}
