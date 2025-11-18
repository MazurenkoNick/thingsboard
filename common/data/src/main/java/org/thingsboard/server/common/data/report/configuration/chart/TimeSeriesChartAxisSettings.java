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
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

@Data
public abstract class TimeSeriesChartAxisSettings {

    private Boolean show;
    private String label;
    private Font labelFont;
    private String labelColor;
    private AxisPosition position;
    private Boolean showTickLabels;
    private Font tickLabelFont;
    private String tickLabelColor;
    private Boolean showTicks;
    private String ticksColor;
    private Boolean showLine;
    private String lineColor;
    private Boolean showSplitLines;
    private String splitLinesColor;

    protected TimeSeriesChartAxisSettings() {}

    protected TimeSeriesChartAxisSettings(TimeSeriesChartAxisSettings input) {
        this.show = input.getShow() != null ? input.getShow() : Boolean.TRUE;
        this.label = input.getLabel() != null ? input.getLabel() : "";
        this.labelFont = input.getLabelFont() != null ? input.getLabelFont() : Font.builder().family("Roboto")
                .size(12f)
                .weight(FontWeight.BOLD)
                .style(FontStyle.NORMAL)
                .build();
        this.labelColor = input.getLabelColor() != null ? input.getLabelColor() : "rgba(0, 0, 0, 0.54)";
        this.showTickLabels = input.getShowTickLabels() != null ? input.getShowTickLabels() : Boolean.TRUE;
        this.tickLabelColor = input.getTickLabelColor() != null ? input.getTickLabelColor() : "rgba(0, 0, 0, 0.54)";
        this.showTicks = input.getShowTicks() != null ? input.getShowTicks() : Boolean.TRUE;
        this.ticksColor = input.getTicksColor() != null ? input.getTicksColor() : "rgba(0, 0, 0, 0.54)";
        this.showLine = input.getShowLine() != null ? input.getShowLine() : Boolean.TRUE;
        this.lineColor = input.getLineColor() != null ? input.getLineColor() : "rgba(0, 0, 0, 0.54)";
        this.showSplitLines = input.getShowSplitLines() != null ? input.getShowSplitLines() : Boolean.TRUE;
        this.splitLinesColor = input.getShowSplitLines() != null ? input.getSplitLinesColor() : "rgba(0, 0, 0, 0.12)";
    }

}
