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
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.concurrent.TimeUnit;

@Service
@TbCoreComponent
@Slf4j
@ConditionalOnExpression("${sql.ttl.agent_app_events.enabled:true} && ${sql.ttl.agent_app_events.ttl:0} > 0")
public class AgentAppEventCleanUpService extends AbstractCleanUpService {

    public static final String RANDOM_DELAY_INTERVAL_MS_EXPRESSION =
            "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.agent_app_events.execution_interval_ms:86400000})}";

    @Value("${sql.ttl.agent_app_events.ttl:604800}")
    private long ttlInSec;

    private final AgentAppEventService agentAppEventService;

    public AgentAppEventCleanUpService(PartitionService partitionService, AgentAppEventService agentAppEventService) {
        super(partitionService);
        this.agentAppEventService = agentAppEventService;
    }

    @Scheduled(initialDelayString = RANDOM_DELAY_INTERVAL_MS_EXPRESSION,
            fixedDelayString = "${sql.ttl.agent_app_events.execution_interval_ms:86400000}")
    public void cleanUp() {
        if (!isSystemTenantPartitionMine()) {
            return;
        }
        long expirationTs = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(ttlInSec);
        int removed = agentAppEventService.cleanUpExpiredEvents(expirationTs);
        if (removed > 0) {
            log.info("Cleaned up {} agent app events older than {}", removed, expirationTs);
        }
    }
}
