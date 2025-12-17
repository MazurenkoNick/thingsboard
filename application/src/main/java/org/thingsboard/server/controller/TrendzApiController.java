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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.trendz.TrendzSummary;
import org.thingsboard.server.common.data.trendz.TrendzUsage;
import org.thingsboard.server.common.data.trendz.TrendzViewConfig;
import org.thingsboard.server.common.data.trendz.TrendzViewConfigLite;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.trendz.TrendzApiService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TRENDZ_ENDPOINT_AVAILABILITY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TRENDZ_VIEW_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TRENDZ_VIEW_TEXT_SEARCH_DESCRIPTION;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api/trendz")
public class TrendzApiController extends BaseController {

    private static final String TRENDZ_VIEW_ID = "viewId";

    private final TrendzApiService trendzApiService;

    @ApiOperation(value = "Get Trendz Views (getTrendzViews)",
            notes = "Returns a page of Trendz views that are available for the current user. " +
                    TRENDZ_ENDPOINT_AVAILABILITY_DESCRIPTION + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @GetMapping(path = "/view/all", params = {"pageSize", "page"})
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public PageData<TrendzViewConfigLite> getTrendzViews(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = TRENDZ_VIEW_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"name", "createdAt", "updatedAt", "favorite"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder
    ) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        User user = getCurrentUser();
        return trendzApiService.getAllViews(user, pageLink);
    }

    @ApiOperation(value = "Get Trendz View by Id (getTrendzViewById)",
            notes = "Fetch the Trendz View object based on the provided Trendz View Id. " +
                    TRENDZ_ENDPOINT_AVAILABILITY_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @GetMapping("/view/{viewId}")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public TrendzViewConfig getTrendzViewById(
            @Parameter(description = TRENDZ_VIEW_ID_PARAM_DESCRIPTION)
            @PathVariable(TRENDZ_VIEW_ID) String strViewId
    ) throws ThingsboardException {
        checkParameter(TRENDZ_VIEW_ID, strViewId);
        UUID viewId = toUUID(strViewId);
        User user = getCurrentUser();
        return trendzApiService.getViewById(user, viewId);
    }

    @ApiOperation(value = "Get Trendz Summary (getTrendzSummary)",
            notes = "Fetch the Trendz summary object. " +
                    TRENDZ_ENDPOINT_AVAILABILITY_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public TrendzSummary getTrendzSummary() throws ThingsboardException {
        User user = getCurrentUser();
        return trendzApiService.getTrendzSummary(user);
    }

    @ApiOperation(value = "Get Trendz Usage (getTrendzUsage)",
            notes = "Fetch the Trendz usage object. " +
                    TRENDZ_ENDPOINT_AVAILABILITY_DESCRIPTION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/usage")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public TrendzUsage getTrendzUsage() throws ThingsboardException {
        User user = getCurrentUser();
        return trendzApiService.getTrendzUsage(user);
    }
}
