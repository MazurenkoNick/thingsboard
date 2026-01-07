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
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

@Data
public class ReportBarChartWithLabelsSettings extends ReportTimeSeriesChartSettings {

    private Boolean showBarLabel;
    private Font barLabelFont;
    private String barLabelColor;
    private Boolean showBarValue;
    private Font barValueFont;
    private String barValueColor;
    private Boolean showBarBorder;
    private Float barBorderWidth;
    private Float barBorderRadius;
    private ChartFillSettings barBackgroundSettings;
    private String barUnits;
    private Integer barDecimals;

    public ReportBarChartWithLabelsSettings() {}

    public ReportBarChartWithLabelsSettings(ReportBarChartWithLabelsSettings input) {
        super(input);
        if (input == null) {
            input = new ReportBarChartWithLabelsSettings();
        }
        this.setTitle(input.getTitle() != null ? input.getTitle() : "Bar chart with labels");
        this.showBarLabel = input.getShowBarLabel() != null ? input.getShowBarLabel() : Boolean.TRUE;
        this.barLabelFont = input.getBarLabelFont() != null ? input.getBarLabelFont() : Font.builder().family("Roboto")
                .size(12f)
                .weight(FontWeight.NORMAL)
                .style(FontStyle.NORMAL)
                .build();
        this.barLabelColor = input.getBarLabelColor() != null ? input.getBarLabelColor() : "rgba(0, 0, 0, 0.54)";
        this.showBarValue = input.getShowBarValue() != null ? input.getShowBarValue() : Boolean.TRUE;
        this.barValueFont = input.getBarValueFont() != null ? input.getBarValueFont() : Font.builder().family("Roboto")
                .size(12f)
                .weight(FontWeight.BOLD)
                .style(FontStyle.NORMAL)
                .build();
        this.barValueColor = input.getBarValueColor() != null ? input.getBarValueColor() : "rgba(0, 0, 0, 0.76)";
        this.showBarBorder = input.getShowBarBorder() != null ? input.getShowBarBorder() : Boolean.FALSE;
        this.barBorderWidth = input.getBarBorderWidth() != null ? input.getBarBorderWidth() : 2f;
        this.barBorderRadius = input.getBarBorderRadius() != null ? input.getBarBorderRadius() : 0f;
        this.barBackgroundSettings = new ChartFillSettings(input.getBarBackgroundSettings());
        this.barUnits = input.getBarUnits() != null ? input.getBarUnits() : "%";
        this.barDecimals = input.getBarDecimals() != null ? input.getBarDecimals() : 0;
        TimeSeriesChartBarWidthSettings barWidthSettings = input.getBarWidthSettings();
        if (barWidthSettings == null) {
            barWidthSettings = new TimeSeriesChartBarWidthSettings();
        }
        barWidthSettings.setBarGap(barWidthSettings.getBarGap() != null ? barWidthSettings.getBarGap() : 0f);
        barWidthSettings.setIntervalGap(barWidthSettings.getIntervalGap() != null ? barWidthSettings.getIntervalGap() : 0.5f);
        this.setBarWidthSettings(barWidthSettings);
    }

    @JsonIgnore
    public TimeSeriesChartKeySettings toTimeSeriesChartKeySettings() {
        TimeSeriesChartKeySettings keySettings = new TimeSeriesChartKeySettings();
        keySettings.setSeriesType(TimeSeriesChartSeriesType.bar);

        BarWithLabelsSeriesSettings barSettings = new BarWithLabelsSeriesSettings();

        barSettings.setShowBorder(getShowBarBorder());
        barSettings.setBorderWidth(getBarBorderWidth());
        barSettings.setBorderRadius(getBarBorderRadius());
        barSettings.setBackgroundSettings(getBarBackgroundSettings());

        barSettings.setShowLabel(getShowBarValue());
        barSettings.setLabelFont(getBarValueFont());
        barSettings.setLabelColor(getBarValueColor());

        barSettings.setShowSeriesLabel(getShowBarLabel());
        barSettings.setSeriesLabelFont(getBarLabelFont());
        barSettings.setSeriesLabelColor(getBarLabelColor());

        keySettings.setBarSettings(barSettings);

        return keySettings;
    }

}
