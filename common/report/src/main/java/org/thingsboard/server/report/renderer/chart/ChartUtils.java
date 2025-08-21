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
package org.thingsboard.server.report.renderer.chart;

import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnitSource;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.time.SimpleTimePeriod;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.report.configuration.chart.AxisPosition;
import org.thingsboard.server.common.data.report.configuration.chart.FormatTimeUnit;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartAxisSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartNoAggregationBarWidthStrategy;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartXAxisSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartYAxisSettings;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator;
import org.thingsboard.server.report.context.chart.TsChartSeriesEntry;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static org.jfree.chart.axis.Axis.DEFAULT_AXIS_LABEL_INSETS;
import static org.thingsboard.server.report.util.AwtFontUtils.ZERO_FONT;
import static org.thingsboard.server.report.util.AwtFontUtils.toAwtFont;
import static org.thingsboard.server.report.util.ColorUtils.safeParseCssColor;

public interface ChartUtils {

    static SimpleTimePeriod calculateBarTimePeriod(TsChartSeriesEntry entry,
                                                    TimeseriesBarRenderCtx barRenderCtx,
                                                    int barsCount, int barIndex) {

        long time = entry.getTs();
        long start = entry.getInterval().startTs;
        long end = entry.getInterval().endTs;
        long interval = end - start;

        if (barRenderCtx.isNoAggregation()) {
            if (barRenderCtx.isNoAggregationWidthRelative()) {
                interval = (long)(barRenderCtx.getTimeWindow() * barRenderCtx.getNoAggregationWidth() / 100f);
            } else {
                interval = (long)barRenderCtx.getNoAggregationWidth();
            }
            start = time - interval / 2;
        }

        double barGapRatio = barRenderCtx.getBarGap();
        double intervalGapRatio = barRenderCtx.getIntervalGap();
        boolean separateBar = barRenderCtx.isNoAggregation() && TimeSeriesChartNoAggregationBarWidthStrategy.separate.equals(barRenderCtx.getNoAggregationBarWidthStrategy());

        long barInterval = separateBar ? interval : (long)((double)interval / (barsCount + barGapRatio * (barsCount - 1) + intervalGapRatio * 2));

        long intervalGap = (long)(barInterval * intervalGapRatio);
        long barGap = (long)(barInterval * barGapRatio);

        long startTime = separateBar ? start : start + intervalGap + (barInterval + barGap) * barIndex;
        long endTime = startTime + barInterval;

        return new SimpleTimePeriod(startTime, endTime);
    }

    static DateAxis createXAxis(XYPlot plot, TimeSeriesChartXAxisSettings xAxisSettings, TimeIntervalCalculator.TimeRange timeRange, TimeZone timeZone, int index) {
        Locale locale = Locale.getDefault();
        DateAxis xAxis = new DateAxis(xAxisSettings.getLabel(), timeZone, locale);
        plot.setDomainAxis(index, xAxis);
        xAxis.setStandardTickUnits(createDateTickUnitsFromTicksFormat(xAxisSettings.getTicksFormat(), timeZone, locale));
        AxisLocation location = AxisPosition.bottom.equals(xAxisSettings.getPosition()) ? AxisLocation.BOTTOM_OR_LEFT : AxisLocation.TOP_OR_RIGHT;
        plot.setDomainAxisLocation(index, location);
        xAxis.setMinimumDate(new Date(timeRange.startTs));
        xAxis.setMaximumDate(new Date(timeRange.endTs));
        setupAxisAppearance(xAxis, xAxisSettings);
        return xAxis;
    }

    static TbNumberAxis createYAxis(XYPlot plot, TimeSeriesChartYAxisSettings yAxisSettings, int index) {
        TbNumberAxis yAxis = new TbNumberAxis(yAxisSettings.getLabel());
        plot.setRangeAxis(index, yAxis);
        AxisLocation location = AxisPosition.left.equals(yAxisSettings.getPosition()) ? AxisLocation.BOTTOM_OR_LEFT : AxisLocation.TOP_OR_RIGHT;
        plot.setRangeAxisLocation(index, location);
        int decimals = yAxisSettings.getDecimals() != null ? yAxisSettings.getDecimals() : 2;
        StringBuilder patternBuilder = new StringBuilder("#");
        if (decimals > 0) {
            patternBuilder.append(".");
        }
        patternBuilder.append("#".repeat(Math.max(0, decimals)));
        if (StringUtils.isNotBlank(yAxisSettings.getUnits())) {
            patternBuilder.append(" '").append(yAxisSettings.getUnits()).append("'");
        }
        yAxis.setAutoRangeIncludesZero(false);
        if (yAxisSettings.getSplitNumber() != null) {
            yAxis.setSplitNumber(yAxisSettings.getSplitNumber());
        } else if (yAxisSettings.getInterval() != null && yAxisSettings.getInterval() > 0) {
            yAxis.setTickUnit(new NumberTickUnit(yAxisSettings.getInterval()));
        }
        yAxis.setNumberFormatOverride(new DecimalFormat(patternBuilder.toString()));
        setupAxisAppearance(yAxis, yAxisSettings);
        return yAxis;
    }

