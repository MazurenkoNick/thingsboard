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
package org.thingsboard.server.common.data.agent;

import lombok.Getter;
import org.thingsboard.server.common.data.EntityType;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public enum AgentApplicationType {
    GENERIC(null, "1.0.0", null, Collections.emptyList()),
    EDGE("thingsboard/tb-edge:.+", null, EntityType.EDGE,
            List.of("CLOUD_ROUTING_KEY", "CLOUD_ROUTING_SECRET")),
    GATEWAY("thingsboard/tb-gateway:.+", null, EntityType.DEVICE,
            List.of("TB_GW_SECURITY_TYPE", "TB_GW_ACCESS_TOKEN", "TB_GW_CLIENT_ID", "TB_GW_USERNAME", "TB_GW_PASSWORD"));

    @Getter
    private final Pattern mainImagePattern;
    @Getter
    private final String defaultVersion;
    @Getter
    private final EntityType relatedEntityType;
    @Getter
    private final List<String> credentialEnvKeys;

    AgentApplicationType(String mainImageRegex, String defaultVersion, EntityType relatedEntityType,
                         List<String> credentialEnvKeys) {
        this.mainImagePattern = mainImageRegex != null ? Pattern.compile(mainImageRegex) : null;
        this.defaultVersion = defaultVersion;
        this.relatedEntityType = relatedEntityType;
        this.credentialEnvKeys = credentialEnvKeys;
    }
}
