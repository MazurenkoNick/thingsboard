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
package org.thingsboard.server.common.data.agent.step.state;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.agent.step.AgentAppStepType;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Collections;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class RollBackStepState extends AgentAppStepState {

    private AgentAppEventId failedEventId;

    public RollBackStepState(AgentAppEventId failedEventId) {
        this.failedEventId = failedEventId;
    }

    @Override
    public AgentAppStepType getType() {
        return AgentAppStepType.ROLLBACK;
    }

    @Override
    public void validate() throws DataValidationException {
        if (failedEventId == null) {
            throw new DataValidationException("Rollback event must have RollBackStepState with failedEventId!");
        }
    }

    @Override
    public Map<String, String> getCommandMetadata() {
        if (failedEventId == null) {
            return Collections.emptyMap();
        }
        return Map.of("failedCommandId", failedEventId.getId().toString());
    }

}
