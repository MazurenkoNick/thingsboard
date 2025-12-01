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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.trendz.TrendzConfiguration;
import org.thingsboard.server.common.data.trendz.TrendzSettings;
import org.thingsboard.server.common.data.trendz.TrendzHealthcheckResult;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationResult;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationResultType;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationStatus;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.pat.ApiKeyService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.trendz.TrendzSettingsService;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.service.trendz.DefaultTrendzSyncService.TRENDZ_HEALTHCHECK_URI;
import static org.thingsboard.server.service.trendz.DefaultTrendzSyncService.TRENDZ_INFO_URI;
import static org.thingsboard.server.service.trendz.DefaultTrendzSyncService.TRENDZ_SYNC_INIT_URI;

@DaoSqlTest
@TestPropertySource(properties = {
        "trendz.default_trendz_url=https://trendz.example.com",
        "trendz.default_tb_url=https://tb.example.com"
})
public class TrendzSyncServiceTest extends AbstractControllerTest {

    @MockitoSpyBean
    private DefaultTrendzSyncService trendzSyncService;

    @MockitoBean
    private RestTemplate restTemplate;

    @MockitoBean
    private SystemSecurityService systemSecurityService;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private TrendzSettingsService trendzSettingsService;

    private static final String TEST_TRENDZ_URL = "https://trendz.example.com";
    private static final String TEST_TB_URL = "https://tb.example.com";
    private static final String TEST_EXTERNAL_TB_URL = "https://external.tb.example.com";
    private static final String TEST_TRENDZ_VERSION = "1.15.0";

    @Before
    public void setUp() throws Exception {
        loginSysAdmin();

        ReflectionTestUtils.setField(trendzSyncService, "trendzEnabled", true);
        ReflectionTestUtils.setField(trendzSyncService, "restTemplate", restTemplate);
        reset(restTemplate, systemSecurityService);
    }

    @After
    public void tearDown() {
        ApiKey trendzApiKey = apiKeyService.findInternalApiKeyByDescription(TenantId.SYS_TENANT_ID, DefaultTrendzSyncService.TRENDZ_API_KEY_DESCRIPTION);
        if (trendzApiKey != null) {
            apiKeyService.deleteApiKey(TenantId.SYS_TENANT_ID, trendzApiKey, true);
        }

        TrendzSettings settings = trendzSettingsService.findTrendzSettings(TenantId.SYS_TENANT_ID);
        if (settings != null) {
            trendzSettingsService.deleteTrendzSettings(TenantId.SYS_TENANT_ID);
        }
    }

    @Test
    public void testPerformSync_whenDisabled() {
        ReflectionTestUtils.setField(trendzSyncService, "trendzEnabled", false);

        TrendzSettings result = trendzSyncService.performSync(TenantId.SYS_TENANT_ID, currentUserId);

        assertNotNull(result);
        assertEquals(TrendzSynchronizationResultType.SYNC_DISABLED, result.synchronizationResult().type());
        assertEquals(TrendzSynchronizationStatus.NOT_AVAILABLE, result.synchronizationResult().status());

        TrendzSettings savedSettings = trendzSettingsService.findTrendzSettings(TenantId.SYS_TENANT_ID);
        assertNotNull(savedSettings);
        assertEquals(TrendzSynchronizationResultType.SYNC_DISABLED, savedSettings.synchronizationResult().type());
    }

