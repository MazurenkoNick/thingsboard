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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

@Data
public class TimeSeriesChartThreshold extends ValueSourceConfig {

    @JsonProperty("yAxisId")
    private String yAxisId;
    private String units;
    private Integer decimals;
    private String lineColor;
    private ChartLineType lineType;
    private Float lineWidth;
    private ChartShape startSymbol;
    private Float startSymbolSize;
    private ChartShape endSymbol;
    private Float endSymbolSize;
    private Boolean showLabel;
    private ThresholdLabelPosition labelPosition;
    private Font labelFont;
    private String labelColor;
    private Boolean enableLabelBackground;
    private String labelBackground;

    public TimeSeriesChartThreshold() {}

    public TimeSeriesChartThreshold(TimeSeriesChartThreshold input) {
        if (input == null) {
            input = new TimeSeriesChartThreshold();
        }
        this.setType(input.getType() != null ? input.getType() : ValueSourceType.constant);
        this.yAxisId = input.getYAxisId() != null ? input.getYAxisId() : "default";
        this.units = input.getUnits();
        this.decimals = input.getDecimals();
        this.lineColor = input.getLineColor() != null ? input.getLineColor() : "rgba(0, 0, 0, 0.76)";
        this.lineType = input.getLineType() != null ? input.getLineType() : ChartLineType.solid;
        this.lineWidth = input.getLineWidth() != null ? input.getLineWidth() : 1f;
        this.startSymbol = input.getStartSymbol() != null ? input.getStartSymbol() : ChartShape.none;
        this.startSymbolSize = input.getStartSymbolSize() != null ? input.getStartSymbolSize() : 5f;
        this.endSymbol = input.getEndSymbol() != null ? input.getEndSymbol() : ChartShape.arrow;
        this.endSymbolSize = input.getEndSymbolSize() != null ? input.getEndSymbolSize() : 5f;
        this.showLabel = input.getShowLabel() != null ? input.getShowLabel() : Boolean.TRUE;
        this.labelPosition = input.getLabelPosition() != null ? input.getLabelPosition() : ThresholdLabelPosition.end;
        this.labelFont = input.getLabelFont() != null ? input.getLabelFont() : Font.builder().family("Roboto")
                .size(12f)
                .weight(FontWeight.NORMAL)
                .style(FontStyle.NORMAL)
                .build();
        this.labelColor = input.getLabelColor() != null ? input.getLabelColor() : "rgba(0, 0, 0, 0.76)";
        this.enableLabelBackground = input.getEnableLabelBackground() != null ? input.getEnableLabelBackground() : Boolean.FALSE;
        this.labelBackground = input.getLabelBackground() != null ? input.getLabelBackground() : "rgba(255,255,255,0.56)";
    }

}
