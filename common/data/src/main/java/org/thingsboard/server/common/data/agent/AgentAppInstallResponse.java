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
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description = "Response payload for the install-agent-application endpoint. Carries both the created application "
        + "and the INSTALL event so the caller can open a progress dialog without a second round-trip.")
@Data
@AllArgsConstructor
public class AgentAppInstallResponse {

    @Schema(description = "The newly created agent application.")
    private AgentApplication application;

    @Schema(description = "The INSTALL event created alongside the application.")
    private AgentAppEvent event;
}
