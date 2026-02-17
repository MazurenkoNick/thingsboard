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
package org.thingsboard.server.dao.sql.agent;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventDeliveryState;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.agent.AgentAppEventDao;
import org.thingsboard.server.dao.agent.AgentAppTemplateDao;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.agent.AgentDao;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JpaAgentApplicationDaoTest extends AbstractJpaDaoTest {

    UUID tenantId1;
    UUID agentId1;
    Agent agent1;

    @Autowired
    private AgentDao agentDao;
    @Autowired
    private AgentApplicationDao agentApplicationDao;
    @Autowired
    private AgentAppTemplateDao agentAppTemplateDao;
    @Autowired
    private AgentAppEventDao agentAppEventDao;

    @Before
    public void setUp() {
        tenantId1 = Uuids.timeBased();
        agentId1 = Uuids.timeBased();
        agent1 = saveAgent(agentId1, tenantId1, Uuids.timeBased(), "AGENT_APP_TEST");
    }

    @After
    public void tearDown() {
        if (agent1 != null) {
            List<AgentApplication> apps = agentApplicationDao.findByAgentId(TenantId.fromUUID(tenantId1), agentId1);
            for (AgentApplication app : apps) {
                agentApplicationDao.removeById(TenantId.fromUUID(tenantId1), app.getId().getId());
            }
            agentDao.removeById(TenantId.fromUUID(tenantId1), agentId1);
        }
    }

    @Test
    public void testSaveFindByIdFindByAgentId() {
        AgentAppTemplate template = saveTemplate();
        AgentApplication app = new AgentApplication();
        app.setAgentId(new AgentId(agentId1));
        app.setAppType(AgentApplicationType.EDGE);
        app.setTemplateId(template.getId());
        app.setStartSteps(Collections.emptyList());
        app.setUpdateSteps(Collections.emptyList());

        AgentApplication saved = agentApplicationDao.save(TenantId.fromUUID(tenantId1), app);
        assertNotNull(saved.getId());

        AgentApplication found = agentApplicationDao.findById(TenantId.fromUUID(tenantId1), saved.getId().getId());
        assertNotNull(found);
        assertEquals(saved.getId(), found.getId());
        assertEquals(agentId1, found.getAgentId().getId());
        assertEquals(template.getId(), found.getTemplateId());

        List<AgentApplication> byAgent = agentApplicationDao.findByAgentId(TenantId.fromUUID(tenantId1), agentId1);
        assertEquals(1, byAgent.size());
        assertEquals(saved.getId(), byAgent.get(0).getId());
        assertEquals(template.getId(), byAgent.get(0).getTemplateId());

        agentApplicationDao.removeById(TenantId.fromUUID(tenantId1), saved.getId().getId());
    }

    @Test
    public void testRemoveById() {
        AgentApplication app = saveApplication("v1");
        agentApplicationDao.removeById(TenantId.fromUUID(tenantId1), app.getId().getId());
        AgentApplication found = agentApplicationDao.findById(TenantId.fromUUID(tenantId1), app.getId().getId());
        assertNull(found);
    }

    @Test
    public void testRemoveByAgentId() {
        saveApplication("a1");
        saveApplication("a2");
        List<AgentApplication> before = agentApplicationDao.findByAgentId(TenantId.fromUUID(tenantId1), agentId1);
        assertEquals(2, before.size());

        agentApplicationDao.removeByAgentId(TenantId.fromUUID(tenantId1), agentId1);
        List<AgentApplication> after = agentApplicationDao.findByAgentId(TenantId.fromUUID(tenantId1), agentId1);
        assertTrue(after.isEmpty());
    }

    @Test
    public void testFindByTemplateId() {
        AgentAppTemplate template = saveTemplate();
        AgentApplication app1 = saveApplicationWithTemplate("t1", template);
        AgentApplication app2 = saveApplicationWithTemplate("t2", template);
        // app with a different template – must not appear in the results
        saveApplication("other");

        List<AgentApplication> byTemplate = agentApplicationDao.findByTemplateId(TenantId.fromUUID(tenantId1), template.getId().getId());
        assertEquals(2, byTemplate.size());
        assertTrue(byTemplate.stream().anyMatch(a -> a.getId().equals(app1.getId())));
        assertTrue(byTemplate.stream().anyMatch(a -> a.getId().equals(app2.getId())));
    }

    @Test
    public void testRemoveByTemplateId() {
        AgentAppTemplate template = saveTemplate();
        saveApplicationWithTemplate("r1", template);
        saveApplicationWithTemplate("r2", template);
        AgentApplication other = saveApplication("keep");

        List<AgentApplication> before = agentApplicationDao.findByTemplateId(TenantId.fromUUID(tenantId1), template.getId().getId());
        assertEquals(2, before.size());

        agentApplicationDao.removeByTemplateId(TenantId.fromUUID(tenantId1), template.getId().getId());

        List<AgentApplication> after = agentApplicationDao.findByTemplateId(TenantId.fromUUID(tenantId1), template.getId().getId());
        assertTrue(after.isEmpty());

        // the application linked to a different template must survive
        AgentApplication surviving = agentApplicationDao.findById(TenantId.fromUUID(tenantId1), other.getId().getId());
        assertNotNull(surviving);
    }

    @Test
    public void testDeleteAgentRemovesAgentApplications() {
        saveApplication("cascade1");
        saveApplication("cascade2");
        List<AgentApplication> before = agentApplicationDao.findByAgentId(TenantId.fromUUID(tenantId1), agentId1);
        assertEquals(2, before.size());

        agentDao.removeById(TenantId.fromUUID(tenantId1), agentId1);
        agent1 = null;

        List<AgentApplication> after = agentApplicationDao.findByAgentId(TenantId.fromUUID(tenantId1), agentId1);
        assertTrue(after.isEmpty());
    }

    @Test
    public void testFindByEventId() {
        AgentApplication app = saveApplication("eventApp");
        TenantId tid = TenantId.fromUUID(tenantId1);

        AgentAppEvent event = new AgentAppEvent();
        event.setTenantId(tid);
        event.setApplicationId(app.getId());
        event.setActionType(AgentAppEventActionType.INSTALL);
        event.setDeliveryState(AgentAppEventDeliveryState.PENDING);
        event.setStatus(AgentAppEventStatus.PENDING);
        event.setUpdatedTime(System.currentTimeMillis());
        AgentAppEvent savedEvent = agentAppEventDao.save(tid, event);

        AgentApplication found = agentApplicationDao.findByEventId(tid, savedEvent.getId().getId());
        assertNotNull(found);
        assertEquals(app.getId(), found.getId());

        // non-existent event id
        AgentApplication notFound = agentApplicationDao.findByEventId(tid, Uuids.timeBased());
        assertNull(notFound);
    }

    private Agent saveAgent(UUID id, UUID tenantId, UUID customerId, String name) {
        Agent agent = new Agent();
        agent.setId(new AgentId(id));
        agent.setTenantId(TenantId.fromUUID(tenantId));
        agent.setCustomerId(new CustomerId(customerId));
        agent.setName(name);
        return agentDao.save(TenantId.fromUUID(tenantId), agent);
    }

    private AgentApplication saveApplication(String name) {
        return saveApplicationWithTemplate(name, saveTemplate());
    }

    private AgentApplication saveApplicationWithTemplate(String name, AgentAppTemplate template) {
        AgentApplication app = new AgentApplication();
        app.setAgentId(new AgentId(agentId1));
        app.setAppType(AgentApplicationType.EDGE);
        app.setName(name);
        app.setTemplateId(template.getId());
        app.setStartSteps(Collections.emptyList());
        app.setUpdateSteps(Collections.emptyList());
        return agentApplicationDao.save(TenantId.fromUUID(tenantId1), app);
    }

    private AgentAppTemplate saveTemplate() {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(AgentApplicationType.GENERIC);
        template.setCurrentVersion("1.0.0");
        template.setPreviousVersion("0.9.0");
        template.setNextVersion(null);
        template.setStartSteps(Collections.emptyList());
        template.setUpgradeSteps(Collections.emptyList());
        return agentAppTemplateDao.save(TenantId.SYS_TENANT_ID, template);
    }
}
