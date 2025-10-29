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
package org.thingsboard.server.service.cf.ctx.state.aggregation.function;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunction;

import java.util.Optional;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AvgAggEntry.class, name = "AVG"),
        @JsonSubTypes.Type(value = CountAggEntry.class, name = "COUNT"),
        @JsonSubTypes.Type(value = CountUniqueAggEntry.class, name = "COUNT_UNIQUE"),
        @JsonSubTypes.Type(value = MaxAggEntry.class, name = "MAX"),
        @JsonSubTypes.Type(value = MinAggEntry.class, name = "MIN"),
        @JsonSubTypes.Type(value = SumAggEntry.class, name = "SUM")
})
public interface AggEntry {

    @JsonIgnore
    AggFunction getType();

    void update(Object value);

    Optional<Object> result(Integer precision);

    static AggEntry createAggFunction(AggFunction function) {
        return switch (function) {
            case MIN -> new MinAggEntry();
            case MAX -> new MaxAggEntry();
            case SUM -> new SumAggEntry();
            case AVG -> new AvgAggEntry();
            case COUNT -> new CountAggEntry();
            case COUNT_UNIQUE -> new CountUniqueAggEntry();
        };
    }

}
