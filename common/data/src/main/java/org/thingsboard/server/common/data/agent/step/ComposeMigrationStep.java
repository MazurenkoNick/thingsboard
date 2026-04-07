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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;
import org.thingsboard.server.common.data.agent.step.state.ComposeMigrationStepState;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ComposeMigrationStep extends StepWithDefaultState<ComposeMigrationStepState> {

    private List<ServiceOverride> serviceOverrides;
    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    private ComposeMigrationStepState state;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    private ComposeMigrationStepState defaultState;

    @Override
    public AgentAppStepType getType() {
        return AgentAppStepType.COMPOSE_MIGRATION;
    }

    @Override
    public Map<String, String> getCommandMetadata(AgentApplication application, @Nullable AgentAppStepState resolvedState) {
        if (!(application.getConfig() instanceof DockerComposeConfig d) || d.getCompose() == null) {
            return Collections.emptyMap();
        }
        HashMap<String, String> res = new HashMap<>();
        JsonNode compose = d.getCompose().deepCopy();
        if (!CollectionUtils.isEmpty(serviceOverrides)) {
            applyOverrides(compose);
        }
        res.put("compose", compose.toString());
        res.put("abortOnContainerExit", "true");

        res.putAll(super.getCommandMetadata(application, resolvedState));
        return res;
    }

    private void applyOverrides(JsonNode compose) {
        JsonNode services = compose.get("services");
        if (services == null || !services.isObject() || services.isEmpty()) {
            throw new IllegalStateException("Compose has no services defined");
        }
        for (ServiceOverride override : serviceOverrides) {
            applyOverride(services, override);
        }
    }

    private void applyOverride(JsonNode services, ServiceOverride override) {
        boolean found = false;
        Iterator<Map.Entry<String, JsonNode>> it = services.fields();
        while (it.hasNext()) {
            JsonNode service = it.next().getValue();
            if (service.isObject() && service.has("image")
                    && service.get("image").asText().startsWith(override.getServiceImageName())) {
                override.getProperties().forEach(((ObjectNode) service)::put);
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalStateException("Service with image '" + override.getServiceImageName() + "' not found in compose");
        }
    }

    @Data
    @NoArgsConstructor
    public static class ServiceOverride {
        private String serviceImageName;
        private Map<String, String> properties;
    }
}
