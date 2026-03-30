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
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppProfileDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

@Component
@AllArgsConstructor
public class AgentAppProfileDataValidator extends DataValidator<AgentAppProfile> {

    private final AgentAppProfileDao profileDao;
    private final TenantService tenantService;

    @Override
    protected AgentAppProfile validateUpdate(TenantId tenantId, AgentAppProfile profile) {
        AgentAppProfile old = profileDao.findById(profile.getTenantId(), profile.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing agent application profile!");
        }
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, AgentAppProfile profile) {
        validateString("Agent application profile name", profile.getName());
        if (profile.getAppType() == null) {
            throw new DataValidationException("Agent application profile app type must not be null!");
        }
        if (profile.getTemplateId() == null) {
            throw new DataValidationException("Agent application profile template must not be null!");
        }
        if (profile.getTenantId() == null) {
            throw new DataValidationException("Agent application profile should be assigned to tenant!");
        }
        if (profile.getConfig() == null) {
            throw new DataValidationException("Agent application config must not be null!");
        }
        profile.getConfig().validate();
        profile.getConfig().validateForProfile(profile.getAppType());
        if (!tenantService.tenantExists(profile.getTenantId())) {
            throw new DataValidationException("Agent application profile is referencing to non-existent tenant!");
        }
    }
}
