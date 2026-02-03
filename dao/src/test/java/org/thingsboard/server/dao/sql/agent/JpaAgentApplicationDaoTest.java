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
import org.thingsboard.server.common.data.agent.AgentAppConfig;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.agent.AgentDao;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        AgentApplication app = new AgentApplication();
        app.setAgentId(new AgentId(agentId1));
        app.setType(AgentApplicationType.GENERIC);
        app.setTemplateVersion("1.0");
        app.setPlaceholders(Collections.emptyMap());
        Map<String, AgentAppConfig> config = Map.of("queue_type", new AgentAppConfig(false, Collections.singletonList("IN_MEMORY")));
        app.setConfiguration(config);
        app.setSteps(Collections.emptyList());

        AgentApplication saved = agentApplicationDao.save(TenantId.fromUUID(tenantId1), app);
        assertNotNull(saved.getId());

        AgentApplication found = agentApplicationDao.findById(TenantId.fromUUID(tenantId1), saved.getId().getId());
        assertNotNull(found);
        assertEquals(saved.getId(), found.getId());
        assertEquals(agentId1, found.getAgentId().getId());
        assertEquals(config, found.getConfiguration());

        List<AgentApplication> byAgent = agentApplicationDao.findByAgentId(TenantId.fromUUID(tenantId1), agentId1);
        assertEquals(1, byAgent.size());
        assertEquals(saved.getId(), byAgent.get(0).getId());
        assertEquals(config, byAgent.get(0).getConfiguration());

        agentApplicationDao.removeById(TenantId.fromUUID(tenantId1), saved.getId().getId());
    }

    @Test
    public void testRemoveById() {
        AgentApplication app = saveApplication(AgentApplicationType.EDGE, "v1");
        agentApplicationDao.removeById(TenantId.fromUUID(tenantId1), app.getId().getId());
        AgentApplication found = agentApplicationDao.findById(TenantId.fromUUID(tenantId1), app.getId().getId());
        assertNull(found);
    }

    @Test
    public void testRemoveByAgentId() {
        saveApplication(AgentApplicationType.GENERIC, "a1");
        saveApplication(AgentApplicationType.GATEWAY, "a2");
        List<AgentApplication> before = agentApplicationDao.findByAgentId(TenantId.fromUUID(tenantId1), agentId1);
        assertEquals(2, before.size());

        agentApplicationDao.removeByAgentId(TenantId.fromUUID(tenantId1), agentId1);
        List<AgentApplication> after = agentApplicationDao.findByAgentId(TenantId.fromUUID(tenantId1), agentId1);
        assertTrue(after.isEmpty());
    }

    @Test
    public void testDeleteAgentRemovesAgentApplications() {
        saveApplication(AgentApplicationType.GENERIC, "cascade1");
        saveApplication(AgentApplicationType.EDGE, "cascade2");
        List<AgentApplication> before = agentApplicationDao.findByAgentId(TenantId.fromUUID(tenantId1), agentId1);
        assertEquals(2, before.size());

        agentDao.removeById(TenantId.fromUUID(tenantId1), agentId1);
        agent1 = null;

        List<AgentApplication> after = agentApplicationDao.findByAgentId(TenantId.fromUUID(tenantId1), agentId1);
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

    private AgentApplication saveApplication(AgentApplicationType type, String templateVersion) {
        AgentApplication app = new AgentApplication();
        app.setAgentId(new AgentId(agentId1));
        app.setType(type);
        app.setTemplateVersion(templateVersion);
        app.setPlaceholders(Collections.emptyMap());
        app.setConfiguration(Collections.emptyMap());
        app.setSteps(Collections.emptyList());
        return agentApplicationDao.save(TenantId.fromUUID(tenantId1), app);
    }
}
