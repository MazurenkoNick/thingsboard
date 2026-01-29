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

import dev.langchain4j.agent.tool.P;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentDao;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Component
@AllArgsConstructor
public class AgentDataValidator extends DataValidator<Agent> {

    private final AgentDao agentDao;
    private final CustomerDao customerDao;
    private final TenantService tenantService;

    @Override
    protected Agent validateUpdate(TenantId tenantId, Agent agent) {
        Agent old = agentDao.findById(agent.getTenantId(), agent.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing agent!");
        }
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, Agent agent) {
        validateString("Agent name", agent.getName());
        if (agent.getTenantId() == null) {
            throw new DataValidationException("Agent should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(agent.getTenantId())) {
                throw new DataValidationException("Agent is referencing to non-existent tenant!");
            }
        }
        if (agent.getCustomerId() == null) {
            agent.setCustomerId(new CustomerId(NULL_UUID));
        } else if (!agent.getCustomerId().getId().equals(NULL_UUID)) {
            Customer customer = customerDao.findById(tenantId, agent.getCustomerId().getId());
            if (customer == null) {
                throw new DataValidationException("Can't assign agent to non-existent customer!");
            }
            if (!customer.getTenantId().equals(agent.getTenantId())) {
                throw new DataValidationException("Can't assign agent to customer from different tenant!");
            }
        }
    }
}
