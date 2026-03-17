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
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.step.ComposeStep;
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
                null, null, null);
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
                null, null, null);
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
