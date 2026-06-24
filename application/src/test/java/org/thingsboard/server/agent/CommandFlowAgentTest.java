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
                Map.of("tb-edge", "thingsboard/tb-edge-pe:" + version));
        config.setCompose(org.thingsboard.common.util.JacksonUtil.toJsonNode(composeJson));

        ComposeStep step = createComposeStep();

        return createAgentAppTemplate(AgentApplicationType.EDGE, version,
                config, List.of(step));
    }

    // installEdgeApp is inherited from AbstractAgentTest
}
