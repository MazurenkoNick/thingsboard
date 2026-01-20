/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@RequiredArgsConstructor
public class AgentSessionState {

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean closing = new AtomicBoolean(false);
    private TenantId tenantId;
    private AgentId agentId;

    private Agent agent;

    public boolean beginClosing() {
        return closing.compareAndSet(false, true);
    }

    public void setAgent(Agent agent) {
        this.tenantId = agent.getTenantId();
        this.agentId = agent.getId();
        this.agent = agent;
    }

    public void closeAndDo(Runnable runnable) {
        if (closed.compareAndSet(false, true)) {
            runnable.run();
        }
    }

    public boolean isClosed() {
        return closed.get();
    }

    public boolean isClosing() {
        return closing.get();
    }
}
