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
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.HasId;

import java.util.Map;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString
@Setter
public class AgentApplication extends BaseData<AgentApplicationId> implements HasId<AgentApplicationId>, HasVersion {

    private AgentId agentId;
    private String name;
    private Map<String, String> placeholders;
    private Map<String, AgentAppConfig> configuration;
    @Getter
    private Long version;

    public AgentApplication() {
        super();
    }

    public AgentApplication(AgentApplicationId id) {
        super(id);
    }

    public AgentApplication(AgentApplication application) {
        super(application);
        this.agentId = application.getAgentId();
        this.name = application.getName();
        this.placeholders = application.getPlaceholders();
        this.configuration = application.getConfiguration();
        this.version = application.getVersion();
    }

    @Schema(description = "JSON object with the Agent Application Id.")
    @Override
    public AgentApplicationId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the agent application creation, in milliseconds", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "Agent this application belongs to", requiredMode = Schema.RequiredMode.REQUIRED)
    public AgentId getAgentId() {
        return agentId;
    }

    @Schema(description = "Application name")
    public String getName() {
        return name;
    }

    @Schema(description = "Placeholder key-value map")
    public Map<String, String> getPlaceholders() {
        return placeholders;
    }

    @Schema(description = "Configuration key-value map, e.g. {\"queue_type\": {\"multiSelect\": false, \"values\": [\"IN_MEMORY\"] }}")
    public Map<String, AgentAppConfig> getConfiguration() {
        return configuration;
    }
}
