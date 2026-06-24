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
package org.thingsboard.server.service.agent;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import static org.thingsboard.server.service.state.DefaultDeviceStateService.ACTIVITY_STATE;
import static org.thingsboard.server.service.state.DefaultDeviceStateService.LAST_CONNECT_TIME;
import static org.thingsboard.server.service.state.DefaultDeviceStateService.LAST_DISCONNECT_TIME;

@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class DefaultAgentStateService implements AgentStateService {

    private final TelemetrySubscriptionService tsSubService;
    private final TbClusterService clusterService;

    @Override
    public void onAgentConnect(Agent agent, long lastConnectTime) {
        TenantId tenantId = agent.getTenantId();
        AgentId agentId = agent.getId();
        save(tenantId, agentId, ACTIVITY_STATE, true);
        save(tenantId, agentId, LAST_CONNECT_TIME, lastConnectTime);
        pushRuleEngineMessage(tenantId, agentId, lastConnectTime, TbMsgType.CONNECT_EVENT);
        // TODO: fire agent-connection notification rule once the BE trigger stack (AgentConnectionTrigger /
        //  AgentConnectionTriggerProcessor / NotificationRuleTriggerType.AGENT_CONNECTION registries) and the
        //  FE notification rule dialog wiring are in place:
        //  notificationRuleProcessor.process(AgentConnectionTrigger.builder()
        //          .tenantId(tenantId)
        //          .customerId(agent.getCustomerId())
        //          .agentId(agentId)
        //          .agentName(agent.getName())
        //          .connected(true)
        //          .build());
    }

    @Override
    public void onAgentDisconnect(Agent agent, long lastDisconnectTime) {
        TenantId tenantId = agent.getTenantId();
        AgentId agentId = agent.getId();
        save(tenantId, agentId, ACTIVITY_STATE, false);
        save(tenantId, agentId, LAST_DISCONNECT_TIME, lastDisconnectTime);
        pushRuleEngineMessage(tenantId, agentId, lastDisconnectTime, TbMsgType.DISCONNECT_EVENT);
        // TODO: fire agent-connection notification rule once the BE trigger stack and FE rule dialog wiring are in place:
        //  notificationRuleProcessor.process(AgentConnectionTrigger.builder()
        //          .tenantId(tenantId)
        //          .customerId(agent.getCustomerId())
        //          .agentId(agentId)
        //          .agentName(agent.getName())
        //          .connected(false)
        //          .build());
    }

    private void pushRuleEngineMessage(TenantId tenantId, AgentId agentId, long ts, TbMsgType msgType) {
        try {
            ObjectNode agentState = JacksonUtil.newObjectNode();
            boolean isConnected = TbMsgType.CONNECT_EVENT.equals(msgType);
            if (isConnected) {
                agentState.put(ACTIVITY_STATE, true);
                agentState.put(LAST_CONNECT_TIME, ts);
            } else {
                agentState.put(ACTIVITY_STATE, false);
                agentState.put(LAST_DISCONNECT_TIME, ts);
            }
            String data = JacksonUtil.toString(agentState);
            TbMsg tbMsg = TbMsg.newMsg()
                    .type(msgType)
                    .originator(agentId)
                    .copyMetaData(TbMsgMetaData.EMPTY)
                    .dataType(TbMsgDataType.JSON)
                    .data(data)
                    .build();
            clusterService.pushMsgToRuleEngine(tenantId, agentId, tbMsg, null);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push {}", tenantId, agentId, msgType, e);
        }
    }

    private void save(TenantId tenantId, AgentId agentId, String key, boolean value) {
        tsSubService.saveAttributes(AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(agentId)
                .scope(AttributeScope.SERVER_SCOPE)
                .entry(new BooleanDataEntry(key, value))
                .callback(saveCallback(tenantId, agentId, key, value))
                .build());
    }

    private void save(TenantId tenantId, AgentId agentId, String key, long value) {
        tsSubService.saveAttributes(AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(agentId)
                .scope(AttributeScope.SERVER_SCOPE)
                .entry(new LongDataEntry(key, value))
                .callback(saveCallback(tenantId, agentId, key, value))
                .build());
    }

    private <T> FutureCallback<Void> saveCallback(TenantId tenantId, AgentId agentId, String key, T value) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(Void result) {
                log.trace("[{}][{}] Saved attribute [{}]=[{}]", tenantId, agentId, key, value);
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}][{}] Failed to save attribute [{}]=[{}]", tenantId, agentId, key, value, t);
            }
        };
    }

}
