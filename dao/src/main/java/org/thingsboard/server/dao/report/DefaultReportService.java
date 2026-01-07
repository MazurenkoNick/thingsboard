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
package org.thingsboard.server.dao.report;

import com.google.common.util.concurrent.FluentFuture;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.ReportId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportInfo;
import org.thingsboard.server.common.data.report.ReportInfoQuery;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.dao.service.validator.ReportDataValidator;

import java.util.List;
import java.util.Optional;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultReportService extends AbstractEntityService implements ReportService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_REPORT_ID = "Incorrect reportId ";

    private final ReportDao reportDao;
    private final ReportDataValidator reportDataValidator;

    @Transactional
    @Override
    public Report createReport(Report report, byte[] data) {
        if (report.getId() != null) {
            throw new IllegalArgumentException("Report can't be updated");
        }
        ConstraintValidator.validateFields(report);
        reportDataValidator.validateReportSize(report.getTenantId(), data);

        report = reportDao.save(report.getTenantId(), report);
        reportDao.saveData(report.getTenantId(), report.getId(), data);
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(report.getTenantId()).entityId(report.getId())
                .entity(report).created(true).build());
        return report;
    }

    @Override
    public Report findReportById(TenantId tenantId, ReportId reportId) {
        return reportDao.findById(tenantId, reportId.getId());
    }

    @Override
    public byte[] getReportData(TenantId tenantId, ReportId reportId) {
        return reportDao.getData(tenantId, reportId);
    }

    @Override
    public void deleteReport(TenantId tenantId, ReportId reportId) {
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(reportId, id -> INCORRECT_REPORT_ID + id);
        deleteEntity(tenantId, reportId, false);
    }

    @Override
    public PageData<Report> findReportsByTenantId(TenantId tenantId, PageLink pageLink) {
        return reportDao.findByTenantId(tenantId, pageLink);
    }

    @Override
    public PageData<ReportInfo> findReportInfos(TenantId tenantId, ReportInfoQuery query) {
        log.trace("Executing findReportInfos, tenantId [{}]", tenantId);
        return reportDao.findReportInfos(tenantId, query);
    }

    @Override
    public PageData<ReportInfo> findReportInfos(TenantId tenantId, CustomerId customerId, ReportInfoQuery query) {
        log.trace("Executing findReportInfos, tenantId [{}], customerId [{}]", tenantId, customerId);
        return reportDao.findReportInfos(tenantId, customerId, query);
    }

    @Override
    public void deleteReportsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteReportsByTenantId, tenantId [{}]", tenantId);
        reportDao.deleteByTenantId(tenantId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteReportsByTenantId(tenantId);
    }

    @Override
    public void deleteReportsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteReportsByTenantIdAndCustomerId, tenantId [{}], customerId [{}]", tenantId, customerId);
        reportDao.deleteByTenantIdAndCustomerId(tenantId, customerId);
    }

    @Override
    public List<ReportInfo> findReportInfoByIds(TenantId tenantId, List<ReportId> reportIds) {
        log.trace("Executing findReportInfoByIds, reportIds [{}]", reportIds);
        return reportDao.findReportByIds(tenantId, toUUIDs(reportIds));
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(reportDao.findById(tenantId, entityId.getId()));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(reportDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        reportDao.removeById(tenantId, id.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(id).build());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.REPORT;
    }

}
