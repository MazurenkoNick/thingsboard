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
package org.thingsboard.server.common.data.report.configuration.timewindow;

import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.IntervalType;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

public class TimeIntervalCalculator {
    public static class TimeRange {
        public final long startTs;
        public final long endTs;

        public TimeRange(long startTs, long endTs) {
            this.startTs = startTs;
            this.endTs = endTs;
        }
    }

    public static TimeRange getTimeRange(TimeWindowConfiguration timeWindowConf, String timezone) {
        History historyConf = timeWindowConf.getHistory();
        return switch (historyConf.getHistoryType()) {
            case 0 -> {
                ZoneId zoneId = timezone != null ? ZoneId.of(timezone) : ZoneId.systemDefault();
                ZonedDateTime now = ZonedDateTime.now(zoneId);
                long currentTimeMillis = now.toInstant().toEpochMilli();
                yield new TimeRange(currentTimeMillis - historyConf.getTimewindowMs(), currentTimeMillis);
            }
            case 1 -> {
                FixedTimeWindow fixedTimeWindow = historyConf.getFixedTimewindow();
                yield new TimeRange(fixedTimeWindow.getStartTimeMs(), historyConf.getFixedTimewindow().getEndTimeMs());
            }
            case 2 -> getQuickTimeRange(historyConf.getQuickInterval(), timezone);
            case 3 -> new TimeRange(0, 0);
            default -> throw new IllegalArgumentException("Unknown history type: " + historyConf.getHistoryType());
        };
    }

