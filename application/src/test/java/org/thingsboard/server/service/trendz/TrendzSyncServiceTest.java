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
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import org.thingsboard.server.common.data.trendz.TrendzHealthcheckResult;
import org.thingsboard.server.common.data.trendz.TrendzSettings;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationResult;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationResultType;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationStatus;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.pat.ApiKeyService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.trendz.TrendzSettingsService;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@DaoSqlTest
@TestPropertySource(properties = {
        "trendz.enabled=true",
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

    @BeforeEach
    public void setUp() throws Exception {
        loginSysAdmin();

        ApiKey trendzApiKey = new ApiKey();
        trendzApiKey.setTenantId(TenantId.SYS_TENANT_ID);
        trendzApiKey.setUserId(currentUserId);
        trendzApiKey.setEnabled(true);
        trendzApiKey.setInternal(true);
        trendzApiKey.setDescription("Internal API key used to authenticate with Trendz");
        apiKeyService.saveApiKey(tenantId, trendzApiKey);

        ReflectionTestUtils.setField(trendzSyncService, "restTemplate", restTemplate);
        reset(restTemplate, systemSecurityService);
    }

    @AfterEach
    public void tearDown() {
        ApiKey trendzApiKey = apiKeyService.findApiKeyByDescription(TenantId.SYS_TENANT_ID, DefaultTrendzSyncService.TRENDZ_API_KEY_DESCRIPTION);
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

        Assert.assertNotNull(result);
        Assert.assertEquals(TrendzSynchronizationResultType.SYNC_DISABLED, result.trendzSynchronizationResult().resultType());
        Assert.assertEquals(TrendzSynchronizationStatus.NOT_AVAILABLE, result.trendzSynchronizationResult().status());

        TrendzSettings savedSettings = trendzSettingsService.findTrendzSettings(TenantId.SYS_TENANT_ID);
        Assert.assertNotNull(savedSettings);
        Assert.assertEquals(TrendzSynchronizationResultType.SYNC_DISABLED, savedSettings.trendzSynchronizationResult().resultType());
    }

    @Test
    public void testPerformSync_success() {
        ObjectNode trendzInfoResponse = JacksonUtil.newObjectNode();
        trendzInfoResponse.put("version", TEST_TRENDZ_VERSION);
        trendzInfoResponse.put("artifact", "trendz");
        trendzInfoResponse.put("name", "Trendz Analytics");
        trendzInfoResponse.put("cloud", false);
        when(restTemplate.exchange(
                eq(TEST_TRENDZ_URL + "/api/publicApi/info"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(new ResponseEntity<>(trendzInfoResponse, HttpStatus.OK));

        ObjectNode syncResponse = JacksonUtil.newObjectNode();
        syncResponse.put("trendzVersion", TEST_TRENDZ_VERSION);
        syncResponse.put("syncStatus", TrendzSynchronizationResultType.SYNC_COMPLETED.name());
        syncResponse.put("success", true);
        syncResponse.put("message", "Sync completed successfully");
        when(systemSecurityService.getBaseUrl(eq(TenantId.SYS_TENANT_ID), any(), any())).thenReturn(TEST_EXTERNAL_TB_URL);
        when(restTemplate.exchange(
                eq(TEST_TRENDZ_URL + "/apiTrendz/publicApi/sync/init"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(new ResponseEntity<>(syncResponse, HttpStatus.OK));

        TrendzSettings result = trendzSyncService.performSync(TenantId.SYS_TENANT_ID, currentUserId);

        Assert.assertNotNull(result);
        Assert.assertEquals(TrendzSynchronizationResultType.SYNC_COMPLETED, result.trendzSynchronizationResult().resultType());
        Assert.assertEquals(TrendzSynchronizationStatus.SYNCED, result.trendzSynchronizationResult().status());
        Assert.assertEquals(TEST_TRENDZ_VERSION, result.trendzSynchronizationResult().trendzVersion());

        ApiKey apiKey = apiKeyService.findApiKeyByDescription(TenantId.SYS_TENANT_ID, DefaultTrendzSyncService.TRENDZ_API_KEY_DESCRIPTION);
        Assert.assertNotNull(apiKey);
        Assert.assertTrue(apiKey.isInternal());
        Assert.assertTrue(apiKey.isEnabled());

        TrendzSettings savedSettings = trendzSettingsService.findTrendzSettings(TenantId.SYS_TENANT_ID);
        Assert.assertNotNull(savedSettings);
        Assert.assertEquals(TrendzSynchronizationResultType.SYNC_COMPLETED, savedSettings.trendzSynchronizationResult().resultType());
        Assert.assertEquals(TrendzSynchronizationStatus.SYNCED, savedSettings.trendzSynchronizationResult().status());
    }

    @Test
    public void testPerformSync_trendzUnreachable() {
        when(restTemplate.exchange(
                eq(TEST_TRENDZ_URL + "/api/publicApi/info"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenThrow(new RestClientException("Connection refused"));

        TrendzSettings result = trendzSyncService.performSync(TenantId.SYS_TENANT_ID, currentUserId);

        Assert.assertNotNull(result);
        Assert.assertEquals(TrendzSynchronizationResultType.SYNC_NOT_INITIALIZED, result.trendzSynchronizationResult().resultType());
        Assert.assertEquals(TrendzSynchronizationStatus.NOT_AVAILABLE, result.trendzSynchronizationResult().status());

        TrendzSettings savedSettings = trendzSettingsService.findTrendzSettings(TenantId.SYS_TENANT_ID);
        Assert.assertNotNull(savedSettings);
        Assert.assertEquals(TrendzSynchronizationStatus.NOT_AVAILABLE, savedSettings.trendzSynchronizationResult().status());
    }

    @ParameterizedTest
    @MethodSource("versionSupportProvider")
    public void testPerformSync_versionSupport(String version, boolean shouldBeSupported) {
        Boolean result = ReflectionTestUtils.invokeMethod(trendzSyncService, "isVersionSupported", version);
        assertEquals(shouldBeSupported, result);
    }

    private static Stream<Arguments> versionSupportProvider() {
        return Stream.of(
                Arguments.of("1.14.0", false),
                Arguments.of("1.13.9", false),
                Arguments.of("1.12.5", false),
                Arguments.of("0.9.0", false),
                Arguments.of("1.14.1", true),
                Arguments.of("1.14.2", true),
                Arguments.of("1.15.0", true),
                Arguments.of("2.0.0", true),
                Arguments.of("1.15.0-SNAPSHOT", true),
                Arguments.of("1.15.2.44-TRENDZ", true),
                Arguments.of("1.15.2.44.5-PAAS", true),
                Arguments.of("1.13.0-RC1", false)
        );
    }

    @Test
    public void testPerformSync_createApiKey() {
        ObjectNode trendzInfoResponse = JacksonUtil.newObjectNode();
        trendzInfoResponse.put("version", TEST_TRENDZ_VERSION);
        trendzInfoResponse.put("artifact", "trendz");
        when(restTemplate.exchange(
                eq(TEST_TRENDZ_URL + "/api/publicApi/info"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(new ResponseEntity<>(trendzInfoResponse, HttpStatus.OK));

        ObjectNode syncResponse = JacksonUtil.newObjectNode();
        syncResponse.put("trendzVersion", TEST_TRENDZ_VERSION);
        syncResponse.put("syncStatus", TrendzSynchronizationResultType.SYNC_COMPLETED.name());
        syncResponse.put("success", true);
        when(systemSecurityService.getBaseUrl(eq(TenantId.SYS_TENANT_ID), any(), any())).thenReturn(null);
        when(restTemplate.exchange(
                eq(TEST_TRENDZ_URL + "/apiTrendz/publicApi/sync/init"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(new ResponseEntity<>(syncResponse, HttpStatus.OK));

        ApiKey apiKeyBefore = apiKeyService.findApiKeyByDescription(TenantId.SYS_TENANT_ID, DefaultTrendzSyncService.TRENDZ_API_KEY_DESCRIPTION);
        Assert.assertNull(apiKeyBefore);

        TrendzSettings result = trendzSyncService.performSync(TenantId.SYS_TENANT_ID, currentUserId);

        Assert.assertNotNull(result);

        ApiKey apiKeyAfter = apiKeyService.findApiKeyByDescription(TenantId.SYS_TENANT_ID, DefaultTrendzSyncService.TRENDZ_API_KEY_DESCRIPTION);
        Assert.assertNotNull(apiKeyAfter);
        Assert.assertTrue(apiKeyAfter.isInternal());
        Assert.assertEquals(DefaultTrendzSyncService.TRENDZ_API_KEY_DESCRIPTION, apiKeyAfter.getDescription());
    }

    @Test
    public void testPerformHealthcheck_whenDisabled() {
        ReflectionTestUtils.setField(trendzSyncService, "trendzEnabled", false);

        TrendzHealthcheckResult result = trendzSyncService.performHealthcheck();

        Assert.assertNotNull(result);
        Assert.assertEquals(TrendzSynchronizationResultType.SYNC_DISABLED, result.syncStatus());
        Assert.assertFalse(result.success());
    }

    @Test
    public void testPerformHealthcheck_notInitialized() {
        TrendzHealthcheckResult result = trendzSyncService.performHealthcheck();

        Assert.assertNotNull(result);
        Assert.assertEquals(TrendzSynchronizationResultType.SYNC_NOT_INITIALIZED, result.syncStatus());
        Assert.assertFalse(result.success());
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
                eq(TEST_TRENDZ_URL + "/api/publicApi/info"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(new ResponseEntity<>(trendzInfoResponse, HttpStatus.OK));

        ObjectNode syncResponse = JacksonUtil.newObjectNode();
        syncResponse.put("trendzVersion", TEST_TRENDZ_VERSION);
        syncResponse.put("syncStatus", TrendzSynchronizationResultType.SYNC_COMPLETED.name());
        syncResponse.put("success", true);
        when(systemSecurityService.getBaseUrl(eq(TenantId.SYS_TENANT_ID), any(), any())).thenReturn(TEST_EXTERNAL_TB_URL);
        when(restTemplate.exchange(
                eq(TEST_TRENDZ_URL + "/apiTrendz/publicApi/sync/init"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(new ResponseEntity<>(syncResponse, HttpStatus.OK));

        trendzSyncService.performSync(TenantId.SYS_TENANT_ID, currentUserId);

        ObjectNode healthcheckResponse = JacksonUtil.newObjectNode();
        healthcheckResponse.put("trendzVersion", TEST_TRENDZ_VERSION);
        healthcheckResponse.put("syncStatus", TrendzSynchronizationResultType.SYNC_COMPLETED.name());
        healthcheckResponse.put("success", true);
        healthcheckResponse.put("message", "Healthcheck passed");
        when(restTemplate.exchange(
                eq(TEST_TRENDZ_URL + "/apiTrendz/publicApi/sync/check"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(new ResponseEntity<>(healthcheckResponse, HttpStatus.OK));

        TrendzHealthcheckResult result = trendzSyncService.performHealthcheck();

        Assert.assertNotNull(result);
        Assert.assertTrue(result.success());
        Assert.assertEquals(TrendzSynchronizationResultType.SYNC_COMPLETED, result.syncStatus());
        Assert.assertEquals("Healthcheck passed", result.message());
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
                eq(TEST_TRENDZ_URL + "/api/publicApi/info"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(new ResponseEntity<>(trendzInfoResponse, HttpStatus.OK));

        ObjectNode syncResponse = JacksonUtil.newObjectNode();
        syncResponse.put("trendzVersion", TEST_TRENDZ_VERSION);
        syncResponse.put("syncStatus", TrendzSynchronizationResultType.SYNC_COMPLETED.name());
        syncResponse.put("success", true);
        when(systemSecurityService.getBaseUrl(eq(TenantId.SYS_TENANT_ID), any(), any())).thenReturn(TEST_EXTERNAL_TB_URL);
        when(restTemplate.exchange(
                eq(TEST_TRENDZ_URL + "/apiTrendz/publicApi/sync/init"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(new ResponseEntity<>(syncResponse, HttpStatus.OK));

        trendzSyncService.performSync(TenantId.SYS_TENANT_ID, currentUserId);

        when(restTemplate.exchange(
                eq(TEST_TRENDZ_URL + "/apiTrendz/publicApi/sync/check"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenThrow(new RestClientException("Connection timeout"));

        TrendzHealthcheckResult result = trendzSyncService.performHealthcheck();

        Assert.assertNotNull(result);
        Assert.assertFalse(result.success());
        Assert.assertEquals(TrendzSynchronizationResultType.SYNC_INTERNAL_ERROR, result.syncStatus());
        Assert.assertEquals("Network error or Trendz is not reachable", result.message());
    }

}
