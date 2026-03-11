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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.rpc.RpcDao;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.discovery.PartitionService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RpcCleanUpServiceTest {

    @Mock
    private PartitionService partitionService;
    @Mock
    private RpcDao rpcDao;
    @Mock
    private TenantService tenantService;
    @Mock
    private TbTenantProfileCache tenantProfileCache;

    private RpcCleanUpService cleanUpService;

    private static final int BATCH_SIZE = 3;

    @BeforeEach
    public void setUp() {
        cleanUpService = new RpcCleanUpService(tenantService, partitionService, tenantProfileCache, rpcDao);
        ReflectionTestUtils.setField(cleanUpService, "removalBatchSize", BATCH_SIZE);
    }

    @Test
    public void testBatchLoopCallsDaoMultipleTimes() {
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        setupTenant(tenantId, 7);

        // Returns 3 (full batch), 3 (full batch), 1 (partial) -> 3 calls
        when(rpcDao.deleteOutdatedRpcByTenantIdBatch(eq(tenantId), anyLong(), eq(BATCH_SIZE)))
                .thenReturn(BATCH_SIZE)
                .thenReturn(BATCH_SIZE)
                .thenReturn(1);

        cleanUpService.cleanUp();

        verify(rpcDao, times(3)).deleteOutdatedRpcByTenantIdBatch(eq(tenantId), anyLong(), eq(BATCH_SIZE));
    }

    @Test
    public void testSkipsTenantNotOnMyPartition() {
        TenantId myTenant = TenantId.fromUUID(UUID.randomUUID());
        TenantId otherTenant = TenantId.fromUUID(UUID.randomUUID());

        TopicPartitionInfo myPartition = TopicPartitionInfo.builder().topic("tb_core").myPartition(true).build();
        TopicPartitionInfo notMyPartition = TopicPartitionInfo.builder().topic("tb_core").myPartition(false).build();

        when(tenantService.findTenantsIds(any()))
                .thenReturn(new PageData<>(List.of(myTenant, otherTenant), 2, 1, false));
        when(partitionService.resolve(any(), eq(myTenant), eq(myTenant))).thenReturn(myPartition);
        when(partitionService.resolve(any(), eq(otherTenant), eq(otherTenant))).thenReturn(notMyPartition);

        setupTenantProfile(myTenant, 7);
        when(rpcDao.deleteOutdatedRpcByTenantIdBatch(eq(myTenant), anyLong(), eq(BATCH_SIZE)))
                .thenReturn(0);

        cleanUpService.cleanUp();

        verify(rpcDao).deleteOutdatedRpcByTenantIdBatch(eq(myTenant), anyLong(), eq(BATCH_SIZE));
        verify(rpcDao, never()).deleteOutdatedRpcByTenantIdBatch(eq(otherTenant), anyLong(), anyInt());
    }

    @Test
    public void testSkipsTenantWithZeroTtl() {
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        setupTenant(tenantId, 0);

        cleanUpService.cleanUp();

        verify(rpcDao, never()).deleteOutdatedRpcByTenantIdBatch(any(), anyLong(), anyInt());
    }

    private void setupTenant(TenantId tenantId, int rpcTtlDays) {
        TopicPartitionInfo myPartition = TopicPartitionInfo.builder().topic("tb_core").myPartition(true).build();
        when(partitionService.resolve(any(), eq(tenantId), eq(tenantId))).thenReturn(myPartition);
        when(tenantService.findTenantsIds(any()))
                .thenReturn(new PageData<>(List.of(tenantId), 1, 1, false));
        setupTenantProfile(tenantId, rpcTtlDays);
    }

    private void setupTenantProfile(TenantId tenantId, int rpcTtlDays) {
        TenantProfile profile = new TenantProfile();
        TenantProfileData profileData = new TenantProfileData();
        DefaultTenantProfileConfiguration config = new DefaultTenantProfileConfiguration();
        config.setRpcTtlDays(rpcTtlDays);
        profileData.setConfiguration(config);
        profile.setProfileData(profileData);
        when(tenantProfileCache.get(tenantId)).thenReturn(profile);
    }

}
