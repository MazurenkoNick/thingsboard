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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.step.InfoStep;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@DaoSqlTest
public class AgentServiceTest extends AbstractServiceTest {

    @Autowired
    AgentService agentService;
    @Autowired
    AgentApplicationService agentApplicationService;
    @Autowired
    CustomerService customerService;

    private IdComparator<Agent> idComparator = new IdComparator<>();

    @Test
    public void testSaveAgent() {
        Agent agent = new Agent();
        agent.setTenantId(tenantId);
        agent.setName("My agent");
        Agent savedAgent = agentService.saveAgent(agent);

        Assert.assertNotNull(savedAgent);
        Assert.assertNotNull(savedAgent.getId());
        Assert.assertTrue(savedAgent.getCreatedTime() > 0);
        Assert.assertEquals(agent.getTenantId(), savedAgent.getTenantId());
        Assert.assertNotNull(savedAgent.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedAgent.getCustomerId().getId());
        Assert.assertEquals(agent.getName(), savedAgent.getName());

        savedAgent.setName("My new agent");

        agentService.saveAgent(savedAgent);
        Agent foundAgent = agentService.findAgentById(tenantId, savedAgent.getId());
        Assert.assertEquals(foundAgent.getName(), savedAgent.getName());

        agentService.deleteAgent(tenantId, savedAgent.getId());
    }

    @Test
    public void testSaveAgentWithEmptyTenant() {
        Agent agent = new Agent();
        agent.setName("My agent");
        Assertions.assertThrows(DataValidationException.class, () -> {
            agentService.saveAgent(agent);
        });
    }

    @Test
    public void testSaveAgentWithInvalidTenant() {
        Agent agent = new Agent();
        agent.setName("My agent");
        agent.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        Assertions.assertThrows(DataValidationException.class, () -> {
            agentService.saveAgent(agent);
        });
    }

    @Test
    public void testSaveAgentWithEmptyName() {
        Agent agent = new Agent();
        agent.setTenantId(tenantId);
        Assertions.assertThrows(DataValidationException.class, () -> {
            agentService.saveAgent(agent);
        });
    }

    @Test
    public void testSaveAgentWithInvalidName() {
        Agent agent = new Agent();
        agent.setTenantId(tenantId);
        agent.setName(RandomStringUtils.randomAlphabetic(300));
        Assertions.assertThrows(DataValidationException.class, () -> {
            agentService.saveAgent(agent);
        });
    }

