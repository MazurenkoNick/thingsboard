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
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.agent.AgentAppUnitDao;
import org.thingsboard.server.dao.agent.AgentDao;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JpaAgentAppUnitDaoTest extends AbstractJpaDaoTest {

    UUID tenantId1;
    UUID agentId1;
    UUID applicationId1;
    Agent agent1;
    AgentApplication application1;

    @Autowired
    private AgentDao agentDao;
    @Autowired
    private AgentApplicationDao agentApplicationDao;
    @Autowired
    private AgentAppUnitDao agentAppUnitDao;

    @Before
    public void setUp() {
        tenantId1 = Uuids.timeBased();
        agentId1 = Uuids.timeBased();
        agent1 = saveAgent(agentId1, tenantId1, Uuids.timeBased(), "AGENT_APP_UNIT_TEST");
        application1 = saveApplication("APP_UNIT_TEST");
        applicationId1 = application1.getId().getId();
    }

    @After
    public void tearDown() {
        if (application1 != null) {
            List<AgentAppUnit> units = agentAppUnitDao.findByAgentApplicationId(TenantId.fromUUID(tenantId1), applicationId1);
            for (AgentAppUnit unit : units) {
                agentAppUnitDao.removeById(TenantId.fromUUID(tenantId1), unit.getId().getId());
            }
            agentApplicationDao.removeById(TenantId.fromUUID(tenantId1), applicationId1);
        }
        if (agent1 != null) {
            agentDao.removeById(TenantId.fromUUID(tenantId1), agentId1);
        }
    }

    @Test
    public void testSaveFindByIdFindByAgentApplicationId() {
        AgentAppUnit unit = new AgentAppUnit();
        unit.setAgentApplicationId(new AgentApplicationId(applicationId1));
        unit.setIdentifier("unit-1");
        unit.setType("container");

        AgentAppUnit saved = agentAppUnitDao.save(TenantId.fromUUID(tenantId1), unit);
        assertNotNull(saved.getId());

        AgentAppUnit found = agentAppUnitDao.findById(TenantId.fromUUID(tenantId1), saved.getId().getId());
        assertNotNull(found);
        assertEquals(saved.getId(), found.getId());
        assertEquals(applicationId1, found.getAgentApplicationId().getId());
        assertEquals("unit-1", found.getIdentifier());
        assertEquals("container", found.getType());

        List<AgentAppUnit> byApp = agentAppUnitDao.findByAgentApplicationId(TenantId.fromUUID(tenantId1), applicationId1);
        assertEquals(1, byApp.size());
        assertEquals(saved.getId(), byApp.get(0).getId());

        agentAppUnitDao.removeById(TenantId.fromUUID(tenantId1), saved.getId().getId());
    }

    @Test
    public void testRemoveById() {
        AgentAppUnit unit = saveUnit("unit-2", "process");
        agentAppUnitDao.removeById(TenantId.fromUUID(tenantId1), unit.getId().getId());
        AgentAppUnit found = agentAppUnitDao.findById(TenantId.fromUUID(tenantId1), unit.getId().getId());
        assertNull(found);
    }

    @Test
    public void testRemoveByAgentApplicationId() {
        saveUnit("u1", "type1");
        saveUnit("u2", "type2");
        List<AgentAppUnit> before = agentAppUnitDao.findByAgentApplicationId(TenantId.fromUUID(tenantId1), applicationId1);
        assertEquals(2, before.size());

        agentAppUnitDao.removeByAgentApplicationId(TenantId.fromUUID(tenantId1), applicationId1);
        List<AgentAppUnit> after = agentAppUnitDao.findByAgentApplicationId(TenantId.fromUUID(tenantId1), applicationId1);
        assertTrue(after.isEmpty());
    }

    @Test
    public void testRemoveAgentApplicationRemovesAgentAppUnits() {
        saveUnit("cascade1", "t1");
        saveUnit("cascade2", "t2");
        List<AgentAppUnit> before = agentAppUnitDao.findByAgentApplicationId(TenantId.fromUUID(tenantId1), applicationId1);
        assertEquals(2, before.size());

        agentApplicationDao.removeById(TenantId.fromUUID(tenantId1), applicationId1);
        application1 = null;

        List<AgentAppUnit> after = agentAppUnitDao.findByAgentApplicationId(TenantId.fromUUID(tenantId1), applicationId1);
        assertTrue(after.isEmpty());
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
        AgentApplication app = new AgentApplication();
        app.setName(name);
        app.setAgentId(new AgentId(agentId1));
        app.setType(AgentApplicationType.GENERIC);
        app.setPlaceholders(Collections.emptyMap());
        app.setConfiguration(Collections.emptyMap());
        app.setSteps(Collections.emptyList());
        return agentApplicationDao.save(TenantId.fromUUID(tenantId1), app);
    }

    private AgentAppUnit saveUnit(String identifier, String type) {
        AgentAppUnit unit = new AgentAppUnit();
        unit.setAgentApplicationId(new AgentApplicationId(applicationId1));
        unit.setIdentifier(identifier);
        unit.setType(type);
        return agentAppUnitDao.save(TenantId.fromUUID(tenantId1), unit);
    }
}
