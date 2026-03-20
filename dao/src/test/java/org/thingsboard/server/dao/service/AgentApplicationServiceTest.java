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
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventDeliveryState;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppTemplateService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.UUID;

@DaoSqlTest
public class AgentApplicationServiceTest extends AbstractServiceTest {

    @Autowired
    AgentService agentService;
    @Autowired
    AgentApplicationService agentApplicationService;
    @Autowired
    AgentAppTemplateService agentAppTemplateService;
    @Autowired
    AgentAppEventService agentAppEventService;
    @Autowired
    EdgeService edgeService;
    @Autowired
    DeviceService deviceService;
    @Autowired
    DeviceCredentialsService deviceCredentialsService;

    @Test
    public void testSave() {
        Agent agent = createAgent("My agent");
        AgentAppTemplate template = createTemplate();
        AgentApplication app = new AgentApplication();
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.EDGE);
        app.setTemplateId(template.getId());

        AgentApplication saved = agentApplicationService.save(tenantId, app);
        Assert.assertNotNull(saved);
        Assert.assertNotNull(saved.getId());
        Assert.assertTrue(saved.getCreatedTime() > 0);
        Assert.assertEquals(agent.getId(), saved.getAgentId());
        Assert.assertEquals(template.getId(), saved.getTemplateId());