    @Test
    public void testPerformSync_success() {
        ObjectNode trendzInfoResponse = JacksonUtil.newObjectNode();
        trendzInfoResponse.put("version", TEST_TRENDZ_VERSION);
        trendzInfoResponse.put("artifact", "trendz");
        trendzInfoResponse.put("name", "Trendz Analytics");
        trendzInfoResponse.put("cloud", false);
        when(restTemplate.exchange(
                eq(TEST_TRENDZ_URL + TRENDZ_INFO_URI),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(new ResponseEntity<>(trendzInfoResponse, HttpStatus.OK));

        ObjectNode syncResponse = JacksonUtil.newObjectNode();
        syncResponse.put("version", TEST_TRENDZ_VERSION);
        syncResponse.put("type", TrendzSynchronizationResultType.SYNC_COMPLETED.name());
        syncResponse.put("status", TrendzSynchronizationStatus.SYNCED.name());
        syncResponse.put("message", "Sync completed successfully");
        when(systemSecurityService.getBaseUrl(eq(TenantId.SYS_TENANT_ID), any(), any())).thenReturn(TEST_EXTERNAL_TB_URL);
        when(restTemplate.exchange(
                eq(TEST_TRENDZ_URL + TRENDZ_SYNC_INIT_URI),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(Class.class)
        )).thenAnswer(invocation -> {
            Class<?> responseType = invocation.getArgument(3);
            Object convertedResponse = JacksonUtil.convertValue(syncResponse, responseType);
            return new ResponseEntity<>(convertedResponse, HttpStatus.OK);
        });

        TrendzSettings result = trendzSyncService.performSync(TenantId.SYS_TENANT_ID, currentUserId);

        assertNotNull(result);
        assertEquals(TrendzSynchronizationResultType.SYNC_COMPLETED, result.synchronizationResult().type());
        assertEquals(TrendzSynchronizationStatus.SYNCED, result.synchronizationResult().status());
        assertEquals(TEST_TRENDZ_VERSION, result.synchronizationResult().version());

        ApiKey apiKey = apiKeyService.findInternalApiKeyByDescription(TenantId.SYS_TENANT_ID, DefaultTrendzSyncService.TRENDZ_API_KEY_DESCRIPTION);
        assertNotNull(apiKey);
        assertTrue(apiKey.isInternal());
        assertTrue(apiKey.isEnabled());

        TrendzSettings savedSettings = trendzSettingsService.findTrendzSettings(TenantId.SYS_TENANT_ID);
        assertNotNull(savedSettings);
        assertEquals(TrendzSynchronizationResultType.SYNC_COMPLETED, savedSettings.synchronizationResult().type());
        assertEquals(TrendzSynchronizationStatus.SYNCED, savedSettings.synchronizationResult().status());
    }

    @Test
    public void testPerformSync_trendzUnreachable() {
        when(restTemplate.exchange(
                eq(TEST_TRENDZ_URL + TRENDZ_INFO_URI),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenThrow(new RestClientException("Connection refused"));

        TrendzSettings result = trendzSyncService.performSync(TenantId.SYS_TENANT_ID, currentUserId);

        assertNotNull(result);
        assertEquals(TrendzSynchronizationResultType.SYNC_NOT_INITIALIZED, result.synchronizationResult().type());
        assertEquals(TrendzSynchronizationStatus.NOT_AVAILABLE, result.synchronizationResult().status());

        TrendzSettings savedSettings = trendzSettingsService.findTrendzSettings(TenantId.SYS_TENANT_ID);
        assertNotNull(savedSettings);
        assertEquals(TrendzSynchronizationStatus.NOT_AVAILABLE, savedSettings.synchronizationResult().status());
    }

    @Test
    public void testPerformSync_createApiKey() {
        ObjectNode trendzInfoResponse = JacksonUtil.newObjectNode();
        trendzInfoResponse.put("version", TEST_TRENDZ_VERSION);
        trendzInfoResponse.put("artifact", "trendz");
        when(restTemplate.exchange(
                eq(TEST_TRENDZ_URL + TRENDZ_INFO_URI),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(new ResponseEntity<>(trendzInfoResponse, HttpStatus.OK));

        ObjectNode syncResponse = JacksonUtil.newObjectNode();
        syncResponse.put("version", TEST_TRENDZ_VERSION);
        syncResponse.put("type", TrendzSynchronizationResultType.SYNC_COMPLETED.name());
        syncResponse.put("status", TrendzSynchronizationStatus.SYNCED.name());
        syncResponse.put("message", "Sync completed successfully");
        when(systemSecurityService.getBaseUrl(eq(TenantId.SYS_TENANT_ID), any(), any())).thenReturn(null);
        when(restTemplate.exchange(
                eq(TEST_TRENDZ_URL + TRENDZ_SYNC_INIT_URI),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(Class.class)
        )).thenAnswer(invocation -> {
            Class<?> responseType = invocation.getArgument(3);
            Object convertedResponse = JacksonUtil.convertValue(syncResponse, responseType);
            return new ResponseEntity<>(convertedResponse, HttpStatus.OK);
        });

        TrendzSettings result = trendzSyncService.performSync(TenantId.SYS_TENANT_ID, currentUserId);

        assertNotNull(result);

        ApiKey apiKeyByDescription = apiKeyService.findInternalApiKeyByDescription(TenantId.SYS_TENANT_ID, DefaultTrendzSyncService.TRENDZ_API_KEY_DESCRIPTION);
        assertNotNull(apiKeyByDescription);
        assertTrue(apiKeyByDescription.isInternal());
        assertEquals(DefaultTrendzSyncService.TRENDZ_API_KEY_DESCRIPTION, apiKeyByDescription.getDescription());
    }

    @Test
    public void testPerformHealthcheck_whenDisabled() {
        ReflectionTestUtils.setField(trendzSyncService, "trendzEnabled", false);

        TrendzHealthcheckResult result = trendzSyncService.performHealthcheck();

        assertNotNull(result);
        assertEquals(TrendzSynchronizationResultType.SYNC_DISABLED, result.type());
    }

    @Test
    public void testPerformHealthcheck_notInitialized() {
        TrendzHealthcheckResult result = trendzSyncService.performHealthcheck();

        assertNotNull(result);
        assertEquals(TrendzSynchronizationResultType.SYNC_NOT_INITIALIZED, result.type());
    }

    @Test
    public void testPerformHealthcheck_success() {
        TrendzConfiguration config = new TrendzConfiguration(TEST_TRENDZ_URL, TEST_TB_URL);
        TrendzSynchronizationResult syncResult = new TrendzSynchronizationResult(
                TEST_TRENDZ_VERSION, System.currentTimeMillis(),
                TrendzSynchronizationResultType.SYNC_COMPLETED,
                TrendzSynchronizationStatus.SYNCED
        );
        TrendzSettings settings = new TrendzSettings(config, syncResult);
        trendzSettingsService.saveTrendzSettings(TenantId.SYS_TENANT_ID, settings);

        ObjectNode trendzInfoResponse = JacksonUtil.newObjectNode();
        trendzInfoResponse.put("version", TEST_TRENDZ_VERSION);
        trendzInfoResponse.put("artifact", "trendz");
        when(restTemplate.exchange(
                eq(TEST_TRENDZ_URL + TRENDZ_INFO_URI),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(new ResponseEntity<>(trendzInfoResponse, HttpStatus.OK));

        ObjectNode syncResponse = JacksonUtil.newObjectNode();
        syncResponse.put("version", TEST_TRENDZ_VERSION);
        syncResponse.put("type", TrendzSynchronizationResultType.SYNC_COMPLETED.name());
        syncResponse.put("status", TrendzSynchronizationStatus.SYNCED.name());
        syncResponse.put("message", "Sync completed successfully");
        when(systemSecurityService.getBaseUrl(eq(TenantId.SYS_TENANT_ID), any(), any())).thenReturn(TEST_EXTERNAL_TB_URL);
        when(restTemplate.exchange(
                eq(TEST_TRENDZ_URL + TRENDZ_SYNC_INIT_URI),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(Class.class)
        )).thenAnswer(invocation -> {
            Class<?> responseType = invocation.getArgument(3);
            Object convertedResponse = JacksonUtil.convertValue(syncResponse, responseType);
            return new ResponseEntity<>(convertedResponse, HttpStatus.OK);
        });

        trendzSyncService.performSync(TenantId.SYS_TENANT_ID, currentUserId);

        ObjectNode healthcheckResponse = JacksonUtil.newObjectNode();
        healthcheckResponse.put("version", TEST_TRENDZ_VERSION);
        healthcheckResponse.put("type", TrendzSynchronizationResultType.SYNC_COMPLETED.name());
        healthcheckResponse.put("status", TrendzSynchronizationStatus.SYNCED.name());
        healthcheckResponse.put("message", "Healthcheck passed");
        when(restTemplate.exchange(
                eq(TEST_TRENDZ_URL + TRENDZ_HEALTHCHECK_URI),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(Class.class)
        )).thenAnswer(invocation -> {
            Class<?> responseType = invocation.getArgument(3);
            Object convertedResponse = JacksonUtil.convertValue(healthcheckResponse, responseType);
            return new ResponseEntity<>(convertedResponse, HttpStatus.OK);
        });

        TrendzHealthcheckResult result = trendzSyncService.performHealthcheck();

        assertNotNull(result);
        assertEquals(TrendzSynchronizationResultType.SYNC_COMPLETED, result.type());
        assertEquals("Healthcheck passed", result.message());
    }

    @Test
    public void testPerformHealthcheck_trendzUnreachable() {
        TrendzConfiguration config = new TrendzConfiguration(TEST_TRENDZ_URL, TEST_TB_URL);
        TrendzSynchronizationResult syncResult = new TrendzSynchronizationResult(
                TEST_TRENDZ_VERSION, System.currentTimeMillis(),
                TrendzSynchronizationResultType.SYNC_COMPLETED,
                TrendzSynchronizationStatus.SYNCED
        );
        TrendzSettings settings = new TrendzSettings(config, syncResult);
        trendzSettingsService.saveTrendzSettings(TenantId.SYS_TENANT_ID, settings);

        ObjectNode trendzInfoResponse = JacksonUtil.newObjectNode();
        trendzInfoResponse.put("version", TEST_TRENDZ_VERSION);
        trendzInfoResponse.put("artifact", "trendz");
        when(restTemplate.exchange(
                eq(TEST_TRENDZ_URL + TRENDZ_INFO_URI),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(new ResponseEntity<>(trendzInfoResponse, HttpStatus.OK));

        ObjectNode syncResponse = JacksonUtil.newObjectNode();
        syncResponse.put("version", TEST_TRENDZ_VERSION);
        syncResponse.put("type", TrendzSynchronizationResultType.SYNC_COMPLETED.name());
        syncResponse.put("status", TrendzSynchronizationStatus.SYNCED.name());
        syncResponse.put("message", "Sync completed successfully");
        when(systemSecurityService.getBaseUrl(eq(TenantId.SYS_TENANT_ID), any(), any())).thenReturn(TEST_EXTERNAL_TB_URL);
        when(restTemplate.exchange(
                eq(TEST_TRENDZ_URL + TRENDZ_SYNC_INIT_URI),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(Class.class)
        )).thenAnswer(invocation -> {
            Class<?> responseType = invocation.getArgument(3);
            Object convertedResponse = JacksonUtil.convertValue(syncResponse, responseType);
            return new ResponseEntity<>(convertedResponse, HttpStatus.OK);
        });

        trendzSyncService.performSync(TenantId.SYS_TENANT_ID, currentUserId);

        when(restTemplate.exchange(
                eq(TEST_TRENDZ_URL + TRENDZ_HEALTHCHECK_URI),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(Class.class)
        )).thenThrow(new RestClientException("Connection timeout"));

        TrendzHealthcheckResult result = trendzSyncService.performHealthcheck();

        assertNotNull(result);
        assertEquals(TrendzSynchronizationResultType.TRENDZ_URL_UNREACHABLE, result.type());
        assertEquals(TrendzSynchronizationResultType.TRENDZ_URL_UNREACHABLE.getMessage(), result.message());
    }

}
