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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ReportTimeSeriesChartSettings {

    private Boolean showTitle;
    private String title;
    private Font titleFont;
    private String titleColor;

    @JsonProperty("thresholds")
    private List<TimeSeriesChartThreshold> thresholds;

    private Boolean stack;

    @JsonProperty("grid")
    private TimeSeriesChartGridSettings grid;

    @JsonProperty("yAxes")
    private Map<String, TimeSeriesChartYAxisSettings> yAxes;

    @JsonProperty("xAxis")
    private TimeSeriesChartXAxisSettings xAxis;

    @JsonProperty("barWidthSettings")
    private TimeSeriesChartBarWidthSettings barWidthSettings;

    @JsonProperty("noAggregationBarWidthSettings")
    private TimeSeriesChartNoAggregationBarWidthSettings noAggregationBarWidthSettings;

    private Boolean showLegend;
    private Font legendLabelFont;
    private String legendLabelColor;

    @JsonProperty("legendConfig")
    private LegendConfig legendConfig;

    public ReportTimeSeriesChartSettings() {}

    public ReportTimeSeriesChartSettings(ReportTimeSeriesChartSettings input) {
        if (input == null) {
            input = new ReportTimeSeriesChartSettings();
        }
        this.showTitle = input.getShowTitle() != null ? input.getShowTitle() : Boolean.TRUE;
        this.title = input.getTitle() != null ? input.getTitle() : "Time series chart";
        this.titleFont = input.getTitleFont() != null ? input.getTitleFont() : Font.builder().family("Roboto")
                .size(18f)
                .weight(FontWeight.WEIGHT_500)
                .style(FontStyle.NORMAL)
                .build();
        this.titleColor = input.getTitleColor() != null ? input.getTitleColor() : "rgba(0, 0, 0, 0.87)";

        this.thresholds = input.getThresholds() != null ? input.getThresholds() : new ArrayList<>();
        this.stack = input.getStack() != null ? input.getStack() : Boolean.FALSE;
        this.grid = new TimeSeriesChartGridSettings(input.getGrid());
        this.yAxes = new HashMap<>();
        if (input.getYAxes() == null) {
            this.yAxes.put("default", new TimeSeriesChartYAxisSettings(null));
        } else {
            input.getYAxes().forEach((key, value) -> {
                TimeSeriesChartYAxisSettings yAxisSettings = new TimeSeriesChartYAxisSettings(value);
                yAxes.put(key, yAxisSettings);
            });
        }
        this.xAxis = new TimeSeriesChartXAxisSettings(input.getXAxis());

        this.barWidthSettings = new TimeSeriesChartBarWidthSettings(input.getBarWidthSettings());
        this.noAggregationBarWidthSettings = new TimeSeriesChartNoAggregationBarWidthSettings(input.getNoAggregationBarWidthSettings());

        this.showLegend = input.getShowLegend() != null ? input.getShowLegend() : Boolean.TRUE;
        this.legendLabelFont = input.getLegendLabelFont() != null ? input.getLegendLabelFont() : Font.builder().family("Roboto")
                .size(12f)
                .weight(FontWeight.NORMAL)
                .style(FontStyle.NORMAL)
                .build();
        this.legendLabelColor = input.getLegendLabelColor() != null ? input.getLegendLabelColor() : "rgba(0, 0, 0, 0.87)";
        this.legendConfig = new LegendConfig(input.getLegendConfig());

    }

}
