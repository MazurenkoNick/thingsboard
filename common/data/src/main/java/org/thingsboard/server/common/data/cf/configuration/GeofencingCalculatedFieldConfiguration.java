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
package org.thingsboard.server.common.data.cf.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates;
import org.thingsboard.server.common.data.cf.configuration.geofencing.ZoneGroupConfiguration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class GeofencingCalculatedFieldConfiguration implements ArgumentsBasedCalculatedFieldConfiguration, ScheduleSupportedCalculatedFieldConfiguration {

    private EntityCoordinates entityCoordinates;
    private List<ZoneGroupConfiguration> zoneGroups;
    private int scheduledUpdateIntervalSec;

    private Output output;

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.GEOFENCING;
    }

    @Override
    @JsonIgnore
    public Map<String, Argument> getArguments() {
        Map<String, Argument> args = new HashMap<>(entityCoordinates.toArguments());
        zoneGroups.forEach(zg -> args.put(zg.getName(), zg.toArgument()));
        return args;
    }

    @Override
    public Output getOutput() {
        return output;
    }

    @Override
    public boolean isScheduledUpdateEnabled() {
        return scheduledUpdateIntervalSec > 0 && zoneGroups.stream().anyMatch(ZoneGroupConfiguration::hasDynamicSource);
    }

    @Override
    public void validate() {
        if (entityCoordinates == null) {
            throw new IllegalArgumentException("Geofencing calculated field entity coordinates must be specified!");
        }
        if (zoneGroups == null || zoneGroups.isEmpty()) {
            throw new IllegalArgumentException("Geofencing calculated field must contain at least one geofencing zone group defined!");
        }
        entityCoordinates.validate();
        Set<String> seen = new HashSet<>();
        for (var zg : zoneGroups) {
            if (!seen.add(zg.getName())) {
                throw new IllegalArgumentException("Geofencing calculated field zone group name must be unique!");
            }
            zg.validate();
        }
    }

}
