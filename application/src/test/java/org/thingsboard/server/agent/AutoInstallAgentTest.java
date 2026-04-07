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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.agent.imitator.AgentImitator;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AgentGroup;
import org.thingsboard.server.common.data.agent.AgentProvisionType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.ComposeStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentGroupId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.agent.v1.AckStatus;
import org.thingsboard.server.gen.agent.v1.AppCommand;
import org.thingsboard.server.gen.agent.v1.AppCommandAction;
import org.thingsboard.server.gen.agent.v1.ProvisionResponse;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
@TestPropertySource(properties = {
        "edges.enabled=true",
        "queue.rule-engine.stats.enabled=false"
})
public class AutoInstallAgentTest extends AbstractControllerTest {

    @Autowired
    private AgentService agentService;
    @Autowired
    private AgentApplicationService agentApplicationService;
    @Autowired
    private AgentAppProfileService agentAppProfileService;

    private AgentImitator imitator;
    private AgentGroupId groupId;
    private AgentId agentId;

    @Before
    public void setUp() throws Exception {
        loginTenantAdmin();
    }

    @After
    public void tearDown() {
        if (imitator != null) {
            try {
                imitator.disconnect();
            } catch (Exception ignored) {
            }
        }
        try {
            loginTenantAdmin();
            if (agentId != null) {
                try {
                    doDelete("/api/agent/" + agentId.getId()).andExpect(status().isOk());
                } catch (Exception ignored) {
                }
            }
            if (groupId != null) {
                try {
                    doDelete("/api/agent/group/" + groupId.getId()).andExpect(status().isOk());
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    public void testAutoInstallCreatesAppAndDispatchesInstallCommandOnInitialSyncComplete() throws Exception {
        // 1) Create template + profile, create group with provisioning, assign profile to group
        AgentAppTemplate template = getOrCreateGenericTemplate();
        AgentAppProfile profile = createProfile("auto-install-profile", template);
        AgentGroup group = createGroup();
        assignProfileToGroup(group.getId(), profile.getId());
        groupId = group.getId();

        // 2) Provision a new agent via gRPC — returns routing key/secret
        imitator = new AgentImitator("localhost", 7070, "", "");
        imitator.provision(group.getProvisionKey(), group.getProvisionSecret());
        ProvisionResponse resp = imitator.getProvisionResponse();
        Assert.assertTrue("Provision failed: " + resp.getErrorMessage(), resp.getSuccess());

        Agent provisioned = agentService.findAgentByRoutingKey(tenantId, resp.getRoutingKey());
        Assert.assertNotNull(provisioned);
        agentId = provisioned.getId();
        Assert.assertEquals(group.getId(), provisioned.getAgentGroupId());

        // 3) Agent connects via HELLO — at this point no apps should exist yet
        imitator.connect();
        Assert.assertTrue(imitator.getHelloAck().getSuccess());

        // Sanity: no apps yet because InitialSyncComplete hasn't been sent
        PageData<AgentApplication> appsBefore = agentApplicationService.findByAgentId(
                tenantId, agentId, new PageLink(10));
        Assert.assertEquals(0, appsBefore.getData().size());

        // 4) Send InitialSyncComplete — triggers auto-install
        imitator.expectMessageAmount(1);
        imitator.sendInitialSyncComplete();

        // 5) Verify auto-install created the application asynchronously
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> !agentApplicationService.findByAgentId(
                        tenantId, agentId, new PageLink(10)).getData().isEmpty());

        List<AgentApplication> apps = agentApplicationService.findByAgentId(
                tenantId, agentId, new PageLink(10)).getData();
        Assert.assertEquals(1, apps.size());
        AgentApplication created = apps.get(0);
        Assert.assertEquals(AgentApplicationOrigin.AUTO_PROVISIONED, created.getOrigin());
        Assert.assertEquals(profile.getId(), created.getApplicationProfileId());
        Assert.assertEquals(template.getId(), created.getTemplateId());

        // 6) Verify the INSTALL command was dispatched to the agent
        imitator.waitForMessages();
        AppCommand command = imitator.getLatestCommand();
        Assert.assertEquals(AppCommandAction.APP_INSTALL, command.getAction());

        // 7) Complete the command → event should reach FINISHED
        AgentAppEventId eventId = new AgentAppEventId(new java.util.UUID(
                command.getCommandId().getIdMSB(), command.getCommandId().getIdLSB()));
        imitator.sendCommandAck(command.getCommandId(), AckStatus.ACCEPTED);
        imitator.sendCommandResult(command.getCommandId(), command.getStepId(), true);

        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    var ev = agentAppEventService.findById(tenantId, eventId);
                    return ev != null && ev.getStatus() == AgentAppEventStatus.FINISHED;
                });
    }

    @Test
    public void testAutoInstallIsIdempotentAcrossReconnects() throws Exception {
        AgentAppTemplate template = getOrCreateGenericTemplate();
        AgentAppProfile profile = createProfile("idempotent-profile", template);
        AgentGroup group = createGroup();
        assignProfileToGroup(group.getId(), profile.getId());
        groupId = group.getId();

        // First connect: provision + sync complete → 1 app created
        imitator = new AgentImitator("localhost", 7070, "", "");
        imitator.provision(group.getProvisionKey(), group.getProvisionSecret());
        Agent provisioned = agentService.findAgentByRoutingKey(tenantId, imitator.getProvisionResponse().getRoutingKey());
        agentId = provisioned.getId();

        imitator.connect();
        imitator.expectMessageAmount(1);
        imitator.sendInitialSyncComplete();

        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> !agentApplicationService.findByAgentId(
                        tenantId, agentId, new PageLink(10)).getData().isEmpty());
        int appsAfterFirst = agentApplicationService.findByAgentId(
                tenantId, agentId, new PageLink(10)).getData().size();
        Assert.assertEquals(1, appsAfterFirst);

        // Drain the first install command so it does not interfere with the reconnect check
        imitator.waitForMessages();
        imitator.expectMessageAmount(0);

        // Reconnect: send InitialSyncComplete again → should NOT create a new app
        imitator.disconnect();
        imitator = new AgentImitator("localhost", 7070,
                provisioned.getRoutingKey(), provisioned.getSecret());
        imitator.connect();
        imitator.sendInitialSyncComplete();

        // Give the server time to process — then assert count didn't grow
        Thread.sleep(2000);
        int appsAfterReconnect = agentApplicationService.findByAgentId(
                tenantId, agentId, new PageLink(10)).getData().size();
        Assert.assertEquals("Auto-install should be idempotent on reconnect",
                1, appsAfterReconnect);
    }

