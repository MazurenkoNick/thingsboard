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
package org.thingsboard.server.service.cf.ctx.state.alarm;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.KvUtil;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionType;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionValue;
import org.thingsboard.server.common.data.alarm.rule.condition.DurationAlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.RepeatingAlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.AlarmConditionExpression;
import org.thingsboard.server.common.data.alarm.rule.condition.schedule.AlarmSchedule;
import org.thingsboard.server.common.data.alarm.rule.condition.schedule.CustomTimeSchedule;
import org.thingsboard.server.common.data.alarm.rule.condition.schedule.CustomTimeScheduleItem;
import org.thingsboard.server.common.data.alarm.rule.condition.schedule.SpecificTimeSchedule;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.msg.tools.SchedulerUtils;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.Function;

@Data
@Slf4j
public class AlarmRuleState {

    private final AlarmSeverity severity;
    private AlarmRule alarmRule;
    private AlarmCalculatedFieldState state;

    private AlarmCondition condition;

    private long lastEventTs;
    private long duration;
    private long eventCount;

    public AlarmRuleState(AlarmSeverity severity, AlarmRule alarmRule, AlarmCalculatedFieldState state) {
        this.severity = severity;
        if (alarmRule != null) {
            setAlarmRule(alarmRule);
        }
        this.state = state;
    }

    public AlarmEvalResult eval(boolean newEvent, CalculatedFieldCtx ctx) { // on event or config change
        boolean active = isActive(state.getLatestTimestamp());
        return switch (condition.getType()) {
            case SIMPLE -> evalSimple(active, ctx);
            case DURATION -> evalDuration(active, ctx);
            case REPEATING -> evalRepeating(active, newEvent, ctx);
        };
    }

    public AlarmEvalResult eval(long ts) { // on schedule
        switch (condition.getType()) {
            case SIMPLE, REPEATING -> {
                return AlarmEvalResult.NOT_YET_TRUE;
            }
            case DURATION -> {
                long requiredDurationInMs = getRequiredDurationInMs();
                if (requiredDurationInMs > 0 && lastEventTs > 0 && ts > lastEventTs) {
                    long duration = this.duration + (ts - lastEventTs);
                    if (isActive(ts)) {
                        return duration > requiredDurationInMs ? AlarmEvalResult.TRUE : AlarmEvalResult.NOT_YET_TRUE;
                    } else {
                        return AlarmEvalResult.FALSE;
                    }
                }
            }
        }
        return AlarmEvalResult.FALSE;
    }

    private AlarmEvalResult evalSimple(boolean active, CalculatedFieldCtx ctx) {
        return (active && eval(condition.getExpression(), ctx)) ?
                AlarmEvalResult.TRUE : AlarmEvalResult.FALSE;
    }

    private AlarmEvalResult evalRepeating(boolean active, boolean newEvent, CalculatedFieldCtx ctx) {
        if (active && eval(condition.getExpression(), ctx)) {
            if (newEvent) {
                eventCount++;
            }
            long requiredRepeats = getIntValue(((RepeatingAlarmCondition) condition).getCount());
            return eventCount >= requiredRepeats ? AlarmEvalResult.TRUE : AlarmEvalResult.NOT_YET_TRUE;
        } else {
            return AlarmEvalResult.FALSE;
        }
    }

    private AlarmEvalResult evalDuration(boolean active, CalculatedFieldCtx ctx) {
        if (active && eval(condition.getExpression(), ctx)) {
            if (lastEventTs > 0) {
                if (state.getLatestTimestamp() > lastEventTs) {
                    duration = duration + (state.getLatestTimestamp() - lastEventTs);
                    lastEventTs = state.getLatestTimestamp();
                }
            } else {
                lastEventTs = state.getLatestTimestamp();
                duration = 0L;
            }
            long requiredDurationInMs = getRequiredDurationInMs();
            return duration > requiredDurationInMs ? AlarmEvalResult.TRUE : AlarmEvalResult.NOT_YET_TRUE;
        } else {
            return AlarmEvalResult.FALSE;
        }
    }

    private boolean isActive(long eventTs) {
        if (condition.getSchedule() == null) {
            return true;
        }
        AlarmSchedule schedule = getValue(condition.getSchedule(), entry -> Optional.ofNullable(KvUtil.getStringValue(entry))
                .map(str -> JsonConverter.parse(str, AlarmSchedule.class))
                .orElse(null));
        return switch (schedule.getType()) {
            case ANY_TIME -> true;
            case SPECIFIC_TIME -> isActiveSpecific((SpecificTimeSchedule) schedule, eventTs);
            case CUSTOM -> isActiveCustom((CustomTimeSchedule) schedule, eventTs);
        };
    }

