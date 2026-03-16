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

import io.grpc.Status;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.agent.imitator.AgentImitator;
import org.thingsboard.server.dao.service.DaoSqlTest;

@DaoSqlTest
public class ConnectionAgentTest extends AbstractAgentTest {

    @Test
    public void testAgentConnectsSuccessfully() {
        // Agent connects in @Before — just verify the activity attribute
        verifyAttribute(agent.getId(), "active", true);
    }

    @Test
    public void testAgentAuthFailsWithWrongSecret() throws Exception {
        AgentImitator badImitator = new AgentImitator("localhost", 7070,
                agent.getRoutingKey(), "wrong-secret");
        try {
            Status status = badImitator.connectExpectingError();
            Assert.assertNotNull("Expected an error status", status);
            Assert.assertEquals(Status.Code.UNAUTHENTICATED, status.getCode());
        } finally {
            badImitator.disconnect();
        }
    }

    @Test
    public void testAgentDisconnect() throws Exception {
        // Verify agent is active
        verifyAttribute(agent.getId(), "active", true);

        // Disconnect
        agentImitator.disconnect();

        // Verify agent becomes inactive
        verifyAttribute(agent.getId(), "active", false);

        // Reconnect so teardown works cleanly
        agentImitator = new AgentImitator("localhost", 7070,
                agent.getRoutingKey(), agent.getSecret());
        agentImitator.connect();
    }
}
