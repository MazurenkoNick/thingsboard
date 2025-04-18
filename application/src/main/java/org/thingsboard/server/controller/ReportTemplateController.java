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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateInfo;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.report.TbReportTemplateService;
import org.thingsboard.server.service.security.model.SecurityUser;

import static org.thingsboard.server.controller.ControllerConstants.INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_DELETE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_WRITE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.REPORT_TEMPLATE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.REPORT_TEMPLATE_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ReportTemplateController extends BaseController {

    private static final String REPORT_TEMPLATE_INFO_DESCRIPTION = "Report Templates allows you to create reports according to the report template configuration. " +
            "Report service uses report template configuration to generate report. See the 'Model' tab of the Response Class for more details. ";
    private static final String REPORT_TEMPLATE_DESCRIPTION = "Report Template extends Report Template Info object and adds " +
            "'configuration' - a JSON structure of report template configuration. See the 'Model' tab of the Response Class for more details. ";
    private static final String INVALID_REPORT_TEMPLATE_ID = "Referencing non-existing Report Template Id will cause 'Not Found' error.";

    public static final String REPORT_TEMPLATE_ID = "reportTemplateId";

    private final TbReportTemplateService tbReportTemplateService;

    @ApiOperation(value = "Get Report Template (getReportTemplateById)",
            notes = "Fetch the ReportTemplate object based on the provided report template Id. " +
                    REPORT_TEMPLATE_DESCRIPTION + INVALID_REPORT_TEMPLATE_ID +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + "\n\n" + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/reportTemplate/{reportTemplateId}", method = RequestMethod.GET)
    @ResponseBody
    public ReportTemplate getReportTemplateById(@Parameter(description = REPORT_TEMPLATE_ID_PARAM_DESCRIPTION, required = true)
                                                @PathVariable(REPORT_TEMPLATE_ID) String strReportTemplateId) throws ThingsboardException {
        checkParameter(REPORT_TEMPLATE_ID, strReportTemplateId);
        ReportTemplateId reportTemplateId = new ReportTemplateId(toUUID(strReportTemplateId));
        return checkReportTemplateId(reportTemplateId, Operation.READ);
    }

    @ApiOperation(value = "Get Report Template Info (getReportTemplateInfoById)",
            notes = "Fetch the ReportTemplateInfo object based on the provided report template Id. " +
                    REPORT_TEMPLATE_INFO_DESCRIPTION + INVALID_REPORT_TEMPLATE_ID +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + "\n\n" + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/reportTemplate/info/{reportTemplateId}", method = RequestMethod.GET)
    @ResponseBody
    public ReportTemplateInfo getReportTemplateInfoById(@Parameter(description = REPORT_TEMPLATE_ID_PARAM_DESCRIPTION, required = true)
                                                        @PathVariable(REPORT_TEMPLATE_ID) String strReportTemplateId) throws ThingsboardException {
        checkParameter(REPORT_TEMPLATE_ID, strReportTemplateId);
        ReportTemplateId reportTemplateId = new ReportTemplateId(toUUID(strReportTemplateId));
        return checkReportTemplateInfoId(reportTemplateId, Operation.READ);
    }

    @ApiOperation(value = "Save Report Template (saveReportTemplate)",
            notes = "Creates or Updates report template. " + REPORT_TEMPLATE_DESCRIPTION +
                    "When creating report template, platform generates report template Id as " + UUID_WIKI_LINK +
                    "The newly created report template id will be present in the response. Specify existing report template id to update the report template. " +
                    "Referencing non-existing report template Id will cause 'Not Found' error. " +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Report Template entity. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + "\n\n" + RBAC_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/reportTemplate", method = RequestMethod.POST)
    @ResponseBody
    public ReportTemplate saveReportTemplate(
            @Parameter(description = "A JSON value representing the Report Template.")
            @RequestBody ReportTemplate reportTemplate) throws Exception {
        SecurityUser currentUser = getCurrentUser();
        reportTemplate.setTenantId(currentUser.getTenantId());
        if (Authority.CUSTOMER_USER.equals(currentUser.getAuthority())) {
            reportTemplate.setCustomerId(currentUser.getCustomerId());
        }
        checkEntity(reportTemplate.getId(), reportTemplate, Resource.REPORT_TEMPLATE, null);
        return tbReportTemplateService.save(reportTemplate, currentUser);
    }

    @ApiOperation(value = "Delete Report Template (deleteReportTemplate)",
            notes = "Deletes the report template. " + INVALID_REPORT_TEMPLATE_ID + "\n\n" + RBAC_DELETE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/reportTemplate/{reportTemplateId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteReportTemplate(
            @Parameter(description = REPORT_TEMPLATE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(REPORT_TEMPLATE_ID) String strReportTemplateId) throws Exception {
        checkParameter(REPORT_TEMPLATE_ID, strReportTemplateId);
        ReportTemplateId reportTemplateId = new ReportTemplateId(toUUID(strReportTemplateId));
        ReportTemplate reportTemplate = checkReportTemplateId(reportTemplateId, Operation.DELETE);
        tbReportTemplateService.delete(reportTemplate, getCurrentUser());
    }

    @ApiOperation(value = "Get All Report Templates for current user (getAllReportTemplateInfos)",
            notes = "Returns a page of report template info objects owned by the tenant or the customer of a current user. "
                    + REPORT_TEMPLATE_INFO_DESCRIPTION + " " + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/reportTemplateInfos/all", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<ReportTemplateInfo> getAllReportTemplateInfos(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS)
            @RequestParam(required = false) Boolean includeCustomers,
            @Parameter(description = REPORT_TEMPLATE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "ownerName"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.REPORT_TEMPLATE, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (Authority.TENANT_ADMIN.equals(getCurrentUser().getAuthority())) {
            if (includeCustomers != null && includeCustomers) {
                return checkNotNull(reportTemplateService.findReportTemplatesByTenantId(tenantId, pageLink));
            } else {
                return checkNotNull(reportTemplateService.findTenantReportTemplatesByTenantId(tenantId, pageLink));
            }
        } else {
            CustomerId customerId = getCurrentUser().getCustomerId();
            if (includeCustomers != null && includeCustomers) {
                return checkNotNull(reportTemplateService.findReportTemplatesByTenantIdAndCustomerIdIncludingSubCustomers(tenantId, customerId, pageLink));
            } else {
                return checkNotNull(reportTemplateService.findReportTemplatesByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        }
    }
}
