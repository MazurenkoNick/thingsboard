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

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.logexternal.LogChunkBuffer;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.agent.v1.AgentRpcServiceGrpc;
import org.thingsboard.server.gen.agent.v1.AgentToServer;
import org.thingsboard.server.gen.agent.v1.ProvisionRequest;
import org.thingsboard.server.gen.agent.v1.ProvisionResponse;
import org.thingsboard.server.gen.agent.v1.ServerToAgent;
import org.thingsboard.server.gen.agent.v1.StartLogStream;
import org.thingsboard.server.gen.agent.v1.StopLogStream;
import org.thingsboard.server.gen.transport.TransportProtos.LogStreamRequestProto;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.event.AgentEventProcessor;
import org.thingsboard.server.service.agent.msg.inbound.AgentInboundMessageDispatcher;
import org.thingsboard.server.service.agent.session.AgentSession;
import org.thingsboard.server.service.agent.session.AgentSessionRegistry;
import org.thingsboard.server.service.agent.session.BaseAgentSession;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
@TbCoreComponent
@RequiredArgsConstructor
public class AgentGrpcService extends AgentRpcServiceGrpc.AgentRpcServiceImplBase implements AgentRpcService {

    private static final long MAX_REPLAY_LOOKBACK_MS = TimeUnit.MINUTES.toMillis(2);

    @Value("${agents.write_pool_size:8}")
    private int writePoolSize;

    private final LogChunkBuffer logChunkBuffer;
    private final AgentInboundMessageDispatcher inboundMessageDispatcher;
    private final AgentSessionService agentSessionService;
    private final AgentSessionRegistry sessions;
    private final AgentProvisionService agentProvisionService;
    private final AgentEventProcessor agentEventProcessor;
    private final AgentContextComponent agentCtx;

    private ListeningExecutorService writer;

