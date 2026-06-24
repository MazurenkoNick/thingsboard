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

import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.gen.agent.v1.Hello;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.session.AgentSession;
import org.thingsboard.server.service.agent.session.AgentSessionRegistry;
import org.thingsboard.server.service.agent.session.AgentSessionState;

import java.util.Optional;

@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class DefaultAgentSessionService implements AgentSessionService {

    private final AgentService agentService;
    private final AgentSessionRegistry sessions;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbTransactionalCache<AgentId, String> agentIdServiceIdCache;
    private final AgentStateService agentStateService;

    @Override
    public Optional<Status> onConnected(AgentSession session, Hello msg) {
        AgentSessionState state = session.getState();
        Agent agent;
        try {
            agent = findAgentByRoutingKeyAndSecret(msg.getRoutingKey(), msg.getRoutingSecret());
        } catch (SecurityException e) {
            log.warn("Agent authentication failed, routingKey={}: {}", msg.getRoutingKey(), e.getMessage());
            return Optional.of(Status.UNAUTHENTICATED.withDescription(e.getMessage()));
        }
        if (agent == null) {
            log.trace("Agent not found, routingKey={}", msg.getRoutingKey());
            return Optional.of(Status.NOT_FOUND.withDescription("Failed to find the agent. Routing key: " + msg.getRoutingKey()));
        }
        state.setAgent(agent);
        TenantId tenantId = agent.getTenantId();
        AgentId agentId = agent.getId();

        sessions.registerOrReplace(agentId, tenantId, session);
        agentIdServiceIdCache.put(agentId, serviceInfoProvider.getServiceId());
        log.info("[{}] agent [{}] connected successfully", tenantId, agentId);
        agentStateService.onAgentConnect(agent, System.currentTimeMillis());
        return Optional.empty();
    }

    @Override
    public void onCompleted(AgentSession session) {
        disconnect(session);
    }

    @Override
    public void onError(AgentSession session) {
        disconnect(session);
    }

    private void disconnect(AgentSession session) {
        Agent agent = session.getState().getAgent();
        if (agent == null) {
            return;
        }
        if (!sessions.removeIfSame(session)) {
            return;
        }
        agentIdServiceIdCache.evict(agent.getId());
        agentStateService.onAgentDisconnect(agent, System.currentTimeMillis());
    }

    private Agent findAgentByRoutingKeyAndSecret(String routingKey, String routingSecret) {
        Agent agent = agentService.findAgentByRoutingKey(TenantId.SYS_TENANT_ID, routingKey);
        if (agent == null) {
            return null;
        }
        if (!agent.getSecret().equals(routingSecret)) {
            throw new SecurityException("Failed to validate the agent! Routing key: " + routingKey);
        }
        return agent;
    }

}