    private static TimeRange getQuickTimeRange(QuickTimeInterval interval, String timezone) {
        ZoneId zoneId = timezone != null ? ZoneId.of(timezone) : ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        ZonedDateTime start;
        ZonedDateTime end;

        switch (interval) {
            case YESTERDAY:
                start = now.minusDays(1).toLocalDate().atStartOfDay(zoneId);
                end = start.plusDays(1).minusNanos(1);
                break;
            case DAY_BEFORE_YESTERDAY:
                start = now.minusDays(2).toLocalDate().atStartOfDay(zoneId);
                end = start.plusDays(1).minusNanos(1);
                break;
            case THIS_DAY_LAST_WEEK:
                start = now.minusWeeks(1).toLocalDate().atStartOfDay(zoneId);
                end = start.plusDays(1).minusNanos(1);
                break;
            case PREVIOUS_WEEK:
                start = now.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).toLocalDate().atStartOfDay(zoneId);
                end = start.plusDays(7).minusNanos(1);
                break;
            case PREVIOUS_WEEK_ISO:
                start = now.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay(zoneId);
                end = start.plusDays(7).minusNanos(1);
                break;
            case PREVIOUS_MONTH:
                start = now.minusMonths(1).withDayOfMonth(1).toLocalDate().atStartOfDay(zoneId);
                end = start.plusMonths(1).minusNanos(1);
                break;
            case PREVIOUS_QUARTER: {
                int currentQuarter = (now.getMonthValue() - 1) / 3 + 1;
                int prevQuarter = currentQuarter - 1;
                int startMonth = (prevQuarter - 1) * 3 + 1;
                ZonedDateTime refNow = now;
                if (startMonth < 1) {
                    startMonth += 12;
                    refNow = now.minusYears(1);
                }
                start = ZonedDateTime.of(LocalDate.of(refNow.getYear(), startMonth, 1), LocalTime.MIN, zoneId);
                end = start.plusMonths(3).minusNanos(1);
                break;
            }
            case PREVIOUS_HALF_YEAR:
                if (now.getMonthValue() <= 6) {
                    start = ZonedDateTime.of(LocalDate.of(now.getYear() - 1, 7, 1), LocalTime.MIN, zoneId);
                } else {
                    start = ZonedDateTime.of(LocalDate.of(now.getYear(), 1, 1), LocalTime.MIN, zoneId);
                }
                end = start.plusMonths(6).minusNanos(1);
                break;
            case PREVIOUS_YEAR:
                start = ZonedDateTime.of(LocalDate.of(now.getYear() - 1, 1, 1), LocalTime.MIN, zoneId);
                end = start.plusYears(1).minusNanos(1);
                break;
            case CURRENT_HOUR:
                start = now.truncatedTo(ChronoUnit.HOURS);
                end = start.plusHours(1).minusNanos(1);
                break;
            case CURRENT_DAY:
                start = now.toLocalDate().atStartOfDay(zoneId);
                end = start.plusDays(1).minusNanos(1);
                break;
            case CURRENT_DAY_SO_FAR:
                start = now.toLocalDate().atStartOfDay(zoneId);
                end = now;
                break;
            case CURRENT_WEEK:
                start = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).toLocalDate().atStartOfDay(zoneId);
                end = start.plusDays(7).minusNanos(1);
                break;
            case CURRENT_WEEK_ISO:
                start = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay(zoneId);
                end = start.plusDays(7).minusNanos(1);
                break;
            case CURRENT_WEEK_SO_FAR:
                start = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).toLocalDate().atStartOfDay(zoneId);
                end = now;
                break;
            case CURRENT_WEEK_ISO_SO_FAR:
                start = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay(zoneId);
                end = now;
                break;
            case CURRENT_MONTH:
                start = now.withDayOfMonth(1).toLocalDate().atStartOfDay(zoneId);
                end = start.plusMonths(1).minusNanos(1);
                break;
            case CURRENT_MONTH_SO_FAR:
                start = now.withDayOfMonth(1).toLocalDate().atStartOfDay(zoneId);
                end = now;
                break;
            case CURRENT_QUARTER: {
                int thisQuarter = (now.getMonthValue() - 1) / 3 + 1;
                int quarterStartMonth = (thisQuarter - 1) * 3 + 1;
                start = ZonedDateTime.of(LocalDate.of(now.getYear(), quarterStartMonth, 1), LocalTime.MIN, zoneId);
                end = start.plusMonths(3).minusNanos(1);
                break;
            }
            case CURRENT_QUARTER_SO_FAR: {
                int thisQuarter = (now.getMonthValue() - 1) / 3 + 1;
                int quarterStartMonth = (thisQuarter - 1) * 3 + 1;
                start = ZonedDateTime.of(LocalDate.of(now.getYear(), quarterStartMonth, 1), LocalTime.MIN, zoneId);
                end = now;
                break;
            }
            case CURRENT_HALF_YEAR:
                if (now.getMonthValue() <= 6) {
                    start = ZonedDateTime.of(LocalDate.of(now.getYear(), 1, 1), LocalTime.MIN, zoneId);
                } else {
                    start = ZonedDateTime.of(LocalDate.of(now.getYear(), 7, 1), LocalTime.MIN, zoneId);
                }
                end = start.plusMonths(6).minusNanos(1);
                break;
            case CURRENT_HALF_YEAR_SO_FAR:
                if (now.getMonthValue() <= 6) {
                    start = ZonedDateTime.of(LocalDate.of(now.getYear(), 1, 1), LocalTime.MIN, zoneId);
                } else {
                    start = ZonedDateTime.of(LocalDate.of(now.getYear(), 7, 1), LocalTime.MIN, zoneId);
                }
                end = now;
                break;
            case CURRENT_YEAR:
                start = ZonedDateTime.of(LocalDate.of(now.getYear(), 1, 1), LocalTime.MIN, zoneId);
                end = start.plusYears(1).minusNanos(1);
                break;
            case CURRENT_YEAR_SO_FAR:
                start = ZonedDateTime.of(LocalDate.of(now.getYear(), 1, 1), LocalTime.MIN, zoneId);
                end = now;
                break;
            default:
                throw new IllegalArgumentException("Unsupported interval: " + interval);
        }

        return new TimeRange(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli());
    }

    public static TimeRange getAggTimeRange(TimeRange timeWindow, Interval interval, Aggregation aggregation, ZoneId zoneId, long ts) {
        if (Aggregation.NONE.equals(aggregation)) {
            return new TimeRange(ts, ts);
        } else {
            long startIntervalTs;
            long endIntervalTs;
            if (IntervalType.MILLISECONDS.equals(interval.getIntervalType())) {
                long startTs = timeWindow.startTs;
                startIntervalTs = startTs + (long) (Math.floor( ((double)(ts - startTs)) / (double)interval.getInterval() ) * interval.getInterval());
                endIntervalTs = startIntervalTs + interval.getInterval();
            } else {
                Instant instant = Instant.ofEpochMilli(ts);
                ZonedDateTime time = instant.atZone(zoneId);

                ZonedDateTime startInterval = startIntervalDate(time, interval.getIntervalType());
                ZonedDateTime endInterval = endIntervalDateFromStart(startInterval, interval.getIntervalType()).plus(Duration.ofMillis(1));

                Instant startInstant = Instant.ofEpochMilli(timeWindow.startTs);
                ZonedDateTime start = startInstant.atZone(zoneId);

                if (start.isAfter(startInterval)) {
                    startInterval = start;
                }

                startIntervalTs = startInterval.toInstant().toEpochMilli();
                endIntervalTs = endInterval.toInstant().toEpochMilli();
            }
            endIntervalTs = Math.min(endIntervalTs, timeWindow.endTs);
            return new TimeRange(startIntervalTs, endIntervalTs);
        }
    }
    
    private static ZonedDateTime startIntervalDate(ZonedDateTime current, IntervalType intervalType) {
        switch (intervalType) {
            case WEEK -> {
                return current.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).toLocalDate().atStartOfDay(current.getZone());
            }
            case WEEK_ISO -> {
                return current.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay(current.getZone());
            }
            case MONTH -> {
                return current.withDayOfMonth(1).toLocalDate().atStartOfDay(current.getZone());
            }
            case QUARTER -> {
                int thisQuarter = (current.getMonthValue() - 1) / 3 + 1;
                int quarterStartMonth = (thisQuarter - 1) * 3 + 1;
                return ZonedDateTime.of(LocalDate.of(current.getYear(), quarterStartMonth, 1), LocalTime.MIN, current.getZone());
            }
            case MILLISECONDS -> {
                return current;
            }
            default -> throw new IllegalStateException("Unexpected value: " + intervalType);
        }
    }

    private static ZonedDateTime endIntervalDateFromStart(ZonedDateTime start, IntervalType intervalType) {
        switch (intervalType) {
            case WEEK, WEEK_ISO -> {
                return start.plusDays(7).minusNanos(1);
            }
            case MONTH -> {
                return start.plusMonths(1).minusNanos(1);
            }
            case QUARTER -> {
                return start.plusMonths(3).minusNanos(1);
            }
            case MILLISECONDS -> {
                return start;
            }
            default -> throw new IllegalStateException("Unexpected value: " + intervalType);
        }
    }
    
}