        AgentApplication found = agentApplicationService.findById(tenantId, saved.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(saved.getId(), found.getId());

        List<AgentApplication> byAgent = agentApplicationService.findByAgentId(tenantId, agent.getId(), new PageLink(100)).getData();
        Assert.assertEquals(1, byAgent.size());
        Assert.assertEquals(saved.getId(), byAgent.get(0).getId());

        agentApplicationService.delete(tenantId, saved.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveAgentApplicationWithNullAgentId() {
        AgentApplication app = new AgentApplication();
        Assertions.assertThrows(DataValidationException.class, () ->
                agentApplicationService.save(tenantId, app));
    }

    @Test
    public void testSaveAgentApplicationWithNonExistentAgent() {
        AgentAppTemplate template = createTemplate();
        AgentApplication app = new AgentApplication();
        app.setAgentId(new AgentId(UUID.randomUUID()));
        app.setAppType(AgentApplicationType.EDGE);
        app.setTemplateId(template.getId());
        Assertions.assertThrows(DataValidationException.class, () ->
                agentApplicationService.save(tenantId, app));
    }

    @Test
    public void testFindAllByAgentId() {
        Agent agent = createAgent("Agent for list");
        AgentApplication app1 = saveApplication(agent, "app1");
        AgentApplication app2 = saveApplication(agent, "app2");

        List<AgentApplication> list = agentApplicationService.findByAgentId(tenantId, agent.getId(), new PageLink(100)).getData();
        Assert.assertEquals(2, list.size());

        agentApplicationService.delete(tenantId, app1.getId());
        agentApplicationService.delete(tenantId, app2.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testDelete() throws Exception {
        Agent agent = createAgent("Agent for delete");
        AgentApplication app = saveApplication(agent, "toDelete");

        agentApplicationService.delete(tenantId, app.getId());
        AgentApplication found = agentApplicationService.findById(tenantId, app.getId());
        Assert.assertNull(found);

        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testDeleteByAgentId() {
        Agent agent = createAgent("Agent for deleteByAgentId");
        saveApplication(agent, "a1");
        saveApplication(agent, "a2");

        List<AgentApplication> before = agentApplicationService.findByAgentId(tenantId, agent.getId(), new PageLink(100)).getData();
        Assert.assertEquals(2, before.size());

        agentApplicationService.deleteByAgentId(tenantId, agent.getId());
        List<AgentApplication> after = agentApplicationService.findByAgentId(tenantId, agent.getId(), new PageLink(100)).getData();
        Assert.assertTrue(after.isEmpty());

        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testUpdateAgentApplication() throws Exception {
        Agent agent = createAgent("Agent for update");
        AgentAppTemplate template = createTemplate();
        AgentApplication app = saveApplication(agent, "v1");

        app.setName("v2");
        AgentApplication updated = agentApplicationService.save(tenantId, app);
        Assert.assertEquals("v2", updated.getName());

        AgentApplication found = agentApplicationService.findById(tenantId, app.getId());
        Assert.assertEquals("v2", found.getName());

        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testFindByEventId() {
        Agent agent = createAgent("Agent for findByEventId");
        AgentApplication app = saveApplication(agent, "eventApp");

        AgentAppEvent event = new AgentAppEvent();
        event.setTenantId(tenantId);
        event.setApplicationId(app.getId());
        event.setActionType(AgentAppEventActionType.INSTALL);
        event.setDeliveryState(AgentAppEventDeliveryState.PENDING);
        event.setStatus(AgentAppEventStatus.PENDING);
        event.setUpdatedTime(System.currentTimeMillis());
        AgentAppEvent savedEvent = agentAppEventService.save(tenantId, event);

        AgentApplication found = agentApplicationService.findByEventId(tenantId, savedEvent.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(app.getId(), found.getId());

        AgentApplication notFound = agentApplicationService.findByEventId(tenantId, new AgentAppEventId(UUID.randomUUID()));
        Assert.assertNull(notFound);

        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSave_projectNameIsGenerated() {
        Agent agent = createAgent("Agent for project name");
        AgentAppTemplate template = createTemplate();

        AgentApplication app = new AgentApplication();
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.EDGE);
        app.setTemplateId(template.getId());

        AgentApplication saved = agentApplicationService.save(tenantId, app);
        Assert.assertNotNull(saved);
        Assert.assertNotNull("Project name should be auto-generated on create", saved.getProjectName());
        Assert.assertFalse(saved.getProjectName().isBlank());

        agentApplicationService.delete(tenantId, saved.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testUpdate_projectNameIsPreserved() {
        Agent agent = createAgent("Agent for project name update");
        AgentAppTemplate template = createTemplate();

        AgentApplication app = new AgentApplication();
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.EDGE);
        app.setTemplateId(template.getId());

        AgentApplication saved = agentApplicationService.save(tenantId, app);
        String originalProjectName = saved.getProjectName();
        Assert.assertNotNull(originalProjectName);

        saved.setName("updated-name");
        AgentApplication updated = agentApplicationService.save(tenantId, saved);

        Assert.assertEquals("Project name should be preserved on update", originalProjectName, updated.getProjectName());

        agentApplicationService.delete(tenantId, updated.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveEdgeAppResolvesRelatedEntity() {
        Agent agent = createAgent("Agent for edge resolution");
        Edge edge = createEdge("Test Edge", "test-routing-key");
        AgentApplication app = saveApplicationWithEdgeConfig(agent, "edgeApp", edge.getRoutingKey());

        AgentApplication found = agentApplicationService.findByRelatedEntity(tenantId, edge.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(app.getId(), found.getId());

        agentApplicationService.delete(tenantId, app.getId());
        edgeService.deleteEdge(tenantId, edge.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveEdgeAppWithMissingRoutingKey() {
        Agent agent = createAgent("Agent for missing key");
        AgentApplication app = saveApplicationWithEdgeConfig(agent, "noKeyApp", null);

        AgentApplication found = agentApplicationService.findByRelatedEntity(tenantId, app.getAgentId());
        Assert.assertNull(found);

        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveEdgeAppWithUnknownRoutingKey() {
        Agent agent = createAgent("Agent for unknown key");
        AgentApplication app = saveApplicationWithEdgeConfig(agent, "unknownKeyApp", "non-existent-key");

        AgentApplication found = agentApplicationService.findByRelatedEntity(tenantId, app.getAgentId());
        Assert.assertNull(found);

        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveGenericAppNoRelatedEntity() {
        Agent agent = createAgent("Agent for generic");
        AgentAppTemplate template = createTemplate();
        AgentApplication app = new AgentApplication();
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.GENERIC);
        app.setTemplateId(template.getId());

        AgentApplication saved = agentApplicationService.save(tenantId, app);
        AgentApplication found = agentApplicationService.findByRelatedEntity(tenantId, saved.getAgentId());
        Assert.assertNull(found);

        agentApplicationService.delete(tenantId, saved.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testUpdateEdgeAppChangesRelatedEntity() {
        Agent agent = createAgent("Agent for update edge");
        Edge edge1 = createEdge("Edge 1", "routing-key-1");
        Edge edge2 = createEdge("Edge 2", "routing-key-2");

        AgentApplication app = saveApplicationWithEdgeConfig(agent, "updateEdgeApp", edge1.getRoutingKey());
        Assert.assertNotNull(agentApplicationService.findByRelatedEntity(tenantId, edge1.getId()));

        // Update config to point to edge2
        DockerComposeConfig newConfig = new DockerComposeConfig();
        newConfig.setCompose(createEdgeComposeJson(edge2.getRoutingKey()));
        app.setConfig(newConfig);
        agentApplicationService.save(tenantId, app);

        Assert.assertNull(agentApplicationService.findByRelatedEntity(tenantId, edge1.getId()));
        AgentApplication found = agentApplicationService.findByRelatedEntity(tenantId, edge2.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(app.getId(), found.getId());

        agentApplicationService.delete(tenantId, app.getId());
        edgeService.deleteEdge(tenantId, edge1.getId());
        edgeService.deleteEdge(tenantId, edge2.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testFindByRelatedEntity() {
        Agent agent = createAgent("Agent for findByRelated");
        Edge edge = createEdge("Related Edge", "related-key");

        AgentApplication app1 = saveApplicationWithEdgeConfig(agent, "related1", edge.getRoutingKey());
        AgentApplication app2 = saveApplication(agent, "unrelated");

        AgentApplication found = agentApplicationService.findByRelatedEntity(tenantId, edge.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(app1.getId(), found.getId());

        AgentApplication notFound = agentApplicationService.findByRelatedEntity(tenantId, app2.getAgentId());
        Assert.assertNull(notFound);

        agentApplicationService.delete(tenantId, app1.getId());
        agentApplicationService.delete(tenantId, app2.getId());
        edgeService.deleteEdge(tenantId, edge.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveApplicationWithOrigin() {
        Agent agent = createAgent("Agent for origin");
        AgentAppTemplate template = createTemplate();

        AgentApplication app = new AgentApplication();
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.EDGE);
        app.setTemplateId(template.getId());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);

        AgentApplication saved = agentApplicationService.save(tenantId, app);
        Assert.assertEquals(AgentApplicationOrigin.INSTALLED, saved.getOrigin());

        AgentApplication found = agentApplicationService.findById(tenantId, saved.getId());
        Assert.assertEquals(AgentApplicationOrigin.INSTALLED, found.getOrigin());

        agentApplicationService.delete(tenantId, saved.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveApplicationWithDiscoveredOrigin() {
        Agent agent = createAgent("Agent for discovered origin");
        AgentAppTemplate template = createTemplate();

        AgentApplication app = new AgentApplication();
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.GENERIC);
        app.setTemplateId(template.getId());
        app.setOrigin(AgentApplicationOrigin.DISCOVERED);

        AgentApplication saved = agentApplicationService.save(tenantId, app);
        Assert.assertEquals(AgentApplicationOrigin.DISCOVERED, saved.getOrigin());

        AgentApplication found = agentApplicationService.findById(tenantId, saved.getId());
        Assert.assertEquals(AgentApplicationOrigin.DISCOVERED, found.getOrigin());

        agentApplicationService.delete(tenantId, saved.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveApplicationWithNullOrigin() {
        Agent agent = createAgent("Agent for null origin");
        AgentAppTemplate template = createTemplate();

        AgentApplication app = new AgentApplication();
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.GENERIC);
        app.setTemplateId(template.getId());

        AgentApplication saved = agentApplicationService.save(tenantId, app);
        Assert.assertNull(saved.getOrigin());

        agentApplicationService.delete(tenantId, saved.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testFindEventsByAgentId() {
        Agent agent = createAgent("Agent for events by agentId");
        AgentApplication app1 = saveApplication(agent, "app1");
        AgentApplication app2 = saveApplication(agent, "app2");

        AgentAppEvent event1 = new AgentAppEvent();
        event1.setTenantId(tenantId);
        event1.setApplicationId(app1.getId());
        event1.setActionType(AgentAppEventActionType.INSTALL);
        event1.setDeliveryState(AgentAppEventDeliveryState.PENDING);
        event1.setUpdatedTime(System.currentTimeMillis());
        agentAppEventService.save(tenantId, event1);

        AgentAppEvent event2 = new AgentAppEvent();
        event2.setTenantId(tenantId);
        event2.setApplicationId(app2.getId());
        event2.setActionType(AgentAppEventActionType.UPDATE);
        event2.setDeliveryState(AgentAppEventDeliveryState.PENDING);
        event2.setUpdatedTime(System.currentTimeMillis());
        agentAppEventService.save(tenantId, event2);

        AgentAppEvent event3 = new AgentAppEvent();
        event3.setTenantId(tenantId);
        event3.setApplicationId(app1.getId());
        event3.setActionType(AgentAppEventActionType.RESTART);
        event3.setDeliveryState(AgentAppEventDeliveryState.PENDING);
        event3.setUpdatedTime(System.currentTimeMillis());
        agentAppEventService.save(tenantId, event3);

        List<AgentAppEvent> events = agentAppEventService.findByAgentId(tenantId, agent.getId(), new PageLink(100)).getData();
        Assert.assertEquals(3, events.size());

        // Verify events from both apps are included
        long app1Events = events.stream().filter(e -> e.getApplicationId().equals(app1.getId())).count();
        long app2Events = events.stream().filter(e -> e.getApplicationId().equals(app2.getId())).count();
        Assert.assertEquals(2, app1Events);
        Assert.assertEquals(1, app2Events);

        agentApplicationService.delete(tenantId, app1.getId());
        agentApplicationService.delete(tenantId, app2.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveGatewayAppResolvesRelatedEntity_accessToken() {
        Agent agent = createAgent("Agent for GW access token");
        Device device = createDevice("GW Device 1");
        DeviceCredentials creds = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        String accessToken = creds.getCredentialsId();

        AgentApplication app = saveGatewayApplicationWithCompose(agent, "gwApp1",
                createGatewayAccessTokenCompose(accessToken));

        AgentApplication found = agentApplicationService.findByRelatedEntity(tenantId, device.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(app.getId(), found.getId());

        agentApplicationService.delete(tenantId, app.getId());
        deviceService.deleteDevice(tenantId, device.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveGatewayAppResolvesRelatedEntity_mqttBasicUserNameOnly() {
        Agent agent = createAgent("Agent for GW MQTT userName");
        Device device = createDevice("GW Device 2");
        DeviceCredentials creds = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        creds.setCredentialsType(DeviceCredentialsType.MQTT_BASIC);
        creds.setCredentialsId("gwUser1");
        creds.setCredentialsValue("{\"userName\":\"gwUser1\",\"password\":\"pass\"}");
        deviceCredentialsService.updateDeviceCredentials(tenantId, creds);

        AgentApplication app = saveGatewayApplicationWithCompose(agent, "gwApp2",
                createGatewayMqttBasicCompose(null, "gwUser1"));

        AgentApplication found = agentApplicationService.findByRelatedEntity(tenantId, device.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(app.getId(), found.getId());

        agentApplicationService.delete(tenantId, app.getId());
        deviceService.deleteDevice(tenantId, device.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveGatewayAppResolvesRelatedEntity_mqttBasicClientIdOnly() {
        Agent agent = createAgent("Agent for GW MQTT clientId");
        Device device = createDevice("GW Device 3");
        DeviceCredentials creds = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        String clientId = "myGwClient";
        creds.setCredentialsType(DeviceCredentialsType.MQTT_BASIC);
        creds.setCredentialsId(EncryptionUtil.getSha3Hash(clientId));
        creds.setCredentialsValue("{\"clientId\":\"" + clientId + "\"}");
        deviceCredentialsService.updateDeviceCredentials(tenantId, creds);

        AgentApplication app = saveGatewayApplicationWithCompose(agent, "gwApp3",
                createGatewayMqttBasicCompose(clientId, null));

        AgentApplication found = agentApplicationService.findByRelatedEntity(tenantId, device.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(app.getId(), found.getId());

        agentApplicationService.delete(tenantId, app.getId());
        deviceService.deleteDevice(tenantId, device.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveGatewayAppResolvesRelatedEntity_mqttBasicBoth() {
        Agent agent = createAgent("Agent for GW MQTT both");
        Device device = createDevice("GW Device 4");
        DeviceCredentials creds = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        String clientId = "myGwClient2";
        String userName = "myGwUser2";
        creds.setCredentialsType(DeviceCredentialsType.MQTT_BASIC);
        creds.setCredentialsId(EncryptionUtil.getSha3Hash("|", clientId, userName));
        creds.setCredentialsValue("{\"clientId\":\"" + clientId + "\",\"userName\":\"" + userName + "\",\"password\":\"pass\"}");
        deviceCredentialsService.updateDeviceCredentials(tenantId, creds);

        AgentApplication app = saveGatewayApplicationWithCompose(agent, "gwApp4",
                createGatewayMqttBasicCompose(clientId, userName));

        AgentApplication found = agentApplicationService.findByRelatedEntity(tenantId, device.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(app.getId(), found.getId());

        agentApplicationService.delete(tenantId, app.getId());
        deviceService.deleteDevice(tenantId, device.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testFindEventsByAgentIdEmpty() {
        Agent agent = createAgent("Agent with no events");

        List<AgentAppEvent> events = agentAppEventService.findByAgentId(tenantId, agent.getId(), new PageLink(100)).getData();
        Assert.assertTrue(events.isEmpty());

        agentService.deleteAgent(tenantId, agent.getId());
    }

    private Agent createAgent(String name) {
        Agent agent = new Agent();
        agent.setTenantId(tenantId);
        agent.setName(name);
        agent.setRoutingKey(UUID.randomUUID().toString());
        agent.setSecret(StringUtils.randomAlphanumeric(20));
        return agentService.saveAgent(agent);
    }

    private AgentApplication saveApplication(Agent agent, String name) {
        AgentAppTemplate template = createTemplate();
        AgentApplication app = new AgentApplication();
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.EDGE);
        app.setName(name);
        app.setTemplateId(template.getId());
        return agentApplicationService.save(tenantId, app);
    }

    private AgentAppTemplate createTemplate() {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(AgentApplicationType.GENERIC);
        template.setCurrentVersion("1.0.0");
        template.setPreviousVersion("0.9.0");
        template.setNextVersion(null);
        ComposeStartStep step = new ComposeStartStep();
        step.setId(UUID.randomUUID());
        step.setTitle("start");
        template.setStartSteps(List.of(step));
        template.setUpgradeSteps(List.of(step));
        return agentAppTemplateService.save(TenantId.SYS_TENANT_ID, template);
    }

    private Edge createEdge(String name, String routingKey) {
        Edge edge = new Edge();
        edge.setTenantId(tenantId);
        edge.setName(name);
        edge.setType("default");
        edge.setRoutingKey(routingKey);
        edge.setSecret(StringUtils.randomAlphanumeric(20));
        return edgeService.saveEdge(edge);
    }

    private AgentApplication saveApplicationWithEdgeConfig(Agent agent, String name, String routingKey) {
        AgentAppTemplate template = createTemplate();
        AgentApplication app = new AgentApplication();
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.EDGE);
        app.setName(name);
        app.setTemplateId(template.getId());

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(createEdgeComposeJson(routingKey));
        app.setConfig(config);

        return agentApplicationService.save(tenantId, app);
    }

    private Device createDevice(String name) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName(name);
        device.setType("default");
        return deviceService.saveDevice(device);
    }

    private AgentApplication saveGatewayApplicationWithCompose(Agent agent, String name, JsonNode compose) {
        AgentAppTemplate template = createTemplate();
        AgentApplication app = new AgentApplication();
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.GATEWAY);
        app.setName(name);
        app.setTemplateId(template.getId());

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);
        app.setConfig(config);

        return agentApplicationService.save(tenantId, app);
    }

    private JsonNode createGatewayAccessTokenCompose(String accessToken) {
        ObjectNode env = JacksonUtil.newObjectNode();
        env.put("TB_GW_SECURITY_TYPE", "accessToken");
        env.put("TB_GW_ACCESS_TOKEN", accessToken);
        return createGatewayComposeWithEnv(env);
    }

    private JsonNode createGatewayMqttBasicCompose(String clientId, String userName) {
        ObjectNode env = JacksonUtil.newObjectNode();
        env.put("TB_GW_SECURITY_TYPE", "usernamePassword");
        if (clientId != null) {
            env.put("TB_GW_CLIENT_ID", clientId);
        }
        if (userName != null) {
            env.put("TB_GW_USERNAME", userName);
        }
        return createGatewayComposeWithEnv(env);
    }

    private JsonNode createGatewayComposeWithEnv(ObjectNode env) {
        ObjectNode service = JacksonUtil.newObjectNode();
        service.put("image", "thingsboard/tb-gateway:3.7");
        service.set("environment", env);
        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("tb-gateway", service);
        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);
        return compose;
    }

    private JsonNode createEdgeComposeJson(String routingKey) {
        ObjectNode service = JacksonUtil.newObjectNode();
        service.put("image", "thingsboard/tb-edge:3.8.0");
        if (routingKey != null) {
            ObjectNode env = JacksonUtil.newObjectNode();
            env.put("CLOUD_ROUTING_KEY", routingKey);
            service.set("environment", env);
        }
        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("mytbedge", service);
        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);
        return compose;
    }
}
