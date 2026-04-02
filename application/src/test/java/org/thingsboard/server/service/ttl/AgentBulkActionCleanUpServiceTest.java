/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
