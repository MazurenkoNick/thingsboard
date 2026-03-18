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
package org.thingsboard.server.agent;

import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.common.data.agent.step.ComposeStep;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.agent.v1.AckStatus;
import org.thingsboard.server.gen.agent.v1.AppCommand;
import org.thingsboard.server.gen.agent.v1.AppCommandAction;

@DaoSqlTest
public class ReconnectAgentTest extends AbstractAgentTest {

    private static final String EDGE_VERSION = "4.3.0EDGE";

    @Test
    public void testReconnectResumesInFlightEvent() throws Exception {
        // Create template with 2 start steps
        ComposeStep composeStep = createComposeStep();
        ComposeStartStep startStep = createComposeStartStep();
        AgentAppTemplate template = createEdgeTemplateWithSteps(EDGE_VERSION,
                chainSteps(composeStep, startStep),
                null, null, null);

        installEdgeApp(template);

        // Receive first step command
        AppCommand firstCmd = waitForCommand();
        AgentAppEventId eventId = extractEventId(firstCmd);
        Assert.assertEquals(AppCommandAction.APP_INSTALL, firstCmd.getAction());
        Assert.assertEquals(2, firstCmd.getTotalSteps());

        // Ack to make event in-flight (delivered state)
        agentImitator.sendCommandAck(firstCmd.getCommandId(), AckStatus.ACCEPTED);

        // Disconnect and reconnect
        reconnectAgent();

        // Server should resume the in-flight event by resending the current step.
        // Use awaitCommand (poll-based) since the resume message may arrive
        // before a latch-based waitForCommand can be set up.
        AppCommand resumedCmd = awaitCommand(AppCommandAction.APP_INSTALL);

        Assert.assertEquals("Resumed command should have same event id",
                firstCmd.getCommandId(), resumedCmd.getCommandId());

        // Complete step 1
        agentImitator.sendCommandAck(resumedCmd.getCommandId(), AckStatus.ACCEPTED);
        agentImitator.sendCommandResult(resumedCmd.getCommandId(), resumedCmd.getStepId(), true);

        // Wait for and complete step 2
        AppCommand step2Cmd = waitForCommand();
        agentImitator.sendCommandAck(step2Cmd.getCommandId(), AckStatus.ACCEPTED);
        agentImitator.sendCommandResult(step2Cmd.getCommandId(), step2Cmd.getStepId(), true);

        awaitEventStatus(eventId, AgentAppEventStatus.FINISHED);
    }

    @Test
    public void testReconnectDispatchesPendingWhenNoInFlight() throws Exception {
        AgentAppTemplate template = createEdgeTemplateWithSteps(EDGE_VERSION,
                chainSteps(createComposeStep()),
                null, null, null);

        AgentApplication app = installEdgeApp(template);

        // Complete install
        AppCommand installCmd = waitForCommand();
        completeAllSteps(installCmd);
        awaitEventStatus(extractEventId(installCmd), AgentAppEventStatus.FINISHED);

        // Create a RESTART event
        AgentAppEventRequest restartRequest = new AgentAppEventRequest();
        restartRequest.setActionType(AgentAppEventActionType.RESTART);
        createAppEvent(app.getId(), restartRequest);

        // Disconnect and reconnect
        reconnectAgent();

        // On reconnect, resumeEventsOnReconnect finds no in-flight event,
        // dispatches pending RESTART. Use poll-based wait.
        AppCommand restartCmd = awaitCommand(AppCommandAction.APP_RESTART);

        AgentAppEventId restartEventId = extractEventId(restartCmd);
        completeAllSteps(restartCmd);
        awaitEventStatus(restartEventId, AgentAppEventStatus.FINISHED);
    }
}
