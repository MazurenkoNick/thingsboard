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
package org.thingsboard.server.dao.agent.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppProfileService;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProfileConfigResolver {

    private final AgentAppProfileService profileService;

    public AgentAppProfile resolve(TenantId tenantId, AgentApplication application) {
        if (application.getApplicationProfileId() == null) {
            return null;
        }
        AgentAppProfile profile = profileService.findProfileById(tenantId, application.getApplicationProfileId());
        if (profile == null || profile.getConfig() == null) {
            throw new IllegalArgumentException("Couldn't find profile with id: " + application.getApplicationProfileId());
        }
        Map<String, String> incomingCreds = extractCredEnvVars(application);
        AgentAppConfig resolved = profile.getConfig().copy();
        application.setConfig(resolved);
        application.setProfileConfigVersion(profile.getVersion());
        applyCredEnvVars(application, incomingCreds);

        return profile;
    }

    private Map<String, String> extractCredEnvVars(AgentApplication app) {
        List<String> keys = credKeysFor(app);
        if (keys.isEmpty() || !(app.getConfig() instanceof DockerComposeConfig compose) || compose.getCompose() == null) {
            return Map.of();
        }
        var imagePattern = app.getAppType().getMainImagePattern();
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : keys) {
            String value = DockerComposeUtils.getEnvVariable(compose.getCompose(), imagePattern, key);
            if (value != null) {
                values.put(key, value);
            }
        }
        return values;
    }

    private void applyCredEnvVars(AgentApplication app, Map<String, String> creds) {
        if (creds.isEmpty() || !(app.getConfig() instanceof DockerComposeConfig compose) || compose.getCompose() == null) {
            return;
        }
        DockerComposeUtils.setEnvVariables(compose.getCompose(), app.getAppType().getMainImagePattern(), creds);
    }

    private List<String> credKeysFor(AgentApplication app) {
        return app.getAppType() != null ? app.getAppType().getCredentialEnvKeys() : Collections.emptyList();
    }
}
