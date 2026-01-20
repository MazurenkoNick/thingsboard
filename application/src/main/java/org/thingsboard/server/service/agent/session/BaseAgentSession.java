/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.agent.session;

import com.google.common.util.concurrent.ListeningExecutorService;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.gen.agent.v1.ServerToAgent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class BaseAgentSession implements AgentSession {

    private static final int MAX_PENDING_MESSAGES = 10_000;

    private final ServerCallStreamObserver<ServerToAgent> responseObserver;
    private final Queue<ServerToAgent> pending;
    private final Semaphore pendingPermits;
    private final AgentSessionState state;
    private final ListeningExecutorService writer;
    private final AtomicBoolean drainScheduled = new AtomicBoolean(false);
    private final AtomicBoolean completeAfterDrain = new AtomicBoolean(false);

    @Override
    public AgentSessionState getState() {
        return state;
    }

    public BaseAgentSession(ListeningExecutorService writer, ServerCallStreamObserver<ServerToAgent> responseObserver) {
        this.writer = writer;
        this.pending = new ConcurrentLinkedQueue<>();
        this.pendingPermits = new Semaphore(MAX_PENDING_MESSAGES);
        this.state = new AgentSessionState();

        this.responseObserver = responseObserver;
        // backpressure: gRPC calls this when transport becomes writable again
        this.responseObserver.setOnReadyHandler(this::drainIfPossible);
        this.responseObserver.setOnCancelHandler(this::closeSilently);
    }

    @Override
    public boolean push(ServerToAgent msg) {
        if (state.isClosing() || state.isClosed()) {
            log.trace("Couldn't push the command {}, agent session has been closed", msg);
            return false;
        }
        if (!pendingPermits.tryAcquire()) {
            log.trace("The outbound message queue is full, declining the message: {}", msg);
            return false;
        }
        pending.add(msg);
        drainIfPossible();
        return true;
    }

    @Override
    public void onError(Status status) {
        state.closeAndDo(() -> {
            clearPendingAndReleasePermits();
            writer.execute(() -> {
                try {
                    if (responseObserver.isCancelled()) {
                        return;
                    }
                    responseObserver.onError(status.asRuntimeException());
                } catch (Throwable t) {
                    log.error("[{}] Failed to notify agent onError", state.getAgentId(), t);
                }
            });
        });
    }

    @Override
    public void complete() {
        if (!state.beginClosing()) {
            return;
        }
        completeAfterDrain.set(true);

        // kick draining. If not ready, onReadyHandler will continue later.
        drainIfPossible();
    }

    @Override
    public void closeSilently() {
        state.closeAndDo(this::clearPendingAndReleasePermits);
        log.trace("[{}] Stream is closed silently", state.getAgentId());
    }

    private void drainIfPossible() {
        if (state.isClosed()) {
            return;
        }
        if (!drainScheduled.compareAndSet(false, true)) {
            return;
        }
        writer.submit(this::drain);
    }

    private void drain() {
        try {
            while (!state.isClosed() && responseObserver.isReady()) {
                ServerToAgent next = pending.poll();
                if (next == null) {
                    return;
                }
                pendingPermits.release();
                responseObserver.onNext(next);
            }
        } catch (Throwable t) {
            handleErrorOnOutboundQueueProcessing(t);
        } finally {
            drainScheduled.set(false);

            // if we're closing and nothing left to send -> complete exactly once
            if (state.isClosing() && pending.isEmpty() && completeAfterDrain.compareAndSet(true, false)) {
                state.closeAndDo(() -> {
                    clearPendingAndReleasePermits();
                    if (!responseObserver.isCancelled()) {
                        responseObserver.onCompleted();
                    }
                });
                return;
            }
            // race fix: if something arrived after we released the flag, schedule again not to prevent 'missed wakeup'
            if (!state.isClosed() && !pending.isEmpty() && responseObserver.isReady()) {
                drainIfPossible();
            }
        }
    }

    private void clearPendingAndReleasePermits() {
        int removed = 0;
        while (pending.poll() != null) {
            removed++;
        }
        if (removed > 0) {
            pendingPermits.release(removed);
        }
    }

    private void handleErrorOnOutboundQueueProcessing(Throwable t) {
        log.error("[{}] Outbound drain failed", state.getAgentId(), t);

        try {
            onError(Status.INTERNAL
                    .withDescription("Outbound stream failed: " + t.getClass().getSimpleName())
                    .withCause(t)
            );
        } catch (Throwable ignored) {}
    }
}
