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

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface ScheduledUpdateSupportedCalculatedFieldConfiguration extends CalculatedFieldConfiguration {

    Set<TimeUnit> SUPPORTED_TIME_UNITS =
            EnumSet.of(TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS);


    @JsonIgnore
    boolean isScheduledUpdateEnabled();

    int getScheduledUpdateInterval();

    void setScheduledUpdateInterval(int interval);

    TimeUnit getTimeUnit();

    void setTimeUnit(TimeUnit timeUnit);

    @Override
    default void validate() {
        if (!isScheduledUpdateEnabled()) {
            return;
        }
        var timeUnit = getTimeUnit();
        if (timeUnit == null) {
            throw new IllegalArgumentException("Scheduled update time unit should be specified!");
        }
        if (!SUPPORTED_TIME_UNITS.contains(timeUnit)) {
            throw new IllegalArgumentException("Unsupported scheduled update time unit: " + timeUnit +
                                               ". Allowed: " + SUPPORTED_TIME_UNITS);
        }
    }
}
