/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.queue.kafka;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.queue.TbEdgeQueueAdmin;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.util.PropertyUtils;

import java.util.Map;

/**
 * Created by ashvayka on 24.09.18.
 */
@Slf4j
public class TbKafkaAdmin implements TbQueueAdmin, TbEdgeQueueAdmin {

    private final TbKafkaSettings settings;
    private final Map<String, String> topicConfigs;
    @Getter
    private final int numPartitions;

    public TbKafkaAdmin(TbKafkaSettings settings, Map<String, String> topicConfigs) {
        this.settings = settings;
        this.topicConfigs = topicConfigs;
        String numPartitionsStr = topicConfigs.get(TbKafkaTopicConfigs.NUM_PARTITIONS_SETTING);
        if (numPartitionsStr != null) {
            numPartitions = Integer.parseInt(numPartitionsStr);
        } else {
            numPartitions = 1;
        }
    }

    @Override
    public void createTopicIfNotExists(String topic, String properties, boolean force) {
        settings.getAdmin().createTopicIfNotExists(topic, PropertyUtils.getProps(topicConfigs, properties), force);
    }

    @Override
    public void deleteTopic(String topic) {
        settings.getAdmin().deleteTopic(topic);
    }

    @Override
    public void destroy() {
    }

    /**
     * Sync edge notifications offsets from a fat group to a single group per edge
     * */
    public void syncEdgeNotificationsOffsets(String fatGroupId, String newGroupId) {
        try {
            log.info("syncEdgeNotificationsOffsets [{}][{}]", fatGroupId, newGroupId);
            settings.getAdmin().syncOffsetsUnsafe(fatGroupId, newGroupId, newGroupId);
        } catch (Exception e) {
            log.warn("Failed to syncEdgeNotificationsOffsets from {} to {}", fatGroupId, newGroupId, e);
        }
    }

}
