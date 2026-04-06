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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.gen.transport.TransportProtos.AgentBulkOperationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.agent.AgentBulkOperationService;
import org.thingsboard.server.service.queue.processing.AbstractConsumerService;

import java.util.UUID;

@Service
@TbCoreComponent
@Slf4j
public class DefaultTbAgentBulkOpsConsumerService extends AbstractConsumerService<AgentBulkOperationMsg> {

    private static final String CONSUMER_NAME = "tb-agent-bulk-ops";

    @Value("${queue.agent.bulk-ops-poll-interval:1000}")
    private int pollInterval;
    @Value("${queue.agent.bulk-ops-pack-processing-timeout:300000}")
    private int packProcessingTimeout;

    private final TbCoreQueueFactory queueFactory;
    private final AgentBulkOperationService bulkOperationService;

    public DefaultTbAgentBulkOpsConsumerService(TbCoreQueueFactory queueFactory,
                                                ActorSystemContext actorContext,
                                                AgentBulkOperationService bulkOperationService) {
        super(actorContext, null, null, null, null, null, null, null, null, null);
        this.queueFactory = queueFactory;
        this.bulkOperationService = bulkOperationService;
    }

    @PostConstruct
    public void init() {
        super.init(CONSUMER_NAME);
    }

    @Override
    protected ServiceType getServiceType() {
        return ServiceType.TB_CORE;
    }

    @Override
    protected long getNotificationPollDuration() {
        return pollInterval;
    }

    @Override
    protected long getNotificationPackProcessingTimeout() {
        return packProcessingTimeout;
    }

    @Override
    protected int getMgmtThreadPoolSize() {
        return 4;
    }

    @Override
    protected TbQueueConsumer<TbProtoQueueMsg<AgentBulkOperationMsg>> createNotificationsConsumer() {
        return queueFactory.createAgentBulkOpsMsgConsumer();
    }

    @Override
    protected void handleNotification(UUID id, TbProtoQueueMsg<AgentBulkOperationMsg> msg, TbCallback callback) {
        try {
            bulkOperationService.processBulkOperation(msg.getValue());
            callback.onSuccess();
        } catch (Exception e) {
            log.error("Failed to process agent bulk operation message", e);
            callback.onFailure(e);
        }
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {}
}
