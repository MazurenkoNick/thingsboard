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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FutureCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.dao.agent.AgentAppUnitService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class ComposeUnitsSynchronizer {

    private static final String IMAGE_ATTR_KEY = "image";

    private final AgentAppUnitService unitService;
    private final TelemetrySubscriptionService tsSubService;

    public Map<AgentAppUnitKey, AgentAppUnit> syncUnits(TenantId tenantId, AgentApplicationId appId, JsonNode composeJson) {
        log.trace("[{}] Syncing units for app [{}]", tenantId, appId);
        Map<AgentAppUnitKey, AgentAppUnit> units = reconcileUnits(tenantId, appId, composeJson);
        saveImageAttributes(tenantId, units, composeJson);
        return units;
    }

    public Map<AgentAppUnitKey, AgentAppUnit> loadUnits(TenantId tenantId, AgentApplicationId appId) {
        Map<AgentAppUnitKey, AgentAppUnit> units = new LinkedHashMap<>();
        for (AgentAppUnit unit : unitService.findAgentAppUnitsByAgentAppId(tenantId, appId)) {
            units.put(new AgentAppUnitKey(unit.getType(), unit.getIdentifier()), unit);
        }
        return units;
    }

    private Map<AgentAppUnitKey, AgentAppUnit> reconcileUnits(TenantId tenantId, AgentApplicationId appId, JsonNode composeJson) {
        List<AgentAppUnitKey> desiredUnits = parseDesiredUnits(composeJson);
        log.trace("[{}] Desired units for app [{}]: {}", tenantId, appId, desiredUnits);

        // Index existing units by (type, identifier): compose namespaces services/volumes/networks
        // separately, so the same name can legitimately exist as two units of different types.
        Map<AgentAppUnitKey, AgentAppUnit> existing = loadUnits(tenantId, appId);
        log.trace("[{}] Existing units for app [{}]: {}", tenantId, appId, existing.keySet());

        Map<AgentAppUnitKey, AgentAppUnit> result = new LinkedHashMap<>();

        for (AgentAppUnitKey key : desiredUnits) {
            AgentAppUnit unit = existing.remove(key);
            if (unit == null) {
                unit = new AgentAppUnit();
                unit.setAgentApplicationId(appId);
                unit.setIdentifier(key.identifier());
                unit.setType(key.type());
                unit = unitService.saveAgentAppUnit(tenantId, unit);
                log.info("[{}] Created agent app unit [{}] ({}) for app [{}]", tenantId, key.identifier(), key.type(), appId);
            }
            result.put(key, unit);
        }

        for (AgentAppUnit stale : existing.values()) {
            log.info("[{}] Removing stale agent app unit [{}] ({}) from app [{}]", tenantId, stale.getIdentifier(), stale.getType(), appId);
            unitService.deleteAgentAppUnit(tenantId, stale.getId());
        }

        return result;
    }

    private List<AgentAppUnitKey> parseDesiredUnits(JsonNode composeJson) {
        List<AgentAppUnitKey> units = new ArrayList<>();
        collectKeys(composeJson, "services", AgentAppUnitType.CONTAINER, units);
        collectKeys(composeJson, "volumes", AgentAppUnitType.VOLUME, units);
        collectKeys(composeJson, "networks", AgentAppUnitType.NETWORK, units);
        return units;
    }

    private void collectKeys(JsonNode composeJson, String section, AgentAppUnitType type, List<AgentAppUnitKey> units) {
        JsonNode node = composeJson.get(section);
        if (node != null && node.isObject()) {
            node.fieldNames().forEachRemaining(name -> units.add(new AgentAppUnitKey(type, name)));
        }
    }

    private void saveImageAttributes(TenantId tenantId, Map<AgentAppUnitKey, AgentAppUnit> units, JsonNode composeJson) {
        JsonNode services = composeJson.get("services");
        if (services == null || !services.isObject()) {
            return;
        }
        services.properties().iterator().forEachRemaining(entry -> {
            String serviceName = entry.getKey();
            JsonNode service = entry.getValue();
            if (!service.isObject() || !service.has("image")) {
                return;
            }
            AgentAppUnit unit = units.get(new AgentAppUnitKey(AgentAppUnitType.CONTAINER, serviceName));
            if (unit == null) {
                return;
            }
            String image = service.get("image").asText();
            tsSubService.saveAttributes(AttributesSaveRequest.builder()
                    .tenantId(tenantId)
                    .entityId(unit.getId())
                    .scope(AttributeScope.SERVER_SCOPE)
                    .entry(new StringDataEntry(IMAGE_ATTR_KEY, image))
                    .callback(getImageSaveCallback(tenantId, unit, image))
                    .build());
        });
    }

    private FutureCallback<Void> getImageSaveCallback(TenantId tenantId, AgentAppUnit unit, String image) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(Void result) {
                log.trace("[{}] Updated {} [{}] for unit [{}]", tenantId, IMAGE_ATTR_KEY, image, unit.getIdentifier());
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] Failed to update {} [{}] for unit [{}]", tenantId, IMAGE_ATTR_KEY, image, unit.getIdentifier(), t);
            }
        };
    }

}
