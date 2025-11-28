/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.cf.ctx.state.propagation;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.PropagationCalculatedFieldResult;
import org.thingsboard.server.service.cf.TelemetryCalculatedFieldResult;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.ScriptCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;

import java.util.ArrayList;
import java.util.Map;

import static org.thingsboard.server.common.data.cf.configuration.PropagationCalculatedFieldConfiguration.PROPAGATION_CONFIG_ARGUMENT;

public class PropagationCalculatedFieldState extends ScriptCalculatedFieldState {

    public PropagationCalculatedFieldState(EntityId entityId) {
        super(entityId);
    }

    @Override
    public void setCtx(CalculatedFieldCtx ctx, TbActorRef actorCtx) {
        this.ctx = ctx;
        this.actorCtx = actorCtx;
        this.requiredArguments = new ArrayList<>(ctx.getArgNames());
        requiredArguments.add(PROPAGATION_CONFIG_ARGUMENT);
        this.readinessStatus = checkReadiness(requiredArguments, arguments);
        if (ctx.isApplyExpressionForResolvedArguments()) {
            this.tbelExpression = ctx.getTbelExpressions().get(ctx.getExpression());
        }
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.PROPAGATION;
    }

    @Override
    public ListenableFuture<CalculatedFieldResult> performCalculation(Map<String, ArgumentEntry> updatedArgs, CalculatedFieldCtx ctx) {
        ArgumentEntry argumentEntry = arguments.get(PROPAGATION_CONFIG_ARGUMENT);
        if (!(argumentEntry instanceof PropagationArgumentEntry propagationArgumentEntry) || propagationArgumentEntry.isEmpty()) {
            return Futures.immediateFuture(PropagationCalculatedFieldResult.builder().build());
        }
        if (ctx.isApplyExpressionForResolvedArguments()) {
            return Futures.transform(super.performCalculation(updatedArgs, ctx), telemetryCfResult ->
                            PropagationCalculatedFieldResult.builder()
                                    .propagationEntityIds(propagationArgumentEntry.getPropagationEntityIds())
                                    .result((TelemetryCalculatedFieldResult) telemetryCfResult)
                                    .build(),
                    MoreExecutors.directExecutor());
        }
        return Futures.immediateFuture(PropagationCalculatedFieldResult.builder()
                .propagationEntityIds(propagationArgumentEntry.getPropagationEntityIds())
                .result(toTelemetryResult(ctx))
                .build());
    }

    private TelemetryCalculatedFieldResult toTelemetryResult(CalculatedFieldCtx ctx) {
        Output output = ctx.getOutput();
        TelemetryCalculatedFieldResult.TelemetryCalculatedFieldResultBuilder telemetryCfBuilder =
                TelemetryCalculatedFieldResult.builder()
                        .calculatedFieldName(ctx.getCalculatedField().getName())
                        .outputStrategy(output.getStrategy())
                        .type(output.getType())
                        .scope(output.getScope());
        ObjectNode valuesNode = JacksonUtil.newObjectNode();
        arguments.forEach((outputKey, argumentEntry) -> {
            if (argumentEntry instanceof PropagationArgumentEntry) {
                return;
            }
            if (argumentEntry instanceof SingleValueArgumentEntry singleArgumentEntry) {
                JacksonUtil.addKvEntry(valuesNode, singleArgumentEntry.getKvEntryValue(), outputKey);
                return;
            }
            throw new IllegalArgumentException("Unsupported argument type: " + argumentEntry.getType() + " detected for argument: " + outputKey + ". " +
                                               "Only Latest telemetry or Attribute arguments supported for 'Arguments Only' propagation mode!");
        });
        ObjectNode result = toSimpleResult(output.getType() == OutputType.TIME_SERIES, valuesNode);
        telemetryCfBuilder.result(result);
        return telemetryCfBuilder.build();
    }

}
