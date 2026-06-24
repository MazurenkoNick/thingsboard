/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.service.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.AgentProvisionType;
import org.thingsboard.server.dao.agent.AgentProfileService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Service
@Slf4j
@TbCoreComponent
@RequiredArgsConstructor
public class AgentProvisionService {

    public static final String INVALID_CREDENTIALS = "Invalid provisioning credentials";
    public static final String PROVISIONING_DISABLED = "Auto-provisioning is disabled";

    private final AgentProfileService agentProfileService;
    private final AgentService agentService;

    public ProvisionResult provision(String provisionKey, String provisionSecret) {
        if (StringUtils.isEmpty(provisionKey) || StringUtils.isEmpty(provisionSecret)) {
            return ProvisionResult.failure(INVALID_CREDENTIALS);
        }
        AgentProfile agentProfile = agentProfileService.findProfileByProvisionKey(provisionKey);
        if (agentProfile == null || !provisionSecret.equals(agentProfile.getProvisionSecret())) {
            return ProvisionResult.failure(INVALID_CREDENTIALS);
        }
        if (agentProfile.getProvisionType() == null || agentProfile.getProvisionType() == AgentProvisionType.DISABLED) {
            return ProvisionResult.failure(PROVISIONING_DISABLED);
        }
        String routingKey = StringUtils.randomAlphanumeric(20);
        String routingSecret = StringUtils.randomAlphanumeric(20);

        Agent agent = new Agent();
        agent.setTenantId(agentProfile.getTenantId());
        agent.setAgentProfileId(agentProfile.getId());
        agent.setName("Agent-" + routingKey.substring(0, 8));
        agent.setRoutingKey(routingKey);
        agent.setSecret(routingSecret);
        agentService.saveAgent(agent);

        log.info("Provisioned new agent [{}] for agentProfile [{}]", agent.getName(), agentProfile.getId());
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
