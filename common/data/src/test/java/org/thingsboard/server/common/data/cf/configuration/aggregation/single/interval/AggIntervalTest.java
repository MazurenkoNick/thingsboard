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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.thingsboard.server.common.data.util.TbPair;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class AggIntervalTest {

    private static final String TZ = "Europe/Kiev";

    @ParameterizedTest
    @MethodSource("intervals")
    void testGetStartAndEndWithoutOffset(LongFunction<AggInterval> intervalCreator) {
        AggInterval interval = intervalCreator.apply(0L);

        ZonedDateTime dateTime = ZonedDateTime.of(
                // 2025.11.11 00:00:00
                2025, 11, 11, 0, 0, 0, 0, ZoneId.of(TZ)
        );
        long startTs = interval.getDateTimeIntervalStartTs(dateTime);
        long endTs = interval.getDateTimeIntervalEndTs(dateTime);

        assertThat(endTs).isGreaterThan(startTs);
        assertThat(endTs - startTs).isEqualTo(interval.getCurrentIntervalDurationMillis());
    }

    @ParameterizedTest
    @MethodSource("intervals")
    void testApplyOffset(LongFunction<AggInterval> intervalCreator) {
        long offsetSec = TimeUnit.MINUTES.toSeconds(15);
        AggInterval intervalWithOffset = intervalCreator.apply(offsetSec);
        AggInterval intervalNoOffset = intervalCreator.apply(0L);

        ZonedDateTime dateTime = ZonedDateTime.of(
                // 2025.11.11 11:20:00 - chosen so 15m offset shifts into a new interval
                2025, 11, 11, 11, 20, 0, 0, ZoneId.of(TZ)
        );

        long startWithOffsetTs = intervalWithOffset.getDateTimeIntervalStartTs(dateTime);
        long startNoOffsetTs = intervalNoOffset.getDateTimeIntervalStartTs(dateTime);

        ZonedDateTime startWithOffset = Instant.ofEpochMilli(startWithOffsetTs).atZone(intervalWithOffset.getZoneId());
        ZonedDateTime startNoOffset = Instant.ofEpochMilli(startNoOffsetTs).atZone(intervalNoOffset.getZoneId());

        long actualOffset = Duration.between(startNoOffset, startWithOffset).toSeconds();
        assertThat(actualOffset).isEqualTo(offsetSec);
    }

    private static Stream<Arguments> intervals() {
        return Stream.of(
                Arguments.of((LongFunction<AggInterval>) offset -> new HourInterval(TZ, offset)),
                Arguments.of((LongFunction<AggInterval>) offset -> new DayInterval(TZ, offset)),
                Arguments.of((LongFunction<AggInterval>) offset -> new WeekInterval(TZ, offset)),
                Arguments.of((LongFunction<AggInterval>) offset -> new WeekSunSatInterval(TZ, offset)),
                Arguments.of((LongFunction<AggInterval>) offset -> new MonthInterval(TZ, offset)),
                Arguments.of((LongFunction<AggInterval>) offset -> new QuarterInterval(TZ, offset)),
                Arguments.of((LongFunction<AggInterval>) offset -> new YearInterval(TZ, offset)),
                Arguments.of((LongFunction<AggInterval>) offset -> new CustomInterval(TZ, offset, TimeUnit.HOURS.toSeconds(4)))
        );
    }

    @ParameterizedTest
    @MethodSource("nextIntervalFromExactDate")
    void testNextIntervalFromExactDate(LongFunction<AggInterval> intervalCreator, Function<ZonedDateTime, ZonedDateTime> expectedDateTimeFunction) {
        AggInterval interval = intervalCreator.apply(0L);

        ZonedDateTime currentStart = ZonedDateTime.of(
                2025, 11, 11, 0, 0, 0, 0, ZoneId.of(TZ)
        );

        ZonedDateTime nextStart = interval.getNextIntervalStart(currentStart);

        assertThat(nextStart).isEqualTo(expectedDateTimeFunction.apply(currentStart));
    }

    private static Stream<Arguments> nextIntervalFromExactDate() {
        return Stream.of(
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new HourInterval(TZ, offset),
                        (Function<ZonedDateTime, ZonedDateTime>) currentInterval -> currentInterval.plusHours(1)
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new DayInterval(TZ, offset),
                        (Function<ZonedDateTime, ZonedDateTime>) currentInterval -> currentInterval.plusDays(1)
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new WeekInterval(TZ, offset),
                        (Function<ZonedDateTime, ZonedDateTime>) currentInterval -> currentInterval.plusWeeks(1)
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new WeekSunSatInterval(TZ, offset),
                        (Function<ZonedDateTime, ZonedDateTime>) currentInterval -> currentInterval.plusWeeks(1)
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new MonthInterval(TZ, offset),
                        (Function<ZonedDateTime, ZonedDateTime>) currentInterval -> currentInterval.plusMonths(1)
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new QuarterInterval(TZ, offset),
                        (Function<ZonedDateTime, ZonedDateTime>) currentInterval -> currentInterval.plusMonths(3)
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new YearInterval(TZ, offset),
                        (Function<ZonedDateTime, ZonedDateTime>) currentInterval -> currentInterval.plusYears(1)
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new CustomInterval(TZ, offset, TimeUnit.HOURS.toSeconds(4)),
                        (Function<ZonedDateTime, ZonedDateTime>) currentInterval -> currentInterval.plusHours(4)
                )
        );
    }

    @ParameterizedTest
    @MethodSource("intervalBetween")
    void testGetIntervalsBetween(LongFunction<AggInterval> intervalCreator, Long startTs, Long endTs, Consumer<List<TbPair<Long, Long>>> expectedIntervals) {
        AggInterval interval = intervalCreator.apply(0L);

        List<TbPair<Long, Long>> intervalsBetween = interval.getIntervalsBetween(startTs, endTs);

        expectedIntervals.accept(intervalsBetween);
    }

    private static Stream<Arguments> intervalBetween() {
        return Stream.of(
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new HourInterval(TZ, offset),
                        ZonedDateTime.of(2025, 11, 11, 0, 24, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                        ZonedDateTime.of(2025, 11, 11, 3, 25, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                        (Consumer<List<TbPair<Long, Long>>>) intervals -> {
                            assertThat(intervals).hasSize(3);
                            assertThat(intervals.get(0)).isEqualTo(new TbPair<>(
                                            ZonedDateTime.of(2025, 11, 11, 0, 0, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                                            ZonedDateTime.of(2025, 11, 11, 1, 0, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli()
                                    )
                            );
                            assertThat(intervals.get(1)).isEqualTo(new TbPair<>(
                                            ZonedDateTime.of(2025, 11, 11, 1, 0, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                                            ZonedDateTime.of(2025, 11, 11, 2, 0, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli()
                                    )
                            );
                            assertThat(intervals.get(2)).isEqualTo(new TbPair<>(
                                            ZonedDateTime.of(2025, 11, 11, 2, 0, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                                            ZonedDateTime.of(2025, 11, 11, 3, 0, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli()
                                    )
                            );
                        }
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new DayInterval(TZ, offset),
                        ZonedDateTime.of(2025, 11, 10, 0, 24, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                        ZonedDateTime.of(2025, 11, 12, 4, 25, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                        (Consumer<List<TbPair<Long, Long>>>) intervals -> {
                            assertThat(intervals).hasSize(2);
                            assertThat(intervals.get(0)).isEqualTo(new TbPair<>(
                                            ZonedDateTime.of(2025, 11, 10, 0, 0, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                                            ZonedDateTime.of(2025, 11, 11, 0, 0, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli()
                                    )
                            );
                            assertThat(intervals.get(1)).isEqualTo(new TbPair<>(
                                            ZonedDateTime.of(2025, 11, 11, 0, 0, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                                            ZonedDateTime.of(2025, 11, 12, 0, 0, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli()
                                    )
                            );
                        }
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new WeekInterval(TZ, offset),
                        ZonedDateTime.of(2025, 11, 4, 0, 24, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                        ZonedDateTime.of(2025, 11, 12, 4, 25, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                        (Consumer<List<TbPair<Long, Long>>>) intervals -> {
                            assertThat(intervals).hasSize(1);
                            assertThat(intervals.get(0)).isEqualTo(new TbPair<>(
                                            ZonedDateTime.of(2025, 11, 3, 0, 0, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                                            ZonedDateTime.of(2025, 11, 10, 0, 0, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli()
                                    )
                            );
                        }
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new WeekSunSatInterval(TZ, offset),
                        ZonedDateTime.of(2025, 11, 4, 0, 24, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                        ZonedDateTime.of(2025, 11, 12, 4, 25, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                        (Consumer<List<TbPair<Long, Long>>>) intervals -> {
                            assertThat(intervals).hasSize(1);
                            assertThat(intervals.get(0)).isEqualTo(new TbPair<>(
                                            ZonedDateTime.of(2025, 11, 2, 0, 0, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                                            ZonedDateTime.of(2025, 11, 9, 0, 0, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli()
                                    )
                            );
                        }
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new MonthInterval(TZ, offset),
                        ZonedDateTime.of(2025, 9, 4, 0, 24, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                        ZonedDateTime.of(2025, 11, 12, 4, 25, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                        (Consumer<List<TbPair<Long, Long>>>) intervals -> {
                            assertThat(intervals).hasSize(2);
                            assertThat(intervals.get(0)).isEqualTo(new TbPair<>(
                                            ZonedDateTime.of(2025, 9, 1, 0, 0, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                                            ZonedDateTime.of(2025, 10, 1, 0, 0, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli()
                                    )
                            );
                            assertThat(intervals.get(1)).isEqualTo(new TbPair<>(
                                            ZonedDateTime.of(2025, 10, 1, 0, 0, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                                            ZonedDateTime.of(2025, 11, 1, 0, 0, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli()
                                    )
                            );
                        }
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new QuarterInterval(TZ, offset),
                        ZonedDateTime.of(2025, 8, 4, 0, 24, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                        ZonedDateTime.of(2025, 11, 12, 4, 25, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                        (Consumer<List<TbPair<Long, Long>>>) intervals -> {
                            assertThat(intervals).hasSize(1);
                            assertThat(intervals.get(0)).isEqualTo(new TbPair<>(
                                            ZonedDateTime.of(2025, 7, 1, 0, 0, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                                            ZonedDateTime.of(2025, 10, 1, 0, 0, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli()
                                    )
                            );
                        }
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new YearInterval(TZ, offset),
                        ZonedDateTime.of(2024, 8, 4, 0, 24, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                        ZonedDateTime.of(2025, 11, 12, 4, 25, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                        (Consumer<List<TbPair<Long, Long>>>) intervals -> {
                            assertThat(intervals).hasSize(1);
                            assertThat(intervals.get(0)).isEqualTo(new TbPair<>(
                                            ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                                            ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli()
                                    )
                            );
                        }
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new CustomInterval(TZ, offset, TimeUnit.HOURS.toSeconds(4)),
                        ZonedDateTime.of(2025, 11, 10, 22, 24, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                        ZonedDateTime.of(2025, 11, 11, 3, 25, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                        (Consumer<List<TbPair<Long, Long>>>) intervals -> {
                            assertThat(intervals).hasSize(1);
                            assertThat(intervals.get(0)).isEqualTo(new TbPair<>(
                                            ZonedDateTime.of(2025, 11, 10, 20, 0, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli(),
                                            ZonedDateTime.of(2025, 11, 11, 0, 0, 0, 0, ZoneId.of(TZ)).toInstant().toEpochMilli()
                                    )
                            );
                        }
                )
        );
    }

}
