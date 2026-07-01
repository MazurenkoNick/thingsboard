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
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.config.AgentComposeRefUtils;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeUtils;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;
import org.thingsboard.server.common.data.agent.step.state.RunJobStepState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RunJobStep extends StatefulStep<RunJobStepState> {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    private RunJobStepState state;

    @Override
    public AgentAppStepType getType() {
        return AgentAppStepType.RUN_JOB;
    }

    @Override
    @JsonIgnore
    public Map<String, String> getCommandMetadata(AgentApplication application, @Nullable AgentAppStepState resolvedState) {
        if (state == null) {
            return Collections.emptyMap();
        }
        ObjectNode job = state.buildJob(resolvedState);
        AgentAppConfig config = application.getConfig();
        if (config instanceof DockerComposeConfig d && d.getCompose() != null) {
            JsonNode compose = d.getCompose();
            resolveComposeRefs(job, RunJobStepState.BINDS, compose);
            resolveComposeRefs(job, RunJobStepState.ENVIRONMENT, compose);

            String serviceRegexForNetwork = state.effectiveValue(RunJobStepState.NETWORK_FROM_SERVICE_IMAGE_REGEX_PATTERN, resolvedState);
            if (serviceRegexForNetwork != null && !serviceRegexForNetwork.isBlank()) {
                String serviceName = DockerComposeUtils.findServiceNameByImage(compose, Pattern.compile(serviceRegexForNetwork));
                if (serviceName != null) {
                    job.put(RunJobStepState.SERVICE, serviceName);
                }
            }
        }
        return Map.of(RunJobStepState.JOB, job.toString());
    }

    // Expands ${compose...} entries in a job string-array field against the compose node, leaving other entries as-is.
    private static void resolveComposeRefs(ObjectNode job, String field, JsonNode compose) {
        JsonNode array = job.get(field);
        if (array == null || !array.isArray()) {
            return;
        }
        List<String> resolved = new ArrayList<>();
        for (JsonNode element : array) {
            String value = element.asText();
            if (AgentComposeRefUtils.isComposeRef(value)) {
                resolved.addAll(AgentComposeRefUtils.resolveAsList(compose, value));
            } else {
                resolved.add(value);
            }
        }
        ArrayNode replacement = job.putArray(field);
        resolved.forEach(replacement::add);
    }

}
