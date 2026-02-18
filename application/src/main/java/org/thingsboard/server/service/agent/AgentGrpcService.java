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

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.gen.agent.v1.AgentRpcServiceGrpc;
import org.thingsboard.server.gen.agent.v1.AgentToServer;
import org.thingsboard.server.gen.agent.v1.ServerToAgent;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.msg.inbound.AgentInboundMessageDispatcher;
import org.thingsboard.server.service.agent.session.AgentSession;
import org.thingsboard.server.service.agent.session.AgentSessionRegistry;
import org.thingsboard.server.service.agent.session.BaseAgentSession;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
@TbCoreComponent
@RequiredArgsConstructor
public class AgentGrpcService extends AgentRpcServiceGrpc.AgentRpcServiceImplBase implements AgentRpcService {

    @Value("${agents.write_pool_size}")
    private int writePoolSize;

    private final AgentInboundMessageDispatcher inboundMessageDispatcher;
    private final AgentStateService agentStateService; // todo: finish
    private final AgentSessionRegistry sessions;

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
        ServerCallStreamObserver<ServerToAgent> serverCallStreamObserver =
                (ServerCallStreamObserver<ServerToAgent>) responseObserver;
        
        AtomicReference<AgentSession> sessionRef = new AtomicReference<>();
        
        StreamObserver<AgentToServer> requestObserver = new StreamObserver<>() {
            @Override
            public void onNext(AgentToServer msg) {
                AgentSession session = sessionRef.get();
                if (session == null) {
                    tryInitSession(msg, responseObserver).ifPresent(sessionRef::set);
                    session = sessionRef.get();
                }
                processInboundMessage(session, msg);
            }

            @Override
            public void onError(Throwable throwable) {
                AgentSession session = sessionRef.get();
                if (session == null) {
                    log.trace("Ignoring onError for uninitialized session");
                    return;
                }
                agentStateService.onError(session);
                session.closeSilently();
            }

            @Override
            public void onCompleted() {
                AgentSession session = sessionRef.get();
                if (session == null) {
                    log.trace("Ignoring onCompleted for uninitialized session");
                    return;
                }
                agentStateService.onCompleted(session);
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

    private void processInboundMessage(AgentSession session, AgentToServer msg) {
        if (session == null) {
            return;
        }
        inboundMessageDispatcher.process(AgentInboundMsgCtx.builder()
                .session(session)
                .msg(msg)
                .build());
    }

    private Optional<BaseAgentSession> tryInitSession(AgentToServer msg, StreamObserver<ServerToAgent> observer) {
        if (!msg.hasHello()) {
            observer.onError(Status.UNAUTHENTICATED
                    .withDescription("Hello Message must come before any other message")
                    .asRuntimeException());
            return Optional.empty();
        }
        BaseAgentSession session = new BaseAgentSession(writer, (ServerCallStreamObserver<ServerToAgent>) observer);

        Optional<Status> err = agentStateService.onConnected(session, msg.getHello());
        if (err.isPresent()) {
            StatusRuntimeException e = err.get().asRuntimeException();
            log.error("The state couldn't be initialized", e);
            observer.onError(e);
            return Optional.empty();
        }
        session.push(AgentMsgConstructorUtils.helloSuccessResponse());
        return Optional.of(session);
    }
}
