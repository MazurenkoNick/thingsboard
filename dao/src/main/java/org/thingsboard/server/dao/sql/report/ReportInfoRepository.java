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
import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.model.sql.ReportInfoEntity;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SUB_CUSTOMERS_QUERY;

@Repository
public interface ReportInfoRepository extends JpaRepository<ReportInfoEntity, UUID> {

    @Query("SELECT ri FROM ReportInfoEntity ri WHERE ri.tenantId = :tenantId " +
            "AND (:reportTemplateId IS NULL OR (ri.templateId = :reportTemplateId)) " +
            "AND (:userId IS NULL OR (ri.userId = :userId)) " +
            "AND (:searchText IS NULL OR ilike(ri.name, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(ri.customerTitle, CONCAT('%', :searchText, '%')) = true)")
    Page<ReportInfoEntity> findTenantReportInfosIncludingCustomers(UUID tenantId, UUID reportTemplateId, UUID userId, String searchText, Pageable pageable);

    @Query("SELECT ri FROM ReportInfoEntity ri WHERE ri.tenantId = :tenantId " +
            "AND (ri.customerId IS NULL OR ri.customerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID) " +
            "AND (:reportTemplateId IS NULL OR (ri.templateId = :reportTemplateId)) " +
            "AND (:userId IS NULL OR (ri.userId = :userId)) " +
            "AND (:searchText IS NULL OR ilike(ri.name, CONCAT('%', :searchText, '%')) = true)")
    Page<ReportInfoEntity> findTenantReportInfos(UUID tenantId, UUID reportTemplateId, UUID userId, String searchText, Pageable pageable);

    @Query(value = "SELECT e.*, e.created_time as createdtime, e.report_template_name as reportTemplateName, e.customer_title as customertitle, e.user_name as username " +
            "FROM (select r.id, r.created_time, r.tenant_id, r.customer_id, c.title as customer_title, r.template_id, r.report_template_name, " +
            "r.format, r.name, r.user_id, r.data, r.user_name from report_info_view r  " +
            "LEFT JOIN customer c on c.id = r.customer_id AND c.id != :customerId) e  " +
            "WHERE" + SUB_CUSTOMERS_QUERY +
            "AND (:reportTemplateId IS NULL OR (e.template_id = :reportTemplateId)) " +
            "AND (:userId IS NULL OR (e.user_id = :userId)) " +
            "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
            "OR e.customer_title ILIKE CONCAT('%', :searchText, '%')) ",
            countQuery = "SELECT count(e.id) FROM scheduled_reports_info_view e " +
                    "WHERE" + SUB_CUSTOMERS_QUERY +
                    "AND (:reportTemplateId IS NULL OR (e.report_template_id = :reportTemplateId))" +
                    "AND (:userId IS NULL OR (e.user_id = :userId))" +
                    "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%')) ",
            nativeQuery = true)
    Page<ReportInfoEntity> findCustomerReportInfosIncludingSubCustomers(UUID tenantId, UUID customerId, UUID reportTemplateId, UUID userId, String searchText, Pageable pageable);

    @Query("SELECT ri FROM ReportInfoEntity ri WHERE ri.tenantId = :tenantId " +
            "AND (ri.customerId = :customerId) " +
            "AND (:reportTemplateId IS NULL OR (ri.templateId = :reportTemplateId)) " +
            "AND (:userId IS NULL OR (ri.userId = :userId)) " +
            "AND (:searchText IS NULL OR ilike(ri.name, CONCAT('%', :searchText, '%')) = true)")
    Page<ReportInfoEntity> findCustomerReportInfos(UUID tenantId, UUID customerId, UUID reportTemplateId, UUID userId, String searchText, Pageable pageable);

    List<ReportInfoEntity> findByIdIn(List<UUID> toUUIDs);

}
