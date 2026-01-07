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
package org.thingsboard.server.common.data.cf.configuration.aggregation;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationPathLevel;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RelatedEntitiesAggregationCalculatedFieldConfigurationTest {

    @Test
    void typeShouldBeEntityAggregation() {
        var cfg = new RelatedEntitiesAggregationCalculatedFieldConfiguration();
        assertThat(cfg.getType()).isEqualTo(CalculatedFieldType.RELATED_ENTITIES_AGGREGATION);
    }

    @Test
    void validateShouldThrowWhenRelationIsNotSet() {
        var cfg = new RelatedEntitiesAggregationCalculatedFieldConfiguration();

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Relation must be specified!");
    }

    @Test
    void validateShouldThrowWhenRelationIsNotValid() {
        var cfg = new RelatedEntitiesAggregationCalculatedFieldConfiguration();

        cfg.setRelation(new RelationPathLevel(null, null));

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Direction must be specified!");
    }

    @Test
    void validateShouldThrowWhenArgumentsMapIsEmpty() {
        var cfg = new RelatedEntitiesAggregationCalculatedFieldConfiguration();

        cfg.setRelation(new RelationPathLevel(EntitySearchDirection.FROM, EntityRelation.CONTAINS_TYPE));

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Arguments map cannot be empty.");
    }

    @Test
    void validateShouldThrowWhenTsRollingArgumentUsed() {
        var cfg = new RelatedEntitiesAggregationCalculatedFieldConfiguration();

        cfg.setRelation(new RelationPathLevel(EntitySearchDirection.FROM, EntityRelation.CONTAINS_TYPE));
        Argument argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("key", ArgumentType.TS_ROLLING, null));
        cfg.setArguments(Map.of("k", argument));

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Calculated field with type: '" + CalculatedFieldType.RELATED_ENTITIES_AGGREGATION + "' doesn't support TS_ROLLING arguments.");
    }

    @Test
    void validateShouldThrowWhenMetricMapIsEmpty() {
        var cfg = new RelatedEntitiesAggregationCalculatedFieldConfiguration();

        cfg.setRelation(new RelationPathLevel(EntitySearchDirection.FROM, EntityRelation.CONTAINS_TYPE));
        cfg.setArguments(Map.of("k", validArgument(ArgumentType.TS_LATEST)));
        cfg.setMetrics(Map.of());

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Metrics map cannot be empty.");
    }

    @Test
    void validateShouldThrowWhenMetricReferencesUnknownArgument() {
        var cfg = new RelatedEntitiesAggregationCalculatedFieldConfiguration();

        cfg.setRelation(new RelationPathLevel(EntitySearchDirection.FROM, EntityRelation.CONTAINS_TYPE));
        cfg.setArguments(Map.of("k", validArgument(ArgumentType.TS_LATEST)));

        AggMetric metric = new AggMetric();
        metric.setInput(new AggKeyInput("unknown"));
        cfg.setMetrics(Map.of("m", metric));

        cfg.setOutput(new TimeSeriesOutput());

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Metric references unknown argument: 'unknown'.");
    }

    private Argument validArgument(ArgumentType type) {
        Argument a = new Argument();
        a.setRefEntityKey(new ReferencedEntityKey("key", type, null));
        return a;
    }

}
