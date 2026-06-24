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
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.agent.v1.AckStatus;
import org.thingsboard.server.gen.agent.v1.AppCommand;

import java.util.List;

@DaoSqlTest
@TestPropertySource(properties = {
        "agents.event.watchdog_initial_delay_ms=500"
})
public class EventWatchdogAgentTest extends AbstractAgentTest {

    private static final String EDGE_VERSION = "4.3.0EDGE";

    @Test
    public void testWatchdogResendsStaleEvent() throws Exception {
        AgentAppTemplate template = createEdgeTemplateWithSteps(EDGE_VERSION,
                chainSteps(createComposeStep()),
                null, null, null, null);
        installEdgeApp(template);

        // Receive first command
        AppCommand firstCommand = waitForCommand();
        AgentAppEventId eventId = extractEventId(firstCommand);

        // Do NOT respond — let the watchdog detect staleness and resend
        agentImitator.expectMessageAmount(1);
        agentImitator.waitForMessages();

        // Verify the resent command has the same commandId and stepId
        AppCommand resent = agentImitator.getLatestCommand();
        Assert.assertEquals("Resent command should have same commandId",
                firstCommand.getCommandId(), resent.getCommandId());
        Assert.assertEquals("Resent command should have same stepId",
                firstCommand.getStepId(), resent.getStepId());

        // Now complete the event
        completeAllSteps(resent);
        awaitEventStatus(eventId, AgentAppEventStatus.FINISHED);
    }

    @Test
    public void testWatchdogDoesNotResendOnProgress() throws Exception {
        AgentAppTemplate template = createEdgeTemplateWithSteps(EDGE_VERSION,
                chainSteps(createComposeStep()),
                null, null, null, null);
        installEdgeApp(template);

        AppCommand command = waitForCommand();
        AgentAppEventId eventId = extractEventId(command);

        // Ack and complete immediately — the ack updates event's updatedTime,
        // so watchdog should see progress and not resend
        agentImitator.sendCommandAck(command.getCommandId(), AckStatus.ACCEPTED);
        agentImitator.sendCommandResult(command.getCommandId(), command.getStepId(), true);

        awaitEventStatus(eventId, AgentAppEventStatus.FINISHED);

        // Wait a bit longer than the watchdog delay to ensure no extra commands arrive
        Thread.sleep(700);

        // The only messages received should be the initial command (no resends)
        List<AppCommand> allCommands = agentImitator.getReceivedCommands();
        // There should be no additional commands beyond what we already consumed
        Assert.assertTrue("No extra commands should have been received after completion",
                allCommands.isEmpty() || allCommands.size() <= 1);
    }
}
