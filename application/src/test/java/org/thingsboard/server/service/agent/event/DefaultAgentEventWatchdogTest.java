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
package org.thingsboard.server.service.agent.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppEventStepsResolver;
import org.thingsboard.server.service.agent.session.AgentSession;
import org.thingsboard.server.service.agent.session.AgentSessionRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultAgentEventWatchdogTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentId AGENT_ID = new AgentId(UUID.randomUUID());
    private static final AgentAppEventId EVENT_ID = new AgentAppEventId(UUID.randomUUID());

    @Mock
    private AgentAppEventService agentAppEventService;
    @Mock
    private AgentSessionRegistry agentSessionRegistry;
    @Mock
    private AgentAppEventStepsResolver eventStepsResolver;
    @Mock
    private AgentSession session;
    @Mock
    private AgentEventResender resender;

    private DefaultAgentEventWatchdog watchdog;

    @BeforeEach
    void setUp() {
        watchdog = new DefaultAgentEventWatchdog(agentAppEventService, agentSessionRegistry, eventStepsResolver);
        ReflectionTestUtils.setField(watchdog, "watchdogInitialDelayMs", 1000L);
    }

    @Test
    void schedule_noActiveSession_doesNotScheduleWatchdog() {
        when(agentSessionRegistry.getByAgentId(AGENT_ID)).thenReturn(null);

        watchdog.schedule(application(), event(AgentAppEventStatus.QUEUED, 0L), resender);

        verifyNoInteractions(session);
    }

    @Test
    void staleEvent_nullCurrentStep_doesNotResend() {
        AgentApplication application = application();
        AgentAppEvent event = event(AgentAppEventStatus.QUEUED, 0L); // updatedTime 0 -> stale vs now
        when(agentSessionRegistry.getByAgentId(AGENT_ID)).thenReturn(session);
        List<Runnable> tasks = captureScheduledTasks();
        // empty step list -> findByStepId returns null for any currentStepId
        when(eventStepsResolver.resolveSteps(eq(application), any())).thenReturn(Collections.<AgentAppStep>emptyList());
        when(agentAppEventService.findById(eq(TENANT_ID), eq(EVENT_ID))).thenReturn(event);

        watchdog.schedule(application, event, resender);
        tasks.get(0).run();

        verify(resender, never()).resendCurrentStep(any(), any(), any());
        verify(resender, never()).onError(any(), any());
        // checkStaleness completed normally -> only the initial scheduling, no reschedule
        verify(session, times(1)).scheduleEventWatchdog(any(), any(), any(), anyLong(), any());
    }

    @Test
    void watchdogCheckKeepsFailing_marksErrorAfterMaxRetries() {
        AgentApplication application = application();
        AgentAppEvent event = event(AgentAppEventStatus.QUEUED, 0L);
        when(agentSessionRegistry.getByAgentId(AGENT_ID)).thenReturn(session);
        List<Runnable> tasks = captureScheduledTasks();
        when(agentAppEventService.findById(any(), any())).thenThrow(new RuntimeException("db down"));

        watchdog.schedule(application, event, resender);
        int i = 0;
        while (i < tasks.size() && i < 10) {
            tasks.get(i).run();
            i++;
        }

        // initial attempt (count 0) + 3 reschedules (counts 1,2,3) = 4 schedule calls
        verify(session, times(4)).scheduleEventWatchdog(any(), any(), any(), anyLong(), any());
        verify(resender, times(1)).onError(EVENT_ID, application);
    }

    private List<Runnable> captureScheduledTasks() {
        List<Runnable> tasks = new ArrayList<>();
        doAnswer(inv -> {
            tasks.add(inv.getArgument(2));
            return null;
        }).when(session).scheduleEventWatchdog(any(), any(), any(), anyLong(), any());
        return tasks;
    }

    private AgentApplication application() {
        AgentApplication application = new AgentApplication();
        application.setTenantId(TENANT_ID);
        application.setAgentId(AGENT_ID);
        return application;
    }

    private AgentAppEvent event(AgentAppEventStatus status, long updatedTime) {
        AgentAppEvent event = new AgentAppEvent();
        event.setId(EVENT_ID);
        event.setTenantId(TENANT_ID);
        event.setActionType(AgentAppEventActionType.INSTALL);
        event.setStatus(status);
        event.setUpdatedTime(updatedTime);
        event.setCurrentStepId(UUID.randomUUID());
        return event;
    }
}
