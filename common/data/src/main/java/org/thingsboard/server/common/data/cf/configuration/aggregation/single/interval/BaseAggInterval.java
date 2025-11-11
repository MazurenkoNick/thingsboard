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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.thingsboard.server.common.data.util.TbPair;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseAggInterval implements AggInterval {

    @NotBlank
    protected String tz;
    protected Long offsetSec; // delay seconds since start of interval

    @JsonIgnore
    protected long getOffsetSec() {
        return offsetSec != null ? offsetSec : 0L;
    }

    @Override
    public long getIntervalDurationMillis() {
        return switch (getType()) {
            case HOUR -> Duration.ofHours(1).toMillis();
            case DAY -> Duration.ofDays(1).toMillis();
            case WEEK, WEEK_SUN_SAT -> Duration.ofDays(7L).toMillis();
            case MONTH -> Duration.ofDays(Math.round(30)).toMillis(); // average
            case QUARTER -> Duration.ofDays(Math.round(91)).toMillis();
            case YEAR -> Duration.ofDays(Math.round(365)).toMillis();
            default -> throw new IllegalArgumentException("Unsupported type: " + getType());
        };
    }

    @Override
    public long getCurrentIntervalStartTs() {
        ZoneId zoneId = ZoneId.of(tz);
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        long offset = getOffsetSec();
        ZonedDateTime shiftedNow = now.minusSeconds(offset);
        ZonedDateTime alignedStart = getAlignedBoundary(shiftedNow, false);
        ZonedDateTime actualStart = alignedStart.plusSeconds(offset);
        return actualStart.toInstant().toEpochMilli();
    }

    @Override
    public long getCurrentIntervalEndTs() {
        ZoneId zoneId = ZoneId.of(tz);
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        long offset = getOffsetSec();
        ZonedDateTime shiftedNow = now.minusSeconds(offset);
        ZonedDateTime alignedEnd = getAlignedBoundary(shiftedNow, true);
        ZonedDateTime actualEnd = alignedEnd.plusSeconds(offset);
        return actualEnd.toInstant().toEpochMilli();
    }

    @Override
    public long getDelayUntilIntervalEnd() {
        long currentIntervalEndTs = getCurrentIntervalEndTs();
        long now = System.currentTimeMillis();
        return currentIntervalEndTs - now;
    }

    @Override
    public List<TbPair<Long, Long>> getIntervalsBetween(long startTs, long endTs) {
        if (endTs <= startTs) {
            throw new IllegalArgumentException("endTs must be greater than startTs");
        }

        ZoneId zoneId = ZoneId.of(tz != null ? tz : "UTC");
        long offset = getOffsetSec();

        ZonedDateTime start = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startTs), zoneId)
                .minusSeconds(offset);
        ZonedDateTime end = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endTs), zoneId)
                .minusSeconds(offset);

        ZonedDateTime currentStart = getAlignedBoundary(start, false);
        ZonedDateTime currentEnd = getAlignedBoundary(start, true);

        List<TbPair<Long, Long>> intervals = new ArrayList<>();

        // Iterate through intervals until the next interval would contain endTs
        while (currentEnd.isBefore(end)) {
            long intervalStart = currentStart.plusSeconds(offset).toInstant().toEpochMilli();
            long intervalEnd = currentEnd.plusSeconds(offset).toInstant().toEpochMilli();
            intervals.add(TbPair.of(intervalStart, intervalEnd));

            currentStart = currentEnd;
            currentEnd = getAlignedBoundary(currentStart, true);
        }

        return intervals;
    }

    protected ZonedDateTime getAlignedBoundary(ZonedDateTime reference, boolean next) {
        return switch (getType()) {
            case HOUR -> alignByHours(reference, next);
            case DAY -> alignByDays(reference, next);
            case WEEK -> alignByWeeks(reference, DayOfWeek.MONDAY, next);
            case WEEK_SUN_SAT -> alignByWeeks(reference, DayOfWeek.SUNDAY, next);
            case MONTH -> alignByMonths(reference, next);
            case QUARTER -> alignByQuarters(reference, next);
            case YEAR -> alignByYears(reference, next);
            default -> throw new IllegalArgumentException("Unsupported interval type: " + getType());
        };
    }

    private ZonedDateTime alignByHours(ZonedDateTime now, boolean next) {
        ZonedDateTime base = now.truncatedTo(ChronoUnit.HOURS);
        return next ? base.plusHours(1) : base;
    }

    private ZonedDateTime alignByDays(ZonedDateTime now, boolean next) {
        ZonedDateTime base = now.truncatedTo(ChronoUnit.DAYS);
        return next ? base.plusDays(1) : base;
    }

    private ZonedDateTime alignByWeeks(ZonedDateTime now, DayOfWeek startOfWeek, boolean next) {
        ZonedDateTime startOfWeekDate = now.with(TemporalAdjusters.previousOrSame(startOfWeek))
                .truncatedTo(ChronoUnit.DAYS);
        return next ? startOfWeekDate.plusWeeks(1) : startOfWeekDate;
    }

    private ZonedDateTime alignByMonths(ZonedDateTime now, boolean next) {
        ZonedDateTime base = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        return next ? base.plusMonths(1) : base;
    }

    private ZonedDateTime alignByQuarters(ZonedDateTime now, boolean next) {
        int month = now.getMonthValue();
        int quarterStartMonth = ((month - 1) / 3) * 3 + 1; // 1, 4, 7, 10
        ZonedDateTime base = ZonedDateTime.of(
                LocalDate.of(now.getYear(), quarterStartMonth, 1),
                LocalTime.MIDNIGHT,
                now.getZone());
        return next ? base.plusMonths(3) : base;
    }

    private ZonedDateTime alignByYears(ZonedDateTime now, boolean next) {
        ZonedDateTime base = ZonedDateTime.of(
                LocalDate.of(now.getYear(), 1, 1),
                LocalTime.MIDNIGHT,
                now.getZone());
        return next ? base.plusYears(1) : base;
    }

}
