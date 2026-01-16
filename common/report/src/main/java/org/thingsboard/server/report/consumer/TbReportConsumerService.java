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
package org.thingsboard.server.report.consumer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos.ToTbReportNotificationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.QueueConsumerManager;
import org.thingsboard.server.queue.provider.TbReportQueueFactory;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbReportComponent;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@TbReportComponent
@Service
@RequiredArgsConstructor
@Slf4j
public class TbReportConsumerService {

    private final TbReportQueueFactory queueFactory;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${queue.report.poll_interval:125}")
    private int pollInterval;

    private final ExecutorService consumersExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName("tb-report-consumer"));
    private QueueConsumerManager<TbProtoQueueMsg<ToTbReportNotificationMsg>> notificationsConsumer;

    @PostConstruct
    private void init() {
        notificationsConsumer = QueueConsumerManager.<TbProtoQueueMsg<ToTbReportNotificationMsg>>builder()
                .name("TB Report notifications")
                .threadPrefix("notifications")
                .msgPackProcessor(this::processNotificationMsgs)
                .pollInterval(pollInterval)
                .consumerCreator(queueFactory::createTbReportNotificationsConsumer)
                .consumerExecutor(consumersExecutor)
                .build();
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void onApplicationEvent() {
        notificationsConsumer.subscribe();
        notificationsConsumer.launch();
    }

    private void processNotificationMsgs(List<TbProtoQueueMsg<ToTbReportNotificationMsg>> msgs, TbQueueConsumer<TbProtoQueueMsg<ToTbReportNotificationMsg>> consumer) {
        for (TbProtoQueueMsg<ToTbReportNotificationMsg> queueMsg : msgs) {
            try {
                ToTbReportNotificationMsg msg = queueMsg.getValue();
                if (msg.hasComponentLifecycleMsg()) {
                    ComponentLifecycleMsg componentLifecycleMsg = ProtoUtils.fromProto(msg.getComponentLifecycleMsg());
                    eventPublisher.publishEvent(componentLifecycleMsg);
                }
            } catch (Throwable e) {
                log.error("Failed to process notification msg: {}", queueMsg, e);
            }
        }
    }

    @PreDestroy
    private void destroy() {
        if (notificationsConsumer != null) {
            notificationsConsumer.stop();
        }
        consumersExecutor.shutdownNow();
    }

}