    static void adjustAxisMargins(Axis axis, double top, double left, double bottom, double right) {
        if (StringUtils.isBlank(axis.getLabel())) {
            axis.setLabel(" ");
            axis.setLabelFont(ZERO_FONT);
        }
        axis.setLabelInsets(new RectangleInsets(DEFAULT_AXIS_LABEL_INSETS.getTop() + top,
                DEFAULT_AXIS_LABEL_INSETS.getLeft() + left,
                DEFAULT_AXIS_LABEL_INSETS.getBottom() + bottom,
                DEFAULT_AXIS_LABEL_INSETS.getRight() + right));
    }

    private static void setupAxisAppearance(ValueAxis axis, TimeSeriesChartAxisSettings axisSettings) {
        axis.setVisible(axisSettings.getShow());
        axis.setLabelFont(toAwtFont(axisSettings.getLabelFont()));
        axis.setLabelPaint(safeParseCssColor(axisSettings.getLabelColor()));
        axis.setTickLabelsVisible(axisSettings.getShowTickLabels());
        axis.setTickLabelFont(toAwtFont(axisSettings.getTickLabelFont()));
        axis.setTickLabelPaint(safeParseCssColor(axisSettings.getTickLabelColor()));
        axis.setTickMarksVisible(axisSettings.getShowTicks());
        axis.setTickMarkPaint(safeParseCssColor(axisSettings.getTicksColor()));
        axis.setAxisLineVisible(axisSettings.getShowLine());
        axis.setAxisLinePaint(safeParseCssColor(axisSettings.getLineColor()));
    }

