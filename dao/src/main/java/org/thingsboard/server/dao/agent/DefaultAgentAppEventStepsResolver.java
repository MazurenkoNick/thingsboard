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
package org.thingsboard.server.dao.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;
import java.util.function.Predicate;

@Component
@RequiredArgsConstructor
public class DefaultAgentAppEventStepsResolver implements AgentAppEventStepsResolver {

    private final AgentAppTemplateService templateService;

    @Override
    public List<AgentAppStep> resolveSteps(AgentApplication app, AgentAppEventActionType actionType) {
        AgentAppTemplateId templateId = resolveTemplateId(app, actionType);
        AgentAppTemplate template = templateService.findById(TenantId.SYS_TENANT_ID, templateId);
        if (template == null) {
            throw new IllegalStateException("Template not found for application " + app.getId());
        }
        List<AgentAppStep> steps = switch (actionType) {
            case INSTALL, RESTART, UPDATE -> template.getStartSteps();
            case UPGRADE -> template.getUpgradeSteps();
            case DELETE -> template.getDeleteSteps();
            case ROLLBACK -> template.getRollbackSteps();
        };
        if (steps == null || steps.isEmpty()) {
            throw new IllegalStateException("No steps resolved for application " + app.getId() + " and action " + actionType);
        }
        Predicate<AgentAppStep> nonTemplateOnly = s -> !s.isTemplateOnly();
        return StepLinkedListUtils.filter(steps, nonTemplateOnly);
    }

    private AgentAppTemplateId resolveTemplateId(AgentApplication app, AgentAppEventActionType actionType) {
        if ((actionType == AgentAppEventActionType.UPGRADE || actionType == AgentAppEventActionType.ROLLBACK)
                && app.getDesiredTemplateId() != null) {
            return app.getDesiredTemplateId();
        }
        return app.getTemplateId();
    }
}
