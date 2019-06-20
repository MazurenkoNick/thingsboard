/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
import org.springframework.util.StringUtils;
import org.thingsboard.license.client.TbLicenseClient;
import org.thingsboard.license.client.TbLicenseClientListener;
import org.thingsboard.license.shared.exception.LicenseException;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.Version;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.subscription.SubscriptionEntry;
import org.thingsboard.server.common.data.subscription.SubscriptionErrorCode;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.tenant.TenantService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.text.SimpleDateFormat;

@Service
@Slf4j
@Profile("!install")
public class BasicSubscriptionService implements SubscriptionService, TbLicenseClientListener {

    private static final String MAX_DEVICES_KEY = "maxdevices";
    private static final String MAX_ASSETS_KEY = "maxassets";
    private static final String WHITELABELING_KEY = "whitelabeling";

    @Value("${license.secret}")
    private String licenseSecret;

    @Autowired
    protected TenantService tenantService;

    @Autowired
    protected DeviceService deviceService;

    @Autowired
    protected AssetService assetService;

    @Autowired
    private ConfigurableApplicationContext context;

    private TbLicenseClient tbLicenseClient;

    @PostConstruct
    public void init() {
        if (StringUtils.isEmpty(this.licenseSecret)) {
            log.error("License secret is not provided!");
            log.error("Please provide license.secret property value in thingsboard.yml or set TB_LICENSE_SECRET environment variable!");
            doExit();
        } else {
            try {
                tbLicenseClient = TbLicenseClient.builder()
                        .listener(this)
                        .licenseSecret(this.licenseSecret)
                        .releaseDate(new SimpleDateFormat("yyyy-MM-dd").parse(Version.PROJECT_BUILD_DATE).getTime())
                        .build();
                tbLicenseClient.init();
            } catch (Exception e) {
                log.error("Failed to init license client", e);
                doExit();
            }
        }
    }

    @PreDestroy
    public void stop() {
        if (this.tbLicenseClient != null) {
            this.tbLicenseClient.stop();
        }
    }

    @Override
    public void onError(TbLicenseClient tbLicenseClient, LicenseException e) {
        log.error("License Error occurred: {}({}) - {}", e.getErrorCode(),
                e.getErrorCode().getErrorCode(), e.getMessage());
        doExit();
    }

    private void doExit() {
        new Thread(() -> {
            int exitCode = -1;
            log.info("Terminating with exit code [{}]...", exitCode);
            int appExitCode = exitCode;
            try {
                appExitCode = SpringApplication.exit(context, () -> exitCode);
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
            return false;
        }
    }

    @Override
    public void createDeviceAllowed(TenantId tenantId) throws SubscriptionException {
        long actualCount = countDevices();
        if (limitReached(actualCount, MAX_DEVICES_KEY)) {
            log.error("Maximum allowed devices limit reached!");
            throw new SubscriptionException("Maximum allowed devices limit reached!",
                    SubscriptionErrorCode.LIMIT_REACHED, SubscriptionEntry.DEVICE_COUNT, this.tbLicenseClient.getPlanLongValue(MAX_DEVICES_KEY));
        }
    }

    @Override
    public void createAssetAllowed(TenantId tenantId) throws SubscriptionException {
        long actualCount = countAssets();
        if (limitReached(actualCount, MAX_ASSETS_KEY)) {
            log.error("Maximum allowed assets limit reached!");
            throw new SubscriptionException("Maximum allowed assets limit reached!",
                    SubscriptionErrorCode.LIMIT_REACHED, SubscriptionEntry.ASSET_COUNT, this.tbLicenseClient.getPlanLongValue(MAX_ASSETS_KEY));
        }
    }

    @Override
    public void whiteLabelingAllowed(TenantId tenantId) throws SubscriptionException {
        if (!this.tbLicenseClient.getPlanBooleanValue(WHITELABELING_KEY)) {
            throw new SubscriptionException("White Labeling feature is disabled!",
                    SubscriptionErrorCode.FEATURE_DISABLED, SubscriptionEntry.WHITE_LABELING, 0);
        }
    }

    @Override
    public boolean whiteLabelingEnabled(TenantId tenantId) throws SubscriptionException {
        return this.tbLicenseClient.getPlanBooleanValue(WHITELABELING_KEY);
    }

    private long countDevices() {
        long count = 0L;
        TextPageLink pageLink = new TextPageLink(1000);
        TextPageData<Tenant> pageData;
        do {
            pageData = tenantService.findTenants(pageLink);
            for (Tenant tenant : pageData.getData()) {
                count += countDevicesByTenant(tenant.getId());
            }
            pageLink = pageData.getNextPageLink();
        } while (pageData.hasNext());
        return count;
    }

    private long countDevicesByTenant(TenantId tenantId) {
        long count = 0L;
        TextPageLink pageLink = new TextPageLink(1000);
        TextPageData<Device> pageData;
        do {
            pageData = deviceService.findDevicesByTenantId(tenantId, pageLink);
            count += pageData.getData().size();
            pageLink = pageData.getNextPageLink();
        } while (pageData.hasNext());
        return count;
    }

    private long countAssets() {
        long count = 0L;
        TextPageLink pageLink = new TextPageLink(1000);
        TextPageData<Tenant> pageData;
        do {
            pageData = tenantService.findTenants(pageLink);
            for (Tenant tenant : pageData.getData()) {
                count += countAssetsByTenant(tenant.getId());
            }
            pageLink = pageData.getNextPageLink();
        } while (pageData.hasNext());
        return count;
    }

    private long countAssetsByTenant(TenantId tenantId) {
        long count = 0L;
        TextPageLink pageLink = new TextPageLink(1000);
        TextPageData<Asset> pageData;
        do {
            pageData = assetService.findAssetsByTenantId(tenantId, pageLink);
            count += pageData.getData().size();
            pageLink = pageData.getNextPageLink();
        } while (pageData.hasNext());
        return count;
    }

}
