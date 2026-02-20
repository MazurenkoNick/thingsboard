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

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;

import java.util.List;

@Component
public class DefaultAgentAppEventStepsResolver implements AgentAppEventStepsResolver {

    @Override
    public List<AgentAppStep> resolveSteps(AgentApplication app, AgentAppEventActionType actionType) {
        List<AgentAppStep> steps = switch (actionType) {
            case INSTALL, RESTART, UPDATE -> app.getStartSteps(); // todo: add UPGRADE
            case DELETE -> app.getDeleteSteps();
        };
        if (steps == null || steps.isEmpty()) {
            throw new IllegalStateException("No steps resolved for application " + app.getId() + " and action " + actionType);
        }
        return steps;
    }
}
