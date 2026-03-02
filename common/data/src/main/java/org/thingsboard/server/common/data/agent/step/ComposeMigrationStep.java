package org.thingsboard.server.common.data.agent.step;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ComposeMigrationStep extends AgentAppStep {

    private List<ServiceOverride> serviceOverrides;

    @Override
    public @Nullable AgentAppStepState getState() {
        return null;
    }

    @Override
    public AgentAppStepType getType() {
        return AgentAppStepType.COMPOSE_MIGRATION;
    }

    @Override
    public Map<String, String> getCommandMetadata(AgentApplication application, @Nullable AgentAppStepState resolvedState) {
        if (!(application.getConfig() instanceof DockerComposeConfig d) || d.getCompose() == null) {
            return Collections.emptyMap();
        }
        JsonNode compose = d.getCompose().deepCopy();
        if (!CollectionUtils.isEmpty(serviceOverrides)) {
            applyOverrides(compose);
        }
        return Map.of(
                "compose", compose.toString(),
                "abortOnContainerExit", "true"
        );
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
