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
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventDeliveryState;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppTemplateService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Collections;
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
        template.setStartSteps(Collections.emptyList());
        template.setUpgradeSteps(Collections.emptyList());
        return agentAppTemplateService.save(TenantId.SYS_TENANT_ID, template);
    }
}
