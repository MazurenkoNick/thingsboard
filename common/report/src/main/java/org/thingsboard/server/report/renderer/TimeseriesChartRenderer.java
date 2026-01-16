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

import org.jfree.chart.JFreeChart;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.chart.ReportBarChartWithLabelsSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ReportRangeChartSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ReportTimeSeriesChartSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartKeySettings;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesChartComponent;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.context.chart.TsChartData;
import org.thingsboard.server.report.renderer.chart.TbTimeSeriesChart;
import org.thingsboard.server.report.renderer.chart.TbVisualMap;

import java.awt.Graphics2D;

@Component
public class TimeseriesChartRenderer extends ChartRenderer<TimeseriesChartComponent> {

    @Override
    protected JFreeChart createChart(Graphics2D g2, TimeseriesChartComponent component, ComponentData reportDataSource) {

        ReportTimeSeriesChartSettings chartSettings;
        ReportTimeSeriesChartSettings inputSettings = component.getTimeSeriesChartSettings();
        TbVisualMap visualMap = null;
        String units = null;
        Integer decimals = null;
        if (inputSettings instanceof ReportBarChartWithLabelsSettings inputBarChartWithLabelsSettings) {
            ReportBarChartWithLabelsSettings barChartWithLabelsSettings = new ReportBarChartWithLabelsSettings(inputBarChartWithLabelsSettings);

            TimeSeriesChartKeySettings keySettings = barChartWithLabelsSettings.toTimeSeriesChartKeySettings();

            TsChartData data = reportDataSource.getTsChartData();
            data.getChartData().forEach(chartData -> chartData.getDataKeys().forEach(chartDataKey -> {
                chartDataKey.setSettings(keySettings);
                chartDataKey.setDecimals(barChartWithLabelsSettings.getBarDecimals());
                chartDataKey.setUnits("");
            }));

            units = barChartWithLabelsSettings.getBarUnits();
            decimals = barChartWithLabelsSettings.getBarDecimals();
            chartSettings = barChartWithLabelsSettings;
        } else if (inputSettings instanceof ReportRangeChartSettings inputRangeChartSettings) {
            ReportRangeChartSettings reportRangeChartSettings = new ReportRangeChartSettings(inputRangeChartSettings);
            TimeSeriesChartKeySettings keySettings = reportRangeChartSettings.toTimeSeriesChartKeySettings();
            TsChartData data = reportDataSource.getTsChartData();
            data.getChartData().forEach(chartData -> chartData.getDataKeys().forEach(chartDataKey -> {
                chartDataKey.setSettings(keySettings);
                chartDataKey.setDecimals(reportRangeChartSettings.getRangeDecimals());
                chartDataKey.setUnits(reportRangeChartSettings.getRangeUnits());
            }));

            visualMap = new TbVisualMap(reportRangeChartSettings.getOutOfRangeColor(), reportDataSource.getTsChartData().getRangeItems(),
                    reportRangeChartSettings.getFillArea(), reportRangeChartSettings.getFillAreaOpacity());

            units = reportRangeChartSettings.getRangeUnits();
            decimals = reportRangeChartSettings.getRangeDecimals();
            reportRangeChartSettings.getYAxes().get("default").setUnits(units);
            reportRangeChartSettings.getYAxes().get("default").setDecimals(decimals);
            chartSettings = reportRangeChartSettings;
        } else {
            chartSettings = new ReportTimeSeriesChartSettings(inputSettings);
        }

        TbTimeSeriesChart timeSeriesChart = new TbTimeSeriesChart(chartSettings, reportDataSource.getTsChartData(), reportDataSource.getVariables(),
                                                                  visualMap, units, decimals);

        return timeSeriesChart.createChart(g2);
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.TIME_SERIES_CHART;
    }

}
