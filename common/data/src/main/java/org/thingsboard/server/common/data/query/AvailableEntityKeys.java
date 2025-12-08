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
package org.thingsboard.server.common.data.query;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNullElse;

@Schema(
        description = "Contains unique time series and attribute key names discovered from entities matching a query. Used primarily for UI hints such as autocomplete suggestions."
)
public record AvailableEntityKeys(
        @Schema(
                description = "Set of entity types found among the matched entities.",
                example = "[\"DEVICE\", \"ASSET\"]",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Set<EntityType> entityTypes,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @ArraySchema(
                arraySchema = @Schema(description = "List of unique time series key names available on the matched entities."),
                schema = @Schema(implementation = String.class, example = "temperature"),
                uniqueItems = true
        )
        List<String> timeseries,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @ArraySchema(
                arraySchema = @Schema(description = "List of unique attribute key names available on the matched entities."),
                schema = @Schema(implementation = String.class, example = "serialNumber"),
                uniqueItems = true
        )
        List<String> attribute
) {

    public AvailableEntityKeys {
        entityTypes = requireNonNullElse(entityTypes, emptySet());
        timeseries = requireNonNullElse(timeseries, emptyList());
        attribute = requireNonNullElse(attribute, emptyList());
    }

    public static AvailableEntityKeys none() {
        return new AvailableEntityKeys(emptySet(), emptyList(), emptyList());
    }

}
