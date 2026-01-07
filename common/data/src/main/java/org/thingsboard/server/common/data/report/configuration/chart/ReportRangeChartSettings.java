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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReportRangeChartSettings extends ReportTimeSeriesChartSettings {

    private List<ColorRange> rangeColors;
    private String outOfRangeColor;
    private Boolean showRangeThresholds;
    private TimeSeriesChartThreshold rangeThreshold;
    private Boolean fillArea;
    private Float fillAreaOpacity;
    private LineSeriesSettings lineSettings;
    private String rangeUnits;
    private Integer rangeDecimals;

    public ReportRangeChartSettings() {}

    public ReportRangeChartSettings(ReportRangeChartSettings input) {
        super(input);
        if (input == null) {
            input = new ReportRangeChartSettings();
        }
        this.setTitle(input.getTitle() != null ? input.getTitle() : "Range chart");
        this.rangeColors = input.rangeColors != null ? new ArrayList<>(input.rangeColors) : new ArrayList<>();
        this.outOfRangeColor = input.outOfRangeColor != null ? input.outOfRangeColor : "#ccc";
        this.showRangeThresholds = input.showRangeThresholds != null ? input.showRangeThresholds : Boolean.TRUE;
        this.rangeThreshold = new TimeSeriesChartThreshold(input.getRangeThreshold());
        TimeSeriesChartThreshold inputRangeThreshold = input.getRangeThreshold();
        if (inputRangeThreshold == null) {
            inputRangeThreshold = new TimeSeriesChartThreshold();
        }
        this.rangeThreshold.setLineColor(inputRangeThreshold.getLineColor() != null ? inputRangeThreshold.getLineColor() : "#37383b");
        this.rangeThreshold.setLineType(inputRangeThreshold.getLineType() != null ? inputRangeThreshold.getLineType() : ChartLineType.dashed);
        this.rangeThreshold.setStartSymbol(inputRangeThreshold.getStartSymbol() != null ? inputRangeThreshold.getStartSymbol() : ChartShape.circle);
        this.rangeThreshold.setStartSymbolSize(inputRangeThreshold.getStartSymbolSize() != null ? inputRangeThreshold.getStartSymbolSize() : 5);
        this.rangeThreshold.setEndSymbol(inputRangeThreshold.getEndSymbol() != null ? inputRangeThreshold.getEndSymbol() : ChartShape.arrow);
        this.rangeThreshold.setEndSymbolSize(inputRangeThreshold.getEndSymbolSize() != null ? inputRangeThreshold.getEndSymbolSize() : 7);
        this.rangeThreshold.setLabelPosition(inputRangeThreshold.getLabelPosition() != null ? inputRangeThreshold.getLabelPosition() : ThresholdLabelPosition.insideEndTop);
        this.rangeThreshold.setLabelColor(inputRangeThreshold.getLabelColor() != null ? inputRangeThreshold.getLabelColor() : "#37383b");
        this.rangeThreshold.setEnableLabelBackground(inputRangeThreshold.getEnableLabelBackground() != null ? inputRangeThreshold.getEnableLabelBackground() : Boolean.TRUE);
        this.fillArea = input.getFillArea() != null ? input.getFillArea() : Boolean.TRUE;
        this.fillAreaOpacity = input.getFillAreaOpacity() != null ? input.getFillAreaOpacity() : 0.7f;
        this.lineSettings = new LineSeriesSettings(input.getLineSettings());
        this.rangeUnits = input.getRangeUnits() != null ? input.getRangeUnits() : "";
        this.rangeDecimals = input.getRangeDecimals() != null ? input.getRangeDecimals() : 0;
        this.setLegendLabelColor(input.getLegendLabelColor() != null ? input.getLegendLabelColor() : "rgba(0, 0, 0, 0.76)");
    }

    @JsonIgnore
    public TimeSeriesChartKeySettings toTimeSeriesChartKeySettings() {
        TimeSeriesChartKeySettings keySettings = new TimeSeriesChartKeySettings();
        keySettings.setSeriesType(TimeSeriesChartSeriesType.line);
        ChartFillSettings fillSettings = new ChartFillSettings();
        fillSettings.setType(this.fillArea ? ChartFillType.opacity : ChartFillType.none);
        fillSettings.setOpacity(this.fillAreaOpacity);
        LineSeriesSettings lineSettings = new LineSeriesSettings(this.lineSettings);
        lineSettings.setFillAreaSettings(fillSettings);
        keySettings.setLineSettings(lineSettings);
        return keySettings;
    }

}
