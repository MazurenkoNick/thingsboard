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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.agent.AgentBulkActionService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.concurrent.TimeUnit;

@Service
@TbCoreComponent
@Slf4j
@ConditionalOnExpression("${sql.ttl.agent_bulk_actions.enabled:true} && ${sql.ttl.agent_bulk_actions.ttl:0} > 0")
public class AgentBulkActionCleanUpService extends AbstractCleanUpService {

    public static final String RANDOM_DELAY_INTERVAL_MS_EXPRESSION =
            "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.agent_bulk_actions.execution_interval_ms:86400000})}";

    @Value("${sql.ttl.agent_bulk_actions.ttl:604800}")
    private long ttlInSec;

    private final AgentBulkActionService agentBulkActionService;

    public AgentBulkActionCleanUpService(PartitionService partitionService, AgentBulkActionService agentBulkActionService) {
        super(partitionService);
        this.agentBulkActionService = agentBulkActionService;
    }

    @Scheduled(initialDelayString = RANDOM_DELAY_INTERVAL_MS_EXPRESSION,
            fixedDelayString = "${sql.ttl.agent_bulk_actions.execution_interval_ms:86400000}")
    public void cleanUp() {
        if (!isSystemTenantPartitionMine()) {
            return;
        }
        long expirationTs = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(ttlInSec);
        log.info("Cleaning up agent bulk actions older than {}", expirationTs);
        agentBulkActionService.cleanUpExpiredBulkActions(expirationTs);
    }
}
