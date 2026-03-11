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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.notification.NotificationRequestDao;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.discovery.PartitionService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NotificationsCleanUpServiceTest {

    @Mock
    private PartitionService partitionService;
    @Mock
    private SqlPartitioningRepository partitioningRepository;
    @Mock
    private NotificationRequestDao notificationRequestDao;
    @Mock
    private TenantService tenantService;

    private NotificationsCleanUpService cleanUpService;

    private static final int BATCH_SIZE = 3;

    @BeforeEach
    public void setUp() {
        cleanUpService = new NotificationsCleanUpService(partitionService, partitioningRepository, notificationRequestDao, tenantService);
        ReflectionTestUtils.setField(cleanUpService, "ttlInSec", 2592000L);
        ReflectionTestUtils.setField(cleanUpService, "partitionSizeInHours", 168);
        ReflectionTestUtils.setField(cleanUpService, "removalBatchSize", BATCH_SIZE);
    }

    @Test
    public void testBatchLoopCallsDaoMultipleTimes() {
        TopicPartitionInfo myPartition = TopicPartitionInfo.builder().topic("tb_core").myPartition(true).build();
        when(partitionService.resolve(any(), any(), any())).thenReturn(myPartition);
        when(partitioningRepository.dropPartitionsBefore(anyString(), anyLong(), anyLong()))
                .thenReturn(System.currentTimeMillis());

        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        when(tenantService.findTenantsIds(any()))
                .thenReturn(new PageData<>(List.of(tenantId), 1, 1, false));

        // Sysadmin: returns 3 (full batch), then 1 (partial) -> 2 calls
        when(notificationRequestDao.removeByTenantIdAndCreatedTimeBeforeBatch(eq(TenantId.SYS_TENANT_ID), anyLong(), eq(BATCH_SIZE)))
                .thenReturn(BATCH_SIZE)
                .thenReturn(1);
        // Tenant: returns 3, 3, 0 -> 3 calls
        when(notificationRequestDao.removeByTenantIdAndCreatedTimeBeforeBatch(eq(tenantId), anyLong(), eq(BATCH_SIZE)))
                .thenReturn(BATCH_SIZE)
                .thenReturn(BATCH_SIZE)
                .thenReturn(0);

        cleanUpService.cleanUp();

        verify(notificationRequestDao, times(2))
                .removeByTenantIdAndCreatedTimeBeforeBatch(eq(TenantId.SYS_TENANT_ID), anyLong(), eq(BATCH_SIZE));
        verify(notificationRequestDao, times(3))
                .removeByTenantIdAndCreatedTimeBeforeBatch(eq(tenantId), anyLong(), eq(BATCH_SIZE));
    }

    @Test
    public void testSkipsTenantNotOnMyPartition() {
        TopicPartitionInfo myPartition = TopicPartitionInfo.builder().topic("tb_core").myPartition(true).build();
        TopicPartitionInfo notMyPartition = TopicPartitionInfo.builder().topic("tb_core").myPartition(false).build();
        when(partitionService.resolve(any(), eq(TenantId.SYS_TENANT_ID), eq(TenantId.SYS_TENANT_ID)))
                .thenReturn(myPartition);
        when(partitioningRepository.dropPartitionsBefore(anyString(), anyLong(), anyLong()))
                .thenReturn(System.currentTimeMillis());

        // Sysadmin: no records
        when(notificationRequestDao.removeByTenantIdAndCreatedTimeBeforeBatch(eq(TenantId.SYS_TENANT_ID), anyLong(), eq(BATCH_SIZE)))
                .thenReturn(0);

        TenantId myTenant = TenantId.fromUUID(UUID.randomUUID());
        TenantId otherTenant = TenantId.fromUUID(UUID.randomUUID());
        when(tenantService.findTenantsIds(any()))
                .thenReturn(new PageData<>(List.of(myTenant, otherTenant), 2, 1, false));
        when(partitionService.resolve(any(), eq(myTenant), eq(myTenant))).thenReturn(myPartition);
        when(partitionService.resolve(any(), eq(otherTenant), eq(otherTenant))).thenReturn(notMyPartition);

        when(notificationRequestDao.removeByTenantIdAndCreatedTimeBeforeBatch(eq(myTenant), anyLong(), eq(BATCH_SIZE)))
                .thenReturn(0);

        cleanUpService.cleanUp();

        verify(notificationRequestDao).removeByTenantIdAndCreatedTimeBeforeBatch(eq(myTenant), anyLong(), eq(BATCH_SIZE));
        verify(notificationRequestDao, never()).removeByTenantIdAndCreatedTimeBeforeBatch(eq(otherTenant), anyLong(), anyInt());
    }

    @Test
    public void testNoPartitionsDropped_stillCleansUpRequests() {
        TopicPartitionInfo myPartition = TopicPartitionInfo.builder().topic("tb_core").myPartition(true).build();
        when(partitionService.resolve(any(), any(), any())).thenReturn(myPartition);
        when(partitioningRepository.dropPartitionsBefore(anyString(), anyLong(), anyLong()))
                .thenReturn(0L);

        when(notificationRequestDao.removeByTenantIdAndCreatedTimeBeforeBatch(eq(TenantId.SYS_TENANT_ID), anyLong(), eq(BATCH_SIZE)))
                .thenReturn(0);
        when(tenantService.findTenantsIds(any()))
                .thenReturn(new PageData<>(List.of(), 0, 0, false));

        cleanUpService.cleanUp();

        verify(notificationRequestDao).removeByTenantIdAndCreatedTimeBeforeBatch(eq(TenantId.SYS_TENANT_ID), anyLong(), eq(BATCH_SIZE));
    }

}
