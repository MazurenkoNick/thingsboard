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

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.util.TbPair;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public abstract class BaseAggInterval implements AggInterval {

    @NotBlank
    protected String tz;
    protected Long offsetSec; // delay seconds since start of interval

    @Override
    public ZoneId getZoneId() {
        return ZoneId.of(tz);
    }

    protected long getOffsetSafe() {
        return offsetSec != null ? offsetSec : 0L;
    }

    @Override
    public long getCurrentIntervalDurationMillis() {
        return getCurrentIntervalEndTs() - getCurrentIntervalStartTs();
    }

    @Override
    public long getCurrentIntervalStartTs() {
        ZoneId zoneId = getZoneId();
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        return getDateTimeIntervalStartTs(now);
    }

    @Override
    public long getDateTimeIntervalStartTs(ZonedDateTime dateTime) {
        long offset = getOffsetSafe();
        ZonedDateTime shiftedNow = dateTime.minusSeconds(offset);
        ZonedDateTime alignedStart = getAlignedBoundary(shiftedNow, false);
        ZonedDateTime actualStart = alignedStart.plusSeconds(offset);
        return actualStart.toInstant().toEpochMilli();
    }

    @Override
    public long getCurrentIntervalEndTs() {
        ZoneId zoneId = getZoneId();
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        return getDateTimeIntervalEndTs(now);
    }

    @Override
    public long getDateTimeIntervalEndTs(ZonedDateTime dateTime) {
        long offset = getOffsetSafe();
        ZonedDateTime shiftedNow = dateTime.minusSeconds(offset);
        ZonedDateTime alignedEnd = getAlignedBoundary(shiftedNow, true);
        ZonedDateTime actualEnd = alignedEnd.plusSeconds(offset);
        return actualEnd.toInstant().toEpochMilli();
    }

    @Override
    public List<TbPair<Long, Long>> getIntervalsBetween(long startTs, long endTs) {
        List<TbPair<Long, Long>> intervals = new ArrayList<>();

        ZonedDateTime startDateTime = Instant.ofEpochMilli(startTs).atZone(getZoneId());
        long startInterval = getDateTimeIntervalStartTs(startDateTime);
        long endTsInterval = getDateTimeIntervalEndTs(startDateTime);

        ZonedDateTime lastIntervalDateTime = Instant.ofEpochMilli(endTs).atZone(getZoneId());
        long lastIntervalEndTs = getDateTimeIntervalEndTs(lastIntervalDateTime);

        while (endTsInterval < lastIntervalEndTs) {
            intervals.add(new TbPair<>(startInterval, endTsInterval));

            startInterval = endTsInterval;
            ZonedDateTime nextIntervalStart = Instant.ofEpochMilli(endTsInterval).atZone(getZoneId());
            endTsInterval = getNextIntervalStart(nextIntervalStart).toInstant().toEpochMilli();
        }

        return intervals;
    }

    protected abstract ZonedDateTime alignToIntervalStart(ZonedDateTime reference);

    protected ZonedDateTime getAlignedBoundary(ZonedDateTime reference, boolean next) {
        ZonedDateTime base = alignToIntervalStart(reference);
        return next ? getNextIntervalStart(base) : base;
    }

    @Override
    public void validate() {
        try {
            getZoneId();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid timezone in interval: " + ex.getMessage());
        }
        if (offsetSec != null) {
            if (offsetSec < 0) {
                throw new IllegalArgumentException("Offset cannot be negative.");
            }
            if (TimeUnit.SECONDS.toMillis(offsetSec) >= getCurrentIntervalDurationMillis()) {
                throw new IllegalArgumentException("Offset must be greater than interval duration.");
            }
        }
    }

}
