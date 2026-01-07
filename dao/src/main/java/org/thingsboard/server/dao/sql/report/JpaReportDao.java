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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.ReportFields;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportInfo;
import org.thingsboard.server.common.data.report.ReportInfoQuery;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.ReportEntity;
import org.thingsboard.server.dao.report.ReportDao;
import org.thingsboard.server.dao.sql.JpaPartitionedAbstractDao;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
@SqlDao
public class JpaReportDao extends JpaPartitionedAbstractDao<ReportEntity, Report> implements ReportDao {

    private final ReportRepository reportRepository;
    private final ReportInfoRepository reportInfoRepository;
    private final SqlPartitioningRepository partitioningRepository;

    @Value("${sql.reports.partition_size:168}")
    private int partitionSizeInHours;

    private static final String TABLE_NAME = ModelConstants.REPORT_TABLE_NAME;

    public JpaReportDao(ReportRepository reportRepository, ReportInfoRepository reportInfoRepository, SqlPartitioningRepository partitioningRepository) {
        this.reportRepository = reportRepository;
        this.reportInfoRepository = reportInfoRepository;
        this.partitioningRepository = partitioningRepository;
    }

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
    public PageData<ReportInfo> findReportInfos(TenantId tenantId, ReportInfoQuery query) {
        if (query.isIncludeCustomers()) {
            return DaoUtil.toPageData(reportInfoRepository
                    .findTenantReportInfosIncludingCustomers(
                            tenantId.getId(),
                            query.getReportTemplateId(),
                            query.getUserId(),
                            Objects.toString(query.getPageLink().getTextSearch(), ""),
                            DaoUtil.toPageable(query.getPageLink())));
        } else {
            return DaoUtil.toPageData(reportInfoRepository
                    .findTenantReportInfos(
                            tenantId.getId(),
                            query.getReportTemplateId(),
                            query.getUserId(),
                            Objects.toString(query.getPageLink().getTextSearch(), ""),
                            DaoUtil.toPageable(query.getPageLink())));
        }
    }

    @Override
    public PageData<ReportInfo> findReportInfos(TenantId tenantId, CustomerId customerId, ReportInfoQuery query) {
        if (query.isIncludeCustomers()) {
            return DaoUtil.toPageData(reportInfoRepository
                    .findCustomerReportInfosIncludingSubCustomers(
                            tenantId.getId(),
                            customerId.getId(),
                            query.getReportTemplateId(),
                            query.getUserId(),
                            Objects.toString(query.getPageLink().getTextSearch(), ""),
                            DaoUtil.toPageable(query.getPageLink())));
        } else {
            return DaoUtil.toPageData(reportInfoRepository
                    .findCustomerReportInfos(
                            tenantId.getId(),
                            customerId.getId(),
                            query.getReportTemplateId(),
                            query.getUserId(),
                            Objects.toString(query.getPageLink().getTextSearch(), ""),
                            DaoUtil.toPageable(query.getPageLink())));
        }
    }

    @Override
    public List<ReportInfo> findReportByIds(TenantId tenantId, List<UUID> toUUIDs) {
        return DaoUtil.convertDataList(reportInfoRepository.findByIdIn(toUUIDs));
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        reportRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public void deleteByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        reportRepository.deleteByTenantIdAndCustomerId(tenantId.getId(), customerId.getId());
    }

    @Override
    public Map<String, Long> countReportsByType() {
        return reportRepository.countReportsByFormatType()
                .stream()
                .collect(Collectors.toMap(e->e.getFirst().name(), TbPair::getSecond));
    }

    @Override
    public void createPartition(ReportEntity entity) {
        partitioningRepository.createPartitionIfNotExists(TABLE_NAME, entity.getCreatedTime(), TimeUnit.HOURS.toMillis(partitionSizeInHours));
    }

    @Override
    public List<ReportFields> findNextBatch(UUID id, int batchSize) {
        return reportRepository.findNextBatch(id, Limit.of(batchSize));
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
