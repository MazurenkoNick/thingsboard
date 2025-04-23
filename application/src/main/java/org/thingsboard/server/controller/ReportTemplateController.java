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

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;

import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.REPORT_TEMPLATE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api/v2")
@Slf4j
public class ReportTemplateController extends AutoCommitController {

    public static final String TEMPLATE_ID = "templateId";

    @ApiOperation(value = "Get Report Template (getTemplateById)",
            notes = "Fetch the Report Template object based on the provided Report Template Id. " +
                    "The server checks that the template is owned by the same tenant. "
                    + NEW_LINE + RBAC_READ_CHECK
    )
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/report/template/{templateId}", method = RequestMethod.GET)
    @ResponseBody
    public ReportTemplate getTemplateById(@Parameter(required = true, description = REPORT_TEMPLATE_ID_PARAM_DESCRIPTION)
                                      @PathVariable(TEMPLATE_ID) String strTemplateId) throws ThingsboardException {
        checkParameter(TEMPLATE_ID, strTemplateId);
        ReportTemplateId reportTemplateId = new ReportTemplateId(toUUID(strTemplateId));
        return checkReportTemplateId(reportTemplateId, Operation.READ);
    }

    @ApiOperation(value = "Create Or Update Report Template (saveReportTemplate)",
            notes = "Create or update the Report Template. When creating template, platform generates Report Template Id as " + UUID_WIKI_LINK +
                    "The newly created template id will be present in the response. " +
                    "Specify existing Report Template id to update the template. " +
                    "Referencing non-existing template Id will cause 'Not Found' error. " +
                    "Remove 'id', 'tenantId' from the request body example (below) to create new report template entity. " +
                    TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/report/template", method = RequestMethod.POST)
    @ResponseBody
    public ReportTemplate saveReportTemplate(@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, description = "A JSON value representing the report template.") @RequestBody ReportTemplate template) throws Exception {
        template.setTenantId(getCurrentUser().getTenantId());
        checkEntity(template.getId(), template, Resource.REPORT_TEMPLATE, null);
        return reportTemplateService.saveReportTemplate(template);
    }

}
