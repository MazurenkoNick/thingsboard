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
package org.thingsboard.server.service.script;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.script.api.ScriptInvokeService;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.script.api.TbScriptException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

@Slf4j
public abstract class RuleNodeScriptEngine<T extends ScriptInvokeService, R> implements ScriptEngine {

    private final T scriptInvokeService;

    protected final UUID scriptId;
    private final TenantId tenantId;

    public RuleNodeScriptEngine(TenantId tenantId, T scriptInvokeService, ScriptType scriptType, String script, String... argNames) {
        this.tenantId = tenantId;
        this.scriptInvokeService = scriptInvokeService;
        try {
            scriptId = this.scriptInvokeService.eval(tenantId, scriptType, script, argNames).get();
        } catch (Exception e) {
            Throwable t = e;
            if (e instanceof ExecutionException) {
                t = e.getCause();
            }
            if (t instanceof TbScriptException scriptException) {
                throw scriptException;
            }
            throw new RuntimeException("Unexpected error when creating script engine: " + t.getMessage(), t);
        }
    }

    protected abstract Object[] prepareArgs(TbMsg msg);

    @Override
    public ListenableFuture<List<TbMsg>> executeUpdateAsync(TbMsg msg) {
        ListenableFuture<R> result = executeScriptAsync(msg);
        return Futures.transform(result, json -> executeUpdateTransform(msg, json), directExecutor());
    }

    protected abstract List<TbMsg> executeUpdateTransform(TbMsg msg, R result);

    @Override
    public ListenableFuture<TbMsg> executeGenerateAsync(TbMsg prevMsg) {
        return Futures.transform(executeScriptAsync(prevMsg), result -> executeGenerateTransform(prevMsg, result), directExecutor());
    }

    protected abstract TbMsg executeGenerateTransform(TbMsg prevMsg, R result);

    @Override
    public ListenableFuture<Boolean> executeAttributesFilterAsync(Map<String, KvEntry> attributes) {
        Object inArgs = prepareAttributes(attributes);
        return Futures.transform(executeScriptAsync(null, inArgs), this::executeFilterTransform, directExecutor());
    }

    protected abstract Object prepareAttributes(Map<String, KvEntry> attributes);

    @Override
    public ListenableFuture<Boolean> executeFilterAsync(TbMsg msg) {
        return Futures.transform(executeScriptAsync(msg), this::executeFilterTransform, directExecutor());
    }

    protected abstract boolean executeFilterTransform(R result);

    @Override
    public ListenableFuture<Set<String>> executeSwitchAsync(TbMsg msg) {
        return Futures.transform(executeScriptAsync(msg), this::executeSwitchTransform, directExecutor()); // usually runs on a callbackExecutor
    }

    protected abstract Set<String> executeSwitchTransform(R result);

    @Override
    public ListenableFuture<String> executeToStringAsync(TbMsg msg) {
        return Futures.transform(executeScriptAsync(msg), this::executeToStringTransform, directExecutor());
    }

    protected abstract String executeToStringTransform(R result);

    ListenableFuture<R> executeScriptAsync(TbMsg msg) {
        log.trace("execute script async, msg {}", msg);
        Object[] inArgs = prepareArgs(msg);
        return executeScriptAsync(msg.getCustomerId(), inArgs[0], inArgs[1], inArgs[2]);
    }

    private ListenableFuture<R> executeScriptAsync(CustomerId customerId, Object... args) {
        return Futures.transform(scriptInvokeService.invokeScript(tenantId, customerId, scriptId, args), this::convertResult, directExecutor());
    }

    public void destroy() {
        scriptInvokeService.release(scriptId);
    }

    protected abstract R convertResult(Object result);

}
