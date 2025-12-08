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
package org.thingsboard.server.service.trendz;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.MultiValueMap;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.trendz.TrendzConfiguration;
import org.thingsboard.server.common.data.trendz.TrendzSettings;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationResult;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationStatus;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.trendz.TrendzSettingsService;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = {
        "trendz.enabled=true"
})
@DaoSqlTest
public class TrendzProxyServiceTest extends AbstractControllerTest {
    @MockitoBean
    private TrendzSettingsService trendzSettingsService;
    @MockitoBean
    private TrendzClient trendzClient;

    @Autowired
    private TrendzProxyService trendzProxyService;

    @Test
    public void proxyTest_unsyncedTrendz() {
        when(trendzSettingsService.findTrendzSettings(TenantId.SYS_TENANT_ID))
                .thenReturn(new TrendzSettings(
                        new TrendzConfiguration(null, null),
                        new TrendzSynchronizationResult(null, null, null, TrendzSynchronizationStatus.NOT_AVAILABLE)
                ));

        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest("POST", "/apiTrendz/test");
        httpServletRequest.addHeader("trendz_header", "trendz_header_value");
        assertThrows(
                ThingsboardException.class,
                () -> trendzProxyService.proxy(httpServletRequest, "trendz_response_body".getBytes())
        );
    }

    @Test
    public void proxyTest_withValidTrendzUrl() throws ThingsboardException {
        String trendzUrl = "trendz_url:8888";
        String trendzUri = "/apiTrendz/test";

        ResponseEntity<byte[]> expected = new ResponseEntity<>(
                "trendz_response_body".getBytes(),
                MultiValueMap.fromMultiValue(Map.of("trendz_header", List.of("trendz_header_value"))),
                HttpStatusCode.valueOf(201)
        );

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("trendz_request_header", "trendz_request_header_value");
        byte[] requestBody = "trendz_request_body".getBytes();

        when(trendzSettingsService.findTrendzSettings(TenantId.SYS_TENANT_ID))
                .thenReturn(new TrendzSettings(
                        new TrendzConfiguration(trendzUrl, null),
                        new TrendzSynchronizationResult(null, null, null, TrendzSynchronizationStatus.SYNCED)
                ));
        when(trendzClient.sendTrendzProxyRequest(trendzUrl, trendzUri, HttpMethod.POST, requestBody, requestHeaders))
                .thenReturn(expected);

        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest("POST", trendzUri);
        httpServletRequest.addHeader("trendz_request_header", "trendz_request_header_value");

        ResponseEntity<byte[]> actual = trendzProxyService.proxy(httpServletRequest, requestBody);
        assertEquals(expected, actual);
    }

    @Test
    public void proxyTest_withValidTrendzUrlAndQueryParams() throws ThingsboardException {
        String trendzUrl = "trendz_url:8888";

        ResponseEntity<byte[]> expected = new ResponseEntity<>(
                null,
                MultiValueMap.fromMultiValue(Map.of("trendz_header", List.of("trendz_header_value"))),
                HttpStatusCode.valueOf(200)
        );

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("trendz_request_header", "trendz_request_header_value");

        when(trendzSettingsService.findTrendzSettings(TenantId.SYS_TENANT_ID))
                .thenReturn(new TrendzSettings(
                        new TrendzConfiguration(trendzUrl, null),
                        new TrendzSynchronizationResult(null, null, null, TrendzSynchronizationStatus.SYNCED)
                ));
        when(trendzClient.sendTrendzProxyRequest(trendzUrl, "/apiTrendz/test?param1=value1&param2=value2", HttpMethod.GET, null, requestHeaders))
                .thenReturn(expected);

        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest("GET", "/apiTrendz/test");
        httpServletRequest.addHeader("trendz_request_header", "trendz_request_header_value");
        httpServletRequest.setQueryString("param1=value1&param2=value2");

        ResponseEntity<byte[]> actual = trendzProxyService.proxy(httpServletRequest, null);
        assertEquals(expected, actual);
    }
}
