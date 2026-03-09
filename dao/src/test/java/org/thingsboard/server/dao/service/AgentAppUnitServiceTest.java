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
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.agent.AgentAppTemplateService;
import org.thingsboard.server.dao.agent.AgentAppUnitService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Collections;
import java.util.List;

@DaoSqlTest
public class AgentAppUnitServiceTest extends AbstractServiceTest {

    @Autowired
    AgentService agentService;
    @Autowired
    AgentApplicationService agentApplicationService;
    @Autowired
    AgentAppTemplateService agentAppTemplateService;
    @Autowired
    AgentAppUnitService agentAppUnitService;

    @Test
    public void testSaveAgentAppUnit() {
        Agent agent = createAgent("My agent");
        AgentApplication app = saveApplication(agent);
        AgentAppUnit unit = new AgentAppUnit();
        unit.setAgentApplicationId(app.getId());
        unit.setIdentifier("unit-1");
        unit.setType("container");

        AgentAppUnit saved = agentAppUnitService.saveAgentAppUnit(tenantId, unit);
        Assert.assertNotNull(saved);
        Assert.assertNotNull(saved.getId());
        Assert.assertTrue(saved.getCreatedTime() > 0);
        Assert.assertEquals(app.getId(), saved.getAgentApplicationId());
        Assert.assertEquals("unit-1", saved.getIdentifier());
        Assert.assertEquals("container", saved.getType());

        AgentAppUnit found = agentAppUnitService.findAgentAppUnitById(tenantId, saved.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(saved.getId(), found.getId());

        List<AgentAppUnit> byApp = agentAppUnitService.findAgentAppUnitsByAgentAppId(tenantId, app.getId());
        Assert.assertEquals(1, byApp.size());
        Assert.assertEquals(saved.getId(), byApp.get(0).getId());

        agentAppUnitService.deleteAgentAppUnit(tenantId, saved.getId());
        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveAgentAppUnitWithNullAgentApplicationId() {
        AgentAppUnit unit = new AgentAppUnit();
        unit.setIdentifier("id");
        unit.setType("type");
        Assertions.assertThrows(DataValidationException.class, () ->
                agentAppUnitService.saveAgentAppUnit(tenantId, unit));
    }

    @Test
    public void testSaveAgentAppUnitWithNonExistentAgentApplication() {
        AgentAppUnit unit = new AgentAppUnit();
        unit.setAgentApplicationId(new AgentApplicationId(java.util.UUID.randomUUID()));
        unit.setIdentifier("id");
        unit.setType("type");
        Assertions.assertThrows(DataValidationException.class, () ->
                agentAppUnitService.saveAgentAppUnit(tenantId, unit));
    }

    @Test
    public void testSaveAgentAppUnitWithBlankIdentifier() {
        Agent agent = createAgent("Agent");
        AgentApplication app = saveApplication(agent);
        AgentAppUnit unit = new AgentAppUnit();
        unit.setAgentApplicationId(app.getId());
        unit.setIdentifier("  ");
        unit.setType("type");
        Assertions.assertThrows(DataValidationException.class, () ->
                agentAppUnitService.saveAgentAppUnit(tenantId, unit));
        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveAgentAppUnitWithBlankType() {
        Agent agent = createAgent("Agent");
        AgentApplication app = saveApplication(agent);
        AgentAppUnit unit = new AgentAppUnit();
        unit.setAgentApplicationId(app.getId());
        unit.setIdentifier("id");
        unit.setType("  ");
        Assertions.assertThrows(DataValidationException.class, () ->
                agentAppUnitService.saveAgentAppUnit(tenantId, unit));
        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testFindAgentAppUnitsByAgentAppId() {
        Agent agent = createAgent("Agent");
        AgentApplication app = saveApplication(agent);
        AgentAppUnit u1 = saveUnit(app, "id1", "t1");
        AgentAppUnit u2 = saveUnit(app, "id2", "t2");

        List<AgentAppUnit> list = agentAppUnitService.findAgentAppUnitsByAgentAppId(tenantId, app.getId());
        Assert.assertEquals(2, list.size());

        agentAppUnitService.deleteAgentAppUnit(tenantId, u1.getId());
        agentAppUnitService.deleteAgentAppUnit(tenantId, u2.getId());
        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testDeleteAgentAppUnit() {
        Agent agent = createAgent("Agent");
        AgentApplication app = saveApplication(agent);
        AgentAppUnit unit = saveUnit(app, "toDelete", "type");

        agentAppUnitService.deleteAgentAppUnit(tenantId, unit.getId());
        AgentAppUnit found = agentAppUnitService.findAgentAppUnitById(tenantId, unit.getId());
        Assert.assertNull(found);

        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testDeleteByAgentApplicationId() {
        Agent agent = createAgent("Agent");
        AgentApplication app = saveApplication(agent);
        saveUnit(app, "u1", "t1");
        saveUnit(app, "u2", "t2");

        List<AgentAppUnit> before = agentAppUnitService.findAgentAppUnitsByAgentAppId(tenantId, app.getId());
        Assert.assertEquals(2, before.size());

        agentAppUnitService.deleteByAgentApplicationId(tenantId, app.getId());
        List<AgentAppUnit> after = agentAppUnitService.findAgentAppUnitsByAgentAppId(tenantId, app.getId());
        Assert.assertTrue(after.isEmpty());

        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testDeleteAgentApplicationRemovesAgentAppUnits() {
        Agent agent = createAgent("Agent");
        AgentApplication app = saveApplication(agent);
        saveUnit(app, "c1", "t1");
        saveUnit(app, "c2", "t2");

        List<AgentAppUnit> before = agentAppUnitService.findAgentAppUnitsByAgentAppId(tenantId, app.getId());
        Assert.assertEquals(2, before.size());

        agentApplicationService.delete(tenantId, app.getId());
        List<AgentAppUnit> after = agentAppUnitService.findAgentAppUnitsByAgentAppId(tenantId, app.getId());
        Assert.assertTrue(after.isEmpty());

        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testUpdateAgentAppUnit() {
        Agent agent = createAgent("Agent");
        AgentApplication app = saveApplication(agent);
        AgentAppUnit unit = saveUnit(app, "id1", "type1");

        unit.setIdentifier("id1-updated");
        unit.setType("type2");
        AgentAppUnit updated = agentAppUnitService.saveAgentAppUnit(tenantId, unit);
        Assert.assertEquals("id1-updated", updated.getIdentifier());
        Assert.assertEquals("type2", updated.getType());

        AgentAppUnit found = agentAppUnitService.findAgentAppUnitById(tenantId, unit.getId());
        Assert.assertEquals("id1-updated", found.getIdentifier());

        agentAppUnitService.deleteAgentAppUnit(tenantId, unit.getId());
        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    private Agent createAgent(String name) {
        Agent agent = new Agent();
        agent.setTenantId(tenantId);
        agent.setName(name);
        return agentService.saveAgent(agent);
    }

    private AgentApplication saveApplication(Agent agent) {
        AgentAppTemplate template = createTemplate();
        AgentApplication app = new AgentApplication();
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.GENERIC);
        app.setTemplateId(template.getId());
        return agentApplicationService.save(tenantId, app);
    }

    private AgentAppTemplate createTemplate() {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(AgentApplicationType.GENERIC);
        template.setCurrentVersion("1.0.0");
        template.setPreviousVersion("0.9.0");
        template.setStartSteps(Collections.emptyList());
        template.setUpgradeSteps(Collections.emptyList());
        return agentAppTemplateService.save(TenantId.SYS_TENANT_ID, template);
    }

    private AgentAppUnit saveUnit(AgentApplication app, String identifier, String type) {
        AgentAppUnit unit = new AgentAppUnit();
        unit.setAgentApplicationId(app.getId());
        unit.setIdentifier(identifier);
        unit.setType(type);
        return agentAppUnitService.saveAgentAppUnit(tenantId, unit);
    }
}
