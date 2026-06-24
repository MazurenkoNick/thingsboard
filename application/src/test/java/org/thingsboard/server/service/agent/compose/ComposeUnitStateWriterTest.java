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
package org.thingsboard.server.service.agent.compose;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.agent.v1.ContainerInfo;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ComposeUnitStateWriterTest {

    @Mock
    private TelemetrySubscriptionService tsSubService;

    @InjectMocks
    private ComposeUnitStateWriter writer;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentApplicationId APP_ID = new AgentApplicationId(UUID.randomUUID());

    @Test
    void writeStates_savesStateAttributeForKnownContainers() {
        AgentAppUnit web = newUnit("web");
        AgentAppUnit db = newUnit("db");
        Map<AgentAppUnitKey, AgentAppUnit> units = Map.of(
                new AgentAppUnitKey(AgentAppUnitType.CONTAINER, "web"), web,
                new AgentAppUnitKey(AgentAppUnitType.CONTAINER, "db"), db);

        Map<String, ContainerInfo> containerStates = Map.of(
                "web", containerInfo("running"),
                "db", containerInfo("exited"));

        writer.writeStates(TENANT_ID, units, containerStates);

        ArgumentCaptor<AttributesSaveRequest> captor = ArgumentCaptor.forClass(AttributesSaveRequest.class);
        verify(tsSubService, times(2)).saveAttributes(captor.capture());

        List<AttributesSaveRequest> requests = captor.getAllValues();
        assertThat(requests).allSatisfy(req -> {
            assertThat(req.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(req.getScope()).isEqualTo(AttributeScope.SERVER_SCOPE);
            assertThat(req.getEntries()).hasSize(1);
            assertThat(req.getEntries().get(0).getKey()).isEqualTo("state");
        });
        assertThat(requests).anySatisfy(req -> {
            assertThat(req.getEntityId()).isEqualTo(web.getId());
            assertThat(req.getEntries().get(0).getValueAsString()).isEqualTo("running");
        });
        assertThat(requests).anySatisfy(req -> {
            assertThat(req.getEntityId()).isEqualTo(db.getId());
            assertThat(req.getEntries().get(0).getValueAsString()).isEqualTo("exited");
        });
    }

    @Test
    void writeStates_skipsUnknownContainers() {
        AgentAppUnit web = newUnit("web");
        Map<AgentAppUnitKey, AgentAppUnit> units =
                Map.of(new AgentAppUnitKey(AgentAppUnitType.CONTAINER, "web"), web);

        Map<String, ContainerInfo> containerStates = Map.of(
                "web", containerInfo("running"),
                "unknown", containerInfo("exited"));

        writer.writeStates(TENANT_ID, units, containerStates);

        ArgumentCaptor<AttributesSaveRequest> captor = ArgumentCaptor.forClass(AttributesSaveRequest.class);
        verify(tsSubService, times(1)).saveAttributes(captor.capture());
        assertThat(captor.getValue().getEntityId()).isEqualTo(web.getId());
    }

    @Test
    void writeStates_emptyContainerStates_doesNothing() {
        writer.writeStates(TENANT_ID, Map.of(), Map.of());
        verify(tsSubService, never()).saveAttributes(any());
    }

    private AgentAppUnit newUnit(String identifier) {
        AgentAppUnit unit = new AgentAppUnit(new AgentAppUnitId(UUID.randomUUID()));
        unit.setAgentApplicationId(APP_ID);
        unit.setIdentifier(identifier);
        unit.setType(AgentAppUnitType.CONTAINER);
        return unit;
    }

    private static ContainerInfo containerInfo(String state) {
        return ContainerInfo.newBuilder().setState(state).build();
    }
}
