/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.common.data.agent.step.state;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import lombok.Data;
import org.thingsboard.server.common.data.agent.step.AgentAppStepType;
import org.thingsboard.server.exception.DataValidationException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
        @Type(name = "COMPOSE", value = ComposeStepState.class),
        @Type(name = "COMPOSE_DOWN", value = ComposeDownStepState.class),
        @Type(name = "ROLLBACK", value = RollBackStepState.class),
        @Type(name = "BACKUP_VOLUME", value = BackupVolumesStepState.class),
        @Type(name = "RUN_JOB", value = RunJobStepState.class),
})
@Data
public abstract class AgentAppStepState {

    public abstract AgentAppStepType getType();

    public abstract void validate() throws DataValidationException;

    /**
     * This state's fields keyed by their JSON property name. Never null; a state with no fields returns an empty map.
     * Field values may be null, so build with {@link java.util.Collections#singletonMap(Object, Object)} or a
     * {@code LinkedHashMap} rather than {@link Map#of} (which rejects null values).
     */
    @JsonIgnore
    @Nonnull
    protected abstract Map<String, StepField<?>> fields();

    /**
     * Command metadata for this state. The {@code overlay} (user-submitted state) wins per field when it provides a
     * non-null value; otherwise this (template) state's value is used — see {@link #effectiveValue(String, AgentAppStepState)}.
     */
    @JsonIgnore
    public abstract Map<String, String> getCommandMetadata(@Nullable AgentAppStepState overlay);

    /**
     * Whether this state declares at least one field the user is expected to choose ({@code userChoice == true}).
     * Such steps require a corresponding stepInput in the event; {@code userChoice == false} fields are applied
     * silently (template value or, for ROLLBACK, the server-injected value).
     */
    @JsonIgnore
    public boolean hasUserChoice() {
        return fields().values().stream().anyMatch(f -> f != null && f.isUserChoice());
    }

    /**
     * Field names this (template) state marks {@code userChoice == true} for which {@code submitted} provides no
     * non-null value. Required-ness is driven by the template; the submitted state's own {@code userChoice} flags are ignored.
     */
    @JsonIgnore
    public List<String> missingRequiredUserInputs(@Nullable AgentAppStepState submitted) {
        Map<String, StepField<?>> provided = submitted != null ? submitted.fields() : Map.of();
        List<String> missing = new ArrayList<>();
        fields().forEach((name, field) -> {
            if (field != null && field.isUserChoice()) {
                StepField<?> got = provided.get(name);
                if (got == null || got.getValue() == null) {
                    missing.add(name);
                }
            }
        });
        return missing;
    }

    /**
     * Effective value for {@code name}: the overlay's value when present and non-null, else this (template) value.
     */
    @SuppressWarnings("unchecked")
    public <V> V effectiveValue(String name, @Nullable AgentAppStepState overlay) {
        if (overlay != null) {
            StepField<?> o = overlay.fields().get(name);
            if (o != null && o.getValue() != null) {
                return (V) o.getValue();
            }
        }
        StepField<?> t = fields().get(name);
        return t != null ? (V) t.getValue() : null;
    }

}
