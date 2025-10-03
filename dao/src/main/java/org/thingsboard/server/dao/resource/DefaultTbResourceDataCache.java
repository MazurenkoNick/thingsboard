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
package org.thingsboard.server.dao.resource;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.FluentFuture;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.server.common.data.TbResourceDataInfo;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.sql.JpaExecutorService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultTbResourceDataCache implements TbResourceDataCache {

    private final ResourceService resourceService;
    private final JpaExecutorService executorService;

    @Value("${cache.tbResourceData.maxSize:100000}")
    private int cacheMaxSize;
    @Value("${cache.tbResourceData.timeToLiveInMinutes:44640}")
    private int cacheValueTtl;
    private AsyncLoadingCache<ResourceDataKey, TbResourceDataInfo> cache;

    @PostConstruct
    private void init() {
        cache = Caffeine.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterAccess(cacheValueTtl, TimeUnit.MINUTES)
                .executor(executorService)
                .buildAsync((key, executor) -> CompletableFuture.supplyAsync(() -> resourceService.getResourceDataInfo(key.tenantId(), key.resourceId()), executor));
    }

    @Override
    public FluentFuture<TbResourceDataInfo> getResourceDataInfoAsync(TenantId tenantId, TbResourceId resourceId) {
        log.trace("Retrieving resource data info by id [{}], tenant id [{}] from cache", resourceId, tenantId);
        return DonAsynchron.toFluentFuture(cache.get(new ResourceDataKey(tenantId, resourceId)));
    }

    @Override
    public void evictResourceData(TenantId tenantId, TbResourceId resourceId) {
        cache.asMap().remove(new ResourceDataKey(tenantId, resourceId));
        log.trace("Evicted resource data info with id [{}], tenant id [{}]", resourceId, tenantId);
    }

    record ResourceDataKey (TenantId tenantId, TbResourceId resourceId) {}

}
