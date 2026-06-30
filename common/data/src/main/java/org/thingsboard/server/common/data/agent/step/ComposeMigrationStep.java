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
public class ComposeMigrationStep extends StatefulStep<ComposeMigrationStepState> {

    private List<ServiceOverride> serviceOverrides;
    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    private ComposeMigrationStepState state;

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
                    && imageMatches(service.get("image").asText(), override.getServiceImageName())) {
                if (!CollectionUtils.isEmpty(override.getProperties())) {
                    override.getProperties().forEach(((ObjectNode) service)::put);
                }
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalStateException("Service with image '" + override.getServiceImageName() + "' not found in compose");
        }
    }

    private boolean imageMatches(String image, String serviceImageName) {
        if (image.equals(serviceImageName)) {
            return true;
        }
        if (!image.startsWith(serviceImageName)) {
            return false;
        }
        char boundary = image.charAt(serviceImageName.length());
        return boundary == ':' || boundary == '@';
    }

    @Data
    @NoArgsConstructor
    public static class ServiceOverride {
        private String serviceImageName;
        private Map<String, String> properties;
    }
}
