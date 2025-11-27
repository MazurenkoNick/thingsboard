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

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.trendz.TrendzSettings;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.trendz.TrendzSyncService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "trendz", name = "enabled", havingValue = "true")
public class TrendzStartupSynchronizer {

    private final TrendzSyncService trendzSyncService;
    private final UserService userService;
    private final PartitionService partitionService;

    @PostConstruct
    private void init() {
        if (!partitionService.isMyPartition(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID)) {
            return;
        }
        ExecutorService executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("trendz-startup-sync"));
        executor.submit(() -> {
            try {
                performStartupSync();
            } catch (Exception e) {
                log.error("Failed to perform Trendz startup synchronization", e);
            } finally {
                executor.shutdown();
            }
        });
    }

    private void performStartupSync() {
        try {
            PageLink pageLink = new PageLink(1, 0, null, new SortOrder("createdTime", SortOrder.Direction.ASC));
            User sysAdminUser = userService.findSysAdmins(pageLink).getData().get(0);
            TrendzSettings result = trendzSyncService.performSync(TenantId.SYS_TENANT_ID, sysAdminUser.getId());
            log.info("Trendz startup synchronization completed. Status: {}, Result: {}",
                    result.trendzSynchronizationResult().status(),
                    result.trendzSynchronizationResult().resultType());
        } catch (Exception e) {
            log.error("Error during Trendz startup synchronization", e);
        }
    }

}
