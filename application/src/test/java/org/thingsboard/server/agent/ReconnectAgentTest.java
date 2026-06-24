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
package org.thingsboard.server.agent;

import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.step.ComposeRestartStep;
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
        ComposeRestartStep restartStep = createComposeRestartStep();
        AgentAppTemplate template = createEdgeTemplateWithSteps(EDGE_VERSION,
                chainSteps(composeStep, startStep), null, null, null,
                chainSteps(restartStep));

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
        ComposeRestartStep restartStep = createComposeRestartStep();
        AgentAppTemplate template = createEdgeTemplateWithSteps(EDGE_VERSION,
                chainSteps(createComposeStep()), null, null, null, chainSteps(restartStep));

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
