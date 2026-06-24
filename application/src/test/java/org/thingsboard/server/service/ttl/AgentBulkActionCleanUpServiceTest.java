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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.agent.AgentBulkActionService;
import org.thingsboard.server.queue.discovery.PartitionService;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentBulkActionCleanUpServiceTest {

    private static final long DEFAULT_TTL_SEC = 604800L;

    @Mock
    private PartitionService partitionService;
    @Mock
    private AgentBulkActionService agentBulkActionService;
    @Mock
    private TopicPartitionInfo partitionInfo;

    private AgentBulkActionCleanUpService service;

    @BeforeEach
    void setUp() {
        service = new AgentBulkActionCleanUpService(partitionService, agentBulkActionService);
        ReflectionTestUtils.setField(service, "ttlInSec", DEFAULT_TTL_SEC);
        when(partitionService.resolve(any(), any(), any())).thenReturn(partitionInfo);
    }

    @Test
    void cleanUp_skipsWhenNotMyPartition() {
        when(partitionInfo.isMyPartition()).thenReturn(false);

        service.cleanUp();

        verify(agentBulkActionService, never()).cleanUpExpiredBulkActions(any(long.class));
    }

    @Test
    void cleanUp_delegatesWithCorrectExpirationTs() {
        when(partitionInfo.isMyPartition()).thenReturn(true);

        long before = System.currentTimeMillis();
        service.cleanUp();
        long after = System.currentTimeMillis();

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(agentBulkActionService).cleanUpExpiredBulkActions(captor.capture());

        long expectedMin = before - TimeUnit.SECONDS.toMillis(DEFAULT_TTL_SEC);
        long expectedMax = after - TimeUnit.SECONDS.toMillis(DEFAULT_TTL_SEC);
        assertThat(captor.getValue()).isBetween(expectedMin, expectedMax);
    }

    @Test
    void cleanUp_usesConfiguredTtl() {
        long customTtlSec = 86400L;
        ReflectionTestUtils.setField(service, "ttlInSec", customTtlSec);
        when(partitionInfo.isMyPartition()).thenReturn(true);

        long before = System.currentTimeMillis();
        service.cleanUp();
        long after = System.currentTimeMillis();

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(agentBulkActionService).cleanUpExpiredBulkActions(captor.capture());

        long expectedMin = before - TimeUnit.SECONDS.toMillis(customTtlSec);
        long expectedMax = after - TimeUnit.SECONDS.toMillis(customTtlSec);
        assertThat(captor.getValue()).isBetween(expectedMin, expectedMax);
    }
}
