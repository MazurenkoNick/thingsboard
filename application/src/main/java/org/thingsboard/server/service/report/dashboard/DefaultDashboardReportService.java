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
package org.thingsboard.server.service.report.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportConfig;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportData;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.rule.engine.api.DashboardReportService;
import org.thingsboard.server.report.util.WebReportClient;
import org.thingsboard.server.service.security.model.token.AccessJwtToken;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.util.UUID;
import java.util.function.Consumer;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultDashboardReportService implements DashboardReportService {

    @Value("${reports.rate_limits.enabled:false}")
    private boolean rateLimitsEnabled;

    @Value("${reports.rate_limits.configuration:5:300}")
    private String rateLimitsConfiguration;

    private final WebReportClient webReportClient;
    private final SystemSecurityService systemSecurityService;
    private final RateLimitService rateLimitService;

    private void checkLimits(TenantId tenantId) {
        if (rateLimitsEnabled) {
            if (!rateLimitService.checkRateLimit(LimitedApi.REPORTS, (Object) tenantId, rateLimitsConfiguration)) {
                log.trace("[{}] Report generation limits exceeded!", tenantId);
                throw new RuntimeException("Failed to generate report due to rate limits!");
            }
        }
    }

    @Override
    public void generateDashboardReport(String baseUrl, DashboardId dashboardId, TenantId tenantId, UserId userId, String reportName,
                                        JsonNode reportParams, String accessToken, long accessTokenExpiration,
                                        Consumer<DashboardReportData> onSuccess, Consumer<Throwable> onFailure) {
        checkLimits(tenantId);
        log.trace("Executing generateDashboardReport, baseUrl [{}], dashboardId [{}], userId [{}]", baseUrl, dashboardId, userId);

        ObjectNode dashboardReportRequest = JacksonUtil.newObjectNode();
        dashboardReportRequest.put("baseUrl", baseUrl);
        dashboardReportRequest.put("dashboardId", dashboardId.toString());
        dashboardReportRequest.set("reportParams", reportParams);
        dashboardReportRequest.put("name", reportName);
        dashboardReportRequest.put("token", accessToken);
        dashboardReportRequest.put("expiration", accessTokenExpiration);
        webReportClient.requestDashboardReport(dashboardReportRequest, null, onSuccess, onFailure);
    }

    @Override
    public void generateReport(TenantId tenantId, DashboardReportConfig reportConfig, String reportsServerEndpointUrl, Consumer<DashboardReportData> onSuccess, Consumer<Throwable> onFailure) throws ThingsboardException {
        checkLimits(tenantId);
        log.trace("Executing generateReport, reportConfig [{}]", reportConfig);
        AccessJwtToken accessToken = systemSecurityService.createUserAccessToken(tenantId, new UserId(UUID.fromString(reportConfig.getUserId())));
        webReportClient.requestDashboardReport(reportConfig, reportsServerEndpointUrl, accessToken.getToken(),
                accessToken.getClaims().getExpiration().getTime(), onSuccess, onFailure);
    }

}
