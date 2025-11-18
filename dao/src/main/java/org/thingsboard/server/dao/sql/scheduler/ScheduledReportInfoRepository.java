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
package org.thingsboard.server.dao.sql.scheduler;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.ReportTemplateInfoEntity;
import org.thingsboard.server.dao.model.sql.ScheduledReportInfoEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SUB_CUSTOMERS_QUERY;

public interface ScheduledReportInfoRepository extends JpaRepository<ScheduledReportInfoEntity, UUID> {

    @Query("SELECT sei FROM ScheduledReportInfoEntity sei WHERE sei.tenantId = :tenantId " +
            "AND (sei.customerId IS NULL OR sei.customerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID) " +
            "AND (:reportTemplateId IS NULL OR (sei.reportTemplateId = :reportTemplateId)) " +
            "AND (:userId IS NULL OR (sei.userId = :userId)) " +
            "AND (:searchText IS NULL OR ilike(sei.name, CONCAT('%', :searchText, '%')) = true)")
    Page<ScheduledReportInfoEntity> findTenantScheduledReportInfos(@Param("tenantId") UUID tenantId,
                                                                   @Param("reportTemplateId") UUID reportTemplateId,
                                                                   @Param("userId") UUID userId,
                                                                   @Param("searchText") String searchText,
                                                                   Pageable pageable);

    @Query("SELECT sei FROM ScheduledReportInfoEntity sei WHERE sei.tenantId = :tenantId " +
            "AND (:reportTemplateId IS NULL OR (sei.reportTemplateId = :reportTemplateId)) " +
            "AND (:userId IS NULL OR (sei.userId = :userId)) " +
            "AND (:searchText IS NULL OR ilike(sei.name, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(sei.customerTitle, CONCAT('%', :searchText, '%')) = true)")
    Page<ScheduledReportInfoEntity> findTenantScheduledReportInfosIncludingCustomers(@Param("tenantId") UUID tenantId,
                                                                                     @Param("reportTemplateId") UUID reportTemplateId,
                                                                                     @Param("userId") UUID userId,
                                                                                     @Param("searchText") String searchText,
                                                                                     Pageable pageable);

    @Query(value = "SELECT e.*, e.created_time as createdtime, e.customer_title as customertitle, " +
            "e.report_template_id as reporttemplateid, e.report_template_name as reporttemplatename, " +
            "e.user_id as userid, e.user_name as username " +
            "FROM (select s.id, s.created_time, s.tenant_id, s.customer_id, c.title as customer_title, " +
            "s.report_template_id, s.report_template_name, s.user_id, s.user_name, s.\"name\", " +
            "s.originator_id, s.originator_type, s.type, s.additional_info, " +
            "s.schedule, s.enabled, s.version, s.external_id from scheduled_reports_info_view s " +
            "LEFT JOIN customer c on c.id = s.customer_id AND c.id != :customerId) e " +
            "WHERE" + SUB_CUSTOMERS_QUERY +
            "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
            "  OR e.customer_title ILIKE CONCAT('%', :searchText, '%')) " +
            "AND (:reportTemplateId IS NULL OR (e.report_template_id = :reportTemplateId))" +
            "AND (:userId IS NULL OR (e.user_id = :userId))",
            countQuery = "SELECT count(e.id) FROM scheduled_reports_info_view e " +
                    "LEFT JOIN customer c on c.id = e.customer_id AND c.id != :customerId " +
                    "WHERE" + SUB_CUSTOMERS_QUERY +
                    "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
                    "  OR c.title ILIKE CONCAT('%', :searchText, '%')) " +
                    "AND (:reportTemplateId IS NULL OR (e.report_template_id = :reportTemplateId))" +
                    "AND (:userId IS NULL OR (e.user_id = :userId))",
            nativeQuery = true)
    Page<ScheduledReportInfoEntity> findCustomerScheduledReportsIncludingSubCustomers(@Param("tenantId") UUID tenantId,
                                                                                      @Param("customerId") UUID customerId,
                                                                                      @Param("reportTemplateId") UUID reportTemplateId,
                                                                                      @Param("userId") UUID userId,
                                                                                      @Param("searchText") String searchText,
                                                                                      Pageable pageable);

    @Query("SELECT sei FROM ScheduledReportInfoEntity sei WHERE sei.tenantId = :tenantId " +
            "AND (sei.customerId = :customerId) " +
            "AND (:reportTemplateId IS NULL OR (sei.reportTemplateId = :reportTemplateId)) " +
            "AND (:userId IS NULL OR (sei.userId = :userId)) " +
            "AND (:searchText IS NULL OR ilike(sei.name, CONCAT('%', :searchText, '%')) = true)")
    Page<ScheduledReportInfoEntity> findCustomerScheduledReports(@Param("tenantId") UUID tenantId,
                                                                 @Param("customerId") UUID customerId,
                                                                 @Param("reportTemplateId") UUID reportTemplateId,
                                                                 @Param("userId") UUID userId,
                                                                 @Param("searchText") String searchText,
                                                                 Pageable pageable);

    @Query("SELECT count(sei) FROM ScheduledReportInfoEntity sei WHERE sei.tenantId = :tenantId " +
            "AND sei.reportTemplateId = :reportTemplateId")
    int countScheduledReportEventsByTemplateId(@Param("tenantId") UUID tenantId, @Param("reportTemplateId") UUID reportTemplateId);

}
