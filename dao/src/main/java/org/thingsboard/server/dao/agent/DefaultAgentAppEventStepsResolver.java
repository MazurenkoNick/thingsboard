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
            case INSTALL, UPDATE -> template.getStartSteps();
            case UPGRADE -> template.getUpgradeSteps();
            case DELETE -> template.getDeleteSteps();
            case ROLLBACK -> template.getRollbackSteps();
            case RESTART -> template.getRestartSteps();
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
