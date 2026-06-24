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

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppUnitDao;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.exception.DataValidationException;

@Component
@AllArgsConstructor
public class AgentAppUnitDataValidator extends DataValidator<AgentAppUnit> {

    private final AgentApplicationService agentApplicationService;
    private final AgentAppUnitDao agentAppUnitDao;

    @Override
    protected AgentAppUnit validateUpdate(TenantId tenantId, AgentAppUnit agentAppUnit) {
        AgentAppUnit old = agentAppUnitDao.findById(tenantId, agentAppUnit.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing agent app unit!");
        }
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, AgentAppUnit agentAppUnit) {
        if (agentAppUnit.getAgentApplicationId() == null) {
            throw new DataValidationException("Agent app unit should be assigned to agent application!");
        }
        AgentApplication application = agentApplicationService.findById(tenantId, agentAppUnit.getAgentApplicationId());
        if (application == null) {
            throw new DataValidationException("Agent app unit is referencing non-existent agent application!");
        }
        if (StringUtils.isBlank(agentAppUnit.getIdentifier())) {
            throw new DataValidationException("Agent app unit identifier is required!");
        }
        if (agentAppUnit.getType() == null) {
            throw new DataValidationException("Agent app unit type is required!");
        }
    }
}
