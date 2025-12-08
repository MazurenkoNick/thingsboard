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
package org.thingsboard.server.common.data.cf.configuration.aggregation.single;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ArgumentsBasedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggKeyInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggMetric;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.AggInterval;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.Watermark;

import java.util.Map;

@Data
public class EntityAggregationCalculatedFieldConfiguration implements ArgumentsBasedCalculatedFieldConfiguration {

    private Map<String, Argument> arguments;
    @Valid
    @NotEmpty
    private Map<String, AggMetric> metrics;
    @Valid
    @NotNull
    private AggInterval interval;
    @Valid
    private Watermark watermark;
    private boolean produceIntermediateResult;
    @Valid
    @NotNull
    private Output output;

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.ENTITY_AGGREGATION;
    }

    @Override
    public void validate() {
        validateArguments();
        validateMetrics();
        validateInterval();
    }

    private void validateArguments() {
        if (arguments.containsKey("ctx")) {
            throw new IllegalArgumentException("Argument name 'ctx' is reserved and cannot be used.");
        }
        if (arguments.values().stream().anyMatch(argument -> !ArgumentType.TS_LATEST.equals(argument.getRefEntityKey().getType()))) {
            throw new IllegalArgumentException("Calculated field with type: '" + getType() + "' support only TS_LATEST arguments.");
        }
    }

    private void validateMetrics() {
        if (metrics == null || metrics.isEmpty()) {
            throw new IllegalArgumentException("Metrics map cannot be empty.");
        }

        for (AggMetric metric : metrics.values()) {
            if (metric.getInput() instanceof AggKeyInput aggKeyInput) {
                if (!arguments.containsKey(aggKeyInput.getKey())) {
                    throw new IllegalArgumentException(
                            "Metric references unknown argument: '" + aggKeyInput.getKey() + "'."
                    );
                }
            } else {
                throw new IllegalArgumentException("Metric key can only refer to argument.");
            }
        }
    }

    private void validateInterval() {
        if (interval == null) {
            throw new IllegalArgumentException("Interval must be defined.");
        }
        interval.validate();
    }

}
