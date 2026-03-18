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

import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.common.data.agent.step.ComposeStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.agent.v1.AckStatus;
import org.thingsboard.server.gen.agent.v1.AppCommand;
import org.thingsboard.server.gen.agent.v1.AppCommandAction;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class EventLifecycleAgentTest extends AbstractAgentTest {

    private static final String EDGE_VERSION = "4.3.0EDGE";

    @Test
    public void testInstallEventReachesFinishedStatus() throws Exception {
        AgentAppTemplate template = createSimpleEdgeTemplate(EDGE_VERSION);
        AgentApplication app = installEdgeApp(template);

        AppCommand command = waitForCommand();
        Assert.assertEquals(AppCommandAction.APP_INSTALL, command.getAction());

        AgentAppEventId eventId = extractEventId(command);

        agentImitator.sendCommandAck(command.getCommandId(), AckStatus.ACCEPTED);
        agentImitator.sendCommandResult(command.getCommandId(), command.getStepId(), true);

        awaitEventStatus(eventId, AgentAppEventStatus.FINISHED);

        // App should still exist after install
        AgentApplication found = agentApplicationService.findById(tenantId, app.getId());
        Assert.assertNotNull("Application should still exist after install", found);
    }

    @Test
    public void testInstallMultiStepFullFlow() throws Exception {
        ComposeStep composeStep = createComposeStep();
        ComposeStartStep startStep = createComposeStartStep();
        List<AgentAppStep> startSteps = chainSteps(composeStep, startStep);

        AgentAppTemplate template = createEdgeTemplateWithSteps(EDGE_VERSION,
                startSteps, null, null, null);
        AgentApplication app = installEdgeApp(template);

        AppCommand firstCommand = waitForCommand();
        Assert.assertEquals(AppCommandAction.APP_INSTALL, firstCommand.getAction());
        Assert.assertEquals(2, firstCommand.getTotalSteps());

        AgentAppEventId eventId = extractEventId(firstCommand);

        // Complete step 1
        agentImitator.sendCommandAck(firstCommand.getCommandId(), AckStatus.ACCEPTED);
        agentImitator.sendCommandResult(firstCommand.getCommandId(), firstCommand.getStepId(), true);

        // Wait for step 2
        AppCommand secondCommand = waitForCommand();
        Assert.assertEquals(2, secondCommand.getTotalSteps());
        Assert.assertNotEquals("Step IDs should differ",
                firstCommand.getStepId(), secondCommand.getStepId());

        // Complete step 2
        agentImitator.sendCommandAck(secondCommand.getCommandId(), AckStatus.ACCEPTED);
        agentImitator.sendCommandResult(secondCommand.getCommandId(), secondCommand.getStepId(), true);

        awaitEventStatus(eventId, AgentAppEventStatus.FINISHED);
    }

    @Test
    public void testDeleteEventDeletesApplication() throws Exception {
        AgentAppTemplate template = createEdgeTemplateWithSteps(EDGE_VERSION,
                chainSteps(createComposeStep()),
                null,
                chainSteps(createComposeDownStep()),
                null);

        AgentApplication app = installEdgeApp(template);

        // Complete install
        AppCommand installCmd = waitForCommand();
        completeAllSteps(installCmd);
        awaitEventStatus(extractEventId(installCmd), AgentAppEventStatus.FINISHED);

        // Create DELETE event
        AgentAppEventRequest deleteRequest = new AgentAppEventRequest();
        deleteRequest.setActionType(AgentAppEventActionType.DELETE);
        agentImitator.expectMessageAmount(1);
        createAppEvent(app.getId(), deleteRequest);

        // Complete delete
        AppCommand deleteCmd = waitForCommand();
        Assert.assertEquals(AppCommandAction.APP_DELETE, deleteCmd.getAction());
        agentImitator.sendCommandAck(deleteCmd.getCommandId(), AckStatus.ACCEPTED);
        agentImitator.sendCommandResult(deleteCmd.getCommandId(), deleteCmd.getStepId(), true);

        // CommandFeedbackHandler short-circuits DELETE: deletes app directly
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> agentApplicationService.findById(tenantId, app.getId()) == null);
    }

    @Test
    public void testUpgradeEventPromotesTemplateId() throws Exception {
        // V2 template with upgrade steps (create first so V1 can reference its version)
        ComposeStep upgradeComposeStep = createComposeStep();
        AgentAppTemplate templateV2 = createEdgeTemplateWithSteps("4.4.0EDGE",
                chainSteps(createComposeStep()),
                chainSteps(upgradeComposeStep),
                null, null);

        // V1 template with start steps and nextVersion pointing to V2
        AgentAppTemplate templateV1 = createEdgeTemplateWithSteps("4.3.0EDGE", "4.4.0EDGE",
                chainSteps(createComposeStep()),
                null, null, null);

        // Install with V1
        AgentApplication app = installEdgeApp(templateV1);
        AppCommand installCmd = waitForCommand();
        completeAllSteps(installCmd);
        awaitEventStatus(extractEventId(installCmd), AgentAppEventStatus.FINISHED);

        // Create UPGRADE event toward V2
        AgentApplication upgradeApp = new AgentApplication();
        upgradeApp.setAgentId(agent.getId());
        upgradeApp.setName("Test Edge App");
        upgradeApp.setAppType(AgentApplicationType.EDGE);
        upgradeApp.setTemplateId(templateV2.getId());
        upgradeApp.setConfig(templateV2.getConfig() != null ? templateV2.getConfig().copy() : null);

        AgentAppEventRequest upgradeRequest = new AgentAppEventRequest();
        upgradeRequest.setActionType(AgentAppEventActionType.UPGRADE);
        upgradeRequest.setApplication(upgradeApp);

        agentImitator.expectMessageAmount(1);
        createAppEvent(app.getId(), upgradeRequest);

        // Complete upgrade
        AppCommand upgradeCmd = waitForCommand();
        Assert.assertEquals(AppCommandAction.APP_UPGRADE, upgradeCmd.getAction());
        AgentAppEventId upgradeEventId = extractEventId(upgradeCmd);

        completeAllSteps(upgradeCmd);
        awaitEventStatus(upgradeEventId, AgentAppEventStatus.FINISHED);

        // Verify: templateId promoted to V2, desiredTemplateId cleared
        AgentApplication updated = agentApplicationService.findById(tenantId, app.getId());
        Assert.assertNotNull(updated);
        Assert.assertEquals(templateV2.getId(), updated.getTemplateId());
        Assert.assertNull("desiredTemplateId should be null after upgrade", updated.getDesiredTemplateId());
    }

    @Test
    public void testSequentialEventsDispatchAfterCompletion() throws Exception {
        AgentAppTemplate template = createSimpleEdgeTemplate(EDGE_VERSION);
        AgentApplication app = installEdgeApp(template);

        // Receive install command but do NOT complete yet
        AppCommand installCmd = waitForCommand();
        AgentAppEventId installEventId = extractEventId(installCmd);

        // Attempt to create RESTART while install is active — should be rejected with 429
        AgentAppEventRequest restartRequest = new AgentAppEventRequest();
        restartRequest.setActionType(AgentAppEventActionType.RESTART);
        doPost("/api/agent/app/" + app.getId().getId().toString() + "/event", restartRequest)
                .andExpect(status().is(429));

        // Now complete install
        completeAllSteps(installCmd);
        awaitEventStatus(installEventId, AgentAppEventStatus.FINISHED);

        // Create RESTART again — should be accepted now that install is finished
        agentImitator.expectMessageAmount(1);
        createAppEvent(app.getId(), restartRequest);

        // RESTART should be dispatched
        AppCommand restartCmd = waitForCommand();
        Assert.assertEquals(AppCommandAction.APP_RESTART, restartCmd.getAction());
        AgentAppEventId restartEventId = extractEventId(restartCmd);

        completeAllSteps(restartCmd);
        awaitEventStatus(restartEventId, AgentAppEventStatus.FINISHED);
    }

    @Test
    public void testCancelEventMarksError() throws Exception {
        AgentAppTemplate template = createSimpleEdgeTemplate(EDGE_VERSION);
        AgentApplication app = installEdgeApp(template);

        AppCommand command = waitForCommand();
        AgentAppEventId eventId = extractEventId(command);

        // Cancel the in-flight event
        doPost("/api/agent/app/" + app.getId().getId().toString()
                        + "/event/" + eventId.getId().toString() + "/cancel")
                .andExpect(status().isOk());

        awaitEventStatus(eventId, AgentAppEventStatus.ERROR);
    }

    // --- Private helpers ---

    private AgentAppTemplate createSimpleEdgeTemplate(String version) {
        return createEdgeTemplateWithSteps(version,
                chainSteps(createComposeStep()),
                null, null, null);
    }
}
