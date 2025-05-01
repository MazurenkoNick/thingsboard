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
import net.sf.jasperreports.engine.JRException;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.ReportJobConfiguration;
import org.thingsboard.server.common.data.job.ReportTask;
import org.thingsboard.server.common.data.report.ReportRequest;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.job.JobManager;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.AccessJwtToken;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
@RequestMapping("/api/v2")
public class ReportController extends BaseController {

    private final JobManager jobManager;
    private final ReportTemplateService reportTemplateService;
    private final SystemSecurityService systemSecurityService;

    @ApiOperation(value = "Download test report (downloadTestReport)",
            notes = "Generate and download test report." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/report/deprecated/test", produces = {"application/pdf"})
    @Deprecated // FIXME: this is temporary API for testing purposes
    public ResponseEntity<Resource> testReportAndDownload(@RequestBody ReportRequest reportRequest) throws ThingsboardException, JRException {
        SecurityUser currentUser = getCurrentUser();
        ReportTemplate reportTemplate = reportTemplateService.findReportTemplateById(currentUser.getTenantId(), reportRequest.getTemplateId());
        AccessJwtToken accessToken = systemSecurityService.createUserAccessToken(currentUser.getTenantId(), currentUser.getId());

        ReportTask reportTask = ReportTask.builder()
                .tenantId(currentUser.getTenantId())
                .reportTemplate(reportTemplate)
                .reportRequest(reportRequest)
                .accessToken(accessToken.getToken())
                .build();

        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.exchange(
                "http://localhost:8080/api/noauth/report/test", // send request to myself
                HttpMethod.POST,
                new HttpEntity<>(reportTask),
                Resource.class
        );
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/report/test", produces = {"application/pdf"})
    public Job testReport(@RequestBody ReportRequest reportRequest) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        return jobManager.submitJob(Job.builder()
                .tenantId(currentUser.getTenantId())
                .type(JobType.REPORT)
                .key(reportRequest.getTemplateId().toString()) // fixme
                .description("Report generation for request: " + reportRequest) // fixme: tmp
                .configuration(ReportJobConfiguration.builder()
                        .request(reportRequest)
                        .userId(currentUser.getId())
                        .build())
                .build());
    }

}
