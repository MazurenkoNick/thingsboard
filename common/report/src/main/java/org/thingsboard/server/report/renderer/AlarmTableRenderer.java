/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.report.renderer;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;

import java.util.HashMap;
import java.util.Map;


@Component
public class AlarmTableRenderer extends TableWithLayoutComponentRenderer<AlarmTableComponent> {

    private static final Map<String, String> SEVERITY_COLOR = new HashMap<>();
    private static final Map<String, String> DISPLAY_STATUS = new HashMap<>();

    static {
        SEVERITY_COLOR.put("CRITICAL", "rgb(209, 39, 48)");
        SEVERITY_COLOR.put("MAJOR", "rgb(246, 103, 22)");
        SEVERITY_COLOR.put("MINOR", "rgb(250, 164, 5)");
        SEVERITY_COLOR.put("WARNING", "rgb(242, 218, 5)");
        SEVERITY_COLOR.put("INDETERMINATE", "rgba(0, 0, 0, 0.38)");

        DISPLAY_STATUS.put("ACTIVE_UNACK", "Active Unacknowledged");
        DISPLAY_STATUS.put("ACTIVE_ACK", "Active Acknowledged");
        DISPLAY_STATUS.put("CLEARED_UNACK", "Cleared Unacknowledged");
        DISPLAY_STATUS.put("CLEARED_ACK", "Cleared Acknowledged");
    }

    @Override
    protected String dataSourceName() {
        return "alarm source";
    }

    @Override
    protected String noDataMessage() {
        return "No alarms found";
    }

    @Override
    protected String defaultFontWeight(String key) {
        if ("severity".equals(key)) {
            return "bold";
        }
        return null;
    }

    @Override
    protected String defaultColor(String key, String value) {
        if ("severity".equals(key)) {
            return SEVERITY_COLOR.getOrDefault(value, value);
        }
        return null;
    }

    @Override
    protected String defaultValue(String key, String value) {
        if ("status".equals(key)) {
            return DISPLAY_STATUS.getOrDefault(value, value);
        }
        return super.defaultValue(key, value);
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.ALARM_TABLE;
    }

}
