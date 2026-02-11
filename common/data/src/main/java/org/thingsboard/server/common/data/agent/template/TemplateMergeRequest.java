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
package org.thingsboard.server.common.data.agent.template;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.agent.AgentApplication;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateMergeRequest {

    @Schema(description = "User-made agent application which is used during merge with the template")
    // todo: if the agent application is null, then we will have to create a new one and merge with the existing template
    private AgentApplication application;

    @Schema(description = "Template with the pre-selected configuration to be merged into agent application")
    private AgentAppTemplate template;
}

