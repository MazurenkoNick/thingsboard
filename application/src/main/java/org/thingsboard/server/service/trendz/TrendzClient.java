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

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.trendz.TrendzHealthcheckResult;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationResultType;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationStatus;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class TrendzClient {

    public static final String TRENDZ_INFO_URI = "/apiTrendz/publicApi/info";
    public static final String TRENDZ_SYNC_INIT_URI = "/apiTrendz/publicApi/sync/init";
    public static final String TRENDZ_HEALTHCHECK_URI = "/apiTrendz/publicApi/sync/check";

    @Value("${trendz.request_timeout_ms:15000}")
    private int requestTimeoutMs;

    private RestTemplate restTemplate;

    @PostConstruct
    private void init() {
        restTemplate = new RestTemplateBuilder()
                .connectTimeout(Duration.ofMillis(requestTimeoutMs))
                .readTimeout(Duration.ofMillis(requestTimeoutMs))
                .build();
    }

    public JsonNode checkTrendzReachability(String trendzUrl) {
        return sendTrendzRequest(trendzUrl, TRENDZ_INFO_URI, HttpMethod.GET, null, JsonNode.class, "Checking Trendz reachability");
    }

    public TrendzHealthcheckResult processTrendzInitRequest(String trendzUrl, String internalTbUrl, String externalTbUrl, String currentApiKey, String prevApiKey) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("internalTbUrl", internalTbUrl);
        requestBody.put("externalTbUrl", externalTbUrl);
        requestBody.put("currentTbAccessToken", currentApiKey);
        requestBody.put("prevTbAccessToken", prevApiKey);

        return sendTrendzRequest(trendzUrl, TRENDZ_SYNC_INIT_URI, HttpMethod.POST, requestBody, TrendzHealthcheckResult.class, "Initiating Trendz sync");
    }

    public TrendzHealthcheckResult sendHealthcheckRequest(String trendzUrl, String apiKey) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("apiKey", apiKey);

        TrendzHealthcheckResult result = sendTrendzRequest(trendzUrl, TRENDZ_HEALTHCHECK_URI, HttpMethod.POST,
                requestBody, TrendzHealthcheckResult.class, "Performing Trendz healthcheck");

        if (result == null) {
            return new TrendzHealthcheckResult(
                    null,
                    TrendzSynchronizationResultType.TRENDZ_URL_UNREACHABLE,
                    TrendzSynchronizationStatus.AVAILABLE,
                    TrendzSynchronizationResultType.TRENDZ_URL_UNREACHABLE.getMessage()
            );
        }

        return result;
    }

    public <T> T sendTrendzRequest(String trendzUrl, String uriPath, HttpMethod method,
                                    Map<String, Object> requestBody, Class<T> responseType, String operationName) {
        try {
            String url = normalizeUrl(trendzUrl) + uriPath;
            log.debug("{} at: {}", operationName, url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<T> response = restTemplate.exchange(url, method, request, responseType);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("{} completed successfully", operationName);
                return response.getBody();
            }
            log.warn("{} received non-successful response: {}", operationName, response.getStatusCode());
            return null;
        } catch (Exception e) {
            log.error("{} failed at {} [{}]: {}", operationName, trendzUrl, uriPath, e.getMessage(), e);
            return null;
        }
    }

    public ResponseEntity<byte[]> sendTrendzProxyRequest(String trendzUrl, String uriPath, HttpMethod method,
                                                         byte[] body, HttpHeaders headers) throws ThingsboardException {
        try {
            String url = normalizeUrl(trendzUrl) + uriPath;
            log.debug("Trendz proxy request at: {}", url);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url, method, new HttpEntity<>(body, headers), byte[].class
            );

            log.debug("Trendz proxy request completed successfully");
            return response;
        } catch (RestClientResponseException e) {
            log.warn("Trendz proxy request received non-successful response: {}", e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode())
                    .headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAsByteArray());
        } catch (Exception e) {
            log.error("Trendz proxy request failed at {} [{}]: {}", trendzUrl, uriPath, e.getMessage(), e);
            throw new ThingsboardException("Unexpected error during Trendz proxy request", e, ThingsboardErrorCode.GENERAL);
        }
    }

    private String normalizeUrl(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

}
