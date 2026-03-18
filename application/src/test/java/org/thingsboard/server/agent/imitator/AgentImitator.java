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
package org.thingsboard.server.agent.imitator;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.thingsboard.server.controller.AbstractWebTest;
import org.thingsboard.server.gen.agent.v1.AckStatus;
import org.thingsboard.server.gen.agent.v1.AgentRpcServiceGrpc;
import org.thingsboard.server.gen.agent.v1.AgentToServer;
import org.thingsboard.server.gen.agent.v1.AppCommand;
import org.thingsboard.server.gen.agent.v1.CommandAck;
import org.thingsboard.server.gen.agent.v1.CommandId;
import org.thingsboard.server.gen.agent.v1.CommandProgress;
import org.thingsboard.server.gen.agent.v1.CommandResult;
import org.thingsboard.server.gen.agent.v1.Hello;
import org.thingsboard.server.gen.agent.v1.HelloAck;
import org.thingsboard.server.gen.agent.v1.ProjectStateSync;
import org.thingsboard.server.gen.agent.v1.ServerToAgent;
import org.thingsboard.server.gen.agent.v1.StepId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

@Slf4j
public class AgentImitator {

    private final String host;
    private final int port;
    private final String routingKey;
    private final String routingSecret;

    private ManagedChannel channel;
    private StreamObserver<AgentToServer> requestObserver;

    private final Lock lock = new ReentrantLock();
    private CountDownLatch messagesLatch;

    @Getter
    private volatile HelloAck helloAck;
    @Getter
    private final List<ServerToAgent> downlinkMsgs = new ArrayList<>();

    private volatile Throwable streamError;
    private final CountDownLatch helloAckLatch = new CountDownLatch(1);

    public AgentImitator(String host, int port, String routingKey, String routingSecret) {
        this.host = host;
        this.port = port;
        this.routingKey = routingKey;
        this.routingSecret = routingSecret;
        this.messagesLatch = new CountDownLatch(0);
    }

    public void connect() throws InterruptedException {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .keepAliveTime(300, TimeUnit.SECONDS)
                .keepAliveTimeout(5, TimeUnit.SECONDS)
                .build();

        AgentRpcServiceGrpc.AgentRpcServiceStub stub = AgentRpcServiceGrpc.newStub(channel);

        requestObserver = stub.controlStream(new StreamObserver<>() {
            @Override
            public void onNext(ServerToAgent msg) {
                if (msg.hasHelloAck()) {
                    helloAck = msg.getHelloAck();
                    helloAckLatch.countDown();
                } else {
                    lock.lock();
                    try {
                        downlinkMsgs.add(msg);
                    } finally {
                        lock.unlock();
                    }
                    messagesLatch.countDown();
                }
            }

            @Override
            public void onError(Throwable t) {
                streamError = t;
                helloAckLatch.countDown();
                log.info("Agent stream error: {}", t.getMessage());
            }

            @Override
            public void onCompleted() {
                log.info("Agent stream completed");
            }
        });

        requestObserver.onNext(AgentToServer.newBuilder()
                .setHello(Hello.newBuilder()
                        .setRoutingKey(routingKey)
                        .setRoutingSecret(routingSecret)
                        .build())
                .build());

        Assert.assertTrue("Timed out waiting for HelloAck",
                helloAckLatch.await(AbstractWebTest.TIMEOUT, TimeUnit.SECONDS));
    }

