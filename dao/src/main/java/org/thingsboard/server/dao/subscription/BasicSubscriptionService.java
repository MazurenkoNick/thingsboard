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
package org.thingsboard.server.dao.subscription;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.license.client.AbstractTbLicenseClient;
import org.thingsboard.license.client.OfflineTbLicenseClient;
import org.thingsboard.license.client.TbLicenseClient;
import org.thingsboard.license.client.TbLicenseClientListener;
import org.thingsboard.license.client.TbLicenseCtx;
import org.thingsboard.license.client.TbLicenseStatisticsService;
import org.thingsboard.license.shared.PlanDataConstants;
import org.thingsboard.license.shared.SubscriptionData;
import org.thingsboard.license.shared.exception.LicenseErrorCode;
import org.thingsboard.license.shared.exception.LicenseException;
import org.thingsboard.server.common.data.LicenseInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Version;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.subscription.SubscriptionEntry;
import org.thingsboard.server.common.data.subscription.SubscriptionErrorCode;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
import org.thingsboard.server.common.data.subscription.SubscriptionInfo;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.tenant.TenantService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.TimeZone;

@Service
@Slf4j
@Profile("!install & !test")
public class BasicSubscriptionService implements SubscriptionService, TbLicenseClientListener {

    @Value("${license.secret}")
    private String licenseSecret;
    @Value("${license.instance_data_file:instance-license.data}")
    private String instanceDataFilePath;
    @Value("${zk.enabled:false}")
    private boolean zkEnabled;

    @Autowired
    protected TenantService tenantService;

    @Autowired
    protected DeviceService deviceService;

    @Autowired
    protected AssetService assetService;

    @Autowired
    protected EdgeService edgeService;

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private TbLicenseCtx licenseCtx;

    @Autowired(required = false)
    private Optional<TbLicenseStatisticsService> licenseStatisticsService;

    private AbstractTbLicenseClient tbLicenseClient;
    private int licenseVersion;
    private boolean isOfflineLicense = false;

