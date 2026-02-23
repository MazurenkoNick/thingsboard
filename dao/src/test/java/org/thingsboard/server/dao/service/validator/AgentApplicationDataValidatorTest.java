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
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willReturn;

@SpringBootTest(classes = AgentApplicationDataValidator.class)
class AgentApplicationDataValidatorTest {

    @MockitoBean
    AgentService agentService;
    @MockitoBean
    AgentApplicationDao agentApplicationDao;
    @Autowired
    AgentApplicationDataValidator validator;

    TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));
    AgentId agentId = new AgentId(UUID.fromString("8ef79cdf-37a8-4119-b682-2e7ed4e018da"));
    AgentApplicationId applicationId = new AgentApplicationId(UUID.fromString("7ef79cdf-37a8-4119-b682-2e7ed4e018da"));

    @BeforeEach
    void setUp() {
        Agent agent = new Agent();
        agent.setId(agentId);
        agent.setTenantId(tenantId);
        agent.setName("Test Agent");
        willReturn(agent).given(agentService).findAgentById(eq(tenantId), eq(agentId));
    }

    @Test
    void testValidateDataImpl_nullAgentId_thenException() {
        AgentApplication app = new AgentApplication();

        DataValidationException exception = Assertions.assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).containsIgnoringCase("assigned to agent");
    }

    @Test
    void testValidateDataImpl_nonExistentAgent_thenException() {
        AgentId nonExistentAgentId = new AgentId(UUID.randomUUID());
        willReturn(null).given(agentService).findAgentById(eq(tenantId), eq(nonExistentAgentId));

        AgentApplication app = new AgentApplication();
        app.setAgentId(nonExistentAgentId);

        DataValidationException exception = Assertions.assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, app));
        assertThat(exception.getMessage()).contains("non-existent agent");
    }

    @Test
    void testValidateDataImpl_valid_thenOK() {
        AgentApplication app = new AgentApplication();
        app.setAgentId(agentId);

        validator.validateDataImpl(tenantId, app);
    }

    @Test
    void testValidateUpdate_existingApplication_thenReturnOld() {
        AgentApplication oldApp = new AgentApplication();
        oldApp.setId(applicationId);
        oldApp.setAgentId(agentId);
        willReturn(oldApp).given(agentApplicationDao).findById(eq(tenantId), eq(applicationId.getId()));

        AgentApplication newApp = new AgentApplication();
        newApp.setId(applicationId);
        newApp.setAgentId(agentId);

        AgentApplication result = validator.validateUpdate(tenantId, newApp);
        assertThat(result).isEqualTo(oldApp);
    }

    @Test
    void testValidateUpdate_nonExistentApplication_thenException() {
        willReturn(null).given(agentApplicationDao).findById(eq(tenantId), eq(applicationId.getId()));

        AgentApplication app = new AgentApplication();
        app.setId(applicationId);
        app.setAgentId(agentId);

        DataValidationException exception = Assertions.assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, app));
        assertThat(exception.getMessage()).contains("non existing agent application");
    }
}