    private static TickUnitSource createDateTickUnitsFromTicksFormat(Map<FormatTimeUnit, String> ticksFormat,
                                                                     TimeZone zone,
                                                                     Locale locale) {
        TickUnits units = new TickUnits();

        // date formatters
        DateFormat f1 = new SimpleDateFormat(ticksFormat.get(FormatTimeUnit.millisecond), locale);
        DateFormat f2 = new SimpleDateFormat(ticksFormat.get(FormatTimeUnit.second), locale);
        DateFormat f3 = new SimpleDateFormat(ticksFormat.get(FormatTimeUnit.minute), locale);
        DateFormat f4 = new SimpleDateFormat(ticksFormat.get(FormatTimeUnit.hour), locale);
        DateFormat f5 = new SimpleDateFormat(ticksFormat.get(FormatTimeUnit.day), locale);
        DateFormat f6 = new SimpleDateFormat(ticksFormat.get(FormatTimeUnit.month), locale);
        DateFormat f7 = new SimpleDateFormat(ticksFormat.get(FormatTimeUnit.year), locale);

        f1.setTimeZone(zone);
        f2.setTimeZone(zone);
        f3.setTimeZone(zone);
        f4.setTimeZone(zone);
        f5.setTimeZone(zone);
        f6.setTimeZone(zone);
        f7.setTimeZone(zone);

        // milliseconds
        units.add(new DateTickUnit(DateTickUnitType.MILLISECOND, 1, f1));
        units.add(new DateTickUnit(DateTickUnitType.MILLISECOND, 5,
                DateTickUnitType.MILLISECOND, 1, f1));
        units.add(new DateTickUnit(DateTickUnitType.MILLISECOND, 10,
                DateTickUnitType.MILLISECOND, 1, f1));
        units.add(new DateTickUnit(DateTickUnitType.MILLISECOND, 25,
                DateTickUnitType.MILLISECOND, 5, f1));
        units.add(new DateTickUnit(DateTickUnitType.MILLISECOND, 50,
                DateTickUnitType.MILLISECOND, 10, f1));
        units.add(new DateTickUnit(DateTickUnitType.MILLISECOND, 100,
                DateTickUnitType.MILLISECOND, 10, f1));
        units.add(new DateTickUnit(DateTickUnitType.MILLISECOND, 250,
                DateTickUnitType.MILLISECOND, 10, f1));
        units.add(new DateTickUnit(DateTickUnitType.MILLISECOND, 500,
                DateTickUnitType.MILLISECOND, 50, f1));

        // seconds
        units.add(new DateTickUnit(DateTickUnitType.SECOND, 1,
                DateTickUnitType.MILLISECOND, 50, f2));
        units.add(new DateTickUnit(DateTickUnitType.SECOND, 5,
                DateTickUnitType.SECOND, 1, f2));
        units.add(new DateTickUnit(DateTickUnitType.SECOND, 10,
                DateTickUnitType.SECOND, 1, f2));
        units.add(new DateTickUnit(DateTickUnitType.SECOND, 30,
                DateTickUnitType.SECOND, 5, f2));

        // minutes
        units.add(new DateTickUnit(DateTickUnitType.MINUTE, 1,
                DateTickUnitType.SECOND, 5, f3));
        units.add(new DateTickUnit(DateTickUnitType.MINUTE, 2,
                DateTickUnitType.SECOND, 10, f3));
        units.add(new DateTickUnit(DateTickUnitType.MINUTE, 5,
                DateTickUnitType.MINUTE, 1, f3));
        units.add(new DateTickUnit(DateTickUnitType.MINUTE, 10,
                DateTickUnitType.MINUTE, 1, f3));
        units.add(new DateTickUnit(DateTickUnitType.MINUTE, 15,
                DateTickUnitType.MINUTE, 5, f3));
        units.add(new DateTickUnit(DateTickUnitType.MINUTE, 20,
                DateTickUnitType.MINUTE, 5, f3));
        units.add(new DateTickUnit(DateTickUnitType.MINUTE, 30,
                DateTickUnitType.MINUTE, 5, f3));

        // hours
        units.add(new DateTickUnit(DateTickUnitType.HOUR, 1,
                DateTickUnitType.MINUTE, 5, f4));
        units.add(new DateTickUnit(DateTickUnitType.HOUR, 2,
                DateTickUnitType.MINUTE, 10, f4));
        units.add(new DateTickUnit(DateTickUnitType.HOUR, 4,
                DateTickUnitType.MINUTE, 30, f4));
        units.add(new DateTickUnit(DateTickUnitType.HOUR, 6,
                DateTickUnitType.HOUR, 1, f4));
        //units.add(new DateTickUnit(DateTickUnitType.HOUR, 12,
          //      DateTickUnitType.HOUR, 1, f5));

        // days
        units.add(new DateTickUnit(DateTickUnitType.DAY, 1,
                DateTickUnitType.HOUR, 1, f5));
        units.add(new DateTickUnit(DateTickUnitType.DAY, 2,
                DateTickUnitType.HOUR, 1, f5));
        units.add(new DateTickUnit(DateTickUnitType.DAY, 7,
                DateTickUnitType.DAY, 1, f5));
        units.add(new DateTickUnit(DateTickUnitType.DAY, 15,
                DateTickUnitType.DAY, 1, f5));

        // months
        units.add(new DateTickUnit(DateTickUnitType.MONTH, 1,
                DateTickUnitType.DAY, 1, f6));
        units.add(new DateTickUnit(DateTickUnitType.MONTH, 2,
                DateTickUnitType.DAY, 1, f6));
        units.add(new DateTickUnit(DateTickUnitType.MONTH, 3,
                DateTickUnitType.MONTH, 1, f6));
        units.add(new DateTickUnit(DateTickUnitType.MONTH, 4,
                DateTickUnitType.MONTH, 1, f6));
        units.add(new DateTickUnit(DateTickUnitType.MONTH, 6,
                DateTickUnitType.MONTH, 1, f6));

        // years
        units.add(new DateTickUnit(DateTickUnitType.YEAR, 1,
                DateTickUnitType.MONTH, 1, f7));
        units.add(new DateTickUnit(DateTickUnitType.YEAR, 2,
                DateTickUnitType.MONTH, 3, f7));
        units.add(new DateTickUnit(DateTickUnitType.YEAR, 5,
                DateTickUnitType.YEAR, 1, f7));
        units.add(new DateTickUnit(DateTickUnitType.YEAR, 10,
                DateTickUnitType.YEAR, 1, f7));
        units.add(new DateTickUnit(DateTickUnitType.YEAR, 25,
                DateTickUnitType.YEAR, 5, f7));
        units.add(new DateTickUnit(DateTickUnitType.YEAR, 50,
                DateTickUnitType.YEAR, 10, f7));
        units.add(new DateTickUnit(DateTickUnitType.YEAR, 100,
                DateTickUnitType.YEAR, 20, f7));

        return units;
    }
}
