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
package org.thingsboard.server.service.agent;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.gen.agent.v1.Hello;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.session.AgentSession;
import org.thingsboard.server.service.agent.session.AgentSessionRegistry;
import org.thingsboard.server.service.agent.session.AgentSessionState;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.service.state.DefaultDeviceStateService.ACTIVITY_STATE;
import static org.thingsboard.server.service.state.DefaultDeviceStateService.LAST_CONNECT_TIME;
import static org.thingsboard.server.service.state.DefaultDeviceStateService.LAST_DISCONNECT_TIME;

@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class AgentStateService {

    private final AgentSessionRegistry sessions;
    private final TbServiceInfoProvider serviceInfoProvider;
//    private final TbTransactionalCache<AgentId, String> agentIdServiceIdCache; // todo: impl
    private final NotificationRuleProcessor notificationRuleProcessor; // todo: figure out the need of it here
    private final TelemetrySubscriptionService tsSubService;
    private final TbClusterService clusterService;

    public Optional<Status> onConnected(AgentSession session, Hello msg) {
        AgentSessionState state = session.getState();
        AgentId id = AgentId.fromMsgAndLsb(msg.getAgentIdMSB(), msg.getAgentIdLSB());
        Agent agent = findAgent(id);
        if (agent == null) {
            log.trace("Agent not found, id={}", id);
            return Optional.of(Status.NOT_FOUND.withDescription("Could not find agent with the given id: " + id));
        }
        state.setAgent(agent);
        TenantId tenantId = agent.getTenantId();
        AgentId agentId = agent.getId();

        sessions.registerOrReplace(agentId, tenantId, session); // todo: think about epochs
//        agentIdServiceIdCache.put(agentId, serviceInfoProvider.getServiceId()); // todo:
        log.info("[{}] agent [{}] connected successfully", tenantId, agentId);
        save(tenantId, agentId, ACTIVITY_STATE, true);
        long lastConnectTs = System.currentTimeMillis();
        save(tenantId, agentId, LAST_CONNECT_TIME, System.currentTimeMillis());
        pushRuleEngineMessage(tenantId, session.getState().getAgent(), lastConnectTs, TbMsgType.CONNECT_EVENT);

        return Optional.empty();
    }

    public void onCompleted(AgentSession session) {
        disconnect(session);
    }

    public void onError(AgentSession session) {
        disconnect(session);
    }

    private void disconnect(AgentSession session) {
        var agent = session.getState().getAgent();
        TenantId tenantId = agent.getTenantId();
        AgentId agentId = agent.getId();

        sessions.removeIfSame(session);
        save(tenantId, agentId, ACTIVITY_STATE, false);
        long lastDisconnectTs = System.currentTimeMillis();
        save(tenantId, agentId, LAST_DISCONNECT_TIME, lastDisconnectTs);
        pushRuleEngineMessage(tenantId, session.getState().getAgent(), lastDisconnectTs, TbMsgType.DISCONNECT_EVENT);
    }

    private Agent findAgent(AgentId id) { // todo: impl
        Agent agent = new Agent();
        agent.setId(new AgentId(UUID.randomUUID()));
        agent.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        agent.setCustomerId(new CustomerId(UUID.randomUUID()));
        return agent;
    }

    private void pushRuleEngineMessage(TenantId tenantId, Agent agent, long ts, TbMsgType msgType) {
        try {
            AgentId agentId = agent.getId();
            ObjectNode agentState = JacksonUtil.newObjectNode();
            boolean isConnected = TbMsgType.CONNECT_EVENT.equals(msgType);
            if (isConnected) {
                agentState.put(ACTIVITY_STATE, true);
                agentState.put(LAST_CONNECT_TIME, ts);
            } else {
                agentState.put(ACTIVITY_STATE, false);
                agentState.put(LAST_DISCONNECT_TIME, ts);
            }
            // todo: impl if required
//            notificationRuleProcessor.process(AgentConnectionTrigger.builder()
//                    .tenantId(tenantId)
//                    .customerId(agent.getCustomerId())
//                    .agentId(agentId)
//                    .agentName(agent.getName())
//                    .connected(isConnected).build());
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
            log.warn("[{}][{}] Failed to push {}", tenantId, agent.getId(), msgType, e);
        }
    }

    private void save(TenantId tenantId, AgentId agentId, String key, boolean value) {
        log.debug("[{}][{}] Updating long agent telemetry [{}] [{}]", tenantId, agentId, key, value);

        tsSubService.saveAttributes(AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(agentId)
                .scope(AttributeScope.SERVER_SCOPE)
                .entry(new BooleanDataEntry(key, value))
                .callback(getAttributeSaveCallback(tenantId, agentId, key, value))
                .build());
    }

    private void save(TenantId tenantId, AgentId agentId, String key, long value) {
        log.debug("[{}][{}] Updating long agent telemetry [{}] [{}]", tenantId, agentId, key, value);

        tsSubService.saveAttributes(AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(agentId)
                .scope(AttributeScope.SERVER_SCOPE)
                .entry(new LongDataEntry(key, value))
                .callback(getAttributeSaveCallback(tenantId, agentId, key, value))
                .build());
    }

    private <T> FutureCallback<Void> getAttributeSaveCallback(TenantId tenantId, AgentId agentId, String key, T value) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(Void result) {
                log.trace("[{}][{}] Successfully updated attribute [{}] with value [{}]", tenantId, agentId, key, value);
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}][{}] Failed to update attribute [{}] with value [{}]", tenantId, agentId, key, value, t);
            }
        };
    }
}
