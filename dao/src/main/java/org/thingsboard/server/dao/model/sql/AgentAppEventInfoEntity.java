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
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.agent.AgentAppEventInfo;

@Data
@EqualsAndHashCode(callSuper = true)
public class AgentAppEventInfoEntity extends AgentAppEventEntity {

    private String applicationName;
    private String agentName;

    public AgentAppEventInfoEntity() {
        super();
    }

    public AgentAppEventInfoEntity(AgentAppEventEntity entity, String applicationName) {
        this(entity, applicationName, null);
    }

    public AgentAppEventInfoEntity(AgentAppEventEntity entity, String applicationName, String agentName) {
        super(entity.toData());
        this.id = entity.getId();
        this.createdTime = entity.getCreatedTime();
        this.applicationName = applicationName;
        this.agentName = agentName;
    }

    @Override
    public AgentAppEventInfo toData() {
        return new AgentAppEventInfo(super.toData(), applicationName, agentName);
    }
}
