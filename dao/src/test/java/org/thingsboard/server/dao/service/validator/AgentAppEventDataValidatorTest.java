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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventDeliveryState;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.RollbackEventMeta;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.exception.DataValidationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentAppEventDataValidatorTest {

    @Mock
    private AgentApplicationDao agentApplicationDao;

    @InjectMocks
    private AgentAppEventDataValidator validator;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentApplicationId APP_ID = new AgentApplicationId(UUID.randomUUID());

    @Test
    void validate_nullTenantId_throws() {
        AgentAppEvent event = validEvent();
        event.setTenantId(null);

        assertThatThrownBy(() -> validator.validate(event, AgentAppEvent::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    void validate_nullApplicationId_throws() {
        AgentAppEvent event = validEvent();
        event.setApplicationId(null);

        assertThatThrownBy(() -> validator.validate(event, AgentAppEvent::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("applicationId");
    }

    @Test
    void validate_nullActionType_throws() {
        AgentAppEvent event = validEvent();
        event.setActionType(null);

        assertThatThrownBy(() -> validator.validate(event, AgentAppEvent::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("actionType");
    }

    @Test
    void validate_nullDeliveryState_throws() {
        AgentAppEvent event = validEvent();
        event.setDeliveryState(null);

        assertThatThrownBy(() -> validator.validate(event, AgentAppEvent::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("deliveryState");
    }

    @Test
    void validate_deliveredWithNullStatus_throws() {
        AgentAppEvent event = validEvent();
        event.setDeliveryState(AgentAppEventDeliveryState.DELIVERED);
        event.setStatus(null);

        assertThatThrownBy(() -> validator.validate(event, AgentAppEvent::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("status");
    }

    @Test
    void validate_pendingWithNullStatus_doesNotThrowForStatus() {
        AgentAppEvent event = validEvent();
        event.setDeliveryState(AgentAppEventDeliveryState.PENDING);
        event.setStatus(null);
        when(agentApplicationDao.findById(any(), eq(APP_ID.getId()))).thenReturn(new AgentApplication());

        // Should not throw - status is not set when deliveryStatus = PENDING
        validator.validate(event, AgentAppEvent::getTenantId);
    }

    @Test
    void validate_nonExistentApplication_throws() {
        AgentAppEvent event = validEvent();
        when(agentApplicationDao.findById(any(), eq(APP_ID.getId()))).thenReturn(null);

        assertThatThrownBy(() -> validator.validate(event, AgentAppEvent::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("non-existent application");
    }

    @Test
    void validate_rollbackWithoutMetadata_throws() {
        AgentAppEvent event = validEvent();
        event.setActionType(AgentAppEventActionType.ROLLBACK);
        event.setMetadata(null);
        when(agentApplicationDao.findById(any(), eq(APP_ID.getId()))).thenReturn(new AgentApplication());

        assertThatThrownBy(() -> validator.validate(event, AgentAppEvent::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("failedEventId");
    }

    @Test
    void validate_rollbackWithNullFailedEventId_throws() {
        AgentAppEvent event = validEvent();
        event.setActionType(AgentAppEventActionType.ROLLBACK);
        event.setMetadata(new RollbackEventMeta());
        when(agentApplicationDao.findById(any(), eq(APP_ID.getId()))).thenReturn(new AgentApplication());

        assertThatThrownBy(() -> validator.validate(event, AgentAppEvent::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("failedEventId");
    }

    @Test
    void validate_rollbackWithFailedEventId_passes() {
        AgentAppEvent event = validEvent();
        event.setActionType(AgentAppEventActionType.ROLLBACK);
        event.setMetadata(new RollbackEventMeta(new AgentAppEventId(UUID.randomUUID())));
        when(agentApplicationDao.findById(any(), eq(APP_ID.getId()))).thenReturn(new AgentApplication());

        validator.validate(event, AgentAppEvent::getTenantId);
    }

    @Test
    void validate_validInstallEvent_passes() {
        AgentAppEvent event = validEvent();
        when(agentApplicationDao.findById(any(), eq(APP_ID.getId()))).thenReturn(new AgentApplication());

        validator.validate(event, AgentAppEvent::getTenantId);
    }

    private AgentAppEvent validEvent() {
        AgentAppEvent event = new AgentAppEvent();
        event.setTenantId(TENANT_ID);
        event.setApplicationId(APP_ID);
        event.setActionType(AgentAppEventActionType.INSTALL);
        event.setDeliveryState(AgentAppEventDeliveryState.PENDING);
        event.setStatus(AgentAppEventStatus.QUEUED);
        return event;
    }

}
