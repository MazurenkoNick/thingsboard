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
package org.thingsboard.server.report.service;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.context.TbReportCtxProvider;
import org.thingsboard.server.report.datasource.ReportDataService;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class TbReportService {

    private final Map<TbReportFormat, ReportService> reportServices = new EnumMap<>(TbReportFormat.class);
    private final TbReportCtxProvider contextProvider;
    private final ReportDataService dataService;

    private TbReportService(List<ReportService> reportServices, TbReportCtxProvider contextProvider, ReportDataService dataService) {
        reportServices.forEach(service -> {
            TbReportFormat format = service.getFormat();
            if (format != null) {
                this.reportServices.put(format, service);
            }
        });
        this.contextProvider = contextProvider;
        this.dataService = dataService;
    }

    public ReportData generateTestReport(ReportTask task) throws Exception {
        try (TbReportCtx ctx = contextProvider.newContext(task)) {
            return generateReport(task, ctx);
        }
    }

    public BlobEntityInfo generateReport(ReportTask task) throws Exception {
        try (TbReportCtx ctx = contextProvider.newContext(task)) {
            ReportData reportData = generateReport(task, ctx);

            BlobEntity blobEntity = new BlobEntity();
            blobEntity.setTenantId(task.getTenantId());
            blobEntity.setData(ByteBuffer.wrap(reportData.getData()));
            blobEntity.setContentType(reportData.getContentType());
            blobEntity.setName(reportData.getName());
            blobEntity.setType("report");
            return dataService.createBlobEntity(blobEntity, ctx);
        }
    }

    private ReportData generateReport(ReportTask task, TbReportCtx ctx) throws Exception {
        ReportTemplateConfig configuration = task.getReportTemplateConfig();
        return reportServices.get(configuration.getFormat()).generateReport(task, ctx);
    }

}
