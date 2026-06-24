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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.TestSocketUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.agent.imitator.AgentImitator;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.AgentAppInstallResponse;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.ComposeDownStep;
import org.thingsboard.server.common.data.agent.step.ComposeRestartStep;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.common.data.agent.step.ComposeStep;
import org.thingsboard.server.common.data.agent.step.ComposeTypeChoiceStep;
import org.thingsboard.server.common.data.agent.step.RollBackStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppTemplateService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.agent.StepLinkedListUtils;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.gen.agent.v1.AckStatus;
import org.thingsboard.server.gen.agent.v1.AppCommand;
import org.thingsboard.server.gen.agent.v1.AppCommandAction;
import org.thingsboard.server.gen.agent.v1.ComposeState;
import org.thingsboard.server.gen.agent.v1.ContainerInfo;
import org.thingsboard.server.gen.agent.v1.ProjectStateSync;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "edges.enabled=true",
        "queue.rule-engine.stats.enabled=false"
})
@Slf4j
abstract public class AbstractAgentTest extends AbstractControllerTest {

    public static final String AGENT_HOST = "localhost";
    public static final int AGENT_PORT = TestSocketUtils.findAvailableTcpPort();

    @DynamicPropertySource
    static void agentTestProps(DynamicPropertyRegistry registry) {
        registry.add("edges.rpc.port", () -> AGENT_PORT);
    }

    protected AgentImitator agentImitator;
    protected Agent agent;

    @Autowired
    protected AgentAppTemplateService agentAppTemplateService;

    @Autowired
    protected AttributesService attributesService;

    @Autowired
    protected AgentAppEventService agentAppEventService;

    @Autowired
    protected AgentApplicationService agentApplicationService;

    @Before
    public void setupAgentTest() throws Exception {
        loginTenantAdmin();

        agent = createAgent("Test Agent");

        agentImitator = new AgentImitator(AGENT_HOST, AGENT_PORT,
                agent.getRoutingKey(), agent.getSecret());
        agentImitator.connect();

        Assert.assertNotNull("HelloAck should not be null", agentImitator.getHelloAck());
        Assert.assertTrue("HelloAck should be successful", agentImitator.getHelloAck().getSuccess());
    }

    @After
    public void teardownAgentTest() {
        try {
            agentImitator.disconnect();
        } catch (Exception ignored) {
        }
        try {
            loginTenantAdmin();
            doDelete("/api/agent/" + agent.getId().getId().toString())
                    .andExpect(status().isOk());
        } catch (Exception ignored) {
        }
    }

    // --- Agent CRUD helpers ---

    protected Agent createAgent(String name) {
        Agent newAgent = new Agent();
        newAgent.setName(name);
        newAgent.setRoutingKey(StringUtils.randomAlphanumeric(20));
        newAgent.setSecret(StringUtils.randomAlphanumeric(20));
        return doPost("/api/agent", newAgent, Agent.class);
    }

    // --- Template helpers ---

    protected AgentAppTemplate createAgentAppTemplate(AgentApplicationType appType, String version,
                                                       AgentAppConfig config, List<AgentAppStep> startSteps) {
        return createAgentAppTemplate(appType, version, config, startSteps, null);
    }

    protected AgentAppTemplate createAgentAppTemplate(AgentApplicationType appType, String version,
                                                       AgentAppConfig config, List<AgentAppStep> startSteps,
                                                       String imageDigest) {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setTenantId(tenantId);
        template.setAppType(appType);
        template.setCurrentVersion(version);
        template.setConfigType(config != null ? config.getType() : null);
        JsonNode compose = config instanceof DockerComposeConfig dockerConfig ? dockerConfig.getCompose() : null;
        template.setStartSteps(compose != null ? withComposeTemplate(startSteps, compose) : startSteps);
        template.setImageDigest(imageDigest);
        return agentAppTemplateService.save(tenantId, template);
    }

    // Carries the compose body in a template-only ComposeTypeChoiceStep (as production templates do),
    // linked as the chain head so the step list stays a valid linked list; it is filtered out before
    // the agent executes steps, so it does not affect step-completion assertions.
    protected List<AgentAppStep> withComposeTemplate(List<AgentAppStep> startSteps, JsonNode compose) {
        List<AgentAppStep> steps = new ArrayList<>(startSteps != null ? startSteps : List.of());
        ComposeTypeChoiceStep choice = new ComposeTypeChoiceStep();
        choice.setId(UUID.randomUUID());
        choice.setTitle("Choose compose");
        choice.setTemplateOnly(true);
        choice.setComposeTemplates(Map.of("default", compose));
        AgentAppStep head = steps.isEmpty() ? null : StepLinkedListUtils.findFirstStep(steps);
        choice.setNextId(head != null ? head.getId() : null);
        steps.add(choice);
        return steps;
    }

