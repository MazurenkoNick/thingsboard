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
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriodValues;
import org.jfree.data.time.TimePeriodValuesCollection;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.DataKeySettings;
import org.thingsboard.server.common.data.report.configuration.chart.AxisPosition;
import org.thingsboard.server.common.data.report.configuration.chart.LegendConfig;
import org.thingsboard.server.common.data.report.configuration.chart.LegendPosition;
import org.thingsboard.server.common.data.report.configuration.chart.ReportTimeSeriesChartSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartAxisSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartBarWidth;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartKeySettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartNoAggregationBarWidthSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartNoAggregationBarWidthStrategy;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartSeriesType;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartXAxisSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartYAxisSettings;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesChartComponent;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.context.chart.TsChartData;
import org.thingsboard.server.report.context.chart.TsChartDataSource;
import org.thingsboard.server.report.context.chart.TsChartSeriesData;
import org.thingsboard.server.report.context.chart.TsChartSeriesEntry;
import org.thingsboard.server.report.renderer.chart.TbTimeseriesPlot;
import org.thingsboard.server.report.renderer.chart.TimeseriesBarRenderCtx;
import org.thingsboard.server.report.util.ColorUtils;
import org.thingsboard.server.report.util.ThymeleafUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.thingsboard.server.report.renderer.chart.ChartUtils.adjustAxisMargins;
import static org.thingsboard.server.report.renderer.chart.ChartUtils.calculateBarTimePeriod;
import static org.thingsboard.server.report.renderer.chart.ChartUtils.createXAxis;
import static org.thingsboard.server.report.renderer.chart.ChartUtils.createYAxis;
import static org.thingsboard.server.report.util.AwtFontUtils.toAwtFont;
import static org.thingsboard.server.report.util.ColorUtils.safeParseCssColor;

@Component
public class TimeseriesChartRenderer extends ChartRenderer<TimeseriesChartComponent> {

    private ReportTimeSeriesChartSettings chartSettings;
    private TsChartData chartData;

    private XYPlot plot;
    private JFreeChart chart;

    private List<NumberAxis> yAxisList;
    private Map<String, Integer> yAxisIndexMap;

    private List<TsChartSeriesData> seriesList;

    @Override
    protected JFreeChart createChart(TimeseriesChartComponent component, ComponentData reportDataSource) {

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


        this.chartSettings = new ReportTimeSeriesChartSettings(component.getTimeSeriesChartSettings());
        this.plot = new TbTimeseriesPlot();
        this.chartData = reportDataSource.getTsChartData();

        this.chart = new JFreeChart(
                null,
                null,
                plot,
                false);

        currentChartTheme.apply(chart);

        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        plot.setAxisOffset(new RectangleInsets(0.0, 0.0, 0.0, 0.0));
        chart.setBackgroundPaint(ColorUtils.TRANSPARENT);

        if (chartSettings.getShowTitle()) {
            Font titleFont = toAwtFont(chartSettings.getTitleFont());
            TextTitle title = new TextTitle(chartSettings.getTitle(), titleFont);
            title.setPaint(safeParseCssColor(chartSettings.getTitleColor()));
            chart.setTitle(title);
        }

        this.setupGrid();
        this.setupXAxes();
        this.setupYAxes();
        this.setupData();
        this.updateYAxisScale();
        this.setupLegend();

        return chart;
    }

