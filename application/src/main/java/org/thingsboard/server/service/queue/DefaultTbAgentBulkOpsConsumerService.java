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
import org.thingsboard.server.service.agent.bulk.AgentBulkActionProcessingService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultTbAgentBulkOpsConsumerService {

    private static final String CONSUMER_NAME = "tb-agent-bulk-ops";

    @Value("${queue.agent.bulk-ops-poll-interval:1000}")
    private int pollInterval;
    @Value("${queue.agent.bulk-ops-processing-threads:2}")
    private int processingThreads;
    @Value("${queue.agent.bulk-ops-processing-queue-size:20}")
    private int processingQueueSize;

    private final TbCoreQueueFactory queueFactory;
    private final AgentBulkActionProcessingService bulkOperationService;

    private QueueConsumerManager<TbProtoQueueMsg<AgentBulkOperationMsg>> consumer;
    private ExecutorService consumerExecutor;
    private ExecutorService processingExecutor;

    @PostConstruct
    public void init() {
        consumerExecutor = ThingsBoardExecutors.newWorkStealingPool(1, CONSUMER_NAME);
        processingExecutor = ThingsBoardExecutors.newLimitedTasksExecutor(
                processingThreads, processingQueueSize, CONSUMER_NAME + "-processing");
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
            processingExecutor.submit(() -> {
                try {
                    bulkOperationService.processBulkOperation(msg.getValue());
                } catch (Exception e) {
                    log.error("Failed to process agent bulk operation message", e);
                }
            });
        }
        consumer.commit();
    }

    @PreDestroy
    public void destroy() {
        if (consumer != null) {
            consumer.stop();
        }
        if (processingExecutor != null) {
            processingExecutor.shutdown();
            try {
                if (!processingExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.warn("Processing executor did not terminate in time, forcing shutdown");
                    processingExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                processingExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (consumerExecutor != null) {
            consumerExecutor.shutdownNow();
        }
    }
}
