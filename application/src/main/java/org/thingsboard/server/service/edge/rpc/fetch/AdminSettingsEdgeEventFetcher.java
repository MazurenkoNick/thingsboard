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
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Slf4j
public class AdminSettingsEdgeEventFetcher implements EdgeEventFetcher {

    private final AdminSettingsService adminSettingsService;

    @Override
    public PageLink getPageLink(int pageSize) {
        return null;
    }

    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) throws Exception {
        List<EdgeEvent> result = fetchAdminSettingsForKeys(tenantId, edge.getId(), List.of("general", "mail", "connectivity", "jwt", "customMenu"));

        // return PageData object to be in sync with other fetchers
        return new PageData<>(result, 1, result.size(), false);
    }

    private List<EdgeEvent> fetchAdminSettingsForKeys(TenantId tenantId, EdgeId edgeId, List<String> keys) {
        List<EdgeEvent> result = new ArrayList<>();
        for (String key : keys) {
            createAdminSettingsEvent(TenantId.SYS_TENANT_ID, key, edgeId).ifPresent(result::add);
            createAdminSettingsEvent(tenantId, key, edgeId).ifPresent(result::add);
        }
        return result;
    }

    private Optional<EdgeEvent> createAdminSettingsEvent(TenantId tenantId, String key, EdgeId edgeId) {
        AdminSettings settings = adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, key);
        if (settings != null) {
            return Optional.of(EdgeUtils.constructEdgeEvent(tenantId, edgeId, EdgeEventType.ADMIN_SETTINGS,
                    EdgeEventActionType.UPDATED, null, JacksonUtil.valueToTree(settings)));
        }
        return Optional.empty();
    }

}