    @PostConstruct
    private void init() {
        this.writer = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(writePoolSize));
    }

    @PreDestroy
    private void destroy() {
        if (writer != null && !writer.isShutdown()) {
            writer.shutdown();
        }
    }

    @Override
    public StreamObserver<AgentToServer> controlStream(StreamObserver<ServerToAgent> responseObserver) {
        if (!(responseObserver instanceof ServerCallStreamObserver<ServerToAgent> serverCallStreamObserver)) {
            throw new IllegalStateException("Expected ServerCallStreamObserver but got: "
                    + (responseObserver == null ? "null" : responseObserver.getClass().getName()));
        }

        AtomicReference<AgentSession> sessionRef = new AtomicReference<>();
        AtomicBoolean initializing = new AtomicBoolean(false);

        StreamObserver<AgentToServer> requestObserver = new StreamObserver<>() {
            @Override
            public void onNext(AgentToServer msg) {
                AgentSession session = sessionRef.get();
                if (session != null) {
                    processInboundMessage(session, msg);
                    return;
                }
                if (initializing.compareAndSet(false, true)) {
                    tryInitSession(msg, serverCallStreamObserver, sessionRef, initializing);
                } else {
                    log.trace("Dropping message while session is initializing");
                }
            }

            @Override
            public void onError(Throwable throwable) {
                AgentSession session = sessionRef.get();
                if (session == null) {
                    log.trace("Ignoring onError for uninitialized session");
                    return;
                }
                agentSessionService.onError(session);
                session.closeSilently();
            }

            @Override
            public void onCompleted() {
                AgentSession session = sessionRef.get();
                if (session == null) {
                    log.trace("Ignoring onCompleted for uninitialized session");
                    return;
                }
                agentSessionService.onCompleted(session);
                session.complete();
            }
        };
        
        serverCallStreamObserver.setOnReadyHandler(() -> {
            AgentSession session = sessionRef.get();
            if (session instanceof BaseAgentSession s) {
                s.drainIfPossible();
            }
        });
        serverCallStreamObserver.setOnCancelHandler(() -> {
            AgentSession session = sessionRef.get();
            if (session != null) {
                agentSessionService.onError(session);
                session.closeSilently();
            }
        });
        
        return requestObserver;
    }


    @Override
    public boolean push(AgentId agentId, ServerToAgent msg) throws AgentSessionNotFoundException {
        AgentSession session = sessions.getByAgentId(agentId);
        if (session == null) {
            throw new AgentSessionNotFoundException(agentId);
        }
        return session.push(msg);
    }

    @Override
    public void processLogStreamRequest(LogStreamRequestProto req) {
        AgentId agentId = AgentId.fromMsbAndLsb(req.getAgentIdMSB(), req.getAgentIdLSB());
        if (req.getStop()) {
            stopLogStream(agentId, req.getProjectName(), req.getUnitIdentifier());
            return;
        }
        var tenantId = TenantId.fromUUID(new UUID(req.getTenantIdMSB(), req.getTenantIdLSB()));
        var unitId = AgentAppUnitId.fromMsbAndLsb(req.getAgentUnitIdMSB(), req.getAgentUnitIdLSB());
        long lastSeenTs = logChunkBuffer.latestTailLineTs(tenantId, unitId);
        long allowedLastSeenTs = lastSeenTs == 0 ? 0 : Math.max(System.currentTimeMillis() - MAX_REPLAY_LOOKBACK_MS, lastSeenTs);
        ServerToAgent msg = ServerToAgent.newBuilder()
                .setStartLogStream(StartLogStream.newBuilder()
                        .setUnitId(req.getUnitIdentifier())
                        .setProjectName(req.getProjectName())
                        .setLastSeenTs(allowedLastSeenTs))
                .build();
        pushLogStreamMsg(agentId, msg, "Start");
    }

    @Override
    public void stopLogStream(AgentId agentId, String projectName, String unitIdentifier) {
        ServerToAgent msg = ServerToAgent.newBuilder()
                .setStopLogStream(StopLogStream.newBuilder()
                        .setUnitId(unitIdentifier)
                        .setProjectName(projectName))
                .build();
        pushLogStreamMsg(agentId, msg, "Stop");
    }

    private void pushLogStreamMsg(AgentId agentId, ServerToAgent msg, String kind) {
        try {
            push(agentId, msg);
        } catch (AgentSessionNotFoundException e) {
            log.trace("[{}] No local session for agent, dropping {} log stream request", agentId, kind);
        }
    }

    private void processInboundMessage(AgentSession session, AgentToServer msg) {
        inboundMessageDispatcher.process(AgentInboundMsgCtx.builder()
                .session(session)
                .msg(msg)
                .build());
    }

    private void tryInitSession(AgentToServer msg, ServerCallStreamObserver<ServerToAgent> responseObserver,
                                AtomicReference<AgentSession> sessionRef, AtomicBoolean initializing) {
        if (msg.hasProvision()) {
            handleProvisionRequest(msg.getProvision(), responseObserver, initializing);
            return;
        }
        if (!msg.hasHello()) {
            initializing.set(false);
            responseObserver.onError(Status.UNAUTHENTICATED
                    .withDescription("Hello Message must come before any other message")
                    .asRuntimeException());
            return;
        }
        ensureStateInit(msg, responseObserver, sessionRef, initializing);
    }

    private void handleProvisionRequest(ProvisionRequest request, ServerCallStreamObserver<ServerToAgent> responseObserver,
                                        AtomicBoolean initializing) {
        ProvisionResponse.Builder responseBuilder = ProvisionResponse.newBuilder();
        try {
            AgentProvisionService.ProvisionResult result = agentProvisionService.provision(
                    request.getProvisionKey(), request.getProvisionSecret());
            if (result.success()) {
                responseBuilder.setSuccess(true)
                        .setRoutingKey(result.routingKey())
                        .setRoutingSecret(result.routingSecret());
            } else {
                responseBuilder.setSuccess(false).setErrorMessage(result.errorMessage());
            }
        } catch (Exception e) {
            log.error("Failed to provision agent", e);
            responseBuilder.setSuccess(false).setErrorMessage("Failed to provision agent: " + e.getMessage());
        }
        try {
            responseObserver.onNext(ServerToAgent.newBuilder().setProvisionResponse(responseBuilder.build()).build());
            responseObserver.onCompleted();
        } finally {
            initializing.set(false);
        }
    }

    private void ensureStateInit(AgentToServer msg, ServerCallStreamObserver<ServerToAgent> responseObserver,
                                 AtomicReference<AgentSession> sessionRef, AtomicBoolean initializing) {
        BaseAgentSession session = new BaseAgentSession(writer, responseObserver);
        Optional<Status> optErr;
        try {
            optErr = agentSessionService.onConnected(session, msg.getHello());
        } catch (Exception e) {
            handleInitFailure(session, initializing, responseObserver, e);
            return;
        }
        if (optErr.isPresent()) {
            log.warn("The state couldn't be initialized: {}", optErr.get());
            initializing.set(false);
            agentSessionService.onError(session);
            responseObserver.onError(optErr.get().asRuntimeException());
            return;
        }
        sessionRef.set(session);
        session.push(AgentMsgConstructorUtils.helloSuccessResponse());
        resumeEvents(session);
    }

    private void resumeEvents(AgentSession session) {
        var agent = session.getState().getAgent();
        TenantId tenantId = agent.getTenantId();
        AgentId agentId = agent.getId();
        agentCtx.getAgentEventExecutor().submit(() -> {
            agentEventProcessor.resumeEventsOnReconnect(tenantId, agentId);
        });
    }

    private void handleInitFailure(AgentSession session, AtomicBoolean initializing,
                                   StreamObserver<ServerToAgent> responseObserver, Throwable t) {
        log.error("Failed to initialize agent session", t);
        initializing.set(false);
        agentSessionService.onError(session);
        responseObserver.onError(Status.INTERNAL
                .withDescription("Failed to initialize session")
                .withCause(t)
                .asRuntimeException());
    }
}
