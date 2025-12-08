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

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.trendz.TrendzProxyService;

import static org.thingsboard.server.controller.ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping
public class TrendzProxyController extends BaseController {
    private final TrendzProxyService trendzProxyService;

    @ApiOperation(value = "Forward Authorized Requests to Trendz",
            notes = "Forwards authorized requests (/apiTrendz/**) to Trendz using the Trendz internal URL. " +
                    "Can only be used if Trendz is already synchronized and integration is enabled." +
                    AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @RequestMapping("/apiTrendz/**")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public ResponseEntity<byte[]> handleAuthorizedTrendzRequests(HttpServletRequest request, @RequestBody(required = false) byte[] body) throws ThingsboardException {
        return trendzProxyService.proxy(request, body);
    }

    @ApiOperation(value = "Forward Unauthorized Requests to Trendz",
            notes = "Forwards unauthorized requests (/apiTrendz/publicApi/**, /trendz/**) to Trendz using the Trendz internal URL. " +
                    "Can only be used if Trendz is already synchronized and integration is enabled." +
                    AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @RequestMapping({"/apiTrendz/publicApi/**", "/trendz/**"})
    public ResponseEntity<byte[]> handleUnauthorizedTrendzRequests(HttpServletRequest request, @RequestBody(required = false) byte[] body) throws ThingsboardException {
        return trendzProxyService.proxy(request, body);
    }
}
