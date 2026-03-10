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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Objects;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DockerComposeConfig extends AgentAppConfig {

    private JsonNode compose;

    @Override
    public AgentAppConfigType getType() {
        return AgentAppConfigType.DOCKER_COMPOSE;
    }

    @Override
    public AgentAppConfig copy() {
        DockerComposeConfig copy = new DockerComposeConfig();
        copy.setCompose(this.compose != null ? this.compose.deepCopy() : null);
        return copy;
    }

    @Override
    public void validate() {
        if (compose == null) {
            throw new DataValidationException("Docker compose config compose content must be specified!");
        }
    }

    @Override
    public boolean isDeployFieldsChanged(AgentAppConfig other) {
        if (!(other instanceof DockerComposeConfig that)) {
            return true;
        }
        return !Objects.equals(this.compose, that.compose);
    }
}

