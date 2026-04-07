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
package org.thingsboard.server.service.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentGroup;
import org.thingsboard.server.common.data.agent.AgentProvisionType;
import org.thingsboard.server.dao.agent.AgentGroupService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Service
@Slf4j
@TbCoreComponent
@RequiredArgsConstructor
public class AgentProvisionService {

    public static final String INVALID_CREDENTIALS = "Invalid provisioning credentials";
    public static final String PROVISIONING_DISABLED = "Auto-provisioning is disabled";

    private final AgentGroupService agentGroupService;
    private final AgentService agentService;

    public ProvisionResult provision(String provisionKey, String provisionSecret) {
        if (StringUtils.isEmpty(provisionKey) || StringUtils.isEmpty(provisionSecret)) {
            return ProvisionResult.failure(INVALID_CREDENTIALS);
        }
        AgentGroup group = agentGroupService.findGroupByProvisionKey(provisionKey);
        if (group == null || !provisionSecret.equals(group.getProvisionSecret())) {
            return ProvisionResult.failure(INVALID_CREDENTIALS);
        }
        if (group.getProvisionType() == null || group.getProvisionType() == AgentProvisionType.DISABLED) {
            return ProvisionResult.failure(PROVISIONING_DISABLED);
        }
        String routingKey = StringUtils.randomAlphanumeric(20);
        String routingSecret = StringUtils.randomAlphanumeric(20);

        Agent agent = new Agent();
        agent.setTenantId(group.getTenantId());
        agent.setCustomerId(group.getCustomerId());
        agent.setAgentGroupId(group.getId());
        agent.setName("Agent-" + routingKey.substring(0, 8));
        agent.setRoutingKey(routingKey);
        agent.setSecret(routingSecret);
        agentService.saveAgent(agent);

        log.info("Provisioned new agent [{}] for group [{}]", agent.getName(), group.getId());
        return ProvisionResult.success(routingKey, routingSecret);
    }

    public record ProvisionResult(boolean success, String routingKey, String routingSecret, String errorMessage) {
        public static ProvisionResult success(String routingKey, String routingSecret) {
            return new ProvisionResult(true, routingKey, routingSecret, null);
        }

        public static ProvisionResult failure(String errorMessage) {
            return new ProvisionResult(false, null, null, errorMessage);
        }
    }
}