    @Test
    public void testSaveAgentWithSameName() {
        Agent agent = new Agent();
        agent.setTenantId(tenantId);
        agent.setName("My agent");
        Agent savedAgent = agentService.saveAgent(agent);

        Agent agent2 = new Agent();
        agent2.setTenantId(tenantId);
        agent2.setName("My agent");
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                agentService.saveAgent(agent2);
            });
        } finally {
            agentService.deleteAgent(tenantId, savedAgent.getId());
        }
    }

    @Test
    public void testFindAgentById() {
        Agent agent = new Agent();
        agent.setTenantId(tenantId);
        agent.setName("My agent");
        Agent savedAgent = agentService.saveAgent(agent);
        Agent foundAgent = agentService.findAgentById(tenantId, savedAgent.getId());
        Assert.assertNotNull(foundAgent);
        Assert.assertEquals(savedAgent, foundAgent);
        agentService.deleteAgent(tenantId, savedAgent.getId());
    }

    @Test
    public void testFindAgentInfoById() {
        Agent agent = new Agent();
        agent.setTenantId(tenantId);
        agent.setName("My agent");
        Agent savedAgent = agentService.saveAgent(agent);
        AgentInfo foundAgentInfo = agentService.findAgentInfoById(tenantId, savedAgent.getId());
        Assert.assertNotNull(foundAgentInfo);
        Assert.assertEquals(savedAgent.getId(), foundAgentInfo.getId());
        Assert.assertEquals(savedAgent.getName(), foundAgentInfo.getName());
        agentService.deleteAgent(tenantId, savedAgent.getId());
    }

    @Test
    public void testDeleteAgent() {
        Agent agent = new Agent();
        agent.setTenantId(tenantId);
        agent.setName("My agent");
        Agent savedAgent = agentService.saveAgent(agent);
        Agent foundAgent = agentService.findAgentById(tenantId, savedAgent.getId());
        Assert.assertNotNull(foundAgent);
        agentService.deleteAgent(tenantId, savedAgent.getId());
        foundAgent = agentService.findAgentById(tenantId, savedAgent.getId());
        Assert.assertNull(foundAgent);
    }

    @Test
    public void testFindAgentsByTenantId() {
        List<Agent> agents = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Agent agent = new Agent();
            agent.setTenantId(tenantId);
            agent.setName("Agent" + i);
            agents.add(agentService.saveAgent(agent));
        }

        List<Agent> loadedAgents = new ArrayList<>();
        PageLink pageLink = new PageLink(3);
        PageData<Agent> pageData = null;
        do {
            pageData = agentService.findAgentsByTenantId(tenantId, pageLink);
            loadedAgents.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(agents, idComparator);
        Collections.sort(loadedAgents, idComparator);

        Assert.assertEquals(agents, loadedAgents);

        agentService.deleteByTenantId(tenantId);

        pageLink = new PageLink(4);
        pageData = agentService.findAgentsByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    @Test
    public void testFindAgentsByTenantIdAndName() {
        String title1 = "Agent title 1";
        List<AgentInfo> agentsTitle1 = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            Agent agent = new Agent();
            agent.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            agent.setName(name);
            Agent savedAgent = agentService.saveAgent(agent);
            agentsTitle1.add(new AgentInfo(savedAgent, null, false));
        }
        String title2 = "Agent title 2";
        List<AgentInfo> agentsTitle2 = new ArrayList<>();
        for (int i = 0; i < 17; i++) {
            Agent agent = new Agent();
            agent.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            agent.setName(name);
            Agent savedAgent = agentService.saveAgent(agent);
            agentsTitle2.add(new AgentInfo(savedAgent, null, false));
        }

        List<AgentInfo> loadedAgentsTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(3, 0, title1);
        PageData<AgentInfo> pageData = null;
        do {
            pageData = agentService.findAgentInfosByTenantId(tenantId, pageLink);
            loadedAgentsTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(agentsTitle1, idComparator);
        Collections.sort(loadedAgentsTitle1, idComparator);

        Assert.assertEquals(agentsTitle1, loadedAgentsTitle1);

        List<AgentInfo> loadedAgentsTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = agentService.findAgentInfosByTenantId(tenantId, pageLink);
            loadedAgentsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(agentsTitle2, idComparator);
        Collections.sort(loadedAgentsTitle2, idComparator);

        Assert.assertEquals(agentsTitle2, loadedAgentsTitle2);

        for (Agent agent : loadedAgentsTitle1) {
            agentService.deleteAgent(tenantId, agent.getId());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = agentService.findAgentInfosByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Agent agent : loadedAgentsTitle2) {
            agentService.deleteAgent(tenantId, agent.getId());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = agentService.findAgentInfosByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindAgentsByTenantIdAndCustomerId() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("Test customer");
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        List<Agent> agents = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Agent agent = new Agent();
            agent.setTenantId(tenantId);
            agent.setName("Agent" + i);
            agent = agentService.saveAgent(agent);
            agent.setCustomerId(customerId);
            agents.add(agentService.saveAgent(agent));
        }

        List<Agent> loadedAgents = new ArrayList<>();
        PageLink pageLink = new PageLink(3);
        PageData<Agent> pageData = null;
        do {
            pageData = agentService.findAgentsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedAgents.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(agents, idComparator);
        Collections.sort(loadedAgents, idComparator);

        Assert.assertEquals(agents, loadedAgents);

        agentService.unassignCustomerAgents(tenantId, customerId);

        pageLink = new PageLink(4);
        pageData = agentService.findAgentsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindAgentInfosByTenantIdAndCustomerId() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("Test customer");
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        List<AgentInfo> agentInfos = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Agent agent = new Agent();
            agent.setTenantId(tenantId);
            agent.setName("Agent" + i);
            agent = agentService.saveAgent(agent);
            agent.setCustomerId(customerId);
            agent = agentService.saveAgent(agent);
            agentInfos.add(new AgentInfo(agent, customer.getTitle(), false));
        }

        List<AgentInfo> loadedAgentInfos = new ArrayList<>();
        PageLink pageLink = new PageLink(3);
        PageData<AgentInfo> pageData = null;
        do {
            pageData = agentService.findAgentInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedAgentInfos.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(agentInfos, idComparator);
        Collections.sort(loadedAgentInfos, idComparator);

        Assert.assertEquals(agentInfos.size(), loadedAgentInfos.size());

        for (int i = 0; i < agentInfos.size(); i++) {
            Assert.assertEquals(agentInfos.get(i).getId(), loadedAgentInfos.get(i).getId());
            Assert.assertEquals(agentInfos.get(i).getName(), loadedAgentInfos.get(i).getName());
            Assert.assertEquals(agentInfos.get(i).getCustomerTitle(), loadedAgentInfos.get(i).getCustomerTitle());
        }

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testUnassignAgentFromCustomer() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("Test customer");
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        Agent agent = new Agent();
        agent.setTenantId(tenantId);
        agent.setName("My agent");
        agent = agentService.saveAgent(agent);
        agent.setCustomerId(customerId);
        agent = agentService.saveAgent(agent);

        Agent foundAgent = agentService.findAgentById(tenantId, agent.getId());
        Assert.assertNotNull(foundAgent);
        Assert.assertEquals(customerId, foundAgent.getCustomerId());

        Agent unassignedAgent = agentService.unassignAgentFromCustomer(tenantId, agent.getId());
        Assert.assertNotNull(unassignedAgent);
        CustomerId nullCustomerId = new CustomerId(CustomerId.NULL_UUID);
        Assertions.assertEquals(nullCustomerId, unassignedAgent.getCustomerId());

        foundAgent = agentService.findAgentById(tenantId, agent.getId());
        Assertions.assertEquals(nullCustomerId, foundAgent.getCustomerId());

        agentService.deleteAgent(tenantId, agent.getId());
        customerService.deleteCustomer(tenantId, customerId);
    }

    @Test
    public void testDeleteAgentRemovesAgentApplications() throws Exception {
        Agent agent = new Agent();
        agent.setTenantId(tenantId);
        agent.setName("Agent with applications");
        Agent savedAgent = agentService.saveAgent(agent);

        AgentApplication app1 = new AgentApplication();
        app1.setAgentId(savedAgent.getId());
        app1.setAppType(AgentApplicationType.GENERIC);
        app1.setStartSteps(new ArrayList<>(List.of(new InfoStep(UUID.randomUUID(), null, "step", false))));
        app1 = agentApplicationService.save(tenantId, app1);

        AgentApplication app2 = new AgentApplication();
        app2.setAgentId(savedAgent.getId());
        app2.setAppType(AgentApplicationType.GENERIC);
        app2.setStartSteps(new ArrayList<>(List.of(new InfoStep(UUID.randomUUID(), null, "step", false))));
        app2 = agentApplicationService.save(tenantId, app2);

        List<AgentApplication> applicationsBefore = agentApplicationService.findByAgentId(tenantId, savedAgent.getId(), new PageLink(100)).getData();
        Assert.assertEquals(2, applicationsBefore.size());

        agentService.deleteAgent(tenantId, savedAgent.getId());

        List<AgentApplication> applicationsAfter = agentApplicationService.findByAgentId(tenantId, savedAgent.getId(), new PageLink(100)).getData();
        Assert.assertTrue(applicationsAfter.isEmpty());
        Assert.assertNull(agentApplicationService.findById(tenantId, app1.getId()));
        Assert.assertNull(agentApplicationService.findById(tenantId, app2.getId()));
    }

    @Test
    public void testAssignUnassignAgentApplicationsStillRetrieved() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("Test customer for apps");
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        Agent agent = new Agent();
        agent.setTenantId(tenantId);
        agent.setName("Agent assign unassign");
        agent = agentService.saveAgent(agent);

        AgentApplication app = new AgentApplication();
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.GENERIC);
        app.setStartSteps(new ArrayList<>(List.of(new InfoStep(UUID.randomUUID(), null, "step", false))));
        app = agentApplicationService.save(tenantId, app);

        List<AgentApplication> afterCreate = agentApplicationService.findByAgentId(tenantId, agent.getId(), new PageLink(100)).getData();
        Assert.assertEquals(1, afterCreate.size());

        agentService.assignAgentToCustomer(tenantId, agent.getId(), customerId);
        List<AgentApplication> afterAssign = agentApplicationService.findByAgentId(tenantId, agent.getId(), new PageLink(100)).getData();
        Assert.assertEquals(1, afterAssign.size());
        Assert.assertEquals(app.getId(), afterAssign.get(0).getId());

        agentService.unassignAgentFromCustomer(tenantId, agent.getId());
        List<AgentApplication> afterUnassign = agentApplicationService.findByAgentId(tenantId, agent.getId(), new PageLink(100)).getData();
        Assert.assertEquals(1, afterUnassign.size());
        Assert.assertEquals(app.getId(), afterUnassign.get(0).getId());

        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
        customerService.deleteCustomer(tenantId, customerId);
    }

}
