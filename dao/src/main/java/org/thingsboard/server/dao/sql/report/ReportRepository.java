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
package org.thingsboard.server.dao.sql.report;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.edqs.fields.ReportFields;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.model.sql.ReportEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<ReportEntity, UUID> {

    @Query("SELECT r FROM ReportEntity r WHERE r.tenantId = :tenantId " +
           "AND (:searchText IS NULL OR ilike(r.name, CONCAT('%', :searchText, '%')) = true)")
    Page<ReportEntity> findByTenantIdAndSearchText(@Param("tenantId") UUID tenantId,
                                                   @Param("searchText") String searchText,
                                                   Pageable pageable);

    @Query("SELECT r FROM ReportEntity r WHERE r.tenantId = :tenantId AND r.customerId = :customerId " +
           "AND (:searchText IS NULL OR ilike(r.name, CONCAT('%', :searchText, '%')) = true)")
    Page<ReportEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                   @Param("customerId") UUID customerId,
                                                   Pageable pageable);

    @Modifying
    @Query(value = "UPDATE report SET data = :data WHERE id = :id", nativeQuery = true)
    void saveData(UUID id, byte[] data);

    @Query(value = "SELECT data FROM report WHERE id = :id", nativeQuery = true)
    byte[] getDataById(UUID id);

    @Transactional
    @Modifying
    @Query("DELETE FROM ReportEntity r WHERE r.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);

    @Transactional
    @Modifying
    @Query("DELETE FROM ReportEntity r WHERE r.tenantId = :tenantId AND r.customerId = :customerId")
    void deleteByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId, @Param("customerId") UUID customerId);

    @Query("SELECT new org.thingsboard.server.common.data.util.TbPair(r.format, COUNT(*)) FROM ReportEntity r GROUP BY r.format")
    List<TbPair<TbReportFormat, Long>> countReportsByFormatType();

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.ReportFields(r.id, r.createdTime, r.tenantId, " +
           "r.customerId, r.name, r.format) FROM ReportEntity r WHERE r.id > :id ORDER BY r.id")
    List<ReportFields> findNextBatch(@Param("id") UUID id, Limit limit);

}
