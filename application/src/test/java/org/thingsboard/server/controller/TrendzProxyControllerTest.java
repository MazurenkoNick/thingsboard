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

import org.junit.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.MultiValueMap;
import org.thingsboard.server.service.trendz.TrendzProxyService;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class TrendzProxyControllerTest extends AbstractControllerTest {
    @MockitoBean
    private TrendzProxyService trendzProxyService;

    @Test
    public void handleTrendzRequest_withBody() throws Exception {
        String trendzUri = "/trendz/test";
        String expectedBody = "trendz_response_body";
        String expectedHeaderName = "trendz_header";
        List<String> expectedHeaderValue = List.of("trendz_header_value");
        int expectedStatusCode = 201;

        ResponseEntity<byte[]> expected = new ResponseEntity<>(
                expectedBody.getBytes(),
                MultiValueMap.fromMultiValue(Map.of(expectedHeaderName, expectedHeaderValue)),
                HttpStatusCode.valueOf(expectedStatusCode)
        );

        String requestBody = "trendz_request_body";
        when(trendzProxyService.proxy(any(), eq(requestBody.getBytes())))
                .thenReturn(expected);

        doPost(trendzUri, (Object) requestBody)
                .andExpectAll(
                        r -> assertEquals(expectedStatusCode, r.getResponse().getStatus()),
                        r -> assertEquals(expectedHeaderValue, r.getResponse().getHeaders(expectedHeaderName)),
                        r -> assertEquals(expectedBody, r.getResponse().getContentAsString())
                );
    }

    @Test
    public void handleTrendzRequest_withoutBody() throws Exception {
        String trendzUri = "/trendz/test";
        String expectedHeaderName = "trendz_header";
        List<String> expectedHeaderValue = List.of("trendz_header_value");
        int expectedStatusCode = 201;

        ResponseEntity<byte[]> expected = new ResponseEntity<>(
                null,
                MultiValueMap.fromMultiValue(Map.of(expectedHeaderName, expectedHeaderValue)),
                HttpStatusCode.valueOf(expectedStatusCode)
        );

        String requestBody = "trendz_request_body";
        when(trendzProxyService.proxy(any(), eq(requestBody.getBytes())))
                .thenReturn(expected);

        doPost(trendzUri, (Object) requestBody)
                .andExpectAll(
                        r -> assertEquals(expectedStatusCode, r.getResponse().getStatus()),
                        r -> assertEquals(expectedHeaderValue, r.getResponse().getHeaders(expectedHeaderName)),
                        r -> assertTrue(r.getResponse().getContentAsString().isBlank())
                );
    }
}
