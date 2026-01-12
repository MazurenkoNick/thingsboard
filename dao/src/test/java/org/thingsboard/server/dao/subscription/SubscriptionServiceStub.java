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
package org.thingsboard.server.dao.subscription;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.LicenseInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
import org.thingsboard.server.common.data.subscription.SubscriptionInfo;

@Service
@Profile("test")
public class SubscriptionServiceStub implements SubscriptionService {

    private static final long UNLIMITED = 0L;
    private static final String DEFAULT_PLAN = "ThingsBoard PE Test";
    private static final String DEFAULT_SUBSCRIPTION_ID = "test-subscription";
    private static final String DEFAULT_LICENSE_SERVER_ENDPOINT = "test";

    private final LicenseInfo licenseInfo;
    private final SubscriptionInfo subscriptionInfo;

    public SubscriptionServiceStub() {
        this.licenseInfo = buildLicenseInfo();
        this.subscriptionInfo = buildSubscriptionInfo(this.licenseInfo);
    }

    @Override
    public void createDeviceAllowed(TenantId tenantId) throws SubscriptionException {
    }

    @Override
    public void createAssetAllowed(TenantId tenantId) throws SubscriptionException {
    }

    @Override
    public void createEdgeAllowed(TenantId tenantId) throws SubscriptionException {
    }

    @Override
    public boolean isCreateEdgeAllowed(TenantId tenantId) {
        return true;
    }

    @Override
    public void whiteLabelingAllowed(TenantId tenantId) throws SubscriptionException {
    }

    @Override
    public boolean whiteLabelingEnabled(TenantId tenantId) throws SubscriptionException {
        return true;
    }

    @Override
    public boolean edgeEnabled(TenantId tenantId) throws SubscriptionException {
        return true;
    }

    @Override
    public boolean trendzEnabled(TenantId tenantId) throws SubscriptionException {
        return true;
    }

    @Override
    public boolean isDevelopment(TenantId tenantId) throws SubscriptionException {
        return licenseInfo.isDevelopment();
    }

    @Override
    public boolean solutionTemplateLevelAllowed(TenantId tenantId, String solutionTemplateLevel) throws SubscriptionException {
        return true;
    }

    @Override
    public LicenseInfo getLicenseInfo() {
        // Return a copy to prevent accidental mutation between tests.
        return new LicenseInfo(licenseInfo);
    }

    @Override
    public int getLicenseVersion() {
        return 2;
    }

    @Override
    public SubscriptionInfo getSubscriptionInfo() {
        SubscriptionInfo copy = new SubscriptionInfo();
        copy.setSubscriptionId(subscriptionInfo.getSubscriptionId());
        copy.setSubscriptionPlanName(subscriptionInfo.getSubscriptionPlanName());
        copy.setPlanUiType(subscriptionInfo.getPlanUiType());
        copy.setPerpetual(subscriptionInfo.isPerpetual());
        copy.setOffline(subscriptionInfo.isOffline());
        copy.setCurrentPeriodStartTs(subscriptionInfo.getCurrentPeriodStartTs());
        copy.setCurrentPeriodEndTs(subscriptionInfo.getCurrentPeriodEndTs());
        copy.setEndTs(subscriptionInfo.getEndTs());
        copy.setUpcomingInvoiceDate(subscriptionInfo.getUpcomingInvoiceDate());
        copy.setUpcomingInvoiceAmountDue(subscriptionInfo.getUpcomingInvoiceAmountDue());
        copy.setPlanExtraDeviceEnabled(subscriptionInfo.isPlanExtraDeviceEnabled());
        copy.setPlanEdgeEnabled(subscriptionInfo.isPlanEdgeEnabled());
        copy.setPlanExtraEdgeEnabled(subscriptionInfo.isPlanExtraEdgeEnabled());
        copy.setPlanTrendzEnabled(subscriptionInfo.isPlanTrendzEnabled());
        copy.setDataTs(subscriptionInfo.getDataTs());
        copy.setLicenseServerEndpoint(subscriptionInfo.getLicenseServerEndpoint());
        copy.setMaxDevices(subscriptionInfo.getMaxDevices());
        copy.setMaxAssets(subscriptionInfo.getMaxAssets());
        copy.setMaxEdges(subscriptionInfo.getMaxEdges());
        copy.setWhiteLabelingEnabled(subscriptionInfo.isWhiteLabelingEnabled());
        copy.setEdgeEnabled(subscriptionInfo.isEdgeEnabled());
        copy.setTrendzEnabled(subscriptionInfo.isTrendzEnabled());
        copy.setDevelopment(subscriptionInfo.isDevelopment());
        copy.setDevicesCount(subscriptionInfo.getDevicesCount());
        copy.setAssetsCount(subscriptionInfo.getAssetsCount());
        copy.setEdgesCount(subscriptionInfo.getEdgesCount());
        return copy;
    }

    @Override
    public SubscriptionInfo refreshLicense() {
        return getSubscriptionInfo();
    }

    private static LicenseInfo buildLicenseInfo() {
        LicenseInfo licenseInfo = new LicenseInfo();
        licenseInfo.setMaxDevices(UNLIMITED);
        licenseInfo.setMaxAssets(UNLIMITED);
        licenseInfo.setMaxEdges(UNLIMITED);
        licenseInfo.setWhiteLabelingEnabled(true);
        licenseInfo.setDevelopment(false);
        licenseInfo.setPlan(DEFAULT_PLAN);
        return licenseInfo;
    }

    private static SubscriptionInfo buildSubscriptionInfo(LicenseInfo licenseInfo) {
        long now = System.currentTimeMillis();
        SubscriptionInfo info = new SubscriptionInfo();
        info.setSubscriptionId(DEFAULT_SUBSCRIPTION_ID);
        info.setSubscriptionPlanName(licenseInfo.getPlan());
        info.setPlanUiType("test");
        info.setPerpetual(true);
        info.setOffline(true);
        info.setCurrentPeriodStartTs(now);
        info.setCurrentPeriodEndTs(now);
        info.setEndTs(now);
        info.setUpcomingInvoiceDate(null);
        info.setUpcomingInvoiceAmountDue(null);
        info.setPlanExtraDeviceEnabled(true);
        info.setPlanEdgeEnabled(true);
        info.setPlanExtraEdgeEnabled(true);
        info.setPlanTrendzEnabled(true);
        info.setDataTs(now);
        info.setLicenseServerEndpoint(DEFAULT_LICENSE_SERVER_ENDPOINT);
        info.setMaxDevices(licenseInfo.getMaxDevices());
        info.setMaxAssets(licenseInfo.getMaxAssets());
        info.setMaxEdges(licenseInfo.getMaxEdges());
        info.setWhiteLabelingEnabled(licenseInfo.isWhiteLabelingEnabled());
        info.setEdgeEnabled(true);
        info.setTrendzEnabled(true);
        info.setDevelopment(licenseInfo.isDevelopment());
        info.setDevicesCount(0);
        info.setAssetsCount(0);
        info.setEdgesCount(0);
        return info;
    }
}