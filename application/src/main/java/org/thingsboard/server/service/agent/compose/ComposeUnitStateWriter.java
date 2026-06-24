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

import com.google.common.util.concurrent.FutureCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.gen.agent.v1.ContainerInfo;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.Map;

@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class ComposeUnitStateWriter {

    private static final String STATE_ATTR_KEY = "state";

    private final TelemetrySubscriptionService tsSubService;

    public void writeStates(TenantId tenantId, Map<AgentAppUnitKey, AgentAppUnit> units, Map<String, ContainerInfo> containerStates) {
        for (var entry : containerStates.entrySet()) {
            AgentAppUnit unit = units.get(new AgentAppUnitKey(AgentAppUnitType.CONTAINER, entry.getKey()));
            if (unit == null) {
                continue;
            }
            String state = entry.getValue().getState();
            tsSubService.saveAttributes(AttributesSaveRequest.builder()
                    .tenantId(tenantId)
                    .entityId(unit.getId())
                    .scope(AttributeScope.SERVER_SCOPE)
                    .entry(new StringDataEntry(STATE_ATTR_KEY, state))
                    .callback(getSaveCallback(tenantId, unit, state))
                    .build());
        }
    }

    private FutureCallback<Void> getSaveCallback(TenantId tenantId, AgentAppUnit unit, String state) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(Void result) {
                log.trace("[{}] Updated {} [{}] for unit [{}]", tenantId, STATE_ATTR_KEY, state, unit.getIdentifier());
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] Failed to update {} [{}] for unit [{}]", tenantId, STATE_ATTR_KEY, state, unit.getIdentifier(), t);
            }
        };
    }
}