    @Test
    public void testAutoInstallSkippedWhenGroupHasNoProfiles() throws Exception {
        // Group with no profiles assigned
        AgentGroup group = createGroup();
        groupId = group.getId();

        imitator = new AgentImitator("localhost", 7070, "", "");
        imitator.provision(group.getProvisionKey(), group.getProvisionSecret());
        Agent provisioned = agentService.findAgentByRoutingKey(tenantId, imitator.getProvisionResponse().getRoutingKey());
        agentId = provisioned.getId();

        imitator.connect();
        imitator.sendInitialSyncComplete();

        // No apps should ever be created
        Thread.sleep(2000);
        PageData<AgentApplication> apps = agentApplicationService.findByAgentId(
                tenantId, agentId, new PageLink(10));
        Assert.assertEquals(0, apps.getData().size());
    }

    // --- Helpers ---

    /**
     * For GENERIC app type, BaseAgentApplicationService.resolveTemplateId() overrides the
     * templateId on new apps with a lookup by (appType, DOCKER_COMPOSE, defaultVersion).
     * To keep the test's expected templateId consistent with what the save pipeline will
     * resolve, we return whatever that same lookup returns — creating one if none exists.
     * This also makes the test resilient to ordering/pollution across test methods.
     */
    private AgentAppTemplate getOrCreateGenericTemplate() {
        AgentAppTemplate existing = agentAppTemplateService.findByAppTypeAndConfigTypeAndVersion(
                AgentApplicationType.GENERIC, AgentAppConfigType.DOCKER_COMPOSE,
                AgentApplicationType.GENERIC.getDefaultVersion());
        if (existing != null) {
            return existing;
        }
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(JacksonUtil.toJsonNode(
                "{\"services\":{\"my-app\":{\"image\":\"my-app:1.0\"}}}"));

        AgentAppTemplate template = new AgentAppTemplate();
        template.setTenantId(tenantId);
        template.setAppType(AgentApplicationType.GENERIC);
        template.setCurrentVersion(AgentApplicationType.GENERIC.getDefaultVersion());
        template.setConfig(config);

        ComposeStep step = new ComposeStep();
        step.setId(UUID.randomUUID());
        step.setTitle("Deploy compose");
        template.setStartSteps(List.of(step));

        return agentAppTemplateService.save(tenantId, template);
    }

    private AgentAppProfile createProfile(String name, AgentAppTemplate template) {
        AgentAppProfile profile = new AgentAppProfile();
        profile.setTenantId(tenantId);
        profile.setName(name + "-" + System.nanoTime());
        profile.setAppType(template.getAppType());
        profile.setTemplateId(template.getId());
        profile.setConfig(template.getConfig().copy());
        return doPost("/api/agent/app/profile", profile, AgentAppProfile.class);
    }

    private AgentGroup createGroup() {
        AgentGroup g = new AgentGroup();
        g.setName("auto-install-group-" + System.nanoTime());
        g.setProvisionType(AgentProvisionType.ALLOW_CREATE_NEW_AGENTS);
        try {
            return doPost("/api/agent/group", g, AgentGroup.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void assignProfileToGroup(AgentGroupId groupId, AgentAppProfileId profileId) throws Exception {
        doPost("/api/agent/group/" + groupId.getId() + "/profile/" + profileId.getId())
                .andExpect(status().isOk());
    }

    // Autowired services that AbstractControllerTest doesn't expose directly
    @Autowired
    private org.thingsboard.server.dao.agent.AgentAppTemplateService agentAppTemplateService;
    @Autowired
    private org.thingsboard.server.dao.agent.AgentAppEventService agentAppEventService;
}
