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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.agent.imitator.AgentImitator;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.ComposeStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.agent.AgentAppTemplateService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.gen.agent.v1.AppCommand;
import org.thingsboard.server.gen.agent.v1.ComposeState;
import org.thingsboard.server.gen.agent.v1.ContainerInfo;
import org.thingsboard.server.gen.agent.v1.ProjectStateSync;
import org.thingsboard.server.gen.agent.v1.ServerToAgent;

import java.util.Collections;
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

    protected AgentImitator agentImitator;
    protected Agent agent;

    @Autowired
    protected AgentAppTemplateService agentAppTemplateService;

    @Autowired
    protected AttributesService attributesService;

    @Before
    public void setupAgentTest() throws Exception {
        loginTenantAdmin();

        agent = createAgent("Test Agent");

        agentImitator = new AgentImitator("localhost", 7070,
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
        AgentAppTemplate template = new AgentAppTemplate();
        template.setTenantId(tenantId);
        template.setAppType(appType);
        template.setCurrentVersion(version);
        template.setConfig(config);
        template.setStartSteps(startSteps);
        return agentAppTemplateService.save(tenantId, template);
    }

    protected ComposeStep createComposeStep() {
        ComposeStep step = new ComposeStep();
        step.setId(UUID.randomUUID());
        step.setTitle("Deploy compose");
        return step;
    }

    // --- Application helpers ---

    protected AgentApplication installApp(AgentAppEventRequest request) {
        return doPost("/api/agent/app/event", request, AgentApplication.class);
    }

    protected void createAppEvent(AgentApplicationId appId, AgentAppEventRequest request) throws Exception {
        doPost("/api/agent/app/" + appId.getId().toString() + "/event", request)
                .andExpect(status().isOk());
    }

    protected PageData<AgentApplication> getAgentApps(String agentId) {
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
        agentImitator.expectMessageAmount(1);
        agentImitator.waitForMessages();
        return agentImitator.getLatestCommand();
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
}
