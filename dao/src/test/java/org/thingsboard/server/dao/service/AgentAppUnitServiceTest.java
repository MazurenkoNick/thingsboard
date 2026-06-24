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
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.logexternal.LogChunkBuffer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppTemplateService;
import org.thingsboard.server.dao.agent.AgentAppUnitService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
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
    @SpyBean
    LogChunkBuffer logChunkBuffer;

    @Test
    public void testSaveAgentAppUnit() {
        Agent agent = createAgent("My agent");
        AgentApplication app = saveApplication(agent);
        AgentAppUnit unit = new AgentAppUnit();
        unit.setAgentApplicationId(app.getId());
        unit.setIdentifier("unit-1");
        unit.setType(AgentAppUnitType.CONTAINER);

        AgentAppUnit saved = agentAppUnitService.saveAgentAppUnit(tenantId, unit);
        Assert.assertNotNull(saved);
        Assert.assertNotNull(saved.getId());
        Assert.assertTrue(saved.getCreatedTime() > 0);
        Assert.assertEquals(app.getId(), saved.getAgentApplicationId());
        Assert.assertEquals("unit-1", saved.getIdentifier());
        Assert.assertEquals(AgentAppUnitType.CONTAINER, saved.getType());

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
        unit.setType(AgentAppUnitType.CONTAINER);
        Assertions.assertThrows(DataValidationException.class, () ->
                agentAppUnitService.saveAgentAppUnit(tenantId, unit));
    }

    @Test
    public void testSaveAgentAppUnitWithNonExistentAgentApplication() {
        AgentAppUnit unit = new AgentAppUnit();
        unit.setAgentApplicationId(new AgentApplicationId(java.util.UUID.randomUUID()));
        unit.setIdentifier("id");
        unit.setType(AgentAppUnitType.CONTAINER);
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
        unit.setType(AgentAppUnitType.CONTAINER);
        Assertions.assertThrows(DataValidationException.class, () ->
                agentAppUnitService.saveAgentAppUnit(tenantId, unit));
        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveAgentAppUnitWithNullType() {
        Agent agent = createAgent("Agent");
        AgentApplication app = saveApplication(agent);
        AgentAppUnit unit = new AgentAppUnit();
        unit.setAgentApplicationId(app.getId());
        unit.setIdentifier("id");
        unit.setType(null);
        Assertions.assertThrows(DataValidationException.class, () ->
                agentAppUnitService.saveAgentAppUnit(tenantId, unit));
        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testFindAgentAppUnitsByAgentAppId() {
        Agent agent = createAgent("Agent");
        AgentApplication app = saveApplication(agent);
        AgentAppUnit u1 = saveUnit(app, "id1", AgentAppUnitType.CONTAINER);
        AgentAppUnit u2 = saveUnit(app, "id2", AgentAppUnitType.VOLUME);

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
        AgentAppUnit unit = saveUnit(app, "toDelete", AgentAppUnitType.CONTAINER);

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
        saveUnit(app, "u1", AgentAppUnitType.CONTAINER);
        saveUnit(app, "u2", AgentAppUnitType.VOLUME);

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
        saveUnit(app, "c1", AgentAppUnitType.CONTAINER);
        saveUnit(app, "c2", AgentAppUnitType.NETWORK);

        List<AgentAppUnit> before = agentAppUnitService.findAgentAppUnitsByAgentAppId(tenantId, app.getId());
        Assert.assertEquals(2, before.size());

        agentApplicationService.delete(tenantId, app.getId());
        List<AgentAppUnit> after = agentAppUnitService.findAgentAppUnitsByAgentAppId(tenantId, app.getId());
        Assert.assertTrue(after.isEmpty());

        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testDeleteAgentAppUnitCallsLogChunkBufferDeleteUnit() {
        Agent agent = createAgent("Agent");
        AgentApplication app = saveApplication(agent);
        AgentAppUnit unit = saveUnit(app, "withLogs", AgentAppUnitType.CONTAINER);
        org.mockito.Mockito.reset(logChunkBuffer);

        agentAppUnitService.deleteAgentAppUnit(tenantId, unit.getId());

        org.mockito.Mockito.verify(logChunkBuffer).deleteUnit(tenantId, unit.getId());

        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testDeleteByAgentApplicationIdCallsLogChunkBufferDeleteUnit() {
        Agent agent = createAgent("Agent");
        AgentApplication app = saveApplication(agent);
        AgentAppUnit u1 = saveUnit(app, "u1", AgentAppUnitType.CONTAINER);
        AgentAppUnit u2 = saveUnit(app, "u2", AgentAppUnitType.CONTAINER);
        org.mockito.Mockito.reset(logChunkBuffer);

        agentAppUnitService.deleteByAgentApplicationId(tenantId, app.getId());

        org.mockito.Mockito.verify(logChunkBuffer).deleteUnit(tenantId, u1.getId());
        org.mockito.Mockito.verify(logChunkBuffer).deleteUnit(tenantId, u2.getId());

        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveAgentAppUnitDoesNotCallLogChunkBufferDeleteUnit() {
        Agent agent = createAgent("Agent");
        AgentApplication app = saveApplication(agent);
        org.mockito.Mockito.reset(logChunkBuffer);

        AgentAppUnit unit = saveUnit(app, "fresh", AgentAppUnitType.CONTAINER);
        unit.setIdentifier("fresh-updated");
        agentAppUnitService.saveAgentAppUnit(tenantId, unit);

        org.mockito.Mockito.verify(logChunkBuffer, org.mockito.Mockito.never())
                .deleteUnit(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        agentAppUnitService.deleteAgentAppUnit(tenantId, unit.getId());
        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testUpdateAgentAppUnit() {
        Agent agent = createAgent("Agent");
        AgentApplication app = saveApplication(agent);
        AgentAppUnit unit = saveUnit(app, "id1", AgentAppUnitType.CONTAINER);

        unit.setIdentifier("id1-updated");
        unit.setType(AgentAppUnitType.VOLUME);
        AgentAppUnit updated = agentAppUnitService.saveAgentAppUnit(tenantId, unit);
        Assert.assertEquals("id1-updated", updated.getIdentifier());
        Assert.assertEquals(AgentAppUnitType.VOLUME, updated.getType());

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
        agent.setRoutingKey(name);
        agent.setSecret(StringUtils.randomAlphanumeric(20));
        return agentService.saveAgent(agent);
    }

    private AgentApplication saveApplication(Agent agent) {
        AgentAppTemplate template = createTemplate();
        AgentApplication app = new AgentApplication();
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.GENERIC);
        app.setTemplateId(template.getId());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(createGenericComposeJson());
        app.setConfig(config);
        return agentApplicationService.save(tenantId, app);
    }

    private com.fasterxml.jackson.databind.JsonNode createGenericComposeJson() {
        ObjectNode service = JacksonUtil.newObjectNode();
        service.put("image", "nginx:alpine");
        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("generic", service);
        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);
        return compose;
    }

    private AgentAppTemplate createTemplate() {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(AgentApplicationType.GENERIC);
        template.setCurrentVersion("1.0.0");
        template.setNextVersion(null);
        template.setStartSteps(java.util.Collections.emptyList());
        template.setUpgradeSteps(java.util.Collections.emptyList());
        return agentAppTemplateService.save(TenantId.SYS_TENANT_ID, template);
    }

    private AgentAppUnit saveUnit(AgentApplication app, String identifier, AgentAppUnitType type) {
        AgentAppUnit unit = new AgentAppUnit();
        unit.setAgentApplicationId(app.getId());
        unit.setIdentifier(identifier);
        unit.setType(type);
        return agentAppUnitService.saveAgentAppUnit(tenantId, unit);
    }
}
