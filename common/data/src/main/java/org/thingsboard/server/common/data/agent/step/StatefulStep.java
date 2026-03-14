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

import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;

import javax.annotation.Nullable;

public abstract class StatefulStep<T extends AgentAppStepState> extends AgentAppStep {

    /**
     * Template-defined state that describes what fields the frontend should render for user input (stepInputs).
     * When present, the step is considered stateful and the validator may require the user to provide
     * corresponding stepInputs in the event, unless a {@link StepWithDefaultState#getDefaultState() defaultState}
     * is available as a server-side fallback.
     * <p>
     * This is NOT used in command metadata resolution — only resolvedState (from user stepInputs)
     * or defaultState contribute to the command sent to the agent.
     */
    public abstract @Nullable T getState();
}
