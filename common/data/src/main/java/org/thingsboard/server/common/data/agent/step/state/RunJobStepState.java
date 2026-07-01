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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.thingsboard.server.common.data.agent.step.AgentAppStepType;
import org.thingsboard.server.exception.DataValidationException;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class RunJobStepState extends AgentAppStepState {

    public static final String JOB = "job";

    public static final String IMAGE = "image";
    public static final String ENTRYPOINT = "entrypoint";
    public static final String CMD = "cmd";
    public static final String ENVIRONMENT = "env";
    public static final String BINDS = "binds";
    public static final String NETWORKS = "networks";
    public static final String SERVICE = "service";
    public static final String PULL_IMAGES = "pullImages";
    public static final String RETRIES = "retries";
    public static final String NETWORK_FROM_SERVICE_IMAGE_REGEX_PATTERN = "networkFromServiceImageRegexPattern";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private StepField<String> image;
    private StepField<List<String>> entrypoint;
    private StepField<List<String>> cmd;
    private StepField<List<String>> env;
    private StepField<List<String>> binds;
    private StepField<List<String>> networks;
    private StepField<Boolean> pullImages;
    private StepField<Integer> retries;
    private StepField<String> networkFromServiceImageRegexPattern;

    @Override
    public AgentAppStepType getType() {
        return AgentAppStepType.RUN_JOB;
    }

    @Override
    public void validate() throws DataValidationException {
        if (image == null || image.getValue() == null || image.getValue().isBlank()) {
            throw new DataValidationException("Validation error: " + IMAGE + " must not be blank");
        }
    }

    @Override
    protected @NonNull Map<String, StepField<?>> fields() {
        Map<String, StepField<?>> fields = new LinkedHashMap<>();
        fields.put(IMAGE, image);
        fields.put(ENTRYPOINT, entrypoint);
        fields.put(CMD, cmd);
        fields.put(ENVIRONMENT, env);
        fields.put(BINDS, binds);
        fields.put(NETWORKS, networks);
        fields.put(PULL_IMAGES, pullImages);
        fields.put(RETRIES, retries);
        fields.put(NETWORK_FROM_SERVICE_IMAGE_REGEX_PATTERN, networkFromServiceImageRegexPattern);
        return fields;
    }

    /**
     * Builds the JobSpec without the {@code service} field, which {@code RunJobStep} resolves from the compose config
     * (it needs the resolved service name, unavailable here).
     */
    public ObjectNode buildJob(@Nullable AgentAppStepState overlay) {
        ObjectNode job = MAPPER.createObjectNode();
        String img = effectiveValue(IMAGE, overlay);
        job.put(IMAGE, img != null ? img : "");
        putList(job, ENTRYPOINT, effectiveValue(ENTRYPOINT, overlay));
        putList(job, CMD, effectiveValue(CMD, overlay));
        putList(job, ENVIRONMENT, effectiveValue(ENVIRONMENT, overlay));
        putList(job, BINDS, effectiveValue(BINDS, overlay));
        putList(job, NETWORKS, effectiveValue(NETWORKS, overlay));
        job.put(PULL_IMAGES, Boolean.TRUE.equals(effectiveValue(PULL_IMAGES, overlay)));
        Integer retryCount = effectiveValue(RETRIES, overlay);
        if (retryCount != null) {
            job.put(RETRIES, retryCount);
        }
        return job;
    }

    @Override
    public Map<String, String> getCommandMetadata(@Nullable AgentAppStepState overlay) {
        return Map.of(JOB, buildJob(overlay).toString());
    }

    private static void putList(ObjectNode node, String key, @Nullable List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        ArrayNode array = node.putArray(key);
        values.forEach(array::add);
    }
}
