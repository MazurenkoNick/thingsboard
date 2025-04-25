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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import net.sf.jasperreports.engine.JRException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.service.report.ReportService;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.ReportRequest;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequestMapping("/api/v2")
public class ReportController extends BaseController {

    @Autowired
    private ReportService reportService;

    @ApiOperation(value = "Download test report (downloadTestReport)",
            notes = "Generate and download test report." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/report/test", produces = {"application/pdf"})
    public DeferredResult<ResponseEntity<Resource>> downloadTestReport(@RequestBody ReportRequest reportRequest) throws ThingsboardException, JRException {
        DeferredResult<ResponseEntity<Resource>> deferredResult = new DeferredResult<>();
        SecurityUser currentUser = getCurrentUser();
        ListenableFuture<ReportData> reportData = reportService.generateReport(currentUser.getTenantId(), currentUser.getCustomerId(),
                currentUser.getUserPermissions(), reportRequest);
        Futures.addCallback(reportData, new ReportDataCallback(deferredResult), MoreExecutors.directExecutor());
        return deferredResult;
    }

    private static class ReportDataCallback implements FutureCallback<ReportData> {
        private final DeferredResult<ResponseEntity<Resource>> deferredResult;

        public ReportDataCallback(DeferredResult<ResponseEntity<Resource>> deferredResult) {
            this.deferredResult = deferredResult;
        }

        @Override
        public void onSuccess(ReportData report) {
            ByteArrayResource resource = new ByteArrayResource(report.getData());

            ResponseEntity<Resource> response = ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + report.getName())
                    .header("x-filename", report.getName())
                    .contentLength(resource.contentLength())
                    .contentType(MediaType.parseMediaType(report.getContentType()))
                    .body(resource);

            deferredResult.setResult(response);
        }

        @Override
        public void onFailure(Throwable t) {
            deferredResult.setErrorResult(ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to generate report: " + t.getMessage()));
        }
    }

}