    private void setupGrid() {
        boolean gridShow = false;
        String gridBackgroundColor = null;
        int gridBorderWidth = 1;
        String gridBorderColor = "#ccc";

        TimeSeriesChartXAxisSettings mainXAxisSettings = chartSettings.getXAxis();
        boolean xAxisShowSplitLines = mainXAxisSettings.getShowSplitLines();
        String xAxisSplitLineColor = mainXAxisSettings.getSplitLinesColor();

        TimeSeriesChartYAxisSettings defaultYAxisSettings = chartSettings.getYAxes().get("default");
        boolean yAxisShowSplitLines = chartSettings.getYAxes().values().stream()
                .anyMatch(axis -> axis.getShow() && axis.getShowSplitLines());
        String yAxisSplitLineColor = chartSettings.getYAxes().values().stream()
                .filter(axis -> axis.getShow() && axis.getShowSplitLines())
                .map(TimeSeriesChartAxisSettings::getSplitLinesColor).findFirst().orElse(null);

        if (gridShow) {
            plot.setBackgroundPaint(safeParseCssColor(gridBackgroundColor));
            plot.setOutlineStroke(new BasicStroke(gridBorderWidth));
            plot.setOutlinePaint(safeParseCssColor(gridBorderColor));
        } else {
            plot.setBackgroundPaint(null);
            plot.setOutlinePaint(null); // border
        }

        if (xAxisShowSplitLines) {
            plot.setDomainGridlineStroke(new BasicStroke(1.0f));
            plot.setDomainGridlinePaint(safeParseCssColor(xAxisSplitLineColor));
        } else {
            plot.setDomainGridlinesVisible(false);
        }

        if (yAxisShowSplitLines) {
            plot.setRangeGridlineStroke(new BasicStroke(1.0f));
            plot.setRangeGridlinePaint(safeParseCssColor(yAxisSplitLineColor));
        } else {
            plot.setRangeGridlinesVisible(false);
        }
    }

    private void setupXAxes() {
        TimeSeriesChartXAxisSettings mainXAxisSettings = chartSettings.getXAxis();
        createXAxis(plot, mainXAxisSettings, chartData.getTimeRange(), chartData.getTimeZone(), 0);
    }

    private void setupYAxes() {
        this.yAxisList = new ArrayList<>();
        this.yAxisIndexMap = new HashMap<>();

        List<TimeSeriesChartYAxisSettings> yAxisSettingsList = chartSettings.getYAxes().values().stream().sorted(Comparator.comparingInt(TimeSeriesChartYAxisSettings::getOrder)).toList();

        for (int index = 0; index < yAxisSettingsList.size(); index++) {
            TimeSeriesChartYAxisSettings yAxisSettings = yAxisSettingsList.get(index);
            this.yAxisList.add(createYAxis(plot, yAxisSettings, index));
            this.yAxisIndexMap.put(yAxisSettingsList.get(index).getId(), index);
        }

        List<TimeSeriesChartYAxisSettings> leftAxes = yAxisSettingsList.stream().filter(axis -> AxisPosition.left.equals(axis.getPosition())).toList();
        if (leftAxes.size() > 1) {
            for (int i = 0; i < leftAxes.size()-1; i++) {
                TimeSeriesChartYAxisSettings leftAxis = leftAxes.get(i);
                int index = yAxisSettingsList.indexOf(leftAxis);
                NumberAxis axis = this.yAxisList.get(index);
                adjustAxisMargins(axis, 0.0, 4.0, 0.0, 0.0);
            }
        }

        List<TimeSeriesChartYAxisSettings> rightAxes = yAxisSettingsList.stream().filter(axis -> AxisPosition.right.equals(axis.getPosition())).toList();
        if (rightAxes.size() > 1) {
            for (int i = 0; i < rightAxes.size()-1; i++) {
                TimeSeriesChartYAxisSettings rightAxis = rightAxes.get(i);
                int index = yAxisSettingsList.indexOf(rightAxis);
                NumberAxis axis = this.yAxisList.get(index);
                adjustAxisMargins(axis, 0.0, 0.0, 0.0, 4.0);
            }
        }
    }

