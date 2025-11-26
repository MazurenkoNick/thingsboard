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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.cf.configuration.OutputStrategy;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.List;
import java.util.Map;

import static org.thingsboard.server.common.data.DataConstants.SCOPE;

@Data
@Builder
public final class TelemetryCalculatedFieldResult implements CalculatedFieldResult {

    private final OutputType type;
    private final AttributeScope scope;
    private final OutputStrategy outputStrategy;
    private final JsonNode result;

    public static final TelemetryCalculatedFieldResult EMPTY = TelemetryCalculatedFieldResult.builder().result(null).build();

    @Override
    public TbMsg toTbMsg(EntityId entityId, List<CalculatedFieldId> cfIds) {
        TbMsgType msgType = switch (type) {
            case ATTRIBUTES -> TbMsgType.POST_ATTRIBUTES_REQUEST;
            case TIME_SERIES -> TbMsgType.POST_TELEMETRY_REQUEST;
        };
        TbMsgMetaData metaData = switch (type) {
            case ATTRIBUTES -> new TbMsgMetaData(Map.of(SCOPE, scope.name()));
            case TIME_SERIES -> TbMsgMetaData.EMPTY;
        };
        return TbMsg.newMsg()
                .type(msgType)
                .originator(entityId)
                .previousCalculatedFieldIds(cfIds)
                .data(stringValue())
                .metaData(metaData)
                .build();
    }

    @Override
    public String stringValue() {
        return result == null ? null : result.toString();
    }

    @Override
    public boolean isEmpty() {
        return result == null || result.isMissingNode() || result.isNull() ||
                (result.isObject() && result.isEmpty()) ||
                (result.isArray() && result.isEmpty()) ||
                (result.isTextual() && result.asText().isEmpty());
    }

}
