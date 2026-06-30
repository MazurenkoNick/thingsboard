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
        @JsonSubTypes.Type(name = "COMPOSE_RESTART", value = ComposeRestartStep.class),
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

    public abstract AgentAppStepType getType();

    public boolean isStateful() {
        return this instanceof StatefulStep<?> ss && ss.getState() != null;
    }

    @JsonIgnore
    public Map<String, String> getCommandMetadata(AgentApplication application, @Nullable AgentAppStepState resolvedState) {
        return Collections.emptyMap();
    }
}
