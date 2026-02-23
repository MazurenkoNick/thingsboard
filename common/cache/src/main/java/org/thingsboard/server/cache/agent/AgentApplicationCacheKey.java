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
package org.thingsboard.server.cache.agent;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.AgentApplicationId;

import java.io.Serial;
import java.io.Serializable;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class AgentApplicationCacheKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 8657100143498569699L;

    private final AgentApplicationId agentApplicationId;

    public static AgentApplicationCacheKey from(AgentApplicationId id) {
        return new AgentApplicationCacheKey(id);
    }

    @Override
    public String toString() {
        return agentApplicationId.toString();
    }
}
