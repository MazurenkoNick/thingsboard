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
package org.thingsboard.server.service.cf;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.action.TbAlarmResult;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.List;

@Data
@Builder
@RequiredArgsConstructor
public class AlarmCalculatedFieldResult implements CalculatedFieldResult {

    private final TbAlarmResult alarmResult;

    @Override
    public TbMsg toTbMsg(EntityId entityId, String cfName, List<CalculatedFieldId> cfIds) {
        TbMsgType msgType;
        TbMsgMetaData metaData = new TbMsgMetaData();
        if (alarmResult.isCreated()) {
            msgType = TbMsgType.ALARM_CREATED;
            metaData.putValue(DataConstants.IS_NEW_ALARM, Boolean.TRUE.toString());
        } else if (alarmResult.isUpdated()) {
            msgType = TbMsgType.ALARM_UPDATED;
            metaData.putValue(DataConstants.IS_EXISTING_ALARM, Boolean.TRUE.toString());
        } else if (alarmResult.isSeverityUpdated()) {
            msgType = TbMsgType.ALARM_SEVERITY_UPDATED;
            metaData.putValue(DataConstants.IS_EXISTING_ALARM, Boolean.TRUE.toString());
            metaData.putValue(DataConstants.IS_SEVERITY_UPDATED_ALARM, Boolean.TRUE.toString());
        } else {
            msgType = TbMsgType.ALARM_CLEAR;
            metaData.putValue(DataConstants.IS_CLEARED_ALARM, Boolean.TRUE.toString());
        }
        if (alarmResult.getConditionRepeats() != null) {
            metaData.putValue(DataConstants.ALARM_CONDITION_REPEATS, String.valueOf(alarmResult.getConditionRepeats()));
        }
        if (alarmResult.getConditionDuration() != null) {
            metaData.putValue(DataConstants.ALARM_CONDITION_DURATION, String.valueOf(alarmResult.getConditionDuration()));
        }

        return TbMsg.newMsg()
                .type(msgType)
                .originator(entityId)
                .data(JacksonUtil.toString(alarmResult.getAlarm()))
                .metaData(metaData)
                .build();
    }

    @Override
    public String stringValue() {
        return alarmResult != null ? JacksonUtil.toString(alarmResult) : null;
    }

    @Override
    public boolean isEmpty() {
        return alarmResult == null;
    }

}