    private void setupData() {

        List<TsChartSeriesData> allSeries = new ArrayList<>();

        for (TsChartDataSource dataSource : chartData.getChartData()) {
            allSeries.addAll(dataSource.getData());
        }

        this.seriesList = allSeries.stream().sorted((series1, series2) -> {
            TimeSeriesChartKeySettings keySettings1 = this.getSeriesSettings(series1);
            TimeSeriesChartKeySettings keySettings2 = this.getSeriesSettings(series2);
            if (keySettings1.getSeriesType() == keySettings2.getSeriesType()) {
                return series1.getIndex() - series2.getIndex();
            } else if (keySettings1.getSeriesType() == TimeSeriesChartSeriesType.bar) {
                return -1;
            }
            return 1;
        }).toList();

        List<TsChartSeriesData> barsList = seriesList.stream().
                filter(series -> this.getSeriesSettings(series).getSeriesType() == TimeSeriesChartSeriesType.bar).toList();

        TimeSeriesChartNoAggregationBarWidthSettings noAggregationBarWidthSettings = chartSettings.getNoAggregationBarWidthSettings();

        TimeSeriesChartBarWidth targetBarWidth = TimeSeriesChartNoAggregationBarWidthStrategy.group.equals(noAggregationBarWidthSettings.getStrategy()) ?
                noAggregationBarWidthSettings.getGroupWidth() : noAggregationBarWidthSettings.getBarWidth();

        TimeseriesBarRenderCtx barRenderCtx = TimeseriesBarRenderCtx.builder()
                .barGap(chartSettings.getBarWidthSettings().getBarGap())
                .intervalGap(chartSettings.getBarWidthSettings().getIntervalGap())
                .timeWindow(chartData.getTimeRange().endTs - chartData.getTimeRange().startTs)
                .noAggregation(chartData.isNoAggregation())
                .noAggregationBarWidthStrategy(noAggregationBarWidthSettings.getStrategy())
                .noAggregationWidthRelative(targetBarWidth.getRelative())
                .noAggregationWidth(targetBarWidth.getRelative() ? targetBarWidth.getRelativeWidth() : targetBarWidth.getAbsoluteWidth())
                .build();

        for (int index = 0; index < seriesList.size(); index++) {
            this.addChartSeriesData(seriesList.get(index), barsList, barRenderCtx, index);
        }
    }

    private void updateYAxisScale() {
        for (Map.Entry<String, Integer> yAxisEntry : this.yAxisIndexMap.entrySet()) {
            boolean includeZeros = this.seriesList.stream().anyMatch(entry -> {
                TimeSeriesChartKeySettings settings = getSeriesSettings(entry);
                return settings.getYAxisId().equals(yAxisEntry.getKey()) && TimeSeriesChartSeriesType.bar.equals(settings.getSeriesType());
            });
            this.yAxisList.get(yAxisEntry.getValue()).setAutoRangeIncludesZero(includeZeros);
        }
    }

    private void setupLegend() {
        if (chartSettings.getShowLegend()) {
            LegendTitle legend = new LegendTitle(plot);
            legend.setBackgroundPaint(ColorUtils.TRANSPARENT);
            legend.setMargin(new RectangleInsets(1.0, 1.0, 1.0, 1.0));
            legend.setItemLabelPadding(new RectangleInsets(2.0, 4.0, 2.0, 16.0));
            Font legentLabelFont = toAwtFont(chartSettings.getLegendLabelFont());
            legend.setItemFont(legentLabelFont);
            legend.setItemPaint(safeParseCssColor(chartSettings.getLegendLabelColor()));
            LegendConfig legendConfig = chartSettings.getLegendConfig();
            RectangleEdge position = RectangleEdge.TOP;
            LegendPosition legendPosition = legendConfig.getPosition();
            switch (legendPosition) {
                case bottom -> position = RectangleEdge.BOTTOM;
                case left -> position = RectangleEdge.LEFT;
                case right -> position = RectangleEdge.RIGHT;
            }
            legend.setPosition(position);
            chart.addSubtitle(legend);
            legend.addChangeListener(chart);

            boolean sortAlphabetically = legendConfig.getSortDataKeys();
            LegendItemCollection legendItems = plot.getLegendItems();
            List<LegendItem> items = new ArrayList<>();

            for (int i = 0; i < legendItems.getItemCount(); i++) {
                items.add(legendItems.get(i));
            }

            if (sortAlphabetically) {
                items.sort(Comparator.comparing(LegendItem::getLabel));
            } else {
                List<TsChartSeriesData> finalSeriesList = seriesList;
                items.sort((item1, item2) -> {
                    int dataIndex1 = finalSeriesList.get(item1.getDatasetIndex()).getIndex();
                    int dataIndex2 = finalSeriesList.get(item2.getDatasetIndex()).getIndex();
                    return dataIndex1 - dataIndex2;
                });
            }

            LegendItemCollection sortedCollection = new LegendItemCollection();
            for (LegendItem item : items) {
                sortedCollection.add(item);
            }
            plot.setFixedLegendItems(sortedCollection);
        }
    }

