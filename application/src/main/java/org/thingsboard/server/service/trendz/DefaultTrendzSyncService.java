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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.common.data.permission.AuthorityPermissionsInfo;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.trendz.TrendzConfiguration;
import org.thingsboard.server.common.data.trendz.TrendzHealthcheckResult;
import org.thingsboard.server.common.data.trendz.TrendzSettings;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationResult;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationResultType;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationStatus;
import org.thingsboard.server.dao.pat.ApiKeyService;
import org.thingsboard.server.dao.trendz.TrendzSettingsService;
import org.thingsboard.server.dao.trendz.TrendzSyncService;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultTrendzSyncService implements TrendzSyncService {

    public static final String TRENDZ_API_KEY_DESCRIPTION = "Internal API key used to authenticate with Trendz";

    public static final String TRENDZ_INFO_URI = "/apiTrendz/publicApi/info";
    public static final String TRENDZ_SYNC_INIT_URI = "/apiTrendz/publicApi/sync/init";
    public static final String TRENDZ_HEALTHCHECK_URI = "/apiTrendz/publicApi/sync/check";

    private static final String MIN_SUPPORTED_VERSION = "1.15.0";

    private final ApiKeyService apiKeyService;
    private final TrendzSettingsService trendzSettingsService;
    private final SystemSecurityService systemSecurityService;

    @Value("${trendz.enabled:true}")
    private boolean trendzEnabled;

    @Value("${trendz.default_tb_url:}")
    private String defaultTbUrl;

    @Value("${trendz.default_trendz_url:}")
    private String defaultTrendzUrl;

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

    @Override
    public TrendzSettings performSync(TenantId tenantId, UserId userId) {
        if (!trendzEnabled) {
            return saveTrendzSettings(null, null, null, 0L,
                    TrendzSynchronizationResultType.SYNC_DISABLED,
                    TrendzSynchronizationStatus.NOT_AVAILABLE);
        }

        TrendzSettings trendzSettings = trendzSettingsService.findTrendzSettings(TenantId.SYS_TENANT_ID);

        if (!isValidTrendzSettings(trendzSettings)) {
            trendzSettings = createDefaultTrendzSettings();
        }

        String tbUrl = trendzSettings.trendzConfiguration().tbUrl();
        String trendzUrl = trendzSettings.trendzConfiguration().trendzUrl();

        if (tbUrl == null || trendzUrl == null) {
            return saveTrendzSettings(trendzUrl, tbUrl, null, 0L,
                    TrendzSynchronizationResultType.SYNC_DISABLED,
                    TrendzSynchronizationStatus.NOT_AVAILABLE);
        }

        log.info("Starting Trendz synchronization. Trendz URL: {}, TB URL: {}", trendzUrl, tbUrl);

        long updatedTs = System.currentTimeMillis();

        ApiKey trendzApiKey = findOrCreateTrendzApiKey(userId);

        TrendzInfo trendzInfo = validateTrendzConnectionInfo(trendzUrl, tbUrl, updatedTs);
        if (trendzInfo == null) {
            log.debug("Trendz validation failed, sync result is already in settings");
            return trendzSettingsService.findTrendzSettings(TenantId.SYS_TENANT_ID);
        }

        String trendzVersion = trendzInfo.version();

        TrendzHealthcheckResult syncResult = processTrendzInitRequest(trendzUrl, tbUrl, trendzApiKey.getValue(), null);
        if (syncResult == null) {
            log.error("Failed to initiate synchronization with Trendz");
            return saveTrendzSettings(trendzUrl, tbUrl, trendzVersion, updatedTs,
                    TrendzSynchronizationResultType.SYNC_INTERNAL_ERROR,
                    TrendzSynchronizationStatus.AVAILABLE);
        }

        TrendzSettings settings = saveTrendzSettings(trendzUrl, tbUrl, syncResult.version(), updatedTs, syncResult.type(), syncResult.status());
        if (syncResult.type() != TrendzSynchronizationResultType.SYNC_COMPLETED) {
            log.error("Trendz sync failed. Status: {}, Message: {}", syncResult.type(), syncResult.message());
        }

        log.info("Trendz synchronization completed. Status: {}, Result: {}",
                settings.trendzSynchronizationResult().status(),
                settings.trendzSynchronizationResult().type());
        return settings;
    }

    @Override
    public TrendzHealthcheckResult performHealthcheck() {
        if (!trendzEnabled) {
            return new TrendzHealthcheckResult(
                    null,
                    TrendzSynchronizationResultType.SYNC_DISABLED,
                    TrendzSynchronizationStatus.NOT_AVAILABLE,
                    TrendzSynchronizationResultType.SYNC_DISABLED.getMessage()
            );
        }

        TrendzSettings trendzSettings = trendzSettingsService.findTrendzSettings(TenantId.SYS_TENANT_ID);
        if (trendzSettings == null || trendzSettings.trendzSynchronizationResult() == null) {
            return new TrendzHealthcheckResult(
                    null,
                    TrendzSynchronizationResultType.SYNC_NOT_INITIALIZED,
                    TrendzSynchronizationStatus.NOT_AVAILABLE,
                    TrendzSynchronizationResultType.SYNC_NOT_INITIALIZED.getMessage()
            );
        }

        String trendzUrl = trendzSettings.trendzConfiguration().trendzUrl();
        JsonNode rawResponse = checkTrendzReachability(trendzUrl);
        if (rawResponse == null) {
            return new TrendzHealthcheckResult(
                    trendzSettings.trendzSynchronizationResult().version(),
                    TrendzSynchronizationResultType.TRENDZ_URL_UNREACHABLE,
                    TrendzSynchronizationStatus.NOT_AVAILABLE,
                    TrendzSynchronizationResultType.TRENDZ_URL_UNREACHABLE.getMessage()
            );
        }

        TrendzInfo trendzInfo;
        try {
            trendzInfo = JacksonUtil.convertValue(rawResponse, TrendzInfo.class);
        } catch (Exception e) {
            return new TrendzHealthcheckResult(
                    trendzSettings.trendzSynchronizationResult().version(),
                    TrendzSynchronizationResultType.SYNC_INTERNAL_ERROR,
                    TrendzSynchronizationStatus.NOT_AVAILABLE,
                    TrendzSynchronizationResultType.SYNC_INTERNAL_ERROR.getMessage()
            );
        }

        if (trendzInfo != null && !isVersionSupported(trendzInfo.version())) {
            return new TrendzHealthcheckResult(
                    trendzSettings.trendzSynchronizationResult().version(),
                    TrendzSynchronizationResultType.TRENDZ_UNSUPPORTED_VERSION,
                    TrendzSynchronizationStatus.AVAILABLE,
                    TrendzSynchronizationResultType.TRENDZ_UNSUPPORTED_VERSION.getMessage()
            );
        }

        ApiKey trendzApiKey = apiKeyService.findApiKeyByDescription(TenantId.SYS_TENANT_ID, TRENDZ_API_KEY_DESCRIPTION);
        if (trendzApiKey == null || !trendzApiKey.isInternal()) {
            return new TrendzHealthcheckResult(
                    trendzSettings.trendzSynchronizationResult().version(),
                    TrendzSynchronizationResultType.TRENDZ_AUTH_INVALID,
                    TrendzSynchronizationStatus.AVAILABLE,
                    TrendzSynchronizationResultType.TRENDZ_AUTH_INVALID.getMessage()
            );
        }

        return sendHealthcheckRequest(trendzUrl, trendzApiKey.getValue());
    }

    private TrendzHealthcheckResult sendHealthcheckRequest(String trendzUrl, String apiKey) {
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

    @Override
    public void performApiKeyRotationSync(ApiKey newApiKey, ApiKey oldApiKey) {
        if (!trendzEnabled) {
            return;
        }
        try {
            if (!TRENDZ_API_KEY_DESCRIPTION.equals(newApiKey.getDescription()) || !newApiKey.isInternal()) {
                return;
            }

            log.debug("Notifying Trendz about API key rotation. API Key ID: {}", newApiKey.getId());

            TrendzSettings settings = trendzSettingsService.findTrendzSettings(TenantId.SYS_TENANT_ID);
            if (settings == null || settings.trendzConfiguration() == null) {
                log.warn("Trendz settings not found, cannot notify about key rotation");
                return;
            }

            String trendzUrl = settings.trendzConfiguration().trendzUrl();
            String tbUrl = settings.trendzConfiguration().tbUrl();

            if (StringUtils.isEmpty(trendzUrl) || StringUtils.isEmpty(tbUrl)) {
                log.warn("Trendz URL or TB URL not configured, cannot notify about key rotation");
                return;
            }

            processTrendzInitRequest(trendzUrl, tbUrl, newApiKey.getValue(), oldApiKey != null ? oldApiKey.getValue() : null);
        } catch (Exception e) {
            log.error("Error notifying Trendz about API key rotation", e);
        }
    }

    private TrendzSettings createDefaultTrendzSettings() {
        TrendzSettings settings;
        if (StringUtils.isNotBlank(defaultTbUrl) && StringUtils.isNotBlank(defaultTrendzUrl)) {
            settings = createSettings(
                    defaultTrendzUrl, defaultTbUrl,
                    null, 0L,
                    TrendzSynchronizationResultType.SYNC_DISABLED,
                    TrendzSynchronizationStatus.NOT_AVAILABLE
            );
        } else {
            settings = createSettings(
                    null, null,
                    null, 0L,
                    TrendzSynchronizationResultType.SYNC_DISABLED,
                    TrendzSynchronizationStatus.NOT_AVAILABLE
            );
        }
        trendzSettingsService.saveTrendzSettings(TenantId.SYS_TENANT_ID, settings);
        return settings;
    }

    private TrendzInfo validateTrendzConnectionInfo(String trendzUrl, String tbUrl, long updatedTs) {
        // Step 1: Check if Trendz is reachable (get raw JSON response)
        JsonNode rawResponse = checkTrendzReachability(trendzUrl);
        if (rawResponse == null) {
            saveTrendzSettings(trendzUrl, tbUrl, null, updatedTs,
                    TrendzSynchronizationResultType.TRENDZ_URL_UNREACHABLE,
                    TrendzSynchronizationStatus.NOT_AVAILABLE);
            log.warn("Trendz is not reachable at URL: {}", trendzUrl);
            return null;
        }

        // Step 2: Try to parse JSON response into the TrendzInfo structure
        TrendzInfo trendzInfo;
        try {
            trendzInfo = JacksonUtil.convertValue(rawResponse, TrendzInfo.class);
        } catch (Exception e) {
            saveTrendzSettings(trendzUrl, tbUrl, null, updatedTs,
                    TrendzSynchronizationResultType.SYNC_INTERNAL_ERROR,
                    TrendzSynchronizationStatus.NOT_AVAILABLE);
            log.warn("Trendz version info is not recognized from URL: {} - unexpected JSON structure", trendzUrl, e);
            return null;
        }

        // Step 3: Validate version field is present and the Trendz version is supported
        if (trendzInfo != null && !isVersionSupported(trendzInfo.version())) {
            saveTrendzSettings(trendzUrl, tbUrl, trendzInfo.version(), updatedTs,
                    TrendzSynchronizationResultType.TRENDZ_UNSUPPORTED_VERSION,
                    TrendzSynchronizationStatus.AVAILABLE);
            log.warn("Trendz version {} is not supported. Minimum required version: {}", trendzInfo.version(), MIN_SUPPORTED_VERSION);
            return null;
        }

        // All validations passed
        return trendzInfo;
    }

    private TrendzSettings createSettings(String trendzUrl, String tbUrl,
                                          String trendzVersion, Long updatedTs,
                                          TrendzSynchronizationResultType resultType,
                                          TrendzSynchronizationStatus status) {
        TrendzConfiguration config = new TrendzConfiguration(trendzUrl, tbUrl);
        TrendzSynchronizationResult syncResult = new TrendzSynchronizationResult(trendzVersion, updatedTs, resultType, status);
        return new TrendzSettings(config, syncResult);
    }

    private JsonNode checkTrendzReachability(String trendzUrl) {
        return sendTrendzRequest(trendzUrl, TRENDZ_INFO_URI, HttpMethod.GET, null, JsonNode.class, "Checking Trendz reachability");
    }

    private TrendzHealthcheckResult processTrendzInitRequest(String trendzUrl, String tbUrl, String currentApiKey, String prevApiKey) {
        String externalTbUrl = systemSecurityService.getBaseUrl(TenantId.SYS_TENANT_ID, null, null);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("internalTbUrl", tbUrl);
        requestBody.put("externalTbUrl", externalTbUrl != null ? externalTbUrl : tbUrl);
        requestBody.put("currentTbAccessToken", currentApiKey);
        requestBody.put("prevTbAccessToken", prevApiKey);

        return sendTrendzRequest(trendzUrl, TRENDZ_SYNC_INIT_URI, HttpMethod.POST, requestBody, TrendzHealthcheckResult.class, "Initiating Trendz sync");
    }

    private boolean isVersionSupported(String version) {
        if (version == null || version.isBlank()) {
            log.warn("Version is null or empty, treating as unsupported");
            return false;
        }

        try {
            String cleanVersion = version.split("-")[0]; // Remove suffix if present
            String[] versionParts = cleanVersion.split("\\.");
            String[] minVersionParts = MIN_SUPPORTED_VERSION.split("\\.");

            for (int i = 0; i < Math.min(versionParts.length, minVersionParts.length); i++) {
                int current = Integer.parseInt(versionParts[i]);
                int required = Integer.parseInt(minVersionParts[i]);

                if (current > required) {
                    return true;
                } else if (current < required) {
                    return false;
                }
            }

            return true;
        } catch (NumberFormatException e) {
            log.error("Failed to parse version '{}': Invalid number format in version string", version, e);
            return false;
        } catch (Exception e) {
            log.error("Failed to parse version '{}': {}", version, e.getMessage(), e);
            return false;
        }
    }

    private String normalizeUrl(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private <T> T sendTrendzRequest(String trendzUrl, String uriPath, HttpMethod method,
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

    private ApiKey findOrCreateTrendzApiKey(UserId userId) {
        ApiKey trendzApiKey = apiKeyService.findApiKeyByDescription(TenantId.SYS_TENANT_ID, TRENDZ_API_KEY_DESCRIPTION);

        if (trendzApiKey != null && trendzApiKey.isInternal()) {
            log.debug("Found existing Trendz API key: {}", trendzApiKey.getId());
            return trendzApiKey;
        }

        log.info("Creating new Trendz internal API key with configured permissions");
        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
        apiKeyInfo.setTenantId(TenantId.SYS_TENANT_ID);
        apiKeyInfo.setUserId(userId);
        apiKeyInfo.setDescription(TRENDZ_API_KEY_DESCRIPTION);
        apiKeyInfo.setEnabled(true);
        apiKeyInfo.setExpirationTime(0);
        apiKeyInfo.setInternal(true);
        apiKeyInfo.setPermissions(buildTrendzPermissions());

        return apiKeyService.saveApiKey(TenantId.SYS_TENANT_ID, apiKeyInfo);
    }

    private AuthorityPermissionsInfo buildTrendzPermissions() {
        Map<Authority, Map<Resource, Set<Operation>>> permissions = new HashMap<>();

        // TENANT_ADMIN + CUSTOMER_USER permissions (same for both)
        Map<Resource, Set<Operation>> tenantCustomerPermissions = new HashMap<>();
        tenantCustomerPermissions.put(Resource.DEVICE, Set.of(Operation.READ, Operation.READ_ATTRIBUTES, Operation.WRITE_ATTRIBUTES, Operation.READ_TELEMETRY, Operation.WRITE_TELEMETRY));
        tenantCustomerPermissions.put(Resource.ASSET, Set.of(Operation.READ, Operation.READ_ATTRIBUTES, Operation.WRITE_ATTRIBUTES, Operation.READ_TELEMETRY, Operation.WRITE_TELEMETRY));
        tenantCustomerPermissions.put(Resource.ALARM, Set.of(Operation.READ, Operation.CREATE, Operation.WRITE, Operation.DELETE));
        tenantCustomerPermissions.put(Resource.CUSTOMER, Set.of(Operation.READ, Operation.READ_ATTRIBUTES, Operation.WRITE_ATTRIBUTES, Operation.READ_TELEMETRY, Operation.WRITE_TELEMETRY));
        tenantCustomerPermissions.put(Resource.DASHBOARD, Set.of(Operation.READ, Operation.CREATE, Operation.WRITE));

        tenantCustomerPermissions.put(Resource.TENANT, Set.of(Operation.READ));
        tenantCustomerPermissions.put(Resource.USER, Set.of(Operation.READ));
        tenantCustomerPermissions.put(Resource.WHITE_LABELING, Set.of(Operation.READ));
        tenantCustomerPermissions.put(Resource.DEVICE_PROFILE, Set.of(Operation.READ));
        tenantCustomerPermissions.put(Resource.ASSET_PROFILE, Set.of(Operation.READ));

        permissions.put(Authority.TENANT_ADMIN, tenantCustomerPermissions);
        permissions.put(Authority.CUSTOMER_USER, tenantCustomerPermissions);

        // SYS_ADMIN permissions
        Map<Resource, Set<Operation>> sysAdminPermissions = new HashMap<>();
        sysAdminPermissions.put(Resource.ADMIN_SETTINGS, Set.of(Operation.READ, Operation.WRITE));
        sysAdminPermissions.put(Resource.WIDGETS_BUNDLE, Set.of(Operation.READ, Operation.CREATE, Operation.WRITE, Operation.DELETE));
        sysAdminPermissions.put(Resource.WIDGET_TYPE, Set.of(Operation.READ, Operation.CREATE, Operation.WRITE, Operation.DELETE));
        sysAdminPermissions.put(Resource.TB_RESOURCE, Set.of(Operation.READ, Operation.CREATE, Operation.WRITE, Operation.DELETE));

        permissions.put(Authority.SYS_ADMIN, sysAdminPermissions);

        AuthorityPermissionsInfo permissionsInfo = new AuthorityPermissionsInfo();
        permissionsInfo.setOperationsByResource(permissions);
        return permissionsInfo;
    }

    private TrendzSettings saveTrendzSettings(String trendzUrl, String tbUrl,
                                              String version, long updatedTs,
                                              TrendzSynchronizationResultType resultType,
                                              TrendzSynchronizationStatus status) {
        TrendzSettings settings = createSettings(trendzUrl, tbUrl, version, updatedTs, resultType, status);
        trendzSettingsService.saveTrendzSettings(TenantId.SYS_TENANT_ID, settings);
        return settings;
    }

    private boolean isValidTrendzSettings(TrendzSettings settings) {
        return settings != null
                && settings.trendzConfiguration() != null
                && settings.trendzConfiguration().tbUrl() != null
                && settings.trendzConfiguration().trendzUrl() != null;
    }

    private record TrendzInfo(@JsonProperty("version") String version,
                              @JsonProperty("artifact") String artifact,
                              @JsonProperty("name") String name,
                              @JsonProperty("cloud") Boolean cloud,
                              @JsonProperty("test") Boolean test,
                              @JsonProperty("time") String time
    ) implements Serializable {}

}
