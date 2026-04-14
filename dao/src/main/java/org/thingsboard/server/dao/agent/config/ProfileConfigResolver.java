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
