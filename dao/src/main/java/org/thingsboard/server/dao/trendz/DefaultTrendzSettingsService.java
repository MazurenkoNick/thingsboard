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
package org.thingsboard.server.dao.trendz;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.trendz.TrendzSettings;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultTrendzSettingsService implements TrendzSettingsService {

    private final AdminSettingsService adminSettingsService;

    private static final String SETTINGS_KEY = "trendz";

    @CacheEvict(cacheNames = CacheConstants.TRENDZ_SETTINGS_CACHE, key = "'system'")
    @Override
    public void saveTrendzSettings(TrendzSettings settings) {
        log.trace("Executing saveTrendzSettings [{}]", settings);
        AdminSettings adminSettings = Optional.ofNullable(adminSettingsService.findAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, SETTINGS_KEY))
                .orElseGet(() -> {
                    AdminSettings newAdminSettings = new AdminSettings();
                    newAdminSettings.setTenantId(TenantId.SYS_TENANT_ID);
                    newAdminSettings.setKey(SETTINGS_KEY);
                    return newAdminSettings;
                });
        adminSettings.setJsonValue(JacksonUtil.valueToTree(settings));
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, adminSettings);
    }

    @Cacheable(cacheNames = CacheConstants.TRENDZ_SETTINGS_CACHE, key = "'system'")
    @Override
    public TrendzSettings findTrendzSettings() {
        log.trace("Executing findTrendzSettings");
        return Optional.ofNullable(adminSettingsService.findAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, SETTINGS_KEY))
                .map(adminSettings -> JacksonUtil.treeToValue(adminSettings.getJsonValue(), TrendzSettings.class))
                .orElse(null);
    }

    @CacheEvict(cacheNames = CacheConstants.TRENDZ_SETTINGS_CACHE, key = "'system'")
    @Override
    public void deleteTrendzSettings() {
        log.trace("Executing deleteTrendzSettings");
        adminSettingsService.deleteAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, SETTINGS_KEY);
    }

}
