package org.thingsboard.server.common.data.agent.step;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ComposeMigrationStep extends AgentAppStep {

    private String serviceImageName;
    private String entrypoint;

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
        if (serviceImageName != null && entrypoint != null) {
            injectEntrypoint(compose);
        }
        return Map.of(
                "compose", compose.toString(),
                "abortOnContainerExit", "true"
        );
    }

    private void injectEntrypoint(JsonNode compose) {
        JsonNode services = compose.get("services");
        if (services == null || !services.isObject() || services.isEmpty()) {
            throw new IllegalStateException("Compose has no services defined");
        }
        boolean found = false;
        Iterator<Map.Entry<String, JsonNode>> it = services.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            JsonNode service = entry.getValue();
            if (service.isObject() && service.has("image")
                    && serviceImageName.equals(service.get("image").asText())) {
                ((ObjectNode) service).put("entrypoint", entrypoint);
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalStateException("Service with image '" + serviceImageName + "' not found in compose");
        }
    }
}