    private boolean isActiveSpecific(SpecificTimeSchedule schedule, long eventTs) {
        ZoneId zoneId = SchedulerUtils.getZoneId(schedule.getTimezone());
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(eventTs), zoneId);
        if (schedule.getDaysOfWeek().size() != 7) {
            int dayOfWeek = zdt.getDayOfWeek().getValue();
            if (!schedule.getDaysOfWeek().contains(dayOfWeek)) {
                return false;
            }
        }
        long endsOn = schedule.getEndsOn();
        if (endsOn == 0) {
            // 24 hours in milliseconds
            endsOn = 86400000;
        }

        return isActive(eventTs, zoneId, zdt, schedule.getStartsOn(), endsOn);
    }

    private boolean isActiveCustom(CustomTimeSchedule schedule, long eventTs) {
        ZoneId zoneId = SchedulerUtils.getZoneId(schedule.getTimezone());
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(eventTs), zoneId);
        int dayOfWeek = zdt.toLocalDate().getDayOfWeek().getValue();
        for (CustomTimeScheduleItem item : schedule.getItems()) {
            if (item.getDayOfWeek() == dayOfWeek) {
                if (item.isEnabled()) {
                    long endsOn = item.getEndsOn();
                    if (endsOn == 0) {
                        // 24 hours in milliseconds
                        endsOn = 86400000;
                    }
                    return isActive(eventTs, zoneId, zdt, item.getStartsOn(), endsOn);
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    private boolean isActive(long eventTs, ZoneId zoneId, ZonedDateTime zdt, long startsOn, long endsOn) {
        long startOfDay = zdt.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli();
        long msFromStartOfDay = eventTs - startOfDay;
        if (startsOn <= endsOn) {
            return startsOn <= msFromStartOfDay && endsOn > msFromStartOfDay;
        } else {
            return startsOn < msFromStartOfDay || (0 < msFromStartOfDay && msFromStartOfDay < endsOn);
        }
    }

    public void clear() {
        eventCount = 0L;
        lastEventTs = 0L;
        duration = 0L;
    }

    private Integer getIntValue(AlarmConditionValue<Integer> value) {
        return getValue(value, entry -> Optional.ofNullable(KvUtil.getLongValue(entry)).map(Long::intValue).orElse(null));
    }

    private long getRequiredDurationInMs() {
        DurationAlarmCondition durationCondition = (DurationAlarmCondition) condition;
        return durationCondition.getUnit().toMillis(getValue(durationCondition.getValue(), KvUtil::getLongValue));
    }

    private boolean eval(AlarmConditionExpression expression, CalculatedFieldCtx ctx) {
        return state.eval(expression, ctx);
    }

    private <T> T getValue(AlarmConditionValue<T> conditionValue, Function<KvEntry, T> mapper) {
        T value = conditionValue.getStaticValue();
        if (value == null) {
            String argument = conditionValue.getDynamicValueArgument();
            SingleValueArgumentEntry entry = state.getArgument(argument);
            value = mapper.apply(entry.getKvEntryValue());
            if (value == null) {
                throw new IllegalArgumentException("No value found for argument " + argument);
            }
        }
        return value;
    }

    public void setAlarmRule(AlarmRule alarmRule) {
        this.alarmRule = alarmRule;
        this.condition = alarmRule.getCondition();
    }

    public StateInfo getStateInfo() {
        if (condition.getType() == AlarmConditionType.REPEATING) {
            return new StateInfo(eventCount, null);
        } else if (condition.getType() == AlarmConditionType.DURATION) {
            return new StateInfo(null, duration + (System.currentTimeMillis() - lastEventTs));
        } else {
            return StateInfo.EMPTY;
        }
    }

    @Override
    public String toString() {
        return "AlarmRuleState{" +
               "severity=" + severity +
               ", condition=" + condition +
               ", lastEventTs=" + lastEventTs +
               ", duration=" + duration +
               ", eventCount=" + eventCount +
               '}';
    }

    public record StateInfo(Long eventCount, Long duration) {
        static final StateInfo EMPTY = new StateInfo(null, null);

    }

}
