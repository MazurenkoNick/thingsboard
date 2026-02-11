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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "COMPOSE_TEMPLATE", value = ComposeTypeChoiceStep.class),
        @JsonSubTypes.Type(name = "COMPOSE", value = ComposeStep.class),
        @JsonSubTypes.Type(name = "COMPOSE_START", value = ComposeStartStep.class),
        @JsonSubTypes.Type(name = "INFO", value = InfoStep.class)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class AgentAppStep {

    private UUID id;
    private UUID nextId;
    private String title;
    private boolean optional;
    private boolean templateOnly;
    private Boolean enabled;

    public AgentAppStep(UUID nextId, UUID id, String title, boolean templateOnly) {
        this.nextId = nextId;
        this.id = id;
        this.title = title;
        this.templateOnly = templateOnly;
    }

    public abstract AgentAppStepType getType();

    public abstract AgentAppStep copy();

    protected void copyBaseFields(AgentAppStep copy) {
        copy.setId(this.id);
        copy.setNextId(this.nextId);
        copy.setTitle(this.title);
        copy.setOptional(this.optional);
        copy.setTemplateOnly(this.templateOnly);
        copy.setEnabled(this.enabled);
    }
}
