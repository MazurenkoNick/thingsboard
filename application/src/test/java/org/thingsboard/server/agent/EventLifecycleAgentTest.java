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
                startSteps, null, null, null, null);
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
                null, null);

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
                null, null, null);

        // V1 template with start steps and nextVersion pointing to V2
        AgentAppTemplate templateV1 = createEdgeTemplateWithSteps("4.3.0EDGE", "4.4.0EDGE",
                chainSteps(createComposeStep()),
                null, null, null, null);

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
        upgradeApp.setConfig(resolveAppConfig(templateV2));

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
        AgentAppTemplate template = createEdgeTemplateWithSteps(EDGE_VERSION,
                chainSteps(createComposeStep()),
                null, null, null,
                chainSteps(createComposeRestartStep()));
        AgentApplication app = installEdgeApp(template);

        // Receive install command but do NOT complete yet
        AppCommand installCmd = waitForCommand();
        AgentAppEventId installEventId = extractEventId(installCmd);

        // Attempt to create RESTART while install is active — should be rejected
        AgentAppEventRequest restartRequest = new AgentAppEventRequest();
        restartRequest.setActionType(AgentAppEventActionType.RESTART);
        doPost("/api/agent/app/" + app.getId().getId().toString() + "/event", restartRequest)
                .andExpect(status().isBadRequest());

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

    @Test
    public void testUpdateEventAppliesNewNameAndReachesFinished() throws Exception {
        AgentAppTemplate template = createSimpleEdgeTemplate(EDGE_VERSION);
        AgentApplication app = installEdgeApp(template);

        // Complete install first
        AppCommand installCmd = waitForCommand();
        completeAllSteps(installCmd);
        awaitEventStatus(extractEventId(installCmd), AgentAppEventStatus.FINISHED);

        // Create UPDATE event with a renamed application + refreshed config (UPDATE reuses start steps)
        AgentApplication updatedApp = new AgentApplication();
        updatedApp.setAgentId(agent.getId());
        updatedApp.setName("Updated Edge App");
        updatedApp.setAppType(AgentApplicationType.EDGE);
        updatedApp.setTemplateId(template.getId());
        updatedApp.setConfig(resolveAppConfig(template));

        AgentAppEventRequest updateRequest = new AgentAppEventRequest();
        updateRequest.setActionType(AgentAppEventActionType.UPDATE);
        updateRequest.setApplication(updatedApp);

        agentImitator.expectMessageAmount(1);
        createAppEvent(app.getId(), updateRequest);

        AppCommand updateCmd = waitForCommand();
        Assert.assertEquals(AppCommandAction.APP_UPDATE, updateCmd.getAction());
        AgentAppEventId updateEventId = extractEventId(updateCmd);

        completeAllSteps(updateCmd);
        awaitEventStatus(updateEventId, AgentAppEventStatus.FINISHED);

        // Verify the new name was applied to the application
        AgentApplication updated = agentApplicationService.findById(tenantId, app.getId());
        Assert.assertNotNull(updated);
        Assert.assertEquals("Updated Edge App", updated.getName());
    }

    private AgentAppTemplate createSimpleEdgeTemplate(String version) {
        return createEdgeTemplateWithSteps(version,
                chainSteps(createComposeStep()),
                null, null, null, null);
    }
}
