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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Schema
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class AgentAppEventInfo extends AgentAppEvent {

    @Schema(description = "Name of the Agent Application this event belongs to.", accessMode = Schema.AccessMode.READ_ONLY)
    private String applicationName;

    public AgentAppEventInfo() {
        super();
    }

    public AgentAppEventInfo(AgentAppEvent event, String applicationName) {
        super(event);
        this.applicationName = applicationName;
    }
}
