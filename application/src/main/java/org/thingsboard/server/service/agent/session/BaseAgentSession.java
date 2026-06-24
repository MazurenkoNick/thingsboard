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
package org.thingsboard.server.service.agent.session;

import com.google.common.util.concurrent.ListeningExecutorService;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.gen.agent.v1.ServerToAgent;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class BaseAgentSession implements AgentSession {

    private static final int MAX_PENDING_MESSAGES = 10_000;

    private final ConcurrentHashMap<AgentAppEventId, ScheduledFuture<?>> eventWatchdogs;
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
        this.eventWatchdogs = new ConcurrentHashMap<>();
        this.pending = new ConcurrentLinkedQueue<>();
        this.pendingPermits = new Semaphore(MAX_PENDING_MESSAGES);
        this.state = new AgentSessionState();

        this.responseObserver = responseObserver;
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
    public void scheduleEventWatchdog(AgentAppEventId eventId, ScheduledExecutorService scheduler,
                                       Runnable task, long delay, TimeUnit unit) {
        cancelEventWatchdog(eventId);
        ScheduledFuture<?> future = scheduler.schedule(task, delay, unit);
        eventWatchdogs.put(eventId, future);
    }

    @Override
    public void cancelEventWatchdog(AgentAppEventId eventId) {
        ScheduledFuture<?> existing = eventWatchdogs.remove(eventId);
        if (existing != null && !existing.isCancelled() && !existing.isDone()) {
            existing.cancel(false);
        }
    }

    @Override
    public void onError(Status status) {
        state.setErrorStatus(status);
        drainIfPossible();
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
        state.closeAndDo(() -> {
            cancelAllWatchdogs();
            clearPendingAndReleasePermits();
        });
        log.trace("[{}] Stream is closed silently", state.getAgentId());
    }

    @Override
    public void drainIfPossible() {
        if (state.isClosed()) {
            return;
        }
        if (!drainScheduled.compareAndSet(false, true)) {
            return;
        }
        writer.execute(this::drain);
    }

    private void drain() {
        try {
            while (!state.isClosed() && !state.isError() && responseObserver.isReady()) {
                ServerToAgent next = pending.poll();
                if (next == null) {
                    break;
                }
                pendingPermits.release();
                responseObserver.onNext(next);
            }
        } catch (Throwable t) {
            handleErrorOnOutboundQueueProcessing(t);
        } finally {
            drainScheduled.set(false);

            // if we're closing and nothing left to send -> complete exactly once
            boolean gracefulShutdown = state.isClosing() && pending.isEmpty()
                    && completeAfterDrain.compareAndSet(true, false);
            if (state.isError() || gracefulShutdown) {
                state.closeAndDo(() -> {
                    cancelAllWatchdogs();
                    clearPendingAndReleasePermits();
                    if (!responseObserver.isCancelled()) {
                        if (state.isError()) {
                            responseObserver.onError(state.getErrorStatus().asRuntimeException());
                        } else {
                            responseObserver.onCompleted();
                        }
                    }
                });
                return;
            }
            // race fix: if something arrived after we released the flag, schedule again not to prevent 'missed wakeup'
            if (!state.isClosed() && (state.isError() || !pending.isEmpty() && responseObserver.isReady() )) {
                drainIfPossible();
            }
        }
    }

    private void cancelAllWatchdogs() {
        eventWatchdogs.values().forEach(f -> f.cancel(false));
        log.trace("[{}][{}] Clearing {} event watchdogs", state.getTenantId(), state.getAgentId(), eventWatchdogs.size());
        eventWatchdogs.clear();
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
