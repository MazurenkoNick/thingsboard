/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 * <p>
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
 * <p>
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * <p>
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 * <p>
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 * <p>
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
package org.thingsboard.server.service.trendz;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.trendz.TrendzConfiguration;
import org.thingsboard.server.common.data.trendz.TrendzSettings;
import org.thingsboard.server.dao.trendz.TrendzSettingsService;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultTrendzProxyService implements TrendzProxyService {
    private final TrendzSettingsService trendzSettingsService;
    private final TrendzClient trendzClient;

    @Value("${trendz.enabled:true}")
    private boolean trendzEnabled;

    @Override
    public ResponseEntity<byte[]> proxy(HttpServletRequest request, byte[] body) throws ThingsboardException {
        if (!trendzEnabled) {
            throw new ThingsboardException("Trendz is disabled.", ThingsboardErrorCode.GENERAL);
        }
        String path = request.getRequestURI();
        String query = request.getQueryString();

        String trendzUrl = getBaseTrendzUrl();
        String trendzUri = path + (query != null ? "?" + query : "");

        HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod());

        HttpHeaders headers = new HttpHeaders();
        request.getHeaderNames()
                .asIterator()
                .forEachRemaining(name -> request.getHeaders(name)
                        .asIterator()
                        .forEachRemaining(value -> headers.add(name, value))
                );

        return trendzClient.sendTrendzProxyRequest(trendzUrl, trendzUri, httpMethod, body, headers);
    }

    private String getBaseTrendzUrl() throws ThingsboardException {
        return Optional.ofNullable(trendzSettingsService.findTrendzSettings(TenantId.SYS_TENANT_ID))
                .map(TrendzSettings::configuration)
                .map(TrendzConfiguration::trendzUrl)
                .orElseThrow(() -> new ThingsboardException("Trendz url is not present in config.", ThingsboardErrorCode.GENERAL));
    }
}