    protected JsonNode resolveTemplateCompose(AgentAppTemplate template) {
        if (template.getStartSteps() == null) {
            return null;
        }
        return template.getStartSteps().stream()
                .filter(ComposeTypeChoiceStep.class::isInstance)
                .map(step -> ((ComposeTypeChoiceStep) step).getComposeTemplates().values().iterator().next())
                .findFirst()
                .orElse(null);
    }

    protected DockerComposeConfig resolveAppConfig(AgentAppTemplate template) {
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(resolveTemplateCompose(template));
        return config;
    }

    protected ComposeStep createComposeStep() {
        ComposeStep step = new ComposeStep();
        step.setId(UUID.randomUUID());
        step.setTitle("Deploy compose");
        return step;
    }

    protected ComposeRestartStep createComposeRestartStep() {
        ComposeRestartStep step = new ComposeRestartStep();
        step.setId(UUID.randomUUID());
        step.setTitle("Deploy compose restart");
        return step;
    }

    // --- Application helpers ---

    protected AgentApplication installApp(AgentAppEventRequest request) {
        return doPost("/api/agent/app/event", request, AgentAppInstallResponse.class).getApplication();
    }

    protected void createAppEvent(AgentApplicationId appId, AgentAppEventRequest request) throws Exception {
        doPost("/api/agent/app/" + appId.getId().toString() + "/event", request)
                .andExpect(status().isOk());
    }

