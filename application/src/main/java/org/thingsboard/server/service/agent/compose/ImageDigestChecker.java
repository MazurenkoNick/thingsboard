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
package org.thingsboard.server.service.agent.compose;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FutureCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.RegexUtils;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.dao.agent.AgentAppTemplateService;
import org.thingsboard.server.gen.agent.v1.ContainerInfo;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageDigestChecker {

    private static final String PULL_REQUIRED_ATTR_KEY = "pullRequired";

    private final AgentAppTemplateService templateService;
    private final TelemetrySubscriptionService tsSubService;

    public void checkImageDigest(TenantId tenantId, AgentApplication app,
                                  Map<String, ContainerInfo> containerStates, JsonNode composeJson) {
        Pattern mainImagePattern = app.getAppType().getMainImagePattern();
        if (mainImagePattern == null) {
            return;
        }
        if (app.getTemplateId() == null) {
            return;
        }
        AgentAppTemplate template = templateService.findById(tenantId, app.getTemplateId());
        if (template == null) {
            return;
        }
        String templateImageDigest = getTemplateImageDigest(template);
        if (StringUtils.isBlank(templateImageDigest)) {
            return;
        }
        String mainServiceName = findMainServiceName(app, mainImagePattern, composeJson);
        if (mainServiceName == null) {
            log.trace("[{}] Could not find main service for app [{}] with pattern [{}]",
                    tenantId, app.getId(), mainImagePattern);
            return;
        }
        ContainerInfo containerInfo = containerStates.get(mainServiceName);
        if (containerInfo == null) {
            log.trace("[{}] No container state for main service [{}] of app [{}]",
                    tenantId, mainServiceName, app.getId());
            return;
        }
        String containerImageDigest = containerInfo.getImageDigest();
        if (StringUtils.isBlank(containerImageDigest)) {
            return;
        }
        boolean pullRequired = !templateImageDigest.equals(containerImageDigest);
        log.trace("[{}] Image digest check for app [{}]: template={}, container={}, pullRequired={}",
                tenantId, app.getId(), templateImageDigest, containerImageDigest, pullRequired);

        savePullRequiredAttribute(tenantId, app, pullRequired);
    }

    private String getTemplateImageDigest(AgentAppTemplate template) {
        if (template.getConfig() instanceof DockerComposeConfig dockerComposeConfig) {
            return dockerComposeConfig.getImageDigest();
        }
        return null;
    }

    private String findMainServiceName(AgentApplication app, Pattern mainImagePattern, JsonNode composeJson) {
        if (composeJson != null) {
            return findMainServiceNameFromComposeJson(mainImagePattern, composeJson);
        }
        // Fall back to the compose definition stored on the application's config
        if (app.getConfig() instanceof DockerComposeConfig c && c.getCompose() != null) {
            return findMainServiceNameFromComposeJson(mainImagePattern, c.getCompose());
        }
        return null;
    }

    private String findMainServiceNameFromComposeJson(Pattern mainImagePattern, JsonNode composeJson) {
        JsonNode services = composeJson.get("services");
        if (services == null || !services.isObject()) {
            return null;
        }
        Iterator<Map.Entry<String, JsonNode>> it = services.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            JsonNode service = entry.getValue();
            if (service.isObject() && service.has("image")) {
                String image = service.get("image").asText();
                if (RegexUtils.matches(image, mainImagePattern)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private void savePullRequiredAttribute(TenantId tenantId, AgentApplication app, boolean pullRequired) {
        tsSubService.saveAttributes(AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(app.getId())
                .scope(AttributeScope.SERVER_SCOPE)
                .entry(new BooleanDataEntry(PULL_REQUIRED_ATTR_KEY, pullRequired))
                .callback(new FutureCallback<>() {
                    @Override
                    public void onSuccess(Void result) {
                        log.trace("[{}] Saved pullRequired={} for app [{}]", tenantId, pullRequired, app.getId());
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.warn("[{}] Failed to save pullRequired for app [{}]", tenantId, app.getId(), t);
                    }
                })
                .build());
    }
}
