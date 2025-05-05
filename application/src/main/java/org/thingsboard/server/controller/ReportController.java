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
package org.thingsboard.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.ReportJobConfiguration;
import org.thingsboard.server.common.data.job.ReportJobResult;
import org.thingsboard.server.common.data.report.ReportRequest;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.dao.job.JobService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.job.JobManager;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
@RequestMapping("/api/v2")
public class ReportController extends BaseController {

    private final JobManager jobManager;
    private final JobService jobService;
    private final BlobEntityService blobEntityService;

    @ApiOperation(value = "Download test report (downloadTestReport)",
            notes = "Generate and download test report." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/report/deprecated/test", produces = {"application/pdf"})
    @Deprecated // FIXME: this is temporary API for testing purposes
    public ResponseEntity<Resource> testReportAndDownload(@RequestBody ReportRequest reportRequest) throws Exception {
        TenantId tenantId = getTenantId();
        Job job = requestTestReport(reportRequest);
        do {
            Thread.sleep(1000);
            job = jobService.findJobById(tenantId, job.getId());
        } while (!job.getStatus().isOneOf(JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.CANCELLED) && !Thread.currentThread().isInterrupted());

        if (job.getStatus() != JobStatus.COMPLETED) {
            throw new IllegalStateException("Failed to generate report: " + job.getResult().getResults());
        }

        BlobEntityId reportBlobId = ((ReportJobResult) job.getResult()).getReportBlobId();
        BlobEntity reportBlobEntity = blobEntityService.findBlobEntityById(tenantId, reportBlobId);

        ByteArrayResource resource = new ByteArrayResource(reportBlobEntity.getData().array());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + reportBlobEntity.getName())
                .header("x-filename", reportBlobEntity.getName())
                .contentLength(resource.contentLength())
                .contentType(MediaType.parseMediaType(reportBlobEntity.getContentType()))
                .body(resource);
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/report/test")
    public Job requestTestReport(@RequestBody ReportRequest reportRequest) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        ReportTemplateId templateId = reportRequest.getTemplateId();
        ReportTemplate reportTemplate = reportTemplateService.findReportTemplateById(currentUser.getTenantId(), templateId);

        return jobManager.submitJob(Job.builder()
                .tenantId(currentUser.getTenantId())
                .type(JobType.REPORT)
                .key(UUID.randomUUID().toString())
                .description("Test report generation for template '" + reportTemplate.getName() + "'")
                .configuration(ReportJobConfiguration.builder()
                        .request(reportRequest)
                        .userId(currentUser.getId())
                        .testReport(true)
                        .build())
                .build());
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/report")
    public Job requestReport(@RequestBody ReportRequest reportRequest) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        ReportTemplateId templateId = reportRequest.getTemplateId();
        ReportTemplate reportTemplate = reportTemplateService.findReportTemplateById(currentUser.getTenantId(), templateId);

        return jobManager.submitJob(Job.builder()
                .tenantId(currentUser.getTenantId())
                .type(JobType.REPORT)
                .key(UUID.randomUUID().toString())
                .description("Report generation for template '" + reportTemplate.getName() + "'")
                .configuration(ReportJobConfiguration.builder()
                        .request(reportRequest)
                        .userId(currentUser.getId())
                        .build())
                .build());
    }

}
