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
package org.thingsboard.server.transport.mqtt.util.sparkplug;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.transport.mqtt.TbMqttTransportComponent;

import java.util.HashMap;
import java.util.Map;

import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.STATE;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopic.parseTopic;

@Slf4j
@Service
@TbMqttTransportComponent
public class SparkplugTopicService {

    private static final Map<String, SparkplugTopic> SPLIT_TOPIC_CACHE = new HashMap<>();
    public static final String TOPIC_ROOT_SPB_V_1_0 = "spBv1.0";
    public static final String TOPIC_ROOT_CERT_SP = "$sparkplug/certificates/";
    public static final String TOPIC_SPLIT_REGEXP = "/";
    public static final String TOPIC_STATE_REGEXP = TOPIC_ROOT_SPB_V_1_0 + TOPIC_SPLIT_REGEXP + STATE.name() + TOPIC_SPLIT_REGEXP;

    public static SparkplugTopic getSplitTopic(String topic) throws ThingsboardException {
        SparkplugTopic sparkplugTopic = SPLIT_TOPIC_CACHE.get(topic);
        if (sparkplugTopic == null) {
            // validation topic
            sparkplugTopic = parseTopic(topic);
            SPLIT_TOPIC_CACHE.put(topic, sparkplugTopic);
        }
        return sparkplugTopic;
    }

    /**
     * all ID Element MUST be a UTF-8 string
     * and with the exception of the reserved characters of + (plus), / (forward slash).
     * Publish: $sparkplug/certificates/spBv1.0/G1/NBIRTH/E1
     * Publish: spBv1.0/G1/NBIRTH/E1
     * Publish: $sparkplug/certificates/spBv1.0/G1/DBIRTH/E1/D1
     * Publish: spBv1.0/G1/DBIRTH/E1/D1
     * @param topic
     * @return
     * @throws ThingsboardException
     */
    public static SparkplugTopic parseTopicPublish(String topic) throws ThingsboardException {
        topic = topic.startsWith(TOPIC_ROOT_CERT_SP) ? topic.substring(TOPIC_ROOT_CERT_SP.length()) : topic;
        topic = topic.indexOf("+") > 0 ? topic.substring(0, topic.indexOf("+")): topic;
        return getSplitTopic(topic);
    }
}

