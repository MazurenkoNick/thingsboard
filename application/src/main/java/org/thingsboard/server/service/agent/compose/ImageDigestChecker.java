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
package org.thingsboard.server.service.agent.compose;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FutureCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.RegexUtils;
import org.thingsboard.server.queue.util.TbCoreComponent;
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
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class ImageDigestChecker {

    private static final String PULL_REQUIRED_ATTR_KEY = "pullRequired";

    private final AgentAppTemplateService templateService;
    private final TelemetrySubscriptionService tsSubService;

    public void checkImageDigest(TenantId tenantId, AgentApplication app,
                                  Map<String, ContainerInfo> containerStates, JsonNode externalComposeJson) {
        if (containerStates.isEmpty()) {
            return;
        }
        Pattern mainImagePattern = app.getAppType().getMainImagePattern();
        if (mainImagePattern == null || app.getTemplateId() == null) {
            return;
        }
        AgentAppTemplate template = templateService.findById(tenantId, app.getTemplateId());
        if (template == null) {
            return;
        }
        String templateImageDigest = template.getImageDigest();
        if (StringUtils.isBlank(templateImageDigest)) {
            return;
        }
        String mainServiceName = findMainServiceName(app, mainImagePattern, externalComposeJson);
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

    private String findMainServiceName(AgentApplication app, Pattern mainImagePattern, JsonNode externalComposeJson) {
        if (externalComposeJson != null) {
            return findMainServiceNameFromComposeJson(mainImagePattern, externalComposeJson);
        }
        // Use compose definition of app's config in case the externalComposeJson hasn't been sent, meaning it's unchanged since the last synchronization
        if (app.getConfig() instanceof DockerComposeConfig appConfig && appConfig.getCompose() != null) {
            return findMainServiceNameFromComposeJson(mainImagePattern, appConfig.getCompose());
        }
        return null;
    }

    private String findMainServiceNameFromComposeJson(Pattern mainImagePattern, JsonNode externalComposeJson) {
        JsonNode services = externalComposeJson.get("services");
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
