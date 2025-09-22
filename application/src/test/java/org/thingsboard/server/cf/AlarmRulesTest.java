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
package org.thingsboard.server.cf;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.action.TbAlarmResult;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.condition.SimpleAlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.TbelAlarmConditionExpression;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.AlarmCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.event.CalculatedFieldDebugEvent;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EventId;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.event.EventDao;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@DaoSqlTest
public class AlarmRulesTest extends AbstractControllerTest {

    @MockitoSpyBean
    private ActorSystemContext actorSystemContext;

    @Autowired
    private EventDao eventDao;

    private DeviceId deviceId;
    private EventId latestEventId;

    @Before
    public void beforeEach() throws Exception {
        loginTenantAdmin();
        Device device = createDevice("Device A", "aaa");
        deviceId = device.getId();
    }

    @Test
    public void testCreateAndSeverityUpdateAndClear() throws Exception {
        Argument temperatureArgument = new Argument();
        temperatureArgument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        Map<String, Argument> arguments = Map.of(
                "temperature", temperatureArgument
        );

        Map<AlarmSeverity, String> createRules = Map.of(
                AlarmSeverity.MAJOR, "return temperature >= 50;",
                AlarmSeverity.CRITICAL, "return temperature >= 100;"
        );
        String clearRule = "return temperature <= 25;";
        CalculatedField calculatedField = createAlarmCf(deviceId, "High Temperature Alarm",
                arguments, createRules, clearRule);

        postTelemetry(deviceId, "{\"temperature\":50}");
        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isCreated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.MAJOR);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
        });

        postTelemetry(deviceId, "{\"temperature\":100}");
        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isSeverityUpdated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
        });

        postTelemetry(deviceId, "{\"temperature\":101}");
        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isUpdated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
        });

        postTelemetry(deviceId, "{\"temperature\":20}");
        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isCleared()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.CLEARED_UNACK);
        });
    }

    private void checkAlarmResult(CalculatedField calculatedField, Consumer<TbAlarmResult> assertion) {
        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            TbAlarmResult alarmResult = getLatestAlarmResult(calculatedField.getId());
            assertThat(alarmResult).isNotNull();
            assertion.accept(alarmResult);

            Alarm alarm = alarmResult.getAlarm();
            assertThat(alarm.getOriginator()).isEqualTo(deviceId);
            assertThat(alarm.getType()).isEqualTo(calculatedField.getName());
        });
    }

    private TbAlarmResult getLatestAlarmResult(CalculatedFieldId calculatedFieldId) {
        List<CalculatedFieldDebugEvent> debugEvents = getDebugEvents(calculatedFieldId, 1);
        if (debugEvents.isEmpty()) {
            return null;
        }
        CalculatedFieldDebugEvent debugEvent = debugEvents.get(0);
        if (debugEvent.getError() != null) {
            System.err.println("CF error: " + debugEvent.getError());
            Assertions.fail();
        }
        if (debugEvent.getId().equals(latestEventId)) {
            return null;
        }
        latestEventId = debugEvent.getId();
        return JacksonUtil.fromString(debugEvent.getResult(), TbAlarmResult.class);
    }

    private CalculatedField createAlarmCf(EntityId entityId,
                                          String alarmType,
                                          Map<String, Argument> arguments,
                                          Map<AlarmSeverity, String> createConditions,
                                          String clearCondition) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(entityId);
        calculatedField.setName(alarmType);
        calculatedField.setType(CalculatedFieldType.ALARM);
        AlarmCalculatedFieldConfiguration configuration = new AlarmCalculatedFieldConfiguration();
        configuration.setArguments(arguments);
        configuration.setCreateRules(new HashMap<>());
        createConditions.forEach((severity, expression) -> {
            configuration.getCreateRules().put(severity, toAlarmRule(expression));
        });
        configuration.setClearRule(toAlarmRule(clearCondition));
        calculatedField.setConfiguration(configuration);
        calculatedField.setDebugSettings(DebugSettings.all());
        return saveCalculatedField(calculatedField);
    }

    private AlarmRule toAlarmRule(String conditionExpression) {
        if (conditionExpression == null) {
            return null;
        }
        AlarmRule rule = new AlarmRule();
        SimpleAlarmCondition condition = new SimpleAlarmCondition();
        TbelAlarmConditionExpression expression = new TbelAlarmConditionExpression();
        expression.setExpression(conditionExpression);
        condition.setExpression(expression);
        rule.setCondition(condition);
        return rule;
    }

    private List<CalculatedFieldDebugEvent> getDebugEvents(CalculatedFieldId calculatedFieldId, int limit) {
        return eventDao.findLatestEvents(tenantId.getId(), calculatedFieldId.getId(), EventType.DEBUG_CALCULATED_FIELD, limit).stream()
                .map(e -> (CalculatedFieldDebugEvent) e).toList();
    }

}
