/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.mail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.RefreshTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.secret.SecretConfigurationService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static org.thingsboard.server.common.data.mail.MailOauth2Provider.OFFICE_365;

@TbCoreComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenExpCheckService {

    public static final int AZURE_DEFAULT_REFRESH_TOKEN_LIFETIME_IN_DAYS = 90;

    private final TenantService tenantService;
    private final AdminSettingsService adminSettingsService;
    private final SecretConfigurationService secretConfigurationService;

    @Scheduled(initialDelayString = "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${mail.oauth2.refreshTokenCheckingInterval})}",
            fixedDelayString = "${mail.oauth2.refreshTokenCheckingInterval}",
            timeUnit = TimeUnit.SECONDS)
    public void check() throws Exception {
        PageLink pageLink = new PageLink(1000);
        PageData<TenantId> tenantIds;
        do {
            tenantIds = tenantService.findTenantsIds(pageLink);
            for (TenantId tenantId : tenantIds.getData()) {
                try {
                    AdminSettings tenantMailSettings = adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, "mail");
                    refreshTokenIfExpires(tenantId, tenantMailSettings, adminSettingsService::saveAdminSettings);
                } catch (Exception e) {
                    log.error("[{}] Error occurred while checking token", tenantId, e);
                }
            }
            pageLink = pageLink.nextPageLink();
        } while (tenantIds.hasNext());

        AdminSettings systemMailSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail");
        refreshTokenIfExpires(TenantId.SYS_TENANT_ID, systemMailSettings, adminSettingsService::saveAdminSettings);
    }

    private void refreshTokenIfExpires(TenantId tenantId, AdminSettings adminSettings, BiConsumer<TenantId, AdminSettings> saveFunction) throws Exception {
        if (adminSettings != null) {
            JsonNode jsonValue = adminSettings.getJsonValue().deepCopy();
            secretConfigurationService.replaceSecretUsages(tenantId, jsonValue);
            if (jsonValue != null && jsonValue.has("useSystemMailSettings") && !jsonValue.get("useSystemMailSettings").asBoolean() &&
                    jsonValue.has("enableOauth2") && jsonValue.get("enableOauth2").asBoolean() && OFFICE_365.name().equals(jsonValue.get("providerId").asText()) &&
                    jsonValue.has("refreshToken") && jsonValue.has("refreshTokenExpires")) {
                long expiresIn = jsonValue.get("refreshTokenExpires").longValue();
                long tokenLifeDuration = expiresIn - System.currentTimeMillis();
                if (tokenLifeDuration < 0) {
                    ((ObjectNode) adminSettings.getJsonValue()).put("tokenGenerated", false);
                    ((ObjectNode) adminSettings.getJsonValue()).remove("refreshToken");
                    ((ObjectNode) adminSettings.getJsonValue()).remove("refreshTokenExpires");

                    saveFunction.accept(tenantId, adminSettings);
                } else if (tokenLifeDuration < 604800000L) { //less than 7 days
                    log.info("Trying to refresh refresh token.");

                    String clientId = jsonValue.get("clientId").asText();
                    String clientSecret = jsonValue.get("clientSecret").asText();
                    String refreshToken = jsonValue.get("refreshToken").asText();
                    String tokenUri = jsonValue.get("tokenUri").asText();

                    TokenResponse tokenResponse = new RefreshTokenRequest(new NetHttpTransport(), new GsonFactory(),
                            new GenericUrl(tokenUri), refreshToken)
                            .setClientAuthentication(new ClientParametersAuthentication(clientId, clientSecret))
                            .execute();
                    ((ObjectNode) adminSettings.getJsonValue()).put("refreshToken", tokenResponse.getRefreshToken());
                    ((ObjectNode) adminSettings.getJsonValue()).put("refreshTokenExpires", Instant.now().plus(Duration.ofDays(AZURE_DEFAULT_REFRESH_TOKEN_LIFETIME_IN_DAYS)).toEpochMilli());
                    saveFunction.accept(tenantId, adminSettings);
                }
            }
        }
    }

}
