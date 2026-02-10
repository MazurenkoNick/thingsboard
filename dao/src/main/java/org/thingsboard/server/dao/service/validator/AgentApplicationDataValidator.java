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
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppTemplateDao;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.agent.StepLinkedListUtils;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.exception.DataValidationException;

@Component
@AllArgsConstructor
public class AgentApplicationDataValidator extends DataValidator<AgentApplication> {

    private final AgentService agentService;
    private final AgentApplicationDao agentApplicationDao;
    private final AgentAppTemplateDao agentAppTemplateDao;

    @Override
    protected AgentApplication validateUpdate(TenantId tenantId, AgentApplication agentApplication) {
        AgentApplication old = agentApplicationDao.findById(tenantId, agentApplication.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing agent application!");
        }
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, AgentApplication agentApplication) {
        if (agentApplication.getAgentId() == null) {
            throw new DataValidationException("Agent application should be assigned to agent!");
        }
        if (agentApplication.getAppType() == null) {
            throw new DataValidationException("Agent application type must not be null!");
        }
        if (agentApplication.getAppType() == AgentApplicationType.GENERIC) {
            if (agentApplication.getTemplateId() != null) {
                throw new DataValidationException("Generic agent application must not have a template!");
            }
        } else {
            validateTemplate(agentApplication);
        }
        if (CollectionUtils.isEmpty(agentApplication.getStartSteps())) {
            throw new DataValidationException("Agent install steps can't be null nor empty!");
        }
        Agent agent = agentService.findAgentById(tenantId, agentApplication.getAgentId());
        if (agent == null) {
            throw new DataValidationException("Agent application is referencing non-existent agent!");
        }
        if (!agent.getTenantId().equals(tenantId)) {
            throw new DataValidationException("Agent application cannot be assigned to agent from different tenant!");
        }
        if (agentApplication.getName() != null && agentApplication.getName().length() > 255) {
            throw new DataValidationException("Agent application name length must be equal or shorter than 255!");
        }
        validateSteps(agentApplication);
    }

    private void validateTemplate(AgentApplication agentApplication) {
        // todo: validate with template?
        if (agentApplication.getTemplateId() == null) {
            throw new DataValidationException("Agent application should be assigned to template!");
        }
        if (agentAppTemplateDao.findById(TenantId.SYS_TENANT_ID, agentApplication.getTemplateId().getId()) == null) {
            throw new DataValidationException("Agent application is referencing non-existent template!");
        }
    }

    private void validateSteps(AgentApplication agentApplication) {
        try {
            StepLinkedListUtils.validate(agentApplication.getStartSteps());
        } catch (IllegalStateException e) {
            throw new DataValidationException("Invalid install steps: " + e.getMessage());
        }
        try {
            if (!CollectionUtils.isEmpty(agentApplication.getUpdateSteps())) {
                StepLinkedListUtils.validate(agentApplication.getUpdateSteps());
            }
        } catch (IllegalStateException e) {
            throw new DataValidationException("Invalid update steps: " + e.getMessage());
        }
    }
}
