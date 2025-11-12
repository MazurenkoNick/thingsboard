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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingCalculatedFieldConfiguration;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class ScheduledUpdateSupportedCalculatedFieldConfigurationTest {

    @Test
    void validateDoesNotThrowAnyExceptionWhenScheduledUpdateIntervalIsGreaterThanMinAllowedIntervalInTenantProfile() {
        int scheduledUpdateInterval = 60;
        int minAllowedInterval = scheduledUpdateInterval - 1;

        var cfg = new GeofencingCalculatedFieldConfiguration();
        cfg.setScheduledUpdateInterval(scheduledUpdateInterval);
        assertThatCode(() -> cfg.validate(minAllowedInterval)).doesNotThrowAnyException();
    }

    @Test
    void validateShouldThrowWhenScheduledUpdateIntervalIsLessThanMinAllowedIntervalInTenantProfile() {
        int minAllowedInterval = (int) TimeUnit.HOURS.toSeconds(2);

        var cfg = new GeofencingCalculatedFieldConfiguration();
        cfg.setScheduledUpdateInterval(1);

        assertThatThrownBy(() -> cfg.validate(minAllowedInterval))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Scheduled update interval is less than configured " +
                            "minimum allowed interval in tenant profile: " + minAllowedInterval);
    }

}
