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
package org.thingsboard.server.service.queue;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.gen.transport.TransportProtos.AgentBulkOperationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.QueueConsumerManager;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.agent.AgentBulkOperationService;

import java.util.List;
import java.util.concurrent.ExecutorService;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultTbAgentBulkOpsConsumerService {

    private static final String CONSUMER_NAME = "tb-agent-bulk-ops";

    @Value("${queue.agent.bulk-ops-poll-interval:1000}")
    private int pollInterval;

    private final TbCoreQueueFactory queueFactory;
    private final AgentBulkOperationService bulkOperationService;

    private QueueConsumerManager<TbProtoQueueMsg<AgentBulkOperationMsg>> consumer;
    private ExecutorService consumerExecutor;

    @PostConstruct
    public void init() {
        consumerExecutor = ThingsBoardExecutors.newWorkStealingPool(1, CONSUMER_NAME);
        consumer = QueueConsumerManager.<TbProtoQueueMsg<AgentBulkOperationMsg>>builder()
                .name(CONSUMER_NAME)
                .msgPackProcessor(this::processMessages)
                .pollInterval(pollInterval)
                .consumerCreator(queueFactory::createAgentBulkOpsMsgConsumer)
                .consumerExecutor(consumerExecutor)
                .threadPrefix(CONSUMER_NAME)
                .build();
        consumer.subscribe();
        consumer.launch();
    }

    private void processMessages(List<TbProtoQueueMsg<AgentBulkOperationMsg>> msgs,
                                 TbQueueConsumer<TbProtoQueueMsg<AgentBulkOperationMsg>> consumer) {
        for (var msg : msgs) {
            try {
                bulkOperationService.processBulkOperation(msg.getValue());
            } catch (Exception e) {
                log.error("Failed to process agent bulk operation message", e);
            }
        }
        consumer.commit();
    }

    @PreDestroy
    public void destroy() {
        if (consumer != null) {
            consumer.stop();
        }
        if (consumerExecutor != null) {
            consumerExecutor.shutdownNow();
        }
    }
}
