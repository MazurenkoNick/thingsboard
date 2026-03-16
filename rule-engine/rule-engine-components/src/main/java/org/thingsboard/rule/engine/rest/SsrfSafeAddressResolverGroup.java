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
package org.thingsboard.rule.engine.rest;

import io.netty.resolver.AddressResolver;
import io.netty.resolver.AddressResolverGroup;
import io.netty.resolver.DefaultAddressResolverGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.thingsboard.common.util.SsrfProtectionValidator;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom Netty {@link AddressResolverGroup} that validates every resolved IP address
 * against the SSRF block-list at connection time. This eliminates the DNS rebinding
 * TOCTOU gap where a hostname resolves to a safe IP during validation but to a
 * private/metadata IP when the actual connection is made.
 */
public final class SsrfSafeAddressResolverGroup extends AddressResolverGroup<InetSocketAddress> {

    public static final SsrfSafeAddressResolverGroup INSTANCE = new SsrfSafeAddressResolverGroup();

    private SsrfSafeAddressResolverGroup() {
    }

    @Override
    protected AddressResolver<InetSocketAddress> newResolver(EventExecutor executor) throws Exception {
        AddressResolver<InetSocketAddress> delegate = DefaultAddressResolverGroup.INSTANCE.getResolver(executor);
        return new SsrfValidatingResolver(executor, delegate);
    }

    private static final class SsrfValidatingResolver implements AddressResolver<InetSocketAddress> {

        private final EventExecutor executor;
        private final AddressResolver<InetSocketAddress> delegate;

        SsrfValidatingResolver(EventExecutor executor, AddressResolver<InetSocketAddress> delegate) {
            this.executor = executor;
            this.delegate = delegate;
        }

        @Override
        public boolean isSupported(SocketAddress address) {
            return delegate.isSupported(address);
        }

        @Override
        public boolean isResolved(SocketAddress address) {
            return delegate.isResolved(address);
        }

        @Override
        public Future<InetSocketAddress> resolve(SocketAddress address) {
            return resolve(address, executor.newPromise());
        }

        @Override
        public Future<InetSocketAddress> resolve(SocketAddress address, Promise<InetSocketAddress> promise) {
            delegate.resolve(address).addListener((Future<InetSocketAddress> future) -> {
                if (!future.isSuccess()) {
                    promise.tryFailure(future.cause());
                    return;
                }
                InetSocketAddress resolved = future.getNow();
                if (SsrfProtectionValidator.isEnabled() && isBlocked(resolved) && !isOriginalHostAllowed(address)) {
                    promise.tryFailure(new RuntimeException(
                            "SSRF protection: resolved address " + resolved.getAddress().getHostAddress() + " is blocked"));
                } else {
                    promise.trySuccess(resolved);
                }
            });
            return promise;
        }

        @Override
        public Future<List<InetSocketAddress>> resolveAll(SocketAddress address) {
            return resolveAll(address, executor.newPromise());
        }

        @Override
        public Future<List<InetSocketAddress>> resolveAll(SocketAddress address, Promise<List<InetSocketAddress>> promise) {
            delegate.resolveAll(address).addListener((Future<List<InetSocketAddress>> future) -> {
                if (!future.isSuccess()) {
                    promise.tryFailure(future.cause());
                    return;
                }
                List<InetSocketAddress> resolved = future.getNow();
                if (!SsrfProtectionValidator.isEnabled() || isOriginalHostAllowed(address)) {
                    promise.trySuccess(resolved);
                    return;
                }
                List<InetSocketAddress> safe = resolved.stream()
                        .filter(addr -> !isBlocked(addr))
                        .collect(Collectors.toList());
                if (safe.isEmpty()) {
                    String host = address instanceof InetSocketAddress isa ? isa.getHostString() : address.toString();
                    promise.tryFailure(new RuntimeException(
                            "SSRF protection: all resolved addresses for " + host + " are blocked"));
                } else {
                    promise.trySuccess(safe);
                }
            });
            return promise;
        }

        @Override
        public void close() {
            delegate.close();
        }

        private static boolean isBlocked(InetSocketAddress socketAddress) {
            InetAddress addr = socketAddress.getAddress();
            return addr != null && SsrfProtectionValidator.isBlockedAddress(addr);
        }

        private static boolean isOriginalHostAllowed(SocketAddress address) {
            if (address instanceof InetSocketAddress isa) {
                String host = isa.getHostString();
                return host != null && SsrfProtectionValidator.isHostnameAllowed(host);
            }
            return false;
        }
    }
}
