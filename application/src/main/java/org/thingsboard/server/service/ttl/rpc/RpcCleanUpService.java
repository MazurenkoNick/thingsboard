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
package org.thingsboard.server.service.ttl.rpc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.rpc.RpcDao;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ttl.AbstractCleanUpService;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@TbCoreComponent
@ConditionalOnExpression("${sql.ttl.rpc.enabled:true}")
public class RpcCleanUpService extends AbstractCleanUpService {

    @Value("${sql.ttl.rpc.removal_batch_size:10000}")
    private int removalBatchSize;

    private final RpcDao rpcDao;
    private final TenantService tenantService;
    private final TbTenantProfileCache tenantProfileCache;

    public RpcCleanUpService(TenantService tenantService, PartitionService partitionService, TbTenantProfileCache tenantProfileCache, RpcDao rpcDao) {
        super(partitionService);
        this.tenantService = tenantService;
        this.tenantProfileCache = tenantProfileCache;
        this.rpcDao = rpcDao;
    }

    @Scheduled(initialDelayString = "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.rpc.checking_interval})}", fixedDelayString = "${sql.ttl.rpc.checking_interval}")
    public void cleanUp() {
        PageDataIterable<TenantId> tenants = new PageDataIterable<>(tenantService::findTenantsIds, 10_000);
        for (TenantId tenantId : tenants) {
            try {
                if (!isTenantPartitionMine(tenantId)) {
                    continue;
                }

                Optional<DefaultTenantProfileConfiguration> tenantProfileConfiguration = tenantProfileCache.get(tenantId).getProfileConfiguration();
                if (tenantProfileConfiguration.isEmpty() || tenantProfileConfiguration.get().getRpcTtlDays() == 0) {
                    continue;
                }

                long ttl = TimeUnit.DAYS.toMillis(tenantProfileConfiguration.get().getRpcTtlDays());
                long expirationTime = System.currentTimeMillis() - ttl;

                int totalRemoved = cleanUpByTenant(tenantId, expirationTime);

                if (totalRemoved > 0) {
                    log.info("Removed {} outdated rpc(s) for tenant {} older than {}", totalRemoved, tenantId, Instant.ofEpochMilli(expirationTime));
                }
            } catch (Exception e) {
                log.warn("Failed to clean up rpc by ttl for tenant {}", tenantId, e);
            }
        }
    }

    private int cleanUpByTenant(TenantId tenantId, long expirationTime) {
        int totalRemoved = 0;
        int batchRemoved;

        do {
            batchRemoved = rpcDao.deleteOutdatedRpcByTenantIdBatch(tenantId, expirationTime, removalBatchSize);
            totalRemoved += batchRemoved;

            if (batchRemoved > 0) {
                log.trace("Removed {} rpc in batch for tenant {}", batchRemoved, tenantId);
            }
        } while (batchRemoved >= removalBatchSize);

        return totalRemoved;
    }

}
