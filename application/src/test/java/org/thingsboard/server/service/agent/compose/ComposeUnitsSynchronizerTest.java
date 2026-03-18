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
package org.thingsboard.server.service.agent.compose;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppUnitService;
import org.thingsboard.server.gen.agent.v1.ContainerInfo;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComposeUnitsSynchronizerTest {

    @Mock
    private AgentAppUnitService unitService;
    @Mock
    private TelemetrySubscriptionService tsSubService;

    @InjectMocks
    private ComposeUnitsSynchronizer synchronizer;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentApplicationId APP_ID = new AgentApplicationId(UUID.randomUUID());

    // ==================== syncUnitsAndState ====================

    @Test
    void syncUnitsAndState_createsContainersVolumesNetworks() {
        JsonNode composeJson = JacksonUtil.toJsonNode("""
                {
                  "services": {"web": {}, "db": {}},
                  "volumes": {"data": {}},
                  "networks": {"frontend": {}}
                }
                """);

        when(unitService.findAgentAppUnitsByAgentAppId(TENANT_ID, APP_ID)).thenReturn(List.of());
        when(unitService.saveAgentAppUnit(eq(TENANT_ID), any())).thenAnswer(inv -> {
            AgentAppUnit u = inv.getArgument(1);
            u.setId(new AgentAppUnitId(UUID.randomUUID()));
            return u;
        });

        synchronizer.syncUnitsAndState(TENANT_ID, APP_ID, composeJson, Map.of());

        ArgumentCaptor<AgentAppUnit> captor = ArgumentCaptor.forClass(AgentAppUnit.class);
        verify(unitService, times(4)).saveAgentAppUnit(eq(TENANT_ID), captor.capture());

        List<AgentAppUnit> saved = captor.getAllValues();
        assertThat(saved).extracting(AgentAppUnit::getIdentifier)
                .containsExactlyInAnyOrder("web", "db", "data", "frontend");
        assertThat(saved).extracting(AgentAppUnit::getType)
                .containsExactlyInAnyOrder(
                        AgentAppUnitType.CONTAINER, AgentAppUnitType.CONTAINER,
                        AgentAppUnitType.VOLUME, AgentAppUnitType.NETWORK);
    }

    @Test
    void syncUnitsAndState_keepsExistingUnits_createsNewOnes_deletesStale() {
        JsonNode composeJson = JacksonUtil.toJsonNode("""
                {
                  "services": {"web": {}, "new-svc": {}},
                  "volumes": {"data": {}}
                }
                """);

        AgentAppUnit existingWeb = newUnit("web", AgentAppUnitType.CONTAINER);
        AgentAppUnit staleOld = newUnit("old-svc", AgentAppUnitType.CONTAINER);

        when(unitService.findAgentAppUnitsByAgentAppId(TENANT_ID, APP_ID))
                .thenReturn(List.of(existingWeb, staleOld));
        when(unitService.saveAgentAppUnit(eq(TENANT_ID), any())).thenAnswer(inv -> {
            AgentAppUnit u = inv.getArgument(1);
            u.setId(new AgentAppUnitId(UUID.randomUUID()));
            return u;
        });

        synchronizer.syncUnitsAndState(TENANT_ID, APP_ID, composeJson, Map.of());

        // new-svc and data should be created
        ArgumentCaptor<AgentAppUnit> saveCaptor = ArgumentCaptor.forClass(AgentAppUnit.class);
        verify(unitService, times(2)).saveAgentAppUnit(eq(TENANT_ID), saveCaptor.capture());
        assertThat(saveCaptor.getAllValues()).extracting(AgentAppUnit::getIdentifier)
                .containsExactlyInAnyOrder("new-svc", "data");

        // old-svc should be deleted
        verify(unitService).deleteAgentAppUnit(TENANT_ID, staleOld.getId());

        // web should not be saved or deleted
        assertThat(saveCaptor.getAllValues()).extracting(AgentAppUnit::getIdentifier)
                .doesNotContain("web");
    }

    @Test
    void syncUnitsAndState_savesStateAttributesForContainers() {
        JsonNode composeJson = JacksonUtil.toJsonNode("""
                {
                  "services": {"web": {}, "db": {}},
                  "volumes": {"data": {}}
                }
                """);

        AgentAppUnit web = newUnit("web", AgentAppUnitType.CONTAINER);
        AgentAppUnit db = newUnit("db", AgentAppUnitType.CONTAINER);
        AgentAppUnit data = newUnit("data", AgentAppUnitType.VOLUME);

        when(unitService.findAgentAppUnitsByAgentAppId(TENANT_ID, APP_ID))
                .thenReturn(List.of(web, db, data));

        Map<String, ContainerInfo> containerStates = Map.of(
                "web", containerInfo("running", "sha256:aaa"),
                "db", containerInfo("exited", "sha256:bbb"));

        synchronizer.syncUnitsAndState(TENANT_ID, APP_ID, composeJson, containerStates);

        ArgumentCaptor<AttributesSaveRequest> attrCaptor = ArgumentCaptor.forClass(AttributesSaveRequest.class);
        verify(tsSubService, times(2)).saveAttributes(attrCaptor.capture());

        List<AttributesSaveRequest> requests = attrCaptor.getAllValues();
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
    void syncUnitsAndState_savesImageAttributesFromComposeJson() {
        JsonNode composeJson = JacksonUtil.toJsonNode("""
                {
                  "services": {
                    "web": {"image": "nginx:latest"},
                    "db": {"image": "postgres:15"}
                  }
                }
                """);

        AgentAppUnit web = newUnit("web", AgentAppUnitType.CONTAINER);
        AgentAppUnit db = newUnit("db", AgentAppUnitType.CONTAINER);

        when(unitService.findAgentAppUnitsByAgentAppId(TENANT_ID, APP_ID))
                .thenReturn(List.of(web, db));

        synchronizer.syncUnitsAndState(TENANT_ID, APP_ID, composeJson, Map.of());

        ArgumentCaptor<AttributesSaveRequest> attrCaptor = ArgumentCaptor.forClass(AttributesSaveRequest.class);
        // 2 image attribute saves (no state saves since containerStates is empty)
        verify(tsSubService, times(2)).saveAttributes(attrCaptor.capture());

        List<AttributesSaveRequest> requests = attrCaptor.getAllValues();
        assertThat(requests).allSatisfy(req -> {
            assertThat(req.getEntries().get(0).getKey()).isEqualTo("image");
        });

        assertThat(requests).anySatisfy(req -> {
            assertThat(req.getEntityId()).isEqualTo(web.getId());
            assertThat(req.getEntries().get(0).getValueAsString()).isEqualTo("nginx:latest");
        });
        assertThat(requests).anySatisfy(req -> {
            assertThat(req.getEntityId()).isEqualTo(db.getId());
            assertThat(req.getEntries().get(0).getValueAsString()).isEqualTo("postgres:15");
        });
    }

    // ==================== syncState ====================

    @Test
    void syncState_savesAttributesOnExistingUnits() {
        AgentAppUnit web = newUnit("web", AgentAppUnitType.CONTAINER);
        AgentAppUnit db = newUnit("db", AgentAppUnitType.CONTAINER);

        when(unitService.findAgentAppUnitsByAgentAppId(TENANT_ID, APP_ID))
                .thenReturn(List.of(web, db));

        Map<String, ContainerInfo> containerStates = Map.of(
                "web", containerInfo("running", "sha256:aaa"),
                "db", containerInfo("restarting", "sha256:bbb"));

        synchronizer.syncState(TENANT_ID, APP_ID, containerStates);

        verify(tsSubService, times(2)).saveAttributes(any(AttributesSaveRequest.class));
        verify(unitService, never()).saveAgentAppUnit(any(), any());
        verify(unitService, never()).deleteAgentAppUnit(any(), any());
    }

    @Test
    void syncState_skipsUnknownContainers() {
        AgentAppUnit web = newUnit("web", AgentAppUnitType.CONTAINER);

        when(unitService.findAgentAppUnitsByAgentAppId(TENANT_ID, APP_ID))
                .thenReturn(List.of(web));

        Map<String, ContainerInfo> containerStates = Map.of(
                "web", containerInfo("running", "sha256:aaa"),
                "unknown", containerInfo("exited", "sha256:bbb"));

        synchronizer.syncState(TENANT_ID, APP_ID, containerStates);

        ArgumentCaptor<AttributesSaveRequest> captor = ArgumentCaptor.forClass(AttributesSaveRequest.class);
        verify(tsSubService, times(1)).saveAttributes(captor.capture());
        assertThat(captor.getValue().getEntityId()).isEqualTo(web.getId());
    }

    @Test
    void syncState_emptyContainerStates_doesNothing() {
        synchronizer.syncState(TENANT_ID, APP_ID, Collections.emptyMap());

        verify(unitService, never()).findAgentAppUnitsByAgentAppId(any(), any());
        verify(tsSubService, never()).saveAttributes(any(AttributesSaveRequest.class));
    }

    // ==================== Helpers ====================

    private AgentAppUnit newUnit(String identifier, AgentAppUnitType type) {
        AgentAppUnit unit = new AgentAppUnit(new AgentAppUnitId(UUID.randomUUID()));
        unit.setAgentApplicationId(APP_ID);
        unit.setIdentifier(identifier);
        unit.setType(type);
        return unit;
    }

    private static ContainerInfo containerInfo(String state, String imageDigest) {
        return ContainerInfo.newBuilder()
                .setState(state)
                .setImageDigest(imageDigest)
                .build();
    }
}
