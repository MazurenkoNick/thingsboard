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

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppTemplateDao;
import org.thingsboard.server.dao.agent.StepLinkedListUtils;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.exception.DataValidationException;

@Component
public class AgentAppTemplateDataValidator extends DataValidator<AgentAppTemplate> {

    private final AgentAppTemplateDao agentAppTemplateDao;

    public AgentAppTemplateDataValidator(AgentAppTemplateDao agentAppTemplateDao) {
        this.agentAppTemplateDao = agentAppTemplateDao;
    }

    @Override
    protected AgentAppTemplate validateUpdate(TenantId tenantId, AgentAppTemplate template) {
        AgentAppTemplate old = agentAppTemplateDao.findById(tenantId, template.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing agent app template!");
        }
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, AgentAppTemplate template) {
        if (template.getAppType() == null) {
            throw new DataValidationException("Template app type should be specified!");
        }
        if (template.getCurrentVersion() == null || template.getCurrentVersion().isBlank()) {
            throw new DataValidationException("Template current version should be specified!");
        }
        if (template.getPreviousVersion() == null || template.getPreviousVersion().isBlank()) {
            throw new DataValidationException("Template previous version should be specified!");
        }
        validateSteps(template);
    }

    private void validateSteps(AgentAppTemplate template) {
        try {
            if (!CollectionUtils.isEmpty(template.getStartSteps())) {
                StepLinkedListUtils.validate(template.getStartSteps());
            }
        } catch (IllegalStateException e) {
            throw new DataValidationException("Invalid install steps: " + e.getMessage());
        }
        try {
            if (!CollectionUtils.isEmpty(template.getUpgradeSteps())) {
                StepLinkedListUtils.validate(template.getUpgradeSteps());
            }
        } catch (IllegalStateException e) {
            throw new DataValidationException("Invalid upgrade steps: " + e.getMessage());
        }
    }
}
