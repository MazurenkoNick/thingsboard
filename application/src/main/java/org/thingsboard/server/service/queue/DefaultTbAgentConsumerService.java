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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.gen.transport.TransportProtos.ToAgentNotificationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.AgentContextComponent;
import org.thingsboard.server.service.queue.processing.AbstractConsumerService;

import java.util.UUID;

@Service
@TbCoreComponent
public class DefaultTbAgentConsumerService extends AbstractConsumerService<ToAgentNotificationMsg> {

    private static final String CONSUMER_NAME = "tb-agent";

    @Value("${queue.agent.poll-interval:25}")
    private int pollInterval;
    @Value("${queue.agent.pack-processing-timeout:10000}")
    private int packProcessingTimeout;

    private final TbCoreQueueFactory queueFactory;
    private final AgentContextComponent ctx;

    public DefaultTbAgentConsumerService(TbCoreQueueFactory queueFactory,
                                         ActorSystemContext actorContext,
                                         AgentContextComponent ctx) {
        super(actorContext, null, null, null, null, null, null, null, null, null);
        this.queueFactory = queueFactory;
        this.ctx = ctx;
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
        return Math.max(Runtime.getRuntime().availableProcessors(), 4);
    }

    @Override
    protected TbQueueConsumer<TbProtoQueueMsg<ToAgentNotificationMsg>> createNotificationsConsumer() {
        return queueFactory.createToAgentNotificationsMsgConsumer();
    }

    @Override
    protected void handleNotification(UUID id, TbProtoQueueMsg<ToAgentNotificationMsg> msg, TbCallback callback) {
        try {
            ToAgentNotificationMsg notification = msg.getValue();
            if (notification.hasAgentAppEventNotification()) {
                ctx.getAgentEventProcessor().onEventNotification(notification.getAgentAppEventNotification());
            }
            callback.onSuccess();
        } catch (Exception e) {
            log.warn("Failed to process agent notification message", e);
            callback.onFailure(e);
        }
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {}
}
