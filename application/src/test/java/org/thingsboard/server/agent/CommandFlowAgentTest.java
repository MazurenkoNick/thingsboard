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
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.ComposeStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.agent.v1.AckStatus;
import org.thingsboard.server.gen.agent.v1.AppCommand;
import org.thingsboard.server.gen.agent.v1.AppCommandAction;

import java.util.List;
import java.util.Map;

@DaoSqlTest
public class CommandFlowAgentTest extends AbstractAgentTest {

    @Test
    public void testAgentReceivesAppCommand() throws Exception {
        AgentAppTemplate template = createEdgeTemplate("4.3.0EDGE");

        AgentApplication app = installEdgeApp(template);
        Assert.assertNotNull(app.getId());

        AppCommand command = waitForCommand();
        Assert.assertEquals(AppCommandAction.APP_INSTALL, command.getAction());
        Assert.assertFalse(command.getAppName().isEmpty());
    }

    @Test
    public void testCommandAckAndResultFlow() throws Exception {
        AgentAppTemplate template = createEdgeTemplate("4.3.0EDGE");
        AgentApplication app = installEdgeApp(template);

        AppCommand command = waitForCommand();

        // Send ack
        agentImitator.sendCommandAck(command.getCommandId(), AckStatus.ACCEPTED);

        // Send result for the step
        agentImitator.sendCommandResult(command.getCommandId(), command.getStepId(), true);

        // If there are more steps, handle them
        if (command.getTotalSteps() > 1) {
            for (int i = 1; i < command.getTotalSteps(); i++) {
                AppCommand nextCommand = waitForCommand();
                agentImitator.sendCommandAck(nextCommand.getCommandId(), AckStatus.ACCEPTED);
                agentImitator.sendCommandResult(nextCommand.getCommandId(), nextCommand.getStepId(), true);
            }
        }
    }

    @Test
    public void testCommandResultFailure() throws Exception {
        AgentAppTemplate template = createEdgeTemplate("4.3.0EDGE");
        AgentApplication app = installEdgeApp(template);

        AppCommand command = waitForCommand();

        agentImitator.sendCommandAck(command.getCommandId(), AckStatus.ACCEPTED);
        agentImitator.sendCommandResult(command.getCommandId(), command.getStepId(), false);
    }

    // --- Private helpers ---

    private AgentAppTemplate createEdgeTemplate(String version) {
        DockerComposeConfig config = new DockerComposeConfig();
        String composeJson = constructComposeJson(
                Map.of("tb-edge", "thingsboard/tb-edge:" + version));
        config.setCompose(org.thingsboard.common.util.JacksonUtil.toJsonNode(composeJson));

        ComposeStep step = createComposeStep();

        return createAgentAppTemplate(AgentApplicationType.EDGE, version,
                config, List.of(step));
    }

    // installEdgeApp is inherited from AbstractAgentTest
}
