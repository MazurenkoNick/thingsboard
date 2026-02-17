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
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.agent.AgentAppTemplateDao;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.agent.AgentDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class JpaAgentDaoTest extends AbstractJpaDaoTest {

    UUID tenantId1;
    UUID tenantId2;
    UUID customerId1;
    UUID customerId2;
    List<Agent> agents = new ArrayList<>();
    
    @Autowired
    private AgentDao agentDao;
    @Autowired
    private AgentApplicationDao agentApplicationDao;
    @Autowired
    private AgentAppTemplateDao agentAppTemplateDao;

    @Before
    public void setUp() {
        tenantId1 = Uuids.timeBased();
        tenantId2 = Uuids.timeBased();
        customerId1 = Uuids.timeBased();
        customerId2 = Uuids.timeBased();
        for (int i = 0; i < 60; i++) {
            UUID agentId = Uuids.timeBased();
            UUID tenantId = i % 2 == 0 ? tenantId1 : tenantId2;
            UUID customerId = i % 2 == 0 ? customerId1 : customerId2;
            agents.add(saveAgent(agentId, tenantId, customerId, "AGENT_" + i));
        }
        assertEquals(agents.size(), agentDao.find(TenantId.fromUUID(tenantId1)).size());
    }

    @After
    public void tearDown() {
        for (Agent agent : agents) {
            agentDao.removeById(agent.getTenantId(), agent.getUuidId());
        }
        agents.clear();
    }

    @Test
    public void testSaveAgentName0x00_thenSomeDatabaseException() {
        assertThatThrownBy(() -> agents.add(
                saveAgent(UUID.randomUUID(), tenantId2, customerId2, "F0929906\000\000\000\000\000\000\000\000\000")));
    }

    @Test
    public void testFindAgentsByTenantId() {
        PageLink pageLink = new PageLink(20, 0, "AGENT_");
        PageData<Agent> agents1 = agentDao.findAgentsByTenantId(tenantId1, pageLink);
        assertEquals(20, agents1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Agent> agents2 = agentDao.findAgentsByTenantId(tenantId1, pageLink);
        assertEquals(10, agents2.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Agent> agents3 = agentDao.findAgentsByTenantId(tenantId1, pageLink);
        assertEquals(0, agents3.getData().size());
    }

    @Test
    public void testFindAgentInfosByTenantId() {
        PageLink pageLink = new PageLink(20, 0, "AGENT_");
        PageData<AgentInfo> agents1 = agentDao.findAgentInfosByTenantId(tenantId1, pageLink);
        assertEquals(20, agents1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<AgentInfo> agents2 = agentDao.findAgentInfosByTenantId(tenantId1, pageLink);
        assertEquals(10, agents2.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<AgentInfo> agents3 = agentDao.findAgentInfosByTenantId(tenantId1, pageLink);
        assertEquals(0, agents3.getData().size());
    }

    @Test
    public void testFindAgentsByTenantIdAndCustomerId() {
        PageLink pageLink = new PageLink(20, 0, "AGENT_");
        PageData<Agent> agents1 = agentDao.findAgentsByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(20, agents1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Agent> agents2 = agentDao.findAgentsByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(10, agents2.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Agent> agents3 = agentDao.findAgentsByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(0, agents3.getData().size());
    }

    @Test
    public void testFindAgentInfosByTenantIdAndCustomerId() {
        PageLink pageLink = new PageLink(20, 0, "AGENT_");
        PageData<AgentInfo> agents1 = agentDao.findAgentInfosByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(20, agents1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<AgentInfo> agents2 = agentDao.findAgentInfosByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(10, agents2.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<AgentInfo> agents3 = agentDao.findAgentInfosByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(0, agents3.getData().size());
    }

    @Test
    public void testFindAgentInfoById() {
        UUID agentId = Uuids.timeBased();
        String name = "TEST_AGENT";
        agents.add(saveAgent(agentId, tenantId1, customerId1, name));

        AgentInfo agentInfo = agentDao.findAgentInfoById(TenantId.fromUUID(tenantId1), agentId);
        assertNotNull(agentInfo);
        assertEquals(agentId, agentInfo.getId().getId());
        assertEquals(name, agentInfo.getName());
    }

    @Test
    public void testCountByTenantId() {
        Long count = agentDao.countByTenantId(TenantId.fromUUID(tenantId1));
        assertEquals(30, count.longValue());

        Long count2 = agentDao.countByTenantId(TenantId.fromUUID(tenantId2));
        assertEquals(30, count2.longValue());
    }

    @Test
    public void testFindAgentsByTenantIdWithEmptyTextSearch() {
        PageLink pageLink = new PageLink(20, 0, null);
        PageData<Agent> agents1 = agentDao.findAgentsByTenantId(tenantId1, pageLink);
        assertEquals(20, agents1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Agent> agents2 = agentDao.findAgentsByTenantId(tenantId1, pageLink);
        assertEquals(10, agents2.getData().size());
    }

    @Test
    public void testFindAgentsByTenantIdAndCustomerIdWithEmptyTextSearch() {
        PageLink pageLink = new PageLink(20, 0, null);
        PageData<Agent> agents1 = agentDao.findAgentsByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(20, agents1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Agent> agents2 = agentDao.findAgentsByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(10, agents2.getData().size());
    }

    @Test
    public void testFindAgentsByTenantIdWithPartialTextSearch() {
        PageLink pageLink = new PageLink(10, 0, "AGENT_1");
        PageData<Agent> agents1 = agentDao.findAgentsByTenantId(tenantId1, pageLink);
        // Should match AGENT_10, AGENT_12, AGENT_14, AGENT_16, AGENT_18 (5 agents with even indices)
        assertEquals(5, agents1.getData().size());
    }

    @Test
    public void testFindAgentInfosByTenantIdWithPartialTextSearch() {
        PageLink pageLink = new PageLink(10, 0, "AGENT_2");
        PageData<AgentInfo> agents1 = agentDao.findAgentInfosByTenantId(tenantId1, pageLink);
        // Should match AGENT_2, AGENT_20, AGENT_22, AGENT_24, AGENT_26, AGENT_28 (6 agents with even indices)
        assertEquals(6, agents1.getData().size());
    }

    @Test
    public void testDeleteAgentRemovesAgentApplications() {
        UUID agentId = Uuids.timeBased();
        Agent agent = saveAgent(agentId, tenantId1, customerId1, "AGENT_FOR_APPS");
        agents.add(agent);
        AgentAppTemplate template = saveTemplate();

        AgentApplication app1 = new AgentApplication();
        app1.setAgentId(new AgentId(agentId));
        app1.setAppType(AgentApplicationType.EDGE);
        app1.setTemplateId(template.getId());
        app1.setStartSteps(Collections.emptyList());
        app1.setUpdateSteps(Collections.emptyList());
        agentApplicationDao.save(TenantId.fromUUID(tenantId1), app1);

        AgentApplication app2 = new AgentApplication();
        app2.setAgentId(new AgentId(agentId));
        app2.setAppType(AgentApplicationType.EDGE);
        app2.setTemplateId(template.getId());
        app2.setStartSteps(Collections.emptyList());
        app2.setUpdateSteps(Collections.emptyList());
        agentApplicationDao.save(TenantId.fromUUID(tenantId1), app2);

        List<AgentApplication> before = agentApplicationDao.findByAgentId(TenantId.fromUUID(tenantId1), agentId);
        assertEquals(2, before.size());

        agentDao.removeById(TenantId.fromUUID(tenantId1), agentId);
        agents.remove(agent);

        List<AgentApplication> after = agentApplicationDao.findByAgentId(TenantId.fromUUID(tenantId1), agentId);
        assertEquals(0, after.size());
    }

    @Test
    public void testFindByRoutingKey() {
        UUID agentId = Uuids.timeBased();
        String routingKey = "routing-key-" + agentId;
        Agent agent = saveAgent(agentId, tenantId1, customerId1, "ROUTING_KEY_AGENT", routingKey, "test-secret");
        agents.add(agent);

        Agent found = agentDao.findByRoutingKey(tenantId1, routingKey);
        assertNotNull(found);
        assertEquals(agent.getId(), found.getId());
        assertEquals(routingKey, found.getRoutingKey());
        assertEquals("test-secret", found.getSecret());

        Agent notFound = agentDao.findByRoutingKey(tenantId1, "non-existent-key");
        assertNull(notFound);
    }

    private Agent saveAgent(UUID id, UUID tenantId, UUID customerId, String name) {
        return saveAgent(id, tenantId, customerId, name, UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    private Agent saveAgent(UUID id, UUID tenantId, UUID customerId, String name, String routingKey, String secret) {
        Agent agent = new Agent();
        agent.setId(new AgentId(id));
        agent.setTenantId(TenantId.fromUUID(tenantId));
        agent.setCustomerId(new CustomerId(customerId));
        agent.setName(name);
        agent.setRoutingKey(routingKey);
        agent.setSecret(secret);
        return agentDao.save(TenantId.fromUUID(tenantId), agent);
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
