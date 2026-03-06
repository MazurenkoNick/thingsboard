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
package org.thingsboard.server.service.ttl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.dao.notification.NotificationRequestDao;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.discovery.PartitionService;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.dao.model.ModelConstants.NOTIFICATION_TABLE_NAME;

@Slf4j
@Service
@ConditionalOnExpression("${sql.ttl.notifications.enabled:true} && ${sql.ttl.notifications.ttl:0} > 0")
public class NotificationsCleanUpService extends AbstractCleanUpService {

    private final SqlPartitioningRepository partitioningRepository;
    private final NotificationRequestDao notificationRequestDao;
    private final TenantService tenantService;

    @Value("${sql.ttl.notifications.ttl:2592000}")
    private long ttlInSec;
    @Value("${sql.notifications.partition_size:168}")
    private int partitionSizeInHours;
    @Value("${sql.ttl.notifications.removal_batch_size:10000}")
    private int removalBatchSize;

    public NotificationsCleanUpService(PartitionService partitionService, SqlPartitioningRepository partitioningRepository,
                                       NotificationRequestDao notificationRequestDao, TenantService tenantService) {
        super(partitionService);
        this.partitioningRepository = partitioningRepository;
        this.notificationRequestDao = notificationRequestDao;
        this.tenantService = tenantService;
    }

    @Scheduled(initialDelayString = "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.notifications.checking_interval_ms:86400000})}",
            fixedDelayString = "${sql.ttl.notifications.checking_interval_ms:86400000}")
    public void cleanUp() {
        long expTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(ttlInSec);
        long partitionDurationMs = TimeUnit.HOURS.toMillis(partitionSizeInHours);
        if (!isSystemTenantPartitionMine()) {
            partitioningRepository.cleanupPartitionsCache(NOTIFICATION_TABLE_NAME, expTime, partitionDurationMs);
            return;
        }

        long lastRemovedNotificationTs = partitioningRepository.dropPartitionsBefore(NOTIFICATION_TABLE_NAME, expTime, partitionDurationMs);
        if (lastRemovedNotificationTs > 0) {
            long gap = TimeUnit.MINUTES.toMillis(10);
            long requestExpTime = lastRemovedNotificationTs - TimeUnit.SECONDS.toMillis(NotificationRequestConfig.MAX_SENDING_DELAY) - gap;
            cleanUpNotificationRequests(requestExpTime);
        }
    }

    private void cleanUpNotificationRequests(long expirationTime) {
        log.info("Starting notification requests cleanup for records older than {}", Instant.ofEpochMilli(expirationTime));
        int totalRemoved = 0;
        int tenantsProcessed = 0;

        // Clean up SYSADMIN's notification requests:
        try {
            totalRemoved += cleanUpByTenant(TenantId.SYS_TENANT_ID, expirationTime);
        } catch (Exception e) {
            log.warn("Failed to clean up notification requests for sysadmin {}", TenantId.SYS_TENANT_ID, e);
        }
        // Clean up notification requests for tenants
        PageDataIterable<TenantId> tenants = new PageDataIterable<>(tenantService::findTenantsIds, 10_000);
        for (TenantId tenantId : tenants) {
            try {
                if (!isTenantPartitionMine(tenantId)) {
                    continue;
                }
                int tenantRemoved = cleanUpByTenant(tenantId, expirationTime);
                totalRemoved += tenantRemoved;
                tenantsProcessed++;
                if (tenantRemoved > 0) {
                    log.trace("Removed {} notification requests for tenant {}", tenantRemoved, tenantId);
                }
            } catch (Exception e) {
                log.warn("Failed to clean up notification requests for tenant {}", tenantId, e);
            }
        }

        log.info("Notification requests cleanup completed. Processed {} tenants, removed {} total records older than {}", tenantsProcessed, totalRemoved, Instant.ofEpochMilli(expirationTime));
    }

    private int cleanUpByTenant(TenantId tenantId, long expirationTime) {
        int totalRemoved = 0;
        int batchRemoved;

        do {
            batchRemoved = notificationRequestDao.removeByTenantIdAndCreatedTimeBeforeBatch(tenantId, expirationTime, removalBatchSize);
            totalRemoved += batchRemoved;

            if (batchRemoved > 0) {
                log.trace("Removed {} notification requests in batch for tenant {}", batchRemoved, tenantId);
            }
        } while (batchRemoved >= removalBatchSize);

        return totalRemoved;
    }

}
