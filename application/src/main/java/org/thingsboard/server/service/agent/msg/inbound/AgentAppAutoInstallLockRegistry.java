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
package org.thingsboard.server.service.agent.msg.inbound;

import org.springframework.stereotype.Component;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.server.common.data.id.AgentId;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class AgentAppAutoInstallLockRegistry {

    private final ConcurrentMap<AgentId, ReadWriteLock> locks = new ConcurrentReferenceHashMap<>(16, ConcurrentReferenceHashMap.ReferenceType.WEAK);

    public ReadWriteLock forAgent(AgentId agentId) {
        return locks.computeIfAbsent(agentId, k -> new ReentrantReadWriteLock());
    }
}
