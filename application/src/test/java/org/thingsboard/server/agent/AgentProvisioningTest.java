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
package org.thingsboard.server.agent;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.agent.imitator.AgentImitator;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentGroup;
import org.thingsboard.server.common.data.agent.AgentProvisionType;
import org.thingsboard.server.common.data.id.AgentGroupId;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.agent.v1.ProvisionResponse;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
@TestPropertySource(properties = {
        "edges.enabled=true",
        "queue.rule-engine.stats.enabled=false"
})
public class AgentProvisioningTest extends AbstractControllerTest {

    @Autowired
    private AgentService agentService;

    private final List<AgentGroupId> createdGroups = new ArrayList<>();
    private AgentImitator imitator;

    @Before
    public void setUp() throws Exception {
        loginTenantAdmin();
    }

    @After
    public void tearDown() throws Exception {
        if (imitator != null) {
            try {
                imitator.disconnect();
            } catch (Exception ignored) {
            }
        }
        loginTenantAdmin();
        for (AgentGroupId id : createdGroups) {
            try {
                doDelete("/api/agent/group/" + id.getId()).andExpect(status().isOk());
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    public void testHappyPath() throws Exception {
        AgentGroup group = createGroup(AgentProvisionType.ALLOW_CREATE_NEW_AGENTS, null, null);
        Assert.assertNotNull(group.getProvisionKey());
        Assert.assertNotNull(group.getProvisionSecret());
        Assert.assertEquals(20, group.getProvisionKey().length());
        Assert.assertEquals(20, group.getProvisionSecret().length());

        imitator = new AgentImitator("localhost", 7070, "", "");
        imitator.provision(group.getProvisionKey(), group.getProvisionSecret());

        ProvisionResponse resp = imitator.getProvisionResponse();
        Assert.assertNotNull(resp);
        Assert.assertTrue("Expected success, got: " + resp.getErrorMessage(), resp.getSuccess());
        Assert.assertFalse(resp.getRoutingKey().isEmpty());
        Assert.assertFalse(resp.getRoutingSecret().isEmpty());

        // Verify Agent persisted with the returned routing key, tied to the group
        Agent provisioned = agentService.findAgentByRoutingKey(tenantId, resp.getRoutingKey());
        Assert.assertNotNull("Provisioned agent should exist in DB", provisioned);
        Assert.assertEquals(group.getId(), provisioned.getAgentGroupId());
        Assert.assertEquals(resp.getRoutingSecret(), provisioned.getSecret());

        // Reconnect with the routing creds the imitator captured from ProvisionResponse
        imitator.connect();
        Assert.assertNotNull(imitator.getHelloAck());
        Assert.assertTrue(imitator.getHelloAck().getSuccess());
    }

    @Test
    public void testInvalidProvisionKey() throws Exception {
        AgentGroup group = createGroup(AgentProvisionType.ALLOW_CREATE_NEW_AGENTS, null, null);

        imitator = new AgentImitator("localhost", 7070, "", "");
        imitator.provision("not-a-real-key", group.getProvisionSecret());

        ProvisionResponse resp = imitator.getProvisionResponse();
        Assert.assertNotNull(resp);
        Assert.assertFalse(resp.getSuccess());
        Assert.assertEquals("Invalid provisioning credentials", resp.getErrorMessage());
    }

    @Test
    public void testInvalidProvisionSecret() throws Exception {
        AgentGroup group = createGroup(AgentProvisionType.ALLOW_CREATE_NEW_AGENTS, null, null);

        imitator = new AgentImitator("localhost", 7070, "", "");
        imitator.provision(group.getProvisionKey(), "wrong-secret");

        ProvisionResponse resp = imitator.getProvisionResponse();
        Assert.assertNotNull(resp);
        Assert.assertFalse(resp.getSuccess());
        Assert.assertEquals("Invalid provisioning credentials", resp.getErrorMessage());
    }

    @Test
    public void testProvisioningDisabled() throws Exception {
        // Auto-generation only kicks in for non-DISABLED groups; set keys explicitly here
        String key = StringUtils.randomAlphanumeric(20);
        String secret = StringUtils.randomAlphanumeric(20);
        AgentGroup group = createGroup(AgentProvisionType.DISABLED, key, secret);

        imitator = new AgentImitator("localhost", 7070, "", "");
        imitator.provision(key, secret);

        ProvisionResponse resp = imitator.getProvisionResponse();
        Assert.assertNotNull(resp);
        Assert.assertFalse(resp.getSuccess());
        Assert.assertEquals("Auto-provisioning is disabled", resp.getErrorMessage());
    }

    @Test
    public void testAutoGeneratedKeysAreUnique() {
        AgentGroup g1 = createGroup(AgentProvisionType.ALLOW_CREATE_NEW_AGENTS, null, null);
        AgentGroup g2 = createGroup(AgentProvisionType.ALLOW_CREATE_NEW_AGENTS, null, null);
        Assert.assertNotEquals(g1.getProvisionKey(), g2.getProvisionKey());
        Assert.assertNotEquals(g1.getProvisionSecret(), g2.getProvisionSecret());
    }

    private AgentGroup createGroup(AgentProvisionType type, String provisionKey, String provisionSecret) {
        AgentGroup g = new AgentGroup();
        g.setName("provision-test-" + System.nanoTime());
        g.setProvisionType(type);
        if (provisionKey != null) {
            g.setProvisionKey(provisionKey);
        }
        if (provisionSecret != null) {
            g.setProvisionSecret(provisionSecret);
        }
        try {
            AgentGroup saved = doPost("/api/agent/group", g, AgentGroup.class);
            createdGroups.add(saved.getId());
            return saved;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
