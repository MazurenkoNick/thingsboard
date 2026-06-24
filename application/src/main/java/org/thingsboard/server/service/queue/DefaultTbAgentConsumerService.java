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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.resource.TbResourceDataCache;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.gen.transport.TransportProtos.AgentAppEventNotificationProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToAgentNotificationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.AgentContextComponent;
import org.thingsboard.server.service.agent.AgentRpcService;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.cf.CalculatedFieldCache;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.queue.processing.AbstractConsumerService;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;

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
    private final AgentRpcService agentRpcService;

    public DefaultTbAgentConsumerService(TbCoreQueueFactory queueFactory,
                                         ActorSystemContext actorContext,
                                         AgentContextComponent ctx,
                                         AgentRpcService agentRpcService,
                                         TbTenantProfileCache tenantProfileCache,
                                         TbDeviceProfileCache deviceProfileCache,
                                         TbAssetProfileCache assetProfileCache,
                                         TbResourceDataCache tbResourceDataCache,
                                         CalculatedFieldCache calculatedFieldCache,
                                         TbApiUsageStateService apiUsageStateService,
                                         PartitionService partitionService,
                                         ApplicationEventPublisher eventPublisher,
                                         JwtSettingsService jwtSettingsService
                                         ) {
        super(actorContext, tenantProfileCache, deviceProfileCache, assetProfileCache, tbResourceDataCache, calculatedFieldCache, apiUsageStateService, partitionService,
                eventPublisher, jwtSettingsService);
        this.queueFactory = queueFactory;
        this.ctx = ctx;
        this.agentRpcService = agentRpcService;
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
                processAgentEventApp(notification);
            } else if (notification.hasLogStreamRequest()) {
                agentRpcService.processLogStreamRequest(notification.getLogStreamRequest());
            }
            callback.onSuccess();
        } catch (Exception e) {
            log.warn("Failed to process agent notification message", e);
            callback.onFailure(e);
        }
    }

    private void processAgentEventApp(ToAgentNotificationMsg notification) {
        AgentAppEventNotificationProto agentAppEventNotification = notification.getAgentAppEventNotification();
        ctx.getAgentEventProcessor().onEventNotification(agentAppEventNotification);
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {}
}
