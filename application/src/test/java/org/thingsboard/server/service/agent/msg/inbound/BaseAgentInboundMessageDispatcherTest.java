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
package org.thingsboard.server.service.agent.msg.inbound;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.service.agent.AgentInboundMsgCtx;
import org.thingsboard.server.service.agent.session.AgentSession;
import org.thingsboard.server.service.agent.session.AgentSessionState;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaseAgentInboundMessageDispatcherTest {

    @Mock
    private AgentInboundMessageHandler throwingHandler;
    @Mock
    private AgentInboundMessageHandler otherHandler;
    @Mock
    private AgentSession session;

    private AgentInboundMsgCtx ctx() {
        lenient().when(session.getState()).thenReturn(new AgentSessionState());
        return AgentInboundMsgCtx.builder().session(session).build();
    }

    @Test
    void process_handlerThrows_isSwallowedToKeepStreamAlive() {
        when(throwingHandler.canHandle(any())).thenReturn(true);
        doThrow(new RuntimeException("boom")).when(throwingHandler).handle(any());
        AgentInboundMsgCtx ctx = ctx();

        BaseAgentInboundMessageDispatcher dispatcher = new BaseAgentInboundMessageDispatcher(List.of(throwingHandler));

        assertThatCode(() -> dispatcher.process(ctx)).doesNotThrowAnyException();
        verify(throwingHandler).handle(ctx);
    }

    @Test
    void process_oneHandlerThrows_othersStillRun() {
        when(throwingHandler.canHandle(any())).thenReturn(true);
        doThrow(new RuntimeException("boom")).when(throwingHandler).handle(any());
        when(otherHandler.canHandle(any())).thenReturn(true);
        AgentInboundMsgCtx ctx = ctx();

        BaseAgentInboundMessageDispatcher dispatcher =
                new BaseAgentInboundMessageDispatcher(List.of(throwingHandler, otherHandler));

        assertThatCode(() -> dispatcher.process(ctx)).doesNotThrowAnyException();
        verify(otherHandler).handle(ctx);
    }

    @Test
    void process_nonMatchingHandler_notInvoked() {
        when(otherHandler.canHandle(any())).thenReturn(false);
        AgentInboundMsgCtx ctx = ctx();

        BaseAgentInboundMessageDispatcher dispatcher = new BaseAgentInboundMessageDispatcher(List.of(otherHandler));

        dispatcher.process(ctx);

        verify(otherHandler, never()).handle(any());
    }
}
