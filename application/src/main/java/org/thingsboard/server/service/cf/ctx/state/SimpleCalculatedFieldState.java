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
package org.thingsboard.server.service.cf.ctx.state;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.EqualsAndHashCode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.tbel.TbUtils;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.TelemetryCalculatedFieldResult;

@EqualsAndHashCode(callSuper = true)
public class SimpleCalculatedFieldState extends BaseCalculatedFieldState {

    public SimpleCalculatedFieldState(EntityId entityId) {
        super(entityId);
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.SIMPLE;
    }

    @Override
    protected void validateNewEntry(ArgumentEntry newEntry) {
        if (newEntry instanceof TsRollingArgumentEntry) {
            throw new IllegalArgumentException("Rolling argument entry is not supported for simple calculated fields.");
        }
    }

    @Override
    public ListenableFuture<CalculatedFieldResult> performCalculation(CalculatedFieldCtx ctx) {
        double expressionResult = ctx.evaluateSimpleExpression(ctx.getExpression(), this);

        Output output = ctx.getOutput();
        Object result = formatResult(expressionResult, output.getDecimalsByDefault());
        JsonNode outputResult = createResultJson(ctx.isUseLatestTs(), output.getName(), result);

        return Futures.immediateFuture(TelemetryCalculatedFieldResult.builder()
                .type(output.getType())
                .scope(output.getScope())
                .result(outputResult)
                .build());
    }

    private Object formatResult(double expressionResult, Integer decimals) {
        if (decimals == null) {
            return expressionResult;
        }
        if (decimals.equals(0)) {
            return TbUtils.toInt(expressionResult);
        }
        return TbUtils.toFixed(expressionResult, decimals);
    }

    private JsonNode createResultJson(boolean useLatestTs, String outputName, Object result) {
        ObjectNode valuesNode = JacksonUtil.newObjectNode();
        if (result instanceof Double doubleValue) {
            valuesNode.put(outputName, doubleValue);
        } else if (result instanceof Integer integerValue) {
            valuesNode.put(outputName, integerValue);
        } else {
            valuesNode.set(outputName, JacksonUtil.valueToTree(result));
        }

        long latestTs = getLatestTimestamp();
        if (useLatestTs && latestTs != -1) {
            ObjectNode resultNode = JacksonUtil.newObjectNode();
            resultNode.put("ts", latestTs);
            resultNode.set("values", valuesNode);
            return resultNode;
        } else {
            return valuesNode;
        }
    }

}
