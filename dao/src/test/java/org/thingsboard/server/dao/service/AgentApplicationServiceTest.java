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

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppConfig;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@DaoSqlTest
public class AgentApplicationServiceTest extends AbstractServiceTest {

    @Autowired
    AgentService agentService;
    @Autowired
    AgentApplicationService agentApplicationService;

    @Test
    public void testSaveAgentApplication() {
        Agent agent = createAgent("My agent");
        AgentApplication app = new AgentApplication();
        app.setAgentId(agent.getId());
        app.setType(AgentApplicationType.GATEWAY);
        app.setTemplateVersion("1.0");
        app.setPlaceholders(Map.of("key", "value"));
        Map<String, AgentAppConfig> config = Map.of("queue_type", new AgentAppConfig(false, Collections.singletonList("IN_MEMORY")));
        app.setConfiguration(config);
        app.setSteps(List.of("step1", "step2"));

        AgentApplication saved = agentApplicationService.saveAgentApplication(tenantId, app);
        Assert.assertNotNull(saved);
        Assert.assertNotNull(saved.getId());
        Assert.assertTrue(saved.getCreatedTime() > 0);
        Assert.assertEquals(agent.getId(), saved.getAgentId());
        Assert.assertEquals(AgentApplicationType.GATEWAY, saved.getType());
        Assert.assertEquals("1.0", saved.getTemplateVersion());
        Assert.assertEquals(Map.of("key", "value"), saved.getPlaceholders());
        Assert.assertEquals(config, saved.getConfiguration());
        Assert.assertEquals(List.of("step1", "step2"), saved.getSteps());

        AgentApplication found = agentApplicationService.findAgentApplicationById(tenantId, saved.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(saved.getId(), found.getId());

        List<AgentApplication> byAgent = agentApplicationService.findAgentApplicationsByAgentId(tenantId, agent.getId());
        Assert.assertEquals(1, byAgent.size());
        Assert.assertEquals(saved.getId(), byAgent.get(0).getId());

        agentApplicationService.deleteAgentApplication(tenantId, saved.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveAgentApplicationWithNullAgentId() {
        AgentApplication app = new AgentApplication();
        app.setType(AgentApplicationType.EDGE);
        Assertions.assertThrows(DataValidationException.class, () ->
                agentApplicationService.saveAgentApplication(tenantId, app));
    }

    @Test
    public void testSaveAgentApplicationWithNonExistentAgent() {
        AgentApplication app = new AgentApplication();
        app.setAgentId(new AgentId(java.util.UUID.randomUUID()));
        app.setType(AgentApplicationType.GATEWAY);
        Assertions.assertThrows(DataValidationException.class, () ->
                agentApplicationService.saveAgentApplication(tenantId, app));
    }

    @Test
    public void testSaveAgentApplicationWithEdgeTypeWithoutTemplateVersion() {
        Agent agent = createAgent("Agent for edge template test");
        AgentApplication app = new AgentApplication();
        app.setAgentId(agent.getId());
        app.setType(AgentApplicationType.EDGE);

        Assertions.assertThrows(DataValidationException.class, () ->
                agentApplicationService.saveAgentApplication(tenantId, app));

        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveAgentApplicationWithGatewayTypeWithoutTemplateVersion() {
        Agent agent = createAgent("Agent for gateway template test");
        AgentApplication app = new AgentApplication();
        app.setAgentId(agent.getId());
        app.setType(AgentApplicationType.GATEWAY);

        Assertions.assertThrows(DataValidationException.class, () ->
                agentApplicationService.saveAgentApplication(tenantId, app));

        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveAgentApplicationWithEdgeTypeWithTemplateVersion() {
        Agent agent = createAgent("Agent for edge with template");
        AgentApplication app = new AgentApplication();
        app.setAgentId(agent.getId());
        app.setType(AgentApplicationType.EDGE);
        app.setTemplateVersion("4.3.0");

        AgentApplication saved = agentApplicationService.saveAgentApplication(tenantId, app);
        Assert.assertNotNull(saved.getId());
        Assert.assertEquals(AgentApplicationType.EDGE, saved.getType());
        Assert.assertEquals("4.3.0", saved.getTemplateVersion());

        agentApplicationService.deleteAgentApplication(tenantId, saved.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveAgentApplicationWithNullType() {
        Agent agent = createAgent("Agent for type test");
        AgentApplication app = new AgentApplication();
        app.setAgentId(agent.getId());
        try {
            Assertions.assertThrows(DataValidationException.class, () ->
                    agentApplicationService.saveAgentApplication(tenantId, app));
        } finally {
            agentService.deleteAgent(tenantId, agent.getId());
        }
    }

    @Test
    public void testFindAgentApplicationsByAgentId() {
        Agent agent = createAgent("Agent for list");
        AgentApplication app1 = saveApplication(agent, AgentApplicationType.GATEWAY, "app1");
        AgentApplication app2 = saveApplication(agent, AgentApplicationType.EDGE, "app2");

        List<AgentApplication> list = agentApplicationService.findAgentApplicationsByAgentId(tenantId, agent.getId());
        Assert.assertEquals(2, list.size());

        agentApplicationService.deleteAgentApplication(tenantId, app1.getId());
        agentApplicationService.deleteAgentApplication(tenantId, app2.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testDeleteAgentApplication() {
        Agent agent = createAgent("Agent for delete");
        AgentApplication app = saveApplication(agent, AgentApplicationType.GATEWAY, "toDelete");

        agentApplicationService.deleteAgentApplication(tenantId, app.getId());
        AgentApplication found = agentApplicationService.findAgentApplicationById(tenantId, app.getId());
        Assert.assertNull(found);

        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testDeleteByAgentId() {
        Agent agent = createAgent("Agent for deleteByAgentId");
        saveApplication(agent, AgentApplicationType.GATEWAY, "a1");
        saveApplication(agent, AgentApplicationType.EDGE, "a2");

        List<AgentApplication> before = agentApplicationService.findAgentApplicationsByAgentId(tenantId, agent.getId());
        Assert.assertEquals(2, before.size());

        agentApplicationService.deleteByAgentId(tenantId, agent.getId());
        List<AgentApplication> after = agentApplicationService.findAgentApplicationsByAgentId(tenantId, agent.getId());
        Assert.assertTrue(after.isEmpty());

        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testUpdateAgentApplication() {
        Agent agent = createAgent("Agent for update");
        AgentApplication app = saveApplication(agent, AgentApplicationType.GATEWAY, "v1");

        app.setTemplateVersion("2.0");
        app.setSteps(List.of("step1", "step2", "step3"));
        AgentApplication updated = agentApplicationService.saveAgentApplication(tenantId, app);
        Assert.assertEquals("2.0", updated.getTemplateVersion());
        Assert.assertEquals(3, updated.getSteps().size());

        AgentApplication found = agentApplicationService.findAgentApplicationById(tenantId, app.getId());
        Assert.assertEquals("2.0", found.getTemplateVersion());

        agentApplicationService.deleteAgentApplication(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    private Agent createAgent(String name) {
        Agent agent = new Agent();
        agent.setTenantId(tenantId);
        agent.setName(name);
        return agentService.saveAgent(agent);
    }

    private AgentApplication saveApplication(Agent agent, AgentApplicationType type, String templateVersion) {
        AgentApplication app = new AgentApplication();
        app.setAgentId(agent.getId());
        app.setType(type);
        app.setTemplateVersion(templateVersion);
        app.setPlaceholders(Collections.emptyMap());
        app.setConfiguration(Collections.emptyMap());
        app.setSteps(Collections.emptyList());
        return agentApplicationService.saveAgentApplication(tenantId, app);
    }
}
