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
package org.thingsboard.server.common.data.agent.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.agent.AgentApplicationType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "DOCKER_COMPOSE", value = DockerComposeConfig.class)
})
@Data
@NoArgsConstructor
public abstract class AgentAppConfig {

    public abstract AgentAppConfigType getType();

    public abstract AgentAppConfig copy();

    public abstract String getEdgeRoutingKey();

    public void validate() {
    }

    public void validateForProfile(AgentApplicationType appType) {
    }
}
