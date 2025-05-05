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
package org.thingsboard.server.report;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.job.task.ReportTaskResult;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.queue.task.TaskProcessor;
import org.thingsboard.server.queue.util.TbReportComponent;
import org.thingsboard.server.report.service.ReportService;
import org.thingsboard.server.report.service.TbReportCtx;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@TbReportComponent
@Component
public class ReportTaskProcessor extends TaskProcessor<ReportTask, ReportTaskResult> {

    private final Map<TbReportFormat, ReportService> reportServices = new EnumMap<>(TbReportFormat.class);

    @Value("${reports.generation_timeout_ms:120000}")
    private int timeoutMs;

    private ReportTaskProcessor(List<ReportService> reportServices) {
        reportServices.forEach(service -> {
            TbReportFormat format = service.getFormat();
            if (format != null) {
                this.reportServices.put(format, service);
            }
        });
    }

    @Value("${service.tb_core.base_url:http://localhost:${server.port}}") // for monolith - sending request to itself
    private String tbCoreBaseUrl;

    @Override
    public ReportTaskResult process(ReportTask task) throws Exception {
        ReportTemplateConfig configuration = task.getReportTemplate().getConfiguration();
        ReportData reportData;
        try (RestClient restClient = new RestClient(new RestTemplate(), tbCoreBaseUrl, task.getAccessToken())) {
            TbReportCtx reportCtx = TbReportCtx.builder()
                    .tenantId(task.getTenantId())
                    .customerId(task.getReportRequest().getCustomerId())
                    .configuration(configuration)
                    .restClient(restClient)
                    .accessToken(task.getAccessToken())
                    .accessTokenExpTs(task.getAccessTokenExpirationTs())
                    .build();
            reportData = reportServices.get(configuration.getFormat()).generateReport(task, reportCtx);

            BlobEntity blobEntity = new BlobEntity();
            blobEntity.setTenantId(task.getTenantId());
            blobEntity.setCustomerId(null); // fixme: what customer id to use??? one from request or from userId?
            blobEntity.setData(ByteBuffer.wrap(reportData.getData()));
            blobEntity.setContentType(reportData.getContentType());
            blobEntity.setName(reportData.getName());
            blobEntity.setType(task.isTestReport() ? "test_report" : "report");
            BlobEntityInfo savedBlobEntity = restClient.createBlobEntity(blobEntity);
            return ReportTaskResult.success(savedBlobEntity.getId());
        }
    }

    @Override
    public long getTaskProcessingTimeout() {
        return timeoutMs;
    }

    @Override
    public JobType getJobType() {
        return JobType.REPORT;
    }

}
