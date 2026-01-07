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
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

import java.util.HashMap;
import java.util.Map;

@Data
public class TimeSeriesChartXAxisSettings extends TimeSeriesChartAxisSettings {

    private static final Map<FormatTimeUnit, String> defaultXAxisTicksFormat = new HashMap<>();
    static {
        defaultXAxisTicksFormat.put(FormatTimeUnit.millisecond, "HH:mm:ss SSS");
        defaultXAxisTicksFormat.put(FormatTimeUnit.second, "HH:mm:ss");
        defaultXAxisTicksFormat.put(FormatTimeUnit.minute, "HH:mm");
        defaultXAxisTicksFormat.put(FormatTimeUnit.hour, "HH:mm");
        defaultXAxisTicksFormat.put(FormatTimeUnit.day, "MMM dd");
        defaultXAxisTicksFormat.put(FormatTimeUnit.month, "MMM");
        defaultXAxisTicksFormat.put(FormatTimeUnit.year, "yyyy");
    }

    private Map<FormatTimeUnit, String> ticksFormat;

    public TimeSeriesChartXAxisSettings() {}

    public TimeSeriesChartXAxisSettings(TimeSeriesChartXAxisSettings input, boolean comparison) {
        super(input != null ? input : new TimeSeriesChartXAxisSettings());
        if (input == null) {
            input = new TimeSeriesChartXAxisSettings();
        }
        this.setPosition(input.getPosition() != null ? input.getPosition() : (comparison ? AxisPosition.top : AxisPosition.bottom));
        this.setTickLabelFont(input.getTickLabelFont() != null ? input.getTickLabelFont() : Font.builder().family("Roboto")
                .size(10f)
                .weight(FontWeight.NORMAL)
                .style(FontStyle.NORMAL)
                .build());
        if (input.getTicksFormat() == null) {
            this.ticksFormat = defaultXAxisTicksFormat;
        } else {
            this.ticksFormat = input.getTicksFormat();
            defaultXAxisTicksFormat.forEach((key, val) -> {
                if (!this.ticksFormat.containsKey(key)) {
                    this.ticksFormat.put(key, val);
                }
            });
        }
    }

}
