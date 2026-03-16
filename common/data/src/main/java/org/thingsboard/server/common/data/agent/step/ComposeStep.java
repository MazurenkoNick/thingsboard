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
package org.thingsboard.server.common.data.agent.step;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import javax.annotation.Nullable;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;
import org.thingsboard.server.common.data.agent.step.state.ComposeStepState;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ComposeStep extends StepWithDefaultState<ComposeStepState> {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    private ComposeStepState state;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    @JsonProperty(access = Access.WRITE_ONLY)
    private ComposeStepState defaultState;

    @Override
    public AgentAppStepType getType() {
        return AgentAppStepType.COMPOSE;
    }

    @Override
    public Map<String, String> getCommandMetadata(AgentApplication application, @Nullable AgentAppStepState resolvedState) {
        HashMap<String, String> res = new HashMap<>();
        AgentAppConfig config = application.getConfig();
        if (config instanceof DockerComposeConfig d && d.getCompose() != null) {
            res.put("compose", d.getCompose().toString());
        }
        res.putAll(super.getCommandMetadata(application, resolvedState));

        return res;
    }

    @Override
    @JsonIgnore
    protected ComposeStepState getDefaultState() {
        return defaultState != null ? new ComposeStepState(defaultState) : null;
    }

}
