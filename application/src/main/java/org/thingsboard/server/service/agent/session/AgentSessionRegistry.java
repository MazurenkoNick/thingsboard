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
package org.thingsboard.server.service.agent.session;

import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AgentSessionRegistry {

    private final ConcurrentHashMap<AgentId, AgentSession> sessionsByAgentId = new ConcurrentHashMap<>();

    public void registerOrReplace(AgentId agentId, TenantId tenantId, AgentSession session) {
        if (hasByAgentId(agentId)) {
            cleanUpExisting(agentId, tenantId);
        }
        put(session);
    }

    public void removeIfSame(AgentSession session) {
        if (session == null) {
            log.warn("Can't remove session from holder because it's null");
            return;
        }
        sessionsByAgentId.remove(session.getState().getAgentId(), session);
    }

    private void cleanUpExisting(AgentId agentId, TenantId tenantId) {
        var old = getByAgentId(agentId);
        if (old != null) {
            log.info("[{}] Replacing old session for agent [{}]", tenantId, agentId); // todo: test
            old.onError(Status.ABORTED.withDescription("Session replaced by a newer connection for agentId=" + agentId));
        }
    }

    private void put(AgentSession agentSession) {
        AgentSessionState state = agentSession.getState();
        sessionsByAgentId.put(state.getAgentId(), agentSession);
    }

    private AgentSession getByAgentId(AgentId agentId) {
        return sessionsByAgentId.get(agentId);
    }

    private boolean hasByAgentId(AgentId agentId) {
        return sessionsByAgentId.containsKey(agentId);
    }
}
