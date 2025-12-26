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
package org.thingsboard.server.service.solutions.data;

import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.AlarmCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.UUID;
import java.util.stream.Collectors;

public record CreatedAlarmRuleInfo(EntityId entityId, String entityName, String alarmType,
                                   String severities) implements HasAppliedToEntity {

    public static CreatedAlarmRuleInfo from(EntityId entityId, String entityName, CalculatedField calculatedField) {
        if (calculatedField.getType() != CalculatedFieldType.ALARM) {
            throw new UnsupportedOperationException("Only alarm calculated fields are supported");
        }
        String severities = ((AlarmCalculatedFieldConfiguration) calculatedField.getConfiguration())
                .getCreateRules().keySet().stream()
                .map(AlarmSeverity::getDisplayName)
                .sorted()
                .collect(Collectors.joining(", "));
        return new CreatedAlarmRuleInfo(entityId, entityName, calculatedField.getName(), severities);
    }

    @Override
    public String getCfPageLink(UUID cfId) {
        return "/alarms/alarm-rules/" + cfId;
    }
}
