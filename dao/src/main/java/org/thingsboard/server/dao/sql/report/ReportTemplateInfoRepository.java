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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.dao.model.sql.ReportTemplateInfoEntity;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SUB_CUSTOMERS_QUERY;

public interface ReportTemplateInfoRepository extends JpaRepository<ReportTemplateInfoEntity, UUID> {

    @Query("SELECT ri FROM ReportTemplateInfoEntity ri " +
            "WHERE ri.tenantId = :tenantId AND (ri.customerId IS NULL OR ri.customerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID) " +
            "AND (:searchText IS NULL OR ilike(ri.name, CONCAT('%', :searchText, '%')) = true) " +
            "AND ((:#{#reportTemplateTypes == null} = true) OR ri.type IN (:reportTemplateTypes)) " +
            "AND ((:#{#reportTemplateFormats == null} = true) OR ri.format IN (:reportTemplateFormats))")
    Page<ReportTemplateInfoEntity> findTenantReportTemplates(@Param("tenantId") UUID tenantId,
                                                             @Param("searchText") String searchText,
                                                             @Param("reportTemplateTypes") List<ReportTemplateType> reportTemplateTypes,
                                                             @Param("reportTemplateFormats") List<TbReportFormat> reportTemplateFormats,
                                                             Pageable pageable);

    @Query("SELECT ri FROM ReportTemplateInfoEntity ri " +
            "WHERE ri.tenantId = :tenantId " +
            "AND (:searchText IS NULL OR ilike(ri.name, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(ri.ownerName, CONCAT('%', :searchText, '%')) = true) " +
            "AND ((:#{#reportTemplateTypes == null} = true) OR ri.type IN (:reportTemplateTypes)) " +
            "AND ((:#{#reportTemplateFormats == null} = true) OR ri.format IN (:reportTemplateFormats))")
    Page<ReportTemplateInfoEntity> findTenantReportTemplatesIncludingCustomers(@Param("tenantId") UUID tenantId,
                                                                               @Param("searchText") String searchText,
                                                                               @Param("reportTemplateTypes") List<ReportTemplateType> reportTemplateTypes,
                                                                               @Param("reportTemplateFormats") List<TbReportFormat> reportTemplateFormats,
                                                                               Pageable pageable);



    @Query("SELECT ri FROM ReportTemplateInfoEntity ri " +
            "WHERE ri.tenantId = :tenantId AND ri.customerId = :customerId " +
            "AND (:searchText IS NULL OR ilike(ri.name, CONCAT('%', :searchText, '%')) = true) " +
            "AND ((:#{#reportTemplateTypes == null} = true) OR ri.type IN (:reportTemplateTypes)) " +
            "AND ((:#{#reportTemplateFormats == null} = true) OR ri.format IN (:reportTemplateFormats))")
    Page<ReportTemplateInfoEntity> findCustomerReportTemplates(@Param("tenantId") UUID tenantId,
                                                               @Param("customerId") UUID customerId,
                                                               @Param("searchText") String searchText,
                                                               @Param("reportTemplateTypes") List<ReportTemplateType> reportTemplateTypes,
                                                               @Param("reportTemplateFormats") List<TbReportFormat> reportTemplateFormats,
                                                               Pageable pageable);

    @Query(value = "SELECT e.*, e.owner_name as ownername, e.created_time as createdtime " +
            "FROM (select r.id, r.created_time, r.customer_id, r.\"name\", r.format, r.type, r.description, " +
            "r.tenant_id, r.external_id, r.version, " +
            "c.title as owner_name from report_template_info_view r " +
            "LEFT JOIN customer c on c.id = r.customer_id AND c.id != :customerId) e " +
            "WHERE" + SUB_CUSTOMERS_QUERY +
            "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
            "  OR e.owner_name ILIKE CONCAT('%', :searchText, '%')) " +
            "AND (COALESCE(:reportTemplateTypes) IS NULL OR e.type IN (:reportTemplateTypes)) " +
            "AND (COALESCE(:reportTemplateFormats) IS NULL OR e.format IN (:reportTemplateFormats))",
            countQuery = "SELECT count(e.id) FROM report_template e " +
                    "LEFT JOIN customer c on c.id = e.customer_id AND c.id != :customerId " +
                    "WHERE" + SUB_CUSTOMERS_QUERY +
                    "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
                    "  OR c.title ILIKE CONCAT('%', :searchText, '%')) " +
                    "AND (COALESCE(:reportTemplateTypes) IS NULL OR e.type IN (:reportTemplateTypes)) " +
                    "AND (COALESCE(:reportTemplateFormats) IS NULL OR e.format IN (:reportTemplateFormats))",
            nativeQuery = true)
    Page<ReportTemplateInfoEntity> findCustomerReportTemplatesIncludingSubCustomers(@Param("tenantId") UUID tenantId,
                                                                                    @Param("customerId") UUID customerId,
                                                                                    @Param("searchText") String searchText,
                                                                                    @Param("reportTemplateTypes") List<String> reportTemplateTypes,
                                                                                    @Param("reportTemplateFormats") List<String> reportTemplateFormats,
                                                                                    Pageable pageable);

    List<ReportTemplateInfoEntity> findByIdIn(List<UUID> reportTemplateIds);
}
