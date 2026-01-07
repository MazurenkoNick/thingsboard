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
package org.thingsboard.server.common.data.cf.configuration.aggregation.single;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunctionInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggKeyInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggMetric;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.HourInterval;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EntityAggregationCalculatedFieldConfigurationTest {

    @Test
    void typeShouldBeEntityAggregation() {
        var cfg = new EntityAggregationCalculatedFieldConfiguration();
        assertThat(cfg.getType()).isEqualTo(CalculatedFieldType.ENTITY_AGGREGATION);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ATTRIBUTE", "TS_ROLLING"})
    void validateShouldThrowWhenNotTsLatestArgumentUsed(String argumentType) {
        var cfg = new EntityAggregationCalculatedFieldConfiguration();
        cfg.setArguments(Map.of("k", validArgument(ArgumentType.valueOf(argumentType))));
        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Calculated field with type: '" + cfg.getType() + "' support only TS_LATEST arguments.");
    }

    @Test
    void validateShouldThrowWhenMetricMapIsEmpty() {
        var cfg = new EntityAggregationCalculatedFieldConfiguration();

        cfg.setArguments(Map.of("k", validArgument(ArgumentType.TS_LATEST)));
        cfg.setMetrics(Map.of());

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Metrics map cannot be empty.");
    }

    @Test
    void validateShouldThrowWhenMetricInputIsNotAggKeyInput() {
        var cfg = new EntityAggregationCalculatedFieldConfiguration();

        cfg.setArguments(Map.of("k", validArgument(ArgumentType.TS_LATEST)));

        AggMetric metric = new AggMetric();
        metric.setInput(new AggFunctionInput()); // cannot be function
        cfg.setMetrics(Map.of("m", metric));

        cfg.setInterval(new HourInterval("Europe/Kiev", null));
        cfg.setOutput(new TimeSeriesOutput());

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Metric key can only refer to argument.");
    }

    @Test
    void validateShouldThrowWhenMetricReferencesUnknownArgument() {
        var cfg = new EntityAggregationCalculatedFieldConfiguration();

        cfg.setArguments(Map.of("k", validArgument(ArgumentType.TS_LATEST)));

        AggMetric metric = new AggMetric();
        metric.setInput(new AggKeyInput("unknown"));
        cfg.setMetrics(Map.of("m", metric));

        cfg.setInterval(new HourInterval("Europe/Kiev", null));
        cfg.setOutput(new TimeSeriesOutput());

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Metric references unknown argument: 'unknown'.");
    }

    @Test
    void validateShouldThrowWhenIntervalIsNull() {
        var cfg = new EntityAggregationCalculatedFieldConfiguration();

        cfg.setArguments(Map.of("k", validArgument(ArgumentType.TS_LATEST)));
        cfg.setMetrics(Map.of("m", validMetric()));
        cfg.setInterval(null);
        cfg.setOutput(new TimeSeriesOutput());

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Interval must be defined.");
    }

    private Argument validArgument(ArgumentType type) {
        Argument a = new Argument();
        a.setRefEntityKey(new ReferencedEntityKey("key", type, null));
        return a;
    }

    private AggMetric validMetric() {
        AggMetric metric = new AggMetric();
        metric.setInput(new AggKeyInput("k"));
        return metric;
    }

}
