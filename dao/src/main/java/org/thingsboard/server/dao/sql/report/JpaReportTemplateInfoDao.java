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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.report.ReportTemplateInfo;
import org.thingsboard.server.common.data.report.ReportTemplateQuery;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.ReportTemplateInfoEntity;
import org.thingsboard.server.dao.report.ReportTemplateInfoDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@AllArgsConstructor
@Slf4j
@SqlDao
public class JpaReportTemplateInfoDao extends JpaAbstractDao<ReportTemplateInfoEntity, ReportTemplateInfo> implements ReportTemplateInfoDao {

    private final ReportTemplateInfoRepository reportTemplateInfoRepository;

    @Override
    public PageData<ReportTemplateInfo> findReportTemplates(UUID tenantId, ReportTemplateQuery query) {
        List<ReportTemplateType> typeList = query.getTypeList() != null && !query.getTypeList().isEmpty() ? query.getTypeList() : null;
        List<TbReportFormat> formatList = query.getFormatList() != null && !query.getFormatList().isEmpty() ? query.getFormatList() : null;
        if (query.isIncludeCustomers()) {
            return DaoUtil.toPageData(reportTemplateInfoRepository
                    .findTenantReportTemplatesIncludingCustomers(
                            tenantId,
                            Objects.toString(query.getPageLink().getTextSearch(), ""),
                            typeList,
                            formatList,
                            DaoUtil.toPageable(query.getPageLink())));
        } else {
            return DaoUtil.toPageData(reportTemplateInfoRepository
                    .findTenantReportTemplates(
                            tenantId,
                            Objects.toString(query.getPageLink().getTextSearch(), ""),
                            typeList,
                            formatList,
                            DaoUtil.toPageable(query.getPageLink())));
        }
    }

    @Override
    public PageData<ReportTemplateInfo> findCustomerReportTemplates(UUID tenantId, UUID customerId, ReportTemplateQuery query) {
        List<ReportTemplateType> typeList = query.getTypeList() != null && !query.getTypeList().isEmpty() ? query.getTypeList() : null;
        List<TbReportFormat> formatList = query.getFormatList() != null && !query.getFormatList().isEmpty() ? query.getFormatList() : null;
        if (query.isIncludeCustomers()) {
            return DaoUtil.toPageData(reportTemplateInfoRepository
                    .findCustomerReportTemplatesIncludingSubCustomers(
                            tenantId,
                            customerId,
                            Objects.toString(query.getPageLink().getTextSearch(), ""),
                            typeList != null ? typeList.stream().map(Enum::name).toList() : null,
                            formatList != null ? formatList.stream().map(Enum::name).toList() : null,
                            DaoUtil.toPageable(query.getPageLink())));
        } else {
            return DaoUtil.toPageData(reportTemplateInfoRepository
                    .findCustomerReportTemplates(
                            tenantId,
                            customerId,
                            Objects.toString(query.getPageLink().getTextSearch(), ""),
                            typeList,
                            formatList,
                            DaoUtil.toPageable(query.getPageLink())));
        }
    }

    @Override
    public List<ReportTemplateInfo> findReportTemplatesByIds(UUID tenantId, List<UUID> reportTemplateIds) {
        return DaoUtil.convertDataList(reportTemplateInfoRepository.findByIdIn(reportTemplateIds));
    }

    @Override
    protected Class<ReportTemplateInfoEntity> getEntityClass() {
        return ReportTemplateInfoEntity.class;
    }

    @Override
    protected JpaRepository<ReportTemplateInfoEntity, UUID> getRepository() {
        return reportTemplateInfoRepository;
    }
}
