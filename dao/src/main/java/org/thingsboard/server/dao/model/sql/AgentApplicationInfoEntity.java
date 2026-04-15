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
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;

@Data
@EqualsAndHashCode(callSuper = true)
public class AgentApplicationInfoEntity extends AgentApplicationEntity {

    private String currentVersion;
    private String nextVersion;
    private Long profileVersion;
    private String profileName;

    public AgentApplicationInfoEntity() {
        super();
    }

    public AgentApplicationInfoEntity(AgentApplicationEntity entity,
                                      String currentVersion,
                                      String nextVersion,
                                      Long profileVersion,
                                      String profileName) {
        super(entity.toData());
        this.id = entity.getId();
        this.createdTime = entity.getCreatedTime();
        this.version = entity.getVersion();
        this.currentVersion = currentVersion;
        this.nextVersion = nextVersion;
        this.profileVersion = profileVersion;
        this.profileName = profileName;
    }

    @Override
    public AgentApplicationInfo toData() {
        AgentApplicationInfo info = new AgentApplicationInfo(super.toData(), currentVersion, nextVersion);
        info.setProfileConfigOutdated(profileVersion != null
                && !profileVersion.equals(info.getProfileConfigVersion()));
        info.setProfileName(profileName);
        return info;
    }

}
