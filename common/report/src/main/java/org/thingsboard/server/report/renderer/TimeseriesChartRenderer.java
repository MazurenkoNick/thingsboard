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
package org.thingsboard.server.report.renderer;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesChartComponent;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.renderer.chart.TbTimeseriesPlot;

import java.awt.*;
import java.text.DecimalFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

import static org.thingsboard.server.report.util.ColorUtils.safeParseCssColor;

@Component
public class TimeseriesChartRenderer extends ChartRenderer<TimeseriesChartComponent> {

    @Override
    protected JFreeChart createChart(TimeseriesChartComponent component, ComponentData reportDataSource) {
        TimeSeries series = this.generateRandomTemperatureSeries();
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(series);

        /*
          TimeSeriesChartGridSettings

          {
             show: false,
             backgroundColor: null,
             borderWidth: 1,
             borderColor: '#ccc'

          }

        * TimeSeriesChartYAxisSettings
        *
        * {
        *     showSplitLines: true
        *     splitLinesColor: rgba(0, 0, 0, 0.12);
        * }
        *
        * TimeSeriesChartXAxisSettings
        *
        * {
        *     showSplitLines: true
        *     splitLinesColor: rgba(0, 0, 0, 0.12);
        * }
        *
        * */

        boolean gridShow = false;
        String gridBackgroundColor = null;
        int gridBorderWidth = 1;
        String gridBorderColor = "#ccc";

        String yAxisSplitLineColor = "rgba(0, 0, 0, 0.12)";
        boolean yAxisShowSplitLines = true;

        String xAxisSplitLineColor = "rgba(0, 0, 0, 0.12)";
        boolean xAxisShowSplitLines = true;


        // Defaults start

        // Axes
        DateAxis xAxis = new DateAxis("Time");
        xAxis.setLowerMargin(0.02);  // reduce the default margins
        xAxis.setUpperMargin(0.02);

        NumberAxis yAxis = new NumberAxis("Temperature, ℃");
        yAxis.setAutoRangeIncludesZero(true);
        yAxis.setNumberFormatOverride(new DecimalFormat("0.0 '℃'"));

        boolean smooth = false;
        boolean drawPoints = false;

        XYItemRenderer renderer;

        if (smooth) {
            renderer = new XYSplineRenderer(3);
            ((XYSplineRenderer)renderer).setDefaultShapesVisible(drawPoints);
        } else {
            renderer = new XYLineAndShapeRenderer(true,
                    drawPoints);
        }

        TbTimeseriesPlot plot = new TbTimeseriesPlot(dataset, xAxis, yAxis, renderer);

        JFreeChart chart = new JFreeChart(
                "Temperature Chart",
                JFreeChart.DEFAULT_TITLE_FONT,
                plot,
                true);

        currentChartTheme.apply(chart);

        plot.setAxisOffset(new RectangleInsets(0.0, 0.0, 0.0, 0.0));

        // Defaults end

        if (gridShow) {
            plot.setBackgroundPaint(safeParseCssColor(gridBackgroundColor));
            plot.setOutlineStroke(new BasicStroke(gridBorderWidth));
            plot.setOutlinePaint(safeParseCssColor(gridBorderColor));
        } else {
            plot.setBackgroundPaint(null);
            plot.setOutlinePaint(null); // border
        }

        if (yAxisShowSplitLines) {
            plot.setRangeGridlineStroke(new BasicStroke(1.0f));
            plot.setRangeGridlinePaint(safeParseCssColor(yAxisSplitLineColor));
            if (!gridShow) {
                plot.setRightOutlinePaint(safeParseCssColor(yAxisSplitLineColor));
            }
        } else {
            plot.setRangeGridlinesVisible(false);
        }

        if (xAxisShowSplitLines) {
            plot.setDomainGridlineStroke(new BasicStroke(1.0f));
            plot.setDomainGridlinePaint(safeParseCssColor(xAxisSplitLineColor));
            if (!gridShow) {
                plot.setTopOutlinePaint(safeParseCssColor(xAxisSplitLineColor));
            }
        } else {
            plot.setDomainGridlinesVisible(false);
        }

        return chart;
    }

    private void adjustAxisTicks(ValueAxis axis) {
        // NumberAxis numberAxis = (NumberAxis) axis;
        //numberAxis.setTickLabelFont(new Font("Monospace", Font.BOLD, 40));
        //numberAxis.setUpperBound(25);
       /* Range currentRange = numberAxis.getRange();
        double tickSize = numberAxis.getTickUnit().getSize();
        numberAxis.setAutoRange(false);
        double lower = currentRange.getLowerBound();
        double upper = currentRange.getUpperBound();
        double newLowerBound = Math.floor(lower / tickSize) * tickSize;
        double newUpperBound = Math.ceil(upper / tickSize) * tickSize;
        numberAxis.setRange(newLowerBound, newUpperBound);*/
    }

    private TimeSeries generateRandomTemperatureSeries() {
        ZoneId zoneId = ZoneId.systemDefault();
        TimeZone timeZone = TimeZone.getTimeZone(zoneId.getId());
        Locale locale = Locale.getDefault();
        long end = System.currentTimeMillis();
        long day = 24 * 60 * 60 * 1000;

        long hour = 60 * 60 * 1000;

        long timeUnit = hour;
        long start = end - day;
        Random r = new Random();
        TimeSeries series = new TimeSeries("Temperature");
        for (long ts = start; ts < end; ts+=timeUnit) {
            Millisecond millisecond = new Millisecond(new Date(ts), timeZone, locale);
            double val = -25 + r.nextDouble() * 70;
            series.add(millisecond, val);
        }
        series.add(new Millisecond(new Date(end), timeZone, locale), 45);
        return series;
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.TIME_SERIES_CHART;
    }
}
