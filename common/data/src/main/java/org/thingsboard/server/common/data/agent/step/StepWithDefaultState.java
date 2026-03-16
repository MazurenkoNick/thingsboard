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
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

public abstract class StepWithDefaultState<T extends AgentAppStepState> extends StatefulStep<T> {

    /**
     * Returns the default state for this step, used as a fallback in
     * {@link #getCommandMetadata(AgentApplication, AgentAppStepState)} when the user-defined {@code resolvedState}
     * parameter is {@code null}.
     *
     * @return the default state, or {@code null} if no default is defined
     */
    @JsonIgnore
    protected abstract T getDefaultState();

    @Override
    @JsonIgnore
    public Map<String, String> getCommandMetadata(AgentApplication application, @Nullable AgentAppStepState resolvedState) {
        if (resolvedState != null) {
            return resolvedState.getCommandMetadata();
        }
        T def = getDefaultState();
        if (def != null) {
            return def.getCommandMetadata();
        }
        return Collections.emptyMap();
    }

}