    protected PageData<AgentApplicationInfo> getAgentApps(String agentId) {
        try {
            return doGetTypedWithPageLink("/api/agent/" + agentId + "/apps?",
                    new TypeReference<>() {}, new PageLink(100));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // --- Project sync helpers ---

    protected void sendProjectSync(String projectName, String composeJson,
                                    Map<String, ContainerInfo> containerStates) {
        ComposeState.Builder composeBuilder = ComposeState.newBuilder();
        if (composeJson != null) {
            composeBuilder.setComposeJson(composeJson);
        }
        if (containerStates != null) {
            composeBuilder.putAllContainerStates(containerStates);
        }
        ProjectStateSync projectSync = ProjectStateSync.newBuilder()
                .setProjectName(projectName)
                .setCompose(composeBuilder.build())
                .build();
        agentImitator.sendProjectSync(projectSync);
    }

    protected void sendProjectRemoval(String projectName) {
        ProjectStateSync projectSync = ProjectStateSync.newBuilder()
                .setProjectName(projectName)
                .setRemoved(true)
                .build();
        agentImitator.sendProjectSync(projectSync);
    }

    // --- Command helpers ---

    protected AppCommand waitForCommand() throws InterruptedException {
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> agentImitator.hasUnconsumedCommand(c -> true));
        return agentImitator.consumeNextCommand(c -> true);
    }

    // --- Attribute verification ---

    protected void verifyAttribute(EntityId entityId, String key, Object expectedValue) {
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    Optional<AttributeKvEntry> attr = attributesService
                            .find(tenantId, entityId, AttributeScope.SERVER_SCOPE, key).get();
                    return attr.filter(attributeKvEntry -> String.valueOf(expectedValue).equals(
                            String.valueOf(attributeKvEntry.getValue()))).isPresent();
                });
    }

    // --- Compose JSON construction ---

    protected String constructComposeJson(Map<String, String> serviceImageMap) {
        ObjectNode root = JacksonUtil.newObjectNode();
        ObjectNode services = JacksonUtil.newObjectNode();
        serviceImageMap.forEach((name, image) -> {
            ObjectNode service = JacksonUtil.newObjectNode();
            service.put("image", image);
            services.set(name, service);
        });
        root.set("services", services);
        return root.toString();
    }

    // --- Step creation helpers ---

    protected ComposeStartStep createComposeStartStep() {
        ComposeStartStep step = new ComposeStartStep();
        step.setId(UUID.randomUUID());
        step.setTitle("Start compose");
        return step;
    }

    protected ComposeDownStep createComposeDownStep() {
        ComposeDownStep step = new ComposeDownStep();
        step.setId(UUID.randomUUID());
        step.setTitle("Compose down");
        return step;
    }

    protected RollBackStep createRollbackStep() {
        RollBackStep step = new RollBackStep();
        step.setId(UUID.randomUUID());
        step.setTitle("Rollback");
        return step;
    }

    protected List<AgentAppStep> chainSteps(AgentAppStep... steps) {
        if (steps.length == 0) return List.of();
        for (int i = 0; i < steps.length - 1; i++) {
            steps[i].setNextId(steps[i + 1].getId());
        }
        steps[steps.length - 1].setNextId(null);
        return Arrays.asList(steps);
    }

    // --- Full template creation ---

    protected AgentAppTemplate createEdgeTemplateWithSteps(String version,
                                                            List<AgentAppStep> startSteps,
                                                            List<AgentAppStep> upgradeSteps,
                                                            List<AgentAppStep> deleteSteps,
                                                            List<AgentAppStep> rollbackSteps,
                                                            List<AgentAppStep> restartSteps) {
        return createEdgeTemplateWithSteps(version, null, startSteps, upgradeSteps, deleteSteps, rollbackSteps, restartSteps);
    }

    protected AgentAppTemplate createEdgeTemplateWithSteps(String version, String nextVersion,
                                                            List<AgentAppStep> startSteps,
                                                            List<AgentAppStep> upgradeSteps,
                                                            List<AgentAppStep> deleteSteps,
                                                            List<AgentAppStep> rollbackSteps,
                                                            List<AgentAppStep> restartSteps) {
        String composeJson = constructComposeJson(
                Map.of("tb-edge", "thingsboard/tb-edge-pe:" + version));
        JsonNode compose = JacksonUtil.toJsonNode(composeJson);

        AgentAppTemplate template = new AgentAppTemplate();
        template.setTenantId(tenantId);
        template.setAppType(AgentApplicationType.EDGE);
        template.setCurrentVersion(version);
        template.setNextVersion(nextVersion);
        template.setConfigType(AgentAppConfigType.DOCKER_COMPOSE);
        template.setStartSteps(withComposeTemplate(startSteps, compose));
        template.setUpgradeSteps(upgradeSteps);
        template.setDeleteSteps(deleteSteps);
        template.setRollbackSteps(rollbackSteps);
        template.setRestartSteps(restartSteps);
        return agentAppTemplateService.save(tenantId, template);
    }

    // --- Install helpers ---

    protected AgentApplication installEdgeApp(AgentAppTemplate template) {
        AgentApplication app = new AgentApplication();
        app.setAgentId(agent.getId());
        app.setName("Test Edge App");
        app.setAppType(AgentApplicationType.EDGE);
        app.setTemplateId(template.getId());
        app.setConfig(resolveAppConfig(template));

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.INSTALL);
        request.setApplication(app);

        agentImitator.expectMessageAmount(1);
        return installApp(request);
    }

    // --- Event helpers ---

    protected AgentAppEventId extractEventId(AppCommand command) {
        return new AgentAppEventId(new UUID(
                command.getCommandId().getIdMSB(),
                command.getCommandId().getIdLSB()));
    }

    protected void awaitEventStatus(AgentAppEventId eventId, AgentAppEventStatus expected) {
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    AgentAppEvent event = agentAppEventService.findById(tenantId, eventId);
                    return event != null && event.getStatus() == expected;
                });
    }

    protected void completeAllSteps(AppCommand cmd) throws InterruptedException {
        agentImitator.sendCommandAck(cmd.getCommandId(), AckStatus.ACCEPTED);
        agentImitator.sendCommandResult(cmd.getCommandId(), cmd.getStepId(), true);
        for (int i = 1; i < cmd.getTotalSteps(); i++) {
            AppCommand next = waitForCommand();
            agentImitator.sendCommandAck(next.getCommandId(), AckStatus.ACCEPTED);
            agentImitator.sendCommandResult(next.getCommandId(), next.getStepId(), true);
        }
    }

    /**
     * Polls for the next unconsumed command with the given action and advances the
     * consumption cursor past it. Use this when commands of other actions may be
     * interleaved (e.g. an auto-rollback or a resumed event after reconnect);
     * {@link #waitForCommand()} returns the next command regardless of action.
     */
    protected AppCommand awaitCommand(AppCommandAction expectedAction) {
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> agentImitator.hasUnconsumedCommand(c -> c.getAction() == expectedAction));
        return agentImitator.consumeNextCommand(c -> c.getAction() == expectedAction);
    }

    // --- Reconnect helpers ---

    protected void reconnectAgent() throws InterruptedException {
        agentImitator.disconnect();
        agentImitator = new AgentImitator(AGENT_HOST, AGENT_PORT,
                agent.getRoutingKey(), agent.getSecret());
        agentImitator.connect();
        Assert.assertNotNull("HelloAck should not be null after reconnect", agentImitator.getHelloAck());
        Assert.assertTrue("HelloAck should be successful after reconnect", agentImitator.getHelloAck().getSuccess());
    }
}
