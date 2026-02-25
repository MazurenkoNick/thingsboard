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
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventDeliveryState;
import org.thingsboard.server.common.data.agent.RollbackEventMeta;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.exception.DataValidationException;

@Component
@AllArgsConstructor
public class AgentAppEventDataValidator extends DataValidator<AgentAppEvent> {

    private final AgentApplicationDao agentApplicationDao;

    @Override
    protected void validateDataImpl(TenantId tenantId, AgentAppEvent event) {
        if (event.getTenantId() == null) {
            throw new DataValidationException("Agent app event tenantId must not be null!");
        }
        if (event.getApplicationId() == null) {
            throw new DataValidationException("Agent app event applicationId must not be null!");
        }
        if (event.getActionType() == null) {
            throw new DataValidationException("Agent app event actionType must not be null!");
        }
        if (event.getDeliveryState() == null) {
            throw new DataValidationException("Agent app event deliveryState must not be null!");
        }
        if (event.getDeliveryState() != AgentAppEventDeliveryState.PENDING && event.getStatus() == null) {
            throw new DataValidationException("Agent app event status must not be null!");
        }
        if (agentApplicationDao.findById(tenantId, event.getApplicationId().getId()) == null) {
            throw new DataValidationException("Agent app event references non-existent application!");
        }
        if (event.getActionType() == AgentAppEventActionType.ROLLBACK) {
            if (!(event.getMetadata() instanceof RollbackEventMeta meta) || meta.getFailedEventId() == null) {
                throw new DataValidationException("Rollback event must have failedEventId in metadata!");
            }
        }
    }

}
