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
package org.thingsboard.server.service.install.update;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.trendz.TrendzConfiguration;
import org.thingsboard.server.common.data.trendz.TrendzSettings;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationResult;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationResultType;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationStatus;
import org.thingsboard.server.dao.dashboard.DashboardDao;
import org.thingsboard.server.dao.resource.TbResourceDao;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.widget.WidgetTypeDao;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Profile("install")
@Component
@RequiredArgsConstructor
public class TrendzUpdater {

    private final DashboardDao dashboardDao;
    private final WidgetTypeDao widgetTypeDao;
    private final TbResourceDao tbResourceDao;


    public void labelWidgetTypesAsDeprecatedByFqns(Set<String> fqns) {
        if (fqns.isEmpty()) {
            return;
        }
        fqns.forEach(fqn -> Validator.validateString(fqn, f -> "Incorrect fqn " + f));
        this.widgetTypeDao.labelWidgetTypesAsDeprecatedByFqns(fqns);
    }

    public Optional<URL> findUniqueTrendzBaseUrlFromWidgetTypes(Set<String> fqns) throws MalformedURLException {
        if (fqns.isEmpty()) {
            return Optional.empty();
        }
        fqns.forEach(fqn -> Validator.validateString(fqn, f -> "Incorrect fqn " + f));
        Set<String> urls = this.widgetTypeDao.findUniqueExternalHostsInAnalyticsBundleByFqns(fqns);
        if (urls.size() != 1) {
            return Optional.empty();
        }
        String urlString = urls.iterator().next();
        URL url = new URL(urlString);
        return Optional.of(url);
    }

    public void replacePatternInAllDashboardsConfigurations(String pattern, String replacement) {
        this.dashboardDao.replacePatternInAllDashboardsConfigurations(pattern, replacement);
    }

    public void deleteAllTenantResourcesByResourceKey(String resourceKey) {
        Validator.validateString(resourceKey, f -> "Incorrect resource key " + f);
        this.tbResourceDao.deleteAllTenantResourcesByResourceKey(resourceKey);
    }

    public TrendzSettings createSettings(String trendzUrl, String tbUrl) {
        TrendzConfiguration config = new TrendzConfiguration(trendzUrl, tbUrl);
        TrendzSynchronizationResult syncResult = new TrendzSynchronizationResult(
                null, 0L, TrendzSynchronizationResultType.SYNC_NOT_INITIALIZED, TrendzSynchronizationStatus.NOT_AVAILABLE
        );
        return new TrendzSettings(config, syncResult);
    }
}
