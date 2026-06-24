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
        return agentAppTemplateDao.findById(TenantId.SYS_TENANT_ID, template.getId().getId());
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, AgentAppTemplate template) {
        if (template.getAppType() == null) {
            throw new DataValidationException("Template app type should be specified!");
        }
        if (template.getCurrentVersion() == null || template.getCurrentVersion().isBlank()) {
            throw new DataValidationException("Template current version should be specified!");
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
