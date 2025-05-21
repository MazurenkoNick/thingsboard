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
package org.thingsboard.server.dao.sql.report;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.ReportId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.ReportEntity;
import org.thingsboard.server.dao.report.ReportDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Slf4j
@Component
@AllArgsConstructor
@SqlDao
public class JpaReportDao extends JpaAbstractDao<ReportEntity, Report> implements ReportDao {

    private final ReportRepository reportRepository;

    @Override
    public void saveData(TenantId tenantId, ReportId reportId, byte[] data) {
        reportRepository.saveData(reportId.getId(), data);
    }

    @Override
    public byte[] getData(TenantId tenantId, ReportId reportId) {
        return reportRepository.getDataById(reportId.getId());
    }

    @Override
    public PageData<Report> findByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(reportRepository.findByTenantIdAndSearchText(tenantId.getId(),
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    protected Class<ReportEntity> getEntityClass() {
        return ReportEntity.class;
    }

    @Override
    protected JpaRepository<ReportEntity, UUID> getRepository() {
        return reportRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.REPORT;
    }
}
