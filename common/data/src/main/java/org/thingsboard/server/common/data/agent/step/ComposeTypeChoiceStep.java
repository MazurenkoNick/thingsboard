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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ComposeTypeChoiceStep extends AgentAppStep {

    private Map<String, JsonNode> composeTemplates;
    private String selectedComposeType;

    public ComposeTypeChoiceStep(UUID id, UUID nextId, String title) {
        super(id, nextId, title, true);
    }

    @Override
    public AgentAppStepType getType() {
        return AgentAppStepType.COMPOSE_TEMPLATE;
    }

    public Optional<JsonNode> getTemplateByType(String composeType) {
        return Optional.ofNullable(composeTemplates.get(composeType));
    }

    public Set<String> getComposeTypes() {
        return composeTemplates.keySet();
    }

    @Override
    public AgentAppStep copy() {
        ComposeTypeChoiceStep copy = new ComposeTypeChoiceStep();
        copyBaseFields(copy);
        copy.setSelectedComposeType(this.selectedComposeType);
        if (this.composeTemplates != null) {
            Map<String, JsonNode> templatesCopy = new LinkedHashMap<>();
            this.composeTemplates.forEach((key, value) -> templatesCopy.put(key, value.deepCopy()));
            copy.setComposeTemplates(templatesCopy);
        }
        return copy;
    }
}