    @PostConstruct
    public void init() {
        try {
            if (StringUtils.isNotEmpty(licenseSecret)) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                long releaseDate = sdf.parse(Version.PROJECT_BUILD_DATE).getTime();
                try {
                    tbLicenseClient = OfflineTbLicenseClient.builder()
                            .listener(this)
                            .releaseDate(releaseDate)
                            .encodedLicenseData(licenseSecret)
                            .tbLicenseCtx(licenseCtx)
                            .checkInstanceRequired(zkEnabled) //No need to check instance registry if zk disabled
                            .build();
                    this.isOfflineLicense = true;
                } catch (Exception e) {}
                if (tbLicenseClient == null) {
                    tbLicenseClient = TbLicenseClient.builder()
                            .licenseStatisticsService(licenseStatisticsService)
                            .listener(this)
                            .licenseSecret(this.licenseSecret)
                            .licenseDataFilePath(this.instanceDataFilePath)
                            .releaseDate(releaseDate)
                            .build();
                }
                tbLicenseClient.init();
                this.licenseVersion = tbLicenseClient.getLicenseVersion();
            } else {
                log.error("License secret is not provided!");
                log.error("Please provide license.secret property value in thingsboard.yml or set TB_LICENSE_SECRET environment variable!");
                doExit(-1, LicenseErrorCode.GENERAL_ERROR, false);
            }
        } catch (Exception e) {
            log.error("Failed to init license client", e);
            LicenseErrorCode licenseErrorCode = e instanceof LicenseException ?
                    ((LicenseException) e).getErrorCode() : LicenseErrorCode.GENERAL_ERROR;
            doExit(-1, licenseErrorCode, false);
        }
    }

    @PreDestroy
    public void stop() {
        if (this.tbLicenseClient != null) {
            this.tbLicenseClient.stop();
        }
    }

    @Override
    public void onError(LicenseException e) {
        log.error("License Error occurred: {}({}) - {}", e.getErrorCode(),
                e.getErrorCode().getErrorCode(), e.getMessage());
        if (e.isCritical()) {
            doExit(-1, e.getErrorCode(), true);
        }
    }

    @Override
    public LicenseInfo getLicenseInfo() {
        LicenseInfo licenseInfo = new LicenseInfo();
        long maxDevices = this.isUnlimited(PlanDataConstants.MAX_DEVICES_KEY) ? 0 : tbLicenseClient.getPlanLongValue(PlanDataConstants.MAX_DEVICES_KEY);
        licenseInfo.setMaxDevices(maxDevices);
        long maxAssets = this.isUnlimited(PlanDataConstants.MAX_ASSETS_KEY) ? 0 : tbLicenseClient.getPlanLongValue(PlanDataConstants.MAX_ASSETS_KEY);
        licenseInfo.setMaxAssets(maxAssets);
        long maxEdges = this.isUnlimited(PlanDataConstants.MAX_EDGES_KEY) ? 0 : tbLicenseClient.getPlanLongValue(PlanDataConstants.MAX_EDGES_KEY);
        licenseInfo.setMaxEdges(maxEdges);
        licenseInfo.setWhiteLabelingEnabled(tbLicenseClient.getPlanBooleanValue(PlanDataConstants.WHITELABELING_KEY));
        try {
            licenseInfo.setDevelopment(tbLicenseClient.getPlanBooleanValue(PlanDataConstants.DEVELOPMENT_KEY));
        } catch (Exception e) {
            licenseInfo.setDevelopment(false);
        }
        try {
            licenseInfo.setPlan(tbLicenseClient.getPlanStringValue(PlanDataConstants.PLAN_KEY));
        } catch (Exception e) {
            licenseInfo.setPlan("Unknown");
        }
        return licenseInfo;
    }

    @Override
    public int getLicenseVersion() {
        return tbLicenseClient.getLicenseVersion();
    }

    @Override
    public SubscriptionInfo getSubscriptionInfo() {
        SubscriptionInfo subscriptionInfo = new SubscriptionInfo();
        SubscriptionData subscriptionData = this.tbLicenseClient.getSubscriptionData();
        subscriptionInfo.setSubscriptionId(subscriptionData.getSubscriptionId());
        subscriptionInfo.setSubscriptionPlanName(subscriptionData.getSubscriptionPlanName());
        subscriptionInfo.setPlanUiType(subscriptionData.getPlanUiType());
        subscriptionInfo.setPerpetual(subscriptionData.isPerpetual());
        subscriptionInfo.setOffline(this.isOfflineLicense);
        subscriptionInfo.setCurrentPeriodStartTs(subscriptionData.getCurrentPeriodStartTs());
        subscriptionInfo.setCurrentPeriodEndTs(subscriptionData.getCurrentPeriodEndTs());
        subscriptionInfo.setEndTs(subscriptionData.getEndTs());
        subscriptionInfo.setUpcomingInvoiceDate(subscriptionData.getUpcomingInvoiceDate());
        subscriptionInfo.setUpcomingInvoiceAmountDue(subscriptionData.getUpcomingInvoiceAmountDue());
        subscriptionInfo.setPlanExtraDeviceEnabled(subscriptionData.isPlanExtraDeviceEnabled());
        subscriptionInfo.setPlanEdgeEnabled(subscriptionData.isPlanEdgeEnabled());
        subscriptionInfo.setPlanExtraEdgeEnabled(subscriptionData.isPlanExtraEdgeEnabled());
        subscriptionInfo.setPlanTrendzEnabled(subscriptionData.isPlanTrendzEnabled());
        subscriptionInfo.setDataTs(this.tbLicenseClient.getDataTs());
        subscriptionInfo.setLicenseServerEndpoint(this.tbLicenseClient.getLicenseServerEndpoint());
        subscriptionInfo.setMaxDevices(tbLicenseClient.getPlanLongValue(PlanDataConstants.MAX_DEVICES_KEY));
        subscriptionInfo.setMaxAssets(tbLicenseClient.getPlanLongValue(PlanDataConstants.MAX_ASSETS_KEY));
        subscriptionInfo.setMaxEdges(tbLicenseClient.getPlanLongValue(PlanDataConstants.MAX_EDGES_KEY));
        subscriptionInfo.setWhiteLabelingEnabled(tbLicenseClient.getPlanBooleanValue(PlanDataConstants.WHITELABELING_KEY));
        subscriptionInfo.setEdgeEnabled(tbLicenseClient.getPlanBooleanValue(PlanDataConstants.EDGE_KEY));
        subscriptionInfo.setTrendzEnabled(tbLicenseClient.getPlanBooleanValue(PlanDataConstants.TRENDZ_KEY));
        subscriptionInfo.setDevelopment(tbLicenseClient.getPlanBooleanValue(PlanDataConstants.DEVELOPMENT_KEY));
        subscriptionInfo.setDevicesCount(countDevices());
        subscriptionInfo.setAssetsCount(countAssets());
        subscriptionInfo.setEdgesCount(countEdges());
        return subscriptionInfo;
    }

    @Override
    public SubscriptionInfo refreshLicense() {
        this.tbLicenseClient.refreshInstance();
        return this.getSubscriptionInfo();
    }

    private void doExit(int exitCode, LicenseErrorCode licenseErrorCode, boolean gracefullShutdown) {
        new Thread(() -> {
            log.info("Terminating application due to critical License Error {}({}), exit code [{}]...",
                    licenseErrorCode, licenseErrorCode.getErrorCode(), exitCode);
            int appExitCode = exitCode;
            try {
                if (gracefullShutdown) {
                    appExitCode = SpringApplication.exit(context, () -> exitCode);
                }
            } finally {
                System.exit(appExitCode);
            }
        }, "Shutdown Thread").start();
    }

    private boolean limitReached(long actual, String key) {
        long limit = this.tbLicenseClient.getPlanLongValue(key);
        if (limit > 0) {
            return actual >= limit;
        } else {
            return licenseVersion > 1;
        }
    }

    private boolean isUnlimited(String key) {
        return this.licenseVersion < 2 && this.tbLicenseClient.getPlanLongValue(key) <= 0;
    }

    @Override
    public void createDeviceAllowed(TenantId tenantId) throws SubscriptionException {
        if (isUnlimited(PlanDataConstants.MAX_DEVICES_KEY)) {
            return;
        }
        long actualCount = countDevices();
        if (limitReached(actualCount, PlanDataConstants.MAX_DEVICES_KEY)) {
            log.error("Maximum allowed devices limit reached!");
            throw new SubscriptionException("Maximum allowed devices limit reached!",
                    SubscriptionErrorCode.LIMIT_REACHED, SubscriptionEntry.DEVICE_COUNT, this.tbLicenseClient.getPlanLongValue(PlanDataConstants.MAX_DEVICES_KEY));
        }
    }

    @Override
    public void createAssetAllowed(TenantId tenantId) throws SubscriptionException {
        if (isUnlimited(PlanDataConstants.MAX_ASSETS_KEY)) {
            return;
        }
        long actualCount = countAssets();
        if (limitReached(actualCount, PlanDataConstants.MAX_ASSETS_KEY)) {
            log.error("Maximum allowed assets limit reached!");
            throw new SubscriptionException("Maximum allowed assets limit reached!",
                    SubscriptionErrorCode.LIMIT_REACHED, SubscriptionEntry.ASSET_COUNT, this.tbLicenseClient.getPlanLongValue(PlanDataConstants.MAX_ASSETS_KEY));
        }
    }

    @Override
    public void createEdgeAllowed(TenantId tenantId) throws SubscriptionException {
        if (this.licenseVersion > 1) {
            long actualCount = countEdges();
            if (limitReached(actualCount, PlanDataConstants.MAX_EDGES_KEY)) {
                log.error("Maximum allowed edges limit reached!");
                throw new SubscriptionException("Maximum allowed edges limit reached!",
                        SubscriptionErrorCode.LIMIT_REACHED, SubscriptionEntry.EDGE_COUNT, this.tbLicenseClient.getPlanLongValue(PlanDataConstants.MAX_EDGES_KEY));
            }
        }
    }

    @Override
    public void whiteLabelingAllowed(TenantId tenantId) throws SubscriptionException {
        if (!this.tbLicenseClient.getPlanBooleanValue(PlanDataConstants.WHITELABELING_KEY)) {
            throw new SubscriptionException("White Labeling feature is disabled!",
                    SubscriptionErrorCode.FEATURE_DISABLED, SubscriptionEntry.WHITE_LABELING, 0);
        }
    }

    @Override
    public boolean whiteLabelingEnabled(TenantId tenantId) throws SubscriptionException {
        return this.tbLicenseClient.getPlanBooleanValue(PlanDataConstants.WHITELABELING_KEY);
    }

    @Override
    public boolean edgeEnabled(TenantId tenantId) throws SubscriptionException {
        return this.tbLicenseClient.getPlanBooleanValue(PlanDataConstants.EDGE_KEY);
    }

    @Override
    public boolean trendzEnabled(TenantId tenantId) throws SubscriptionException {
        return this.tbLicenseClient.getPlanBooleanValue(PlanDataConstants.TRENDZ_KEY);
    }

    @Override
    public boolean isDevelopment(TenantId tenantId) throws SubscriptionException {
        try {
            return this.tbLicenseClient.getPlanBooleanValue(PlanDataConstants.DEVELOPMENT_KEY);
        } catch (Exception e) {
            return false;
        }
    }

    private long countDevices() {
        return deviceService.countDevices();
    }

    private long countAssets() {
        return assetService.countAssets();
    }

    private long countEdges() {
        return edgeService.countEdges();
    }

}
