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
package org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.util.TbPair;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class CustomInterval extends BaseAggInterval {

    private Long durationSec;

    public CustomInterval(Long durationSec, Long offsetMillis, String tz) {
        this.tz = tz;
        this.offsetSec = offsetMillis;
        this.durationSec = durationSec;
    }

    @Override
    public AggIntervalType getType() {
        return AggIntervalType.CUSTOM;
    }

    @Override
    public long getIntervalDurationMillis() {
        return Duration.ofSeconds(durationSec).toMillis();
    }

    @Override
    public long getCurrentIntervalStartTs() {
        ZoneId zoneId = ZoneId.of(tz);
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        ZonedDateTime shiftedNow = now.minusSeconds(getOffsetSec());

        long durationMillis = getIntervalDurationMillis();
        long shiftedNowMillis = shiftedNow.toInstant().toEpochMilli();
        long alignedStartMillis = (shiftedNowMillis / durationMillis) * durationMillis;

        long offsetMillis = TimeUnit.SECONDS.toMillis(getOffsetSec());
        return alignedStartMillis + offsetMillis;
    }

    @Override
    public long getCurrentIntervalEndTs() {
        return getCurrentIntervalStartTs() + getIntervalDurationMillis();
    }

    @Override
    public long getDelayUntilIntervalEnd() {
        return getCurrentIntervalEndTs() - System.currentTimeMillis();
    }

    @Override
    public List<TbPair<Long, Long>> getIntervalsBetween(long startTs, long endTs) {
        if (endTs <= startTs) {
            throw new IllegalArgumentException("endTs must be greater than startTs");
        }

        List<TbPair<Long, Long>> result = new ArrayList<>();
        long durationMillis = getIntervalDurationMillis();
        long offsetMillis = TimeUnit.SECONDS.toMillis(getOffsetSec());

        // Apply offset correction
        long shiftedStart = startTs - offsetMillis;
        long shiftedEnd = endTs - offsetMillis;

        // Align start to the interval that contains startTs
        long alignedStart = (shiftedStart / durationMillis) * durationMillis;
        long currentStart = alignedStart;
        long currentEnd = alignedStart + durationMillis;

        // Stop before the interval that contains endTs
        while (currentEnd <= shiftedEnd) {
            long actualStart = currentStart + offsetMillis;
            long actualEnd = currentEnd + offsetMillis;
            result.add(TbPair.of(actualStart, actualEnd));

            currentStart = currentEnd;
            currentEnd += durationMillis;
        }

        return result;
    }

}
