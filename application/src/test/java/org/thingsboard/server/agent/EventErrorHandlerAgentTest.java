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
import org.thingsboard.server.common.data.agent.step.ComposeStep;
import org.thingsboard.server.common.data.agent.step.RollBackStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.agent.v1.AckStatus;
import org.thingsboard.server.gen.agent.v1.AppCommand;
import org.thingsboard.server.gen.agent.v1.AppCommandAction;

import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class EventErrorHandlerAgentTest extends AbstractAgentTest {

    private static final String EDGE_VERSION = "4.3.0EDGE";

    @Test
    public void testRejectedAckMarksError() throws Exception {
        AgentAppTemplate template = createSimpleEdgeTemplate(EDGE_VERSION);
        installEdgeApp(template);

        AppCommand command = waitForCommand();
        AgentAppEventId eventId = extractEventId(command);

        // Reject the command
        agentImitator.sendCommandAck(command.getCommandId(), AckStatus.REJECTED);

        awaitEventStatus(eventId, AgentAppEventStatus.ERROR);
    }

    @Test
    public void testCommandResultFailureMarksError() throws Exception {
        AgentAppTemplate template = createSimpleEdgeTemplate(EDGE_VERSION);
        installEdgeApp(template);

        AppCommand command = waitForCommand();
        AgentAppEventId eventId = extractEventId(command);

        agentImitator.sendCommandAck(command.getCommandId(), AckStatus.ACCEPTED);
        agentImitator.sendCommandResult(command.getCommandId(), command.getStepId(), false);

        awaitEventStatus(eventId, AgentAppEventStatus.ERROR);
    }

    @Test
    public void testDeleteFailureRollbacksPendingDeletion() throws Exception {
        AgentAppTemplate template = createEdgeTemplateWithSteps(EDGE_VERSION,
                chainSteps(createComposeStep()),
                null,
                chainSteps(createComposeDownStep()),
                null, null);

        AgentApplication app = installEdgeApp(template);

        // Complete install first
        AppCommand installCmd = waitForCommand();
        completeAllSteps(installCmd);
        awaitEventStatus(extractEventId(installCmd), AgentAppEventStatus.FINISHED);

        // Create DELETE event (sets pendingDeletion=true)
        AgentAppEventRequest deleteRequest = new AgentAppEventRequest();
        deleteRequest.setActionType(AgentAppEventActionType.DELETE);
        agentImitator.expectMessageAmount(1);
        createAppEvent(app.getId(), deleteRequest);

        // Verify pendingDeletion is set
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    AgentApplication a = agentApplicationService.findById(tenantId, app.getId());
                    return a != null && a.isPendingDeletion();
                });

        // Receive and reject delete command
        AppCommand deleteCmd = waitForCommand();
        Assert.assertEquals(AppCommandAction.APP_DELETE, deleteCmd.getAction());
        AgentAppEventId deleteEventId = extractEventId(deleteCmd);

        agentImitator.sendCommandAck(deleteCmd.getCommandId(), AckStatus.REJECTED);

        awaitEventStatus(deleteEventId, AgentAppEventStatus.ERROR);

        // Verify pendingDeletion rolled back and app still exists
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    AgentApplication a = agentApplicationService.findById(tenantId, app.getId());
                    return a != null && !a.isPendingDeletion();
                });
    }

    @Test
    public void testUpgradeCancelTriggersAutoRollback() throws Exception {
        // V2 template with upgrade + rollback steps (create first so V1 can reference version)
        ComposeStep upgradeStep = createComposeStep();
        AgentAppTemplate templateV2 = createEdgeTemplateWithSteps("4.4.0EDGE",
                chainSteps(createComposeStep()),
                chainSteps(upgradeStep),
                null,
                chainSteps(createRollbackStep(), createComposeStep()), null);

        // V1 template with start + rollback steps, nextVersion pointing to V2
        RollBackStep rollbackStep = createRollbackStep();
        ComposeStep rollbackComposeStep = createComposeStep();
        AgentAppTemplate templateV1 = createEdgeTemplateWithSteps("4.3.0EDGE", "4.4.0EDGE",
                chainSteps(createComposeStep()),
                null,
                null,
                chainSteps(rollbackStep, rollbackComposeStep), null);

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

        AppCommand upgradeCmd = waitForCommand();
        Assert.assertEquals(AppCommandAction.APP_UPGRADE, upgradeCmd.getAction());
        AgentAppEventId upgradeEventId = extractEventId(upgradeCmd);

        // Cancel the in-flight upgrade -> SERVER error -> auto ROLLBACK enqueued
        doPost("/api/agent/app/" + app.getId().getId().toString()
                        + "/event/" + upgradeEventId.getId().toString() + "/cancel")
                .andExpect(status().isOk());

        awaitEventStatus(upgradeEventId, AgentAppEventStatus.ERROR);

        // Wait for automatic ROLLBACK command to arrive.
        // Use poll-based wait since the rollback command may arrive asynchronously
        // before a latch-based wait can be set up.
        AppCommand rollbackCmd = awaitCommand(AppCommandAction.APP_ROLLBACK);

        // Complete rollback
        completeAllSteps(rollbackCmd);
        AgentAppEventId rollbackEventId = extractEventId(rollbackCmd);
        awaitEventStatus(rollbackEventId, AgentAppEventStatus.FINISHED);

        // Verify desiredTemplateId cleared
        AgentApplication updated = agentApplicationService.findById(tenantId, app.getId());
        Assert.assertNotNull(updated);
        Assert.assertNull("desiredTemplateId should be cleared after rollback", updated.getDesiredTemplateId());
    }

    @Test
    public void testUpgradeAgentFailureClearsDesiredTemplateId() throws Exception {
        // V2 template with upgrade steps (create first so V1 can reference version)
        AgentAppTemplate templateV2 = createEdgeTemplateWithSteps("4.4.0EDGE",
                chainSteps(createComposeStep()),
                chainSteps(createComposeStep()),
                null, null, null);

        // V1 template with nextVersion pointing to V2
        AgentAppTemplate templateV1 = createEdgeTemplateWithSteps("4.3.0EDGE", "4.4.0EDGE",
                chainSteps(createComposeStep()),
                null, null, null, null);

        // Install with V1
        AgentApplication app = installEdgeApp(templateV1);
        AppCommand installCmd = waitForCommand();
        completeAllSteps(installCmd);
        awaitEventStatus(extractEventId(installCmd), AgentAppEventStatus.FINISHED);

        // Create UPGRADE event
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

        AppCommand upgradeCmd = waitForCommand();
        AgentAppEventId upgradeEventId = extractEventId(upgradeCmd);

        // Verify desiredTemplateId was set
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    AgentApplication a = agentApplicationService.findById(tenantId, app.getId());
                    return a != null && templateV2.getId().equals(a.getDesiredTemplateId());
                });

        // Agent rejects -> AGENT error
        agentImitator.sendCommandAck(upgradeCmd.getCommandId(), AckStatus.REJECTED);

        awaitEventStatus(upgradeEventId, AgentAppEventStatus.ERROR);

        // Verify: desiredTemplateId cleared, templateId unchanged (V1)
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    AgentApplication a = agentApplicationService.findById(tenantId, app.getId());
                    return a != null && a.getDesiredTemplateId() == null;
                });
        AgentApplication updated = agentApplicationService.findById(tenantId, app.getId());
        Assert.assertEquals("templateId should remain V1", templateV1.getId(), updated.getTemplateId());
    }

    @Test
    public void testUpdateCancelTriggersAutoRollback() throws Exception {
        // Template with start steps (used by INSTALL/UPDATE) and rollback steps
        RollBackStep rollbackStep = createRollbackStep();
        ComposeStep rollbackComposeStep = createComposeStep();
        AgentAppTemplate template = createEdgeTemplateWithSteps(EDGE_VERSION,
                chainSteps(createComposeStep()),
                null,
                null,
                chainSteps(rollbackStep, rollbackComposeStep), null);

        // Install with the template
        AgentApplication app = installEdgeApp(template);
        AppCommand installCmd = waitForCommand();
        completeAllSteps(installCmd);
        awaitEventStatus(extractEventId(installCmd), AgentAppEventStatus.FINISHED);

        // Create UPDATE event
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

        // Cancel the in-flight update -> SERVER error -> auto ROLLBACK enqueued
        doPost("/api/agent/app/" + app.getId().getId().toString()
                        + "/event/" + updateEventId.getId().toString() + "/cancel")
                .andExpect(status().isOk());

        awaitEventStatus(updateEventId, AgentAppEventStatus.ERROR);

        // Wait for the automatic ROLLBACK command and complete it
        AppCommand rollbackCmd = awaitCommand(AppCommandAction.APP_ROLLBACK);
        completeAllSteps(rollbackCmd);
        awaitEventStatus(extractEventId(rollbackCmd), AgentAppEventStatus.FINISHED);
    }

    // --- Private helpers ---

    private AgentAppTemplate createSimpleEdgeTemplate(String version) {
        return createEdgeTemplateWithSteps(version,
                chainSteps(createComposeStep()),
                null, null, null, null);
    }
}
