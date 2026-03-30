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
import com.google.common.util.concurrent.FutureCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.dao.agent.AgentAppUnitService;
import org.thingsboard.server.gen.agent.v1.ContainerInfo;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ComposeUnitsSynchronizer {

    private static final String STATE_ATTR_KEY = "state";
    private static final String IMAGE_ATTR_KEY = "image";

    private final AgentAppUnitService unitService;
    private final TelemetrySubscriptionService tsSubService;

    public void syncUnitsAndState(TenantId tenantId, AgentApplicationId appId,
                                   JsonNode externalComposeJson, Map<String, ContainerInfo> containerStates) {
        log.trace("[{}] Syncing units and state for app [{}]", tenantId, appId);
        Map<String, AgentAppUnit> unitsByIdentifier = syncUnits(tenantId, appId, externalComposeJson);
        saveContainerStateAttributes(tenantId, unitsByIdentifier, containerStates);
        saveImageAttributes(tenantId, unitsByIdentifier, externalComposeJson);
    }

    public void syncState(TenantId tenantId, AgentApplicationId appId, Map<String, ContainerInfo> containerStates) {
        if (containerStates.isEmpty()) {
            log.trace("[{}] No container states to sync for app [{}]", tenantId, appId);
            return;
        }
        log.trace("[{}] Syncing state only for app [{}], containers: {}", tenantId, appId, containerStates.keySet());
        List<AgentAppUnit> existingUnits = unitService.findAgentAppUnitsByAgentAppId(tenantId, appId);
        Map<String, AgentAppUnit> unitsByIdentifier = existingUnits.stream()
                .collect(collectByIdentifier());
        saveContainerStateAttributes(tenantId, unitsByIdentifier, containerStates);
    }

    private Map<String, AgentAppUnit> syncUnits(TenantId tenantId, AgentApplicationId appId, JsonNode externalComposeJson) {
        Map<String, AgentAppUnitType> desiredUnits = parseDesiredUnits(externalComposeJson);
        log.trace("[{}] Desired units for app [{}]: {}", tenantId, appId, desiredUnits);

        Map<String, AgentAppUnit> existingByIdentifier = unitService.findAgentAppUnitsByAgentAppId(tenantId, appId)
                .stream()
                .collect(collectByIdentifier());
        log.trace("[{}] Existing units for app [{}]: {}", tenantId, appId, existingByIdentifier.keySet());

        Map<String, AgentAppUnit> result = new LinkedHashMap<>();

        for (var entry : desiredUnits.entrySet()) {
            String identifier = entry.getKey();
            AgentAppUnitType type = entry.getValue();
            AgentAppUnit existing = existingByIdentifier.remove(identifier);
            if (existing != null) {
                result.put(identifier, existing);
            } else {
                AgentAppUnit unit = new AgentAppUnit();
                unit.setAgentApplicationId(appId);
                unit.setIdentifier(identifier);
                unit.setType(type);
                AgentAppUnit saved = unitService.saveAgentAppUnit(tenantId, unit);
                log.info("[{}] Created agent app unit [{}] ({}) for app [{}]", tenantId, identifier, type, appId);
                result.put(identifier, saved);
            }
        }

        for (AgentAppUnit stale : existingByIdentifier.values()) {
            log.info("[{}] Removing stale agent app unit [{}] ({}) from app [{}]", tenantId, stale.getIdentifier(), stale.getType(), appId);
            unitService.deleteAgentAppUnit(tenantId, stale.getId());
        }

        return result;
    }

    private Map<String, AgentAppUnitType> parseDesiredUnits(JsonNode externalComposeJson) {
        Map<String, AgentAppUnitType> units = new LinkedHashMap<>();
        collectKeys(externalComposeJson, "services", AgentAppUnitType.CONTAINER, units);
        collectKeys(externalComposeJson, "volumes", AgentAppUnitType.VOLUME, units);
        collectKeys(externalComposeJson, "networks", AgentAppUnitType.NETWORK, units);
        return units;
    }

    private void collectKeys(JsonNode externalComposeJson, String section, AgentAppUnitType type, Map<String, AgentAppUnitType> units) {
        JsonNode node = externalComposeJson.get(section);
        if (node != null && node.isObject()) {
            node.fieldNames().forEachRemaining(name -> units.put(name, type));
        }
    }

    private void saveContainerStateAttributes(TenantId tenantId, Map<String, AgentAppUnit> units, Map<String, ContainerInfo> containerStates) {
        for (var entry : containerStates.entrySet()) {
            AgentAppUnit unit = units.get(entry.getKey());
            if (unit == null) {
                continue;
            }
            String state = entry.getValue().getState();
            tsSubService.saveAttributes(AttributesSaveRequest.builder()
                    .tenantId(tenantId)
                    .entityId(unit.getId())
                    .scope(AttributeScope.SERVER_SCOPE)
                    .entry(new StringDataEntry(STATE_ATTR_KEY, state))
                    .callback(getAttributeSaveCallback(tenantId, unit, STATE_ATTR_KEY, state))
                    .build());
        }
    }

    private void saveImageAttributes(TenantId tenantId, Map<String, AgentAppUnit> units, JsonNode externalComposeJson) {
        JsonNode services = externalComposeJson.get("services");
        if (services == null || !services.isObject()) {
            return;
        }
        services.fields().forEachRemaining(entry -> {
            String serviceName = entry.getKey();
            JsonNode service = entry.getValue();
            if (!service.isObject() || !service.has("image")) {
                return;
            }
            AgentAppUnit unit = units.get(serviceName);
            if (unit == null) {
                return;
            }
            String image = service.get("image").asText();
            tsSubService.saveAttributes(AttributesSaveRequest.builder()
                    .tenantId(tenantId)
                    .entityId(unit.getId())
                    .scope(AttributeScope.SERVER_SCOPE)
                    .entry(new StringDataEntry(IMAGE_ATTR_KEY, image))
                    .callback(getAttributeSaveCallback(tenantId, unit, IMAGE_ATTR_KEY, image))
                    .build());
        });
    }

    private static Collector<AgentAppUnit, ?, LinkedHashMap<String, AgentAppUnit>> collectByIdentifier() {
        return Collectors.toMap(AgentAppUnit::getIdentifier, Function.identity(), (a, b) -> a, LinkedHashMap::new);
    }

    private FutureCallback<Void> getAttributeSaveCallback(TenantId tenantId, AgentAppUnit unit, String attrKey, String attrValue) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(Void result) {
                log.trace("[{}] Updated {} [{}] for unit [{}]", tenantId, attrKey, attrValue, unit.getIdentifier());
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] Failed to update {} [{}] for unit [{}]", tenantId, attrKey, attrValue, unit.getIdentifier(), t);
            }
        };
    }
}
