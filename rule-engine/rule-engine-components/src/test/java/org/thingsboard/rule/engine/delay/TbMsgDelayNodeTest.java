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
package org.thingsboard.rule.engine.delay;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.TbMsgProcessingCtx;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TbMsgDelayNodeTest {

    final DeviceId deviceId = new DeviceId(UUID.fromString("5770153d-6ca2-4447-8a54-5d8a4538e052"));
    final RuleNodeId ruleNodeId = new RuleNodeId(UUID.fromString("ee682a85-7f5a-4182-91bc-46e555138fe2"));

    TbMsgDelayNode node;

    @Mock
    TbContext ctxMock;

    @BeforeEach
    void setUp() throws TbNodeException {
        node = new TbMsgDelayNode();
        var config = new TbMsgDelayNodeConfiguration().defaultConfiguration();
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        lenient().when(ctxMock.getSelfId()).thenReturn(ruleNodeId);
    }

    @Test
    void shouldPreserveRuleNodeCounterAndResetCallbackWhenEnqueuingDelayedMsg() {
        // GIVEN
        int ruleNodeExecCounter = 5;
        var originalMsg = TbMsg.newMsg()
                .id(UUID.randomUUID())
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .metaData(TbMsgMetaData.EMPTY)
                .data("{\"temperature\":42}")
                .ctx(new TbMsgProcessingCtx(ruleNodeExecCounter))
                .build();

        String originalMsgId = originalMsg.getId().toString();
        var tickMsg = TbMsg.newMsg()
                .type(TbMsgType.DELAY_TIMEOUT_SELF_MSG)
                .originator(ruleNodeId)
                .metaData(TbMsgMetaData.EMPTY)
                .data(originalMsgId)
                .build();
        given(ctxMock.newMsg(null, TbMsgType.DELAY_TIMEOUT_SELF_MSG, ruleNodeId, null, TbMsgMetaData.EMPTY, originalMsgId)).willReturn(tickMsg);

        node.onMsg(ctxMock, originalMsg);

        // WHEN
        node.onMsg(ctxMock, tickMsg);

        // THEN
        var msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().enqueueForTellNext(msgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS));

        var enqueuedMsg = msgCaptor.getValue();
        assertThat(enqueuedMsg).usingRecursiveComparison()
                .ignoringFields("id", "ts", "callback")
                .isEqualTo(originalMsg);

        assertThat(enqueuedMsg.getId()).isNotNull().isNotEqualTo(originalMsg.getId());
        assertThat(enqueuedMsg.getAndIncrementRuleNodeCounter()).isEqualTo(ruleNodeExecCounter);
        assertThat(enqueuedMsg.getCallback()).isSameAs(TbMsgCallback.EMPTY);
    }

}
