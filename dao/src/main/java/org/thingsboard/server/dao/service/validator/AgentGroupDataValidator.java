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
import org.thingsboard.server.common.data.agent.AgentGroup;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentGroupDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

@Component
@AllArgsConstructor
public class AgentGroupDataValidator extends DataValidator<AgentGroup> {

    private final AgentGroupDao groupDao;
    private final TenantService tenantService;

    @Override
    protected AgentGroup validateUpdate(TenantId tenantId, AgentGroup group) {
        AgentGroup old = groupDao.findById(group.getTenantId(), group.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing agent group!");
        }
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, AgentGroup group) {
        validateString("Agent group name", group.getName());
        if (group.getTenantId() == null) {
            throw new DataValidationException("Agent group should be assigned to tenant!");
        }
        if (!tenantService.tenantExists(group.getTenantId())) {
            throw new DataValidationException("Agent group is referencing to non-existent tenant!");
        }
    }
}