    private void addChartSeriesData(TsChartSeriesData chartSeriesData,
                                    List<TsChartSeriesData> barsList,
                                    TimeseriesBarRenderCtx barRenderCtx,
                                    int index) {


        boolean smooth = false;
        boolean drawPoints = false;

        TimeSeriesChartKeySettings keySettings = this.getSeriesSettings(chartSeriesData);

        XYItemRenderer renderer;

        String label = ThymeleafUtil.renderFromTextString(chartSeriesData.getDataKey().getLabel(), chartSeriesData.getDataSource().getVariables());

        Locale locale = Locale.getDefault();

        XYDataset dataset;

        Paint seriesPaint = safeParseCssColor(chartSeriesData.getDataKey().getColor());

        if (keySettings.getSeriesType() == TimeSeriesChartSeriesType.line) {
            if (smooth) {
                renderer = new XYSplineRenderer(3);
                ((XYSplineRenderer)renderer).setDefaultShapesVisible(drawPoints);
            } else {
                renderer = new XYLineAndShapeRenderer(true,
                        drawPoints);
            }
            renderer.setSeriesPaint(0, seriesPaint);
            renderer.setSeriesStroke(0, new BasicStroke(2.0f));

            TimeSeries timeSeries = new TimeSeries(label);

            for (TsChartSeriesEntry tsValue : chartSeriesData.getData()) {
                Millisecond millisecond = new Millisecond(new Date(tsValue.getTs()), chartData.getTimeZone(), locale);
                try {
                    double doubleValue = Double.parseDouble(tsValue.getValue());
                    timeSeries.add(millisecond, doubleValue);
                } catch (NumberFormatException ignored) {}
            }
            dataset = new TimeSeriesCollection(timeSeries, chartData.getTimeZone());

        } else {
            XYBarRenderer barRenderer = new XYBarRenderer();
            barRenderer.setDrawBarOutline(false);
            barRenderer.setShadowVisible(false);
            barRenderer.setBarPainter(new StandardXYBarPainter());
            barRenderer.setSeriesPaint(0, seriesPaint);

            renderer = barRenderer;


            int barIndex = barsList.indexOf(chartSeriesData);
            TimePeriodValues timePeriods = new TimePeriodValues(label);

            for (TsChartSeriesEntry tsValue : chartSeriesData.getData()) {
                try {
                    double doubleValue = Double.parseDouble(tsValue.getValue());
                    SimpleTimePeriod timePeriod = calculateBarTimePeriod(tsValue, barRenderCtx, barsList.size(), barIndex);
                    timePeriods.add(timePeriod, doubleValue);
                } catch (NumberFormatException ignored) {}
            }
            dataset = new TimePeriodValuesCollection(timePeriods);
        }

        renderer.setSeriesVisibleInLegend(0, keySettings.getShowInLegend() != null ? keySettings.getShowInLegend() : true);

        plot.setRenderer(index, renderer);
        plot.setDataset(index, dataset);
        int xAxisIndex = 0;
        int yAxisIndex = this.yAxisIndexMap.get(keySettings.getYAxisId());
        plot.mapDatasetToDomainAxis(index, xAxisIndex);
        plot.mapDatasetToRangeAxis(index, yAxisIndex);
    }

    private TimeSeriesChartKeySettings getSeriesSettings(TsChartSeriesData chartSeriesData) {
        DataKeySettings settings = chartSeriesData.getDataKey().getSettings();
        TimeSeriesChartKeySettings keySettings = null;
        if (settings instanceof TimeSeriesChartKeySettings) {
            keySettings = (TimeSeriesChartKeySettings) settings;
        }
        return new TimeSeriesChartKeySettings(keySettings);
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.TIME_SERIES_CHART;
    }
}
