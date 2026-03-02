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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "COMPOSE_TEMPLATE", value = ComposeTypeChoiceStep.class),
        @JsonSubTypes.Type(name = "COMPOSE", value = ComposeStep.class),
        @JsonSubTypes.Type(name = "COMPOSE_START", value = ComposeStartStep.class),
        @JsonSubTypes.Type(name = "COMPOSE_DOWN", value = ComposeDownStep.class),
        @JsonSubTypes.Type(name = "COMPOSE_MIGRATION", value = ComposeMigrationStep.class),
        @JsonSubTypes.Type(name = "ROLLBACK", value = RollBackStep.class),
        @JsonSubTypes.Type(name = "BACKUP_VOLUME", value = BackupVolumesStep.class),
        @JsonSubTypes.Type(name = "BACKUP_VOLUME_REMOVE", value = BackupVolumesRemoveStep.class),
})
@Data
@NoArgsConstructor
public abstract class AgentAppStep {

    protected UUID id;
    protected UUID nextId;
    protected String title;
    protected boolean templateOnly;

    public AgentAppStep(UUID nextId, UUID id, String title, boolean templateOnly) {
        this.nextId = nextId;
        this.id = id;
        this.title = title;
        this.templateOnly = templateOnly;
    }

    public abstract @Nullable AgentAppStepState getState();
    public abstract AgentAppStepType getType();

    @JsonIgnore
    public Map<String, String> getCommandMetadata(AgentApplication application, @Nullable AgentAppStepState resolvedState) {
        if (resolvedState != null) {
            return resolvedState.getCommandMetadata();
        }
        return Collections.emptyMap();
    }
}
