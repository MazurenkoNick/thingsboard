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
package org.thingsboard.server.common.data.cf.configuration.aggregation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentsBasedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.ScheduledUpdateSupportedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.relation.RelationPathLevel;

import java.util.Map;

@Data
public class RelatedEntitiesAggregationCalculatedFieldConfiguration implements ArgumentsBasedCalculatedFieldConfiguration, ScheduledUpdateSupportedCalculatedFieldConfiguration {

    @NotNull
    private RelationPathLevel relation;
    private Map<String, Argument> arguments;
    private long deduplicationIntervalInSec;
    @Valid
    @NotEmpty
    private Map<String, AggMetric> metrics;
    private Output output;
    private boolean useLatestTs;

    private int scheduledUpdateInterval;

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.RELATED_ENTITIES_AGGREGATION;
    }

    @Override
    public boolean isScheduledUpdateEnabled() {
        return true;
    }

    @Override
    public void validate() {
        relation.validate();
        if (arguments.containsKey("ctx")) {
            throw new IllegalArgumentException("Argument name 'ctx' is reserved and cannot be used.");
        }
        if (arguments.values().stream().anyMatch(Argument::hasTsRollingArgument)) {
            throw new IllegalArgumentException("Calculated field with type: '" + getType() + "' doesn't support TS_ROLLING arguments.");
        }
    }

}