    public Status connectExpectingError() throws InterruptedException {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        AgentRpcServiceGrpc.AgentRpcServiceStub stub = AgentRpcServiceGrpc.newStub(channel);
        CountDownLatch errorLatch = new CountDownLatch(1);

        requestObserver = stub.controlStream(new StreamObserver<>() {
            @Override
            public void onNext(ServerToAgent msg) {
                if (msg.hasHelloAck()) {
                    helloAck = msg.getHelloAck();
                }
            }

            @Override
            public void onError(Throwable t) {
                streamError = t;
                errorLatch.countDown();
            }

            @Override
            public void onCompleted() {
                errorLatch.countDown();
            }
        });

        requestObserver.onNext(AgentToServer.newBuilder()
                .setHello(Hello.newBuilder()
                        .setRoutingKey(routingKey)
                        .setRoutingSecret(routingSecret)
                        .build())
                .build());

        Assert.assertTrue("Timed out waiting for error response",
                errorLatch.await(AbstractWebTest.TIMEOUT, TimeUnit.SECONDS));

        if (streamError instanceof StatusRuntimeException sre) {
            return sre.getStatus();
        }
        if (streamError != null) {
            return Status.fromThrowable(streamError);
        }
        return null;
    }

    public void disconnect() throws InterruptedException {
        if (requestObserver != null) {
            try {
                requestObserver.onCompleted();
            } catch (Exception ignored) {
            }
        }
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
            channel.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    public Throwable getStreamError() {
        return streamError;
    }

    // --- Sending messages ---

    public void sendProjectSync(ProjectStateSync projectSync) {
        requestObserver.onNext(AgentToServer.newBuilder()
                .setProjectSync(projectSync)
                .build());
    }

    public void sendCommandAck(CommandId commandId, AckStatus status) {
        requestObserver.onNext(AgentToServer.newBuilder()
                .setCommandAck(CommandAck.newBuilder()
                        .setCommandId(commandId)
                        .setStatus(status)
                        .build())
                .build());
    }

    public void sendCommandProgress(CommandId commandId, StepId stepId, String stage) {
        requestObserver.onNext(AgentToServer.newBuilder()
                .setProgress(CommandProgress.newBuilder()
                        .setCommandId(commandId)
                        .setStep(stepId)
                        .setStage(stage)
                        .build())
                .build());
    }

    public void sendCommandResult(CommandId commandId, StepId stepId, boolean success) {
        requestObserver.onNext(AgentToServer.newBuilder()
                .setResult(CommandResult.newBuilder()
                        .setCommandId(commandId)
                        .setStep(stepId)
                        .setSuccess(success)
                        .build())
                .build());
    }

    // --- Receiving messages ---

    public void expectMessageAmount(int messageAmount) {
        lock.lock();
        try {
            downlinkMsgs.clear();
        } finally {
            lock.unlock();
        }
        messagesLatch = new CountDownLatch(messageAmount);
    }

    public boolean waitForMessages() throws InterruptedException {
        boolean success = messagesLatch.await(AbstractWebTest.TIMEOUT, TimeUnit.SECONDS);
        if (!success) {
            lock.lock();
            try {
                for (ServerToAgent msg : downlinkMsgs) {
                    log.error("Received: {}", msg);
                }
                log.error("Message count: {}", downlinkMsgs.size());
            } finally {
                lock.unlock();
            }
            Assert.fail("Await for messages was not successful!");
        }
        return true;
    }

    public List<AppCommand> getReceivedCommands() {
        lock.lock();
        try {
            return downlinkMsgs.stream()
                    .filter(ServerToAgent::hasAppCommand)
                    .map(ServerToAgent::getAppCommand)
                    .toList();
        } finally {
            lock.unlock();
        }
    }

    public Optional<AppCommand> findCommand(Predicate<AppCommand> predicate) {
        lock.lock();
        try {
            return downlinkMsgs.stream()
                    .filter(ServerToAgent::hasAppCommand)
                    .map(ServerToAgent::getAppCommand)
                    .filter(predicate)
                    .findFirst();
        } finally {
            lock.unlock();
        }
    }

    public AppCommand getLatestCommand() {
        lock.lock();
        try {
            List<AppCommand> commands = getReceivedCommands();
            Assert.assertFalse("No commands received", commands.isEmpty());
            return commands.get(commands.size() - 1);
        } finally {
            lock.unlock();
        }
    }
}
