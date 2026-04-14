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
package org.thingsboard.server.common.data.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;

import java.util.Map;
import java.util.UUID;

@Schema(description = "Request payload for creating an agent application event (install, update, upgrade, restart, delete, etc.).")
@Data
public class AgentAppEventRequest {

    @Schema(description = "Action to perform against the agent application.")
    private AgentAppEventActionType actionType;

    @Schema(description = "Agent application payload supplied by the caller. "
            + "Used to carry name/config changes for UPDATE and the target state for INSTALL/UPGRADE.")
    private AgentApplication application;

    @Schema(description = "Per-step input overrides keyed by step id (e.g. pullImages flag, backup volume selection).")
    private Map<UUID, AgentAppStepState> stepInputs;

    @Schema(description = "Optional bulk-action correlation id. "
            + "When the same value is used across multiple application events, duplicates are deduplicated server-side.")
    private UUID bulkActionId;

    @Schema(description = "UPDATE-action flag for profile-managed apps. When true, the compose is not re-resolved from the "
            + "(possibly upgraded) profile — only the credentials carried by `application.config` are applied. "
            + "Ignored for non-UPDATE actions and for non-profile-managed apps.")
    private boolean skipProfileRefetch;
}
