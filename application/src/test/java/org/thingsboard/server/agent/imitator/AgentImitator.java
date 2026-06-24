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
import org.thingsboard.server.gen.agent.v1.InitialSyncComplete;
import org.thingsboard.server.gen.agent.v1.ProjectStateSync;
import org.thingsboard.server.gen.agent.v1.ProvisionRequest;
import org.thingsboard.server.gen.agent.v1.ProvisionResponse;
import org.thingsboard.server.gen.agent.v1.ServerToAgent;
import org.thingsboard.server.gen.agent.v1.StepId;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

@Slf4j
public class AgentImitator {

    private final String host;
    private final int port;
    private String routingKey;
    private String routingSecret;

    private ManagedChannel channel;
    private StreamObserver<AgentToServer> requestObserver;

    private final Lock lock = new ReentrantLock();
    private CountDownLatch messagesLatch;
    private int consumedCommands;

    @Getter
    private volatile HelloAck helloAck;
    @Getter
    private volatile ProvisionResponse provisionResponse;
    @Getter
    private final List<ServerToAgent> downlinkMsgs = new ArrayList<>();

    private volatile Throwable streamError;
    private final CountDownLatch helloAckLatch = new CountDownLatch(1);
    private final CountDownLatch provisionLatch = new CountDownLatch(1);

    public AgentImitator(String host, int port, String routingKey, String routingSecret) {
        this.host = host;
        this.port = port;
        this.routingKey = routingKey;
        this.routingSecret = routingSecret;
        this.messagesLatch = new CountDownLatch(0);
    }

    public void connect() throws InterruptedException {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdownNow();
            channel.awaitTermination(5, TimeUnit.SECONDS);
        }
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

    /**
     * Opens a control stream and sends a {@link ProvisionRequest}. Blocks until the
     * {@link ProvisionResponse} arrives. The server closes the stream after responding,
     * so callers should treat this imitator as single-use after provisioning.
     */
    public void provision(String provisionKey, String provisionSecret) throws InterruptedException {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        AgentRpcServiceGrpc.AgentRpcServiceStub stub = AgentRpcServiceGrpc.newStub(channel);

        requestObserver = stub.controlStream(new StreamObserver<>() {
            @Override
            public void onNext(ServerToAgent msg) {
                if (msg.hasProvisionResponse()) {
                    provisionResponse = msg.getProvisionResponse();
                    routingSecret = provisionResponse.getRoutingSecret();
                    routingKey = provisionResponse.getRoutingKey();
                    provisionLatch.countDown();
                }
            }

            @Override
            public void onError(Throwable t) {
                streamError = t;
                provisionLatch.countDown();
                log.info("Provision stream error: {}", t.getMessage());
            }

            @Override
            public void onCompleted() {
                provisionLatch.countDown();
            }
        });

        requestObserver.onNext(AgentToServer.newBuilder()
                .setProvision(ProvisionRequest.newBuilder()
                        .setProvisionKey(provisionKey)
                        .setProvisionSecret(provisionSecret)
                        .build())
                .build());

        Assert.assertTrue("Timed out waiting for ProvisionResponse",
                provisionLatch.await(AbstractWebTest.TIMEOUT, TimeUnit.SECONDS));
    }

    // --- Sending messages ---

    public void sendProjectSync(ProjectStateSync projectSync) {
        requestObserver.onNext(AgentToServer.newBuilder()
                .setProjectSync(projectSync)
                .build());
    }

    public void sendInitialSyncComplete() {
        requestObserver.onNext(AgentToServer.newBuilder()
                .setInitialSyncComplete(InitialSyncComplete.newBuilder().build())
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
            consumedCommands = 0;
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

    /**
     * Poll-based command consumption. Unlike the {@link #expectMessageAmount(int)} /
     * {@link #waitForMessages()} latch path, these methods never clear already-received
     * commands and do not depend on a latch being armed before the command arrives, so
     * they are immune to the race where a downlink lands before the test starts waiting.
     */
    public boolean hasUnconsumedCommand(Predicate<AppCommand> predicate) {
        lock.lock();
        try {
            List<AppCommand> commands = getReceivedCommands();
            for (int i = consumedCommands; i < commands.size(); i++) {
                if (predicate.test(commands.get(i))) {
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public AppCommand consumeNextCommand(Predicate<AppCommand> predicate) {
        lock.lock();
        try {
            List<AppCommand> commands = getReceivedCommands();
            for (int i = consumedCommands; i < commands.size(); i++) {
                if (predicate.test(commands.get(i))) {
                    consumedCommands = i + 1;
                    return commands.get(i);
                }
            }
            Assert.fail("No unconsumed command matching predicate");
            return null;
        } finally {
            lock.unlock();
        }
    }
}
