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
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.labels.XYSeriesLabelGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriodValues;
import org.jfree.data.time.TimePeriodValuesCollection;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeTableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.chart.AxisPosition;
import org.thingsboard.server.common.data.report.configuration.chart.BarSeriesSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ChartFillType;
import org.thingsboard.server.common.data.report.configuration.chart.ChartLabelPosition;
import org.thingsboard.server.common.data.report.configuration.chart.ChartShape;
import org.thingsboard.server.common.data.report.configuration.chart.LegendConfig;
import org.thingsboard.server.common.data.report.configuration.chart.LegendPosition;
import org.thingsboard.server.common.data.report.configuration.chart.LineSeriesSettings;
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
import org.thingsboard.server.report.renderer.chart.TbDatasetKey;
import org.thingsboard.server.report.renderer.chart.TbTimeseriesPlot;
import org.thingsboard.server.report.renderer.chart.TbXYBarRenderer;
import org.thingsboard.server.report.renderer.chart.TbXYItemLabelGenerator;
import org.thingsboard.server.report.renderer.chart.TbXYLineAndShapeRenderer;
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
import java.util.Optional;

import static org.thingsboard.server.report.renderer.chart.ChartUtils.adjustAxisMargins;
import static org.thingsboard.server.report.renderer.chart.ChartUtils.calculateBarTimePeriod;
import static org.thingsboard.server.report.renderer.chart.ChartUtils.createFillPaint;
import static org.thingsboard.server.report.renderer.chart.ChartUtils.createLineStroke;
import static org.thingsboard.server.report.renderer.chart.ChartUtils.createSeriesShape;
import static org.thingsboard.server.report.renderer.chart.ChartUtils.createXAxis;
import static org.thingsboard.server.report.renderer.chart.ChartUtils.createYAxis;
import static org.thingsboard.server.report.renderer.chart.ChartUtils.datasetGroupsFromSeries;
import static org.thingsboard.server.report.renderer.chart.ChartUtils.getSeriesSettings;
import static org.thingsboard.server.report.util.AwtFontUtils.toAwtFont;
import static org.thingsboard.server.report.util.ColorUtils.safeParseCssColor;

@Component
public class TimeseriesChartRenderer extends ChartRenderer<TimeseriesChartComponent> implements XYSeriesLabelGenerator {

    private ReportTimeSeriesChartSettings chartSettings;
    private TsChartData chartData;

    private XYPlot plot;
    private JFreeChart chart;

    private List<NumberAxis> yAxisList;
    private Map<String, Integer> yAxisIndexMap;

    private List<TsChartSeriesData> seriesList;

    private boolean stackMode;

    @Override
    protected JFreeChart createChart(TimeseriesChartComponent component, ComponentData reportDataSource) {

        this.chartSettings = new ReportTimeSeriesChartSettings(component.getTimeSeriesChartSettings());
        this.plot = new TbTimeseriesPlot();
        this.chartData = reportDataSource.getTsChartData();

        this.stackMode = this.chartSettings.getStack();

        this.chart = new JFreeChart(
                null,
                null,
                plot,
                false);

        currentChartTheme.apply(chart);

        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);
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
        TimeSeriesChartXAxisSettings mainXAxisSettings = chartSettings.getXAxis();
        boolean xAxisShowSplitLines = mainXAxisSettings.getShowSplitLines();
        String xAxisSplitLineColor = mainXAxisSettings.getSplitLinesColor();

        boolean yAxisShowSplitLines = chartSettings.getYAxes().values().stream()
                .anyMatch(axis -> axis.getShow() && axis.getShowSplitLines());
        String yAxisSplitLineColor = chartSettings.getYAxes().values().stream()
                .filter(axis -> axis.getShow() && axis.getShowSplitLines())
                .map(TimeSeriesChartAxisSettings::getSplitLinesColor).findFirst().orElse(null);

        if (chartSettings.getGrid().getShow()) {
            String gridBackgroundColor = chartSettings.getGrid().getBackgroundColor();
            plot.setBackgroundPaint(safeParseCssColor(gridBackgroundColor, null));
            plot.setOutlineStroke(new BasicStroke(chartSettings.getGrid().getBorderWidth()));
            plot.setOutlinePaint(safeParseCssColor(chartSettings.getGrid().getBorderColor()));
        } else {
            plot.setBackgroundPaint(null);
            plot.setOutlinePaint(null);
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

        this.seriesList = allSeries.stream().sorted(Comparator.comparingInt(TsChartSeriesData::getIndex)).toList();

        Map<TbDatasetKey, List<TsChartSeriesData>> groupedSeries = datasetGroupsFromSeries(this.seriesList);

        groupedSeries.forEach(this::setupDataset);
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
                items.sort((item1, item2) -> {
                    int dataIndex1 = findSeriesIndex(item1.getDatasetIndex(), item1.getSeriesIndex());
                    int dataIndex2 = findSeriesIndex(item2.getDatasetIndex(), item2.getSeriesIndex());
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


    private void setupDataset(TbDatasetKey datasetKey, List<TsChartSeriesData> seriesList) {
        createDataset(datasetKey, seriesList);
        createDatasetRenderer(datasetKey, seriesList);
    }

    private void createDataset(TbDatasetKey datasetKey, List<TsChartSeriesData> seriesList) {
        int xAxisIndex = 0;
        int yAxisIndex = this.yAxisIndexMap.get(datasetKey.getYAxisId());
        if (datasetKey.getSeriesType() == TimeSeriesChartSeriesType.bar) {
            List<TsChartSeriesData> barsList = this.seriesList.stream().
                    filter(barSeries -> !barSeries.isEmpty() && getSeriesSettings(barSeries).getSeriesType() == TimeSeriesChartSeriesType.bar).toList();
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
            this.createBarsDataset(datasetKey.getDatasetIndex(), xAxisIndex, yAxisIndex, seriesList, barsList, barRenderCtx);
        } else {
            this.createLinesDataset(datasetKey.getDatasetIndex(), xAxisIndex, yAxisIndex, seriesList);
        }
    }

    private void createDatasetRenderer(TbDatasetKey datasetKey, List<TsChartSeriesData> seriesList) {
        if (datasetKey.getSeriesType() == TimeSeriesChartSeriesType.bar) {
            this.createBarsRenderer(datasetKey, seriesList);
        } else {
            this.createLinesRenderer(datasetKey, seriesList);
        }
    }

    private void createBarsDataset(int datasetIndex, int xAxisIndex, int yAxisIndex,
                                   List<TsChartSeriesData> seriesList,
                                   List<TsChartSeriesData> allBarsList,
                                   TimeseriesBarRenderCtx barRenderCtx) {
        XYDataset dataset;
        if (this.stackMode) {
            List<Integer> barDatasets = allBarsList.stream().map(TsChartSeriesData::getDatasetIndex).distinct().sorted().toList();
            int barsCount = barDatasets.size();
            int barIndex = barDatasets.indexOf(datasetIndex);
            TimeTableXYDataset tableDataset = new TimeTableXYDataset(chartData.getTimeZone());
            for (TsChartSeriesData series : seriesList) {
                String seriesName = datasetIndex + "_" + series.getSeriesIndex();
                for (TsChartSeriesEntry tsValue : series.getData()) {
                    SimpleTimePeriod timePeriod = calculateBarTimePeriod(tsValue, barRenderCtx, barsCount, barIndex);
                    try {
                        double doubleValue = Double.parseDouble(tsValue.getValue());
                        tableDataset.add(timePeriod, doubleValue, seriesName);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            dataset = tableDataset;
        } else {
            int barsCount = allBarsList.size();
            TimePeriodValuesCollection tpvDataset = new TimePeriodValuesCollection();
            for (TsChartSeriesData series : seriesList) {
                int barIndex = allBarsList.indexOf(series);
                TimePeriodValues timePeriods = new TimePeriodValues(datasetIndex + "_" + series.getSeriesIndex());
                for (TsChartSeriesEntry tsValue : series.getData()) {
                    try {
                        double doubleValue = Double.parseDouble(tsValue.getValue());
                        SimpleTimePeriod timePeriod = calculateBarTimePeriod(tsValue, barRenderCtx, barsCount, barIndex);
                        timePeriods.add(timePeriod, doubleValue);
                    } catch (NumberFormatException ignored) {}
                }
                tpvDataset.addSeries(timePeriods);
            }
            dataset = tpvDataset;
        }
        plot.setDataset(datasetIndex, dataset);
        plot.mapDatasetToDomainAxis(datasetIndex, xAxisIndex);
        plot.mapDatasetToRangeAxis(datasetIndex, yAxisIndex);
    }

    private void createLinesDataset(int datasetIndex, int xAxisIndex, int yAxisIndex, List<TsChartSeriesData> seriesList) {
        Locale locale = Locale.getDefault();
        XYDataset dataset;
        if (this.stackMode) {
            TimeTableXYDataset tableDataset = new TimeTableXYDataset(chartData.getTimeZone());
            for (TsChartSeriesData series : seriesList) {
                String seriesName = datasetIndex + "_" + series.getSeriesIndex();
                for (TsChartSeriesEntry tsValue : series.getData()) {
                    Millisecond millisecond = new Millisecond(new Date(tsValue.getTs()), chartData.getTimeZone(), locale);
                    try {
                        double doubleValue = Double.parseDouble(tsValue.getValue());
                        tableDataset.add(millisecond, doubleValue, seriesName);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            dataset = tableDataset;
        } else {
            TimeSeriesCollection tsDataset = new TimeSeriesCollection(chartData.getTimeZone());
            for (TsChartSeriesData series : seriesList) {
                TimeSeries timeSeries = new TimeSeries(datasetIndex + "_" + series.getSeriesIndex());
                for (TsChartSeriesEntry tsValue : series.getData()) {
                    Millisecond millisecond = new Millisecond(new Date(tsValue.getTs()), chartData.getTimeZone(), locale);
                    try {
                        double doubleValue = Double.parseDouble(tsValue.getValue());
                        timeSeries.add(millisecond, doubleValue);
                    } catch (NumberFormatException ignored) {
                    }
                }
                tsDataset.addSeries(timeSeries);
            }
            dataset = tsDataset;
        }
        plot.setDataset(datasetIndex, dataset);
        plot.mapDatasetToDomainAxis(datasetIndex, xAxisIndex);
        plot.mapDatasetToRangeAxis(datasetIndex, yAxisIndex);
    }

    private void createBarsRenderer(TbDatasetKey datasetKey, List<TsChartSeriesData> seriesList) {
        TbXYBarRenderer renderer = this.createBarRenderer();
        for (TsChartSeriesData series : seriesList) {
            BarSeriesSettings barSettings = getSeriesSettings(series).getBarSettings();
            Color seriesColor = safeParseCssColor(series.getDataKey().getColor());
            Paint seriesPaint = seriesColor;
            if (!ChartFillType.none.equals(barSettings.getBackgroundSettings().getType())) {
                seriesPaint = createFillPaint(barSettings.getBackgroundSettings(), seriesColor);
            }
            renderer.setSeriesPaint(series.getSeriesIndex(), seriesPaint);
            if (barSettings.getShowBorder()) {
                renderer.setSeriesOutlineStroke(series.getSeriesIndex(), new BasicStroke(barSettings.getBorderWidth()));
                renderer.setSeriesOutlinePaint(series.getSeriesIndex(), seriesColor);
            }
            renderer.setSeriesItemBorderRadius(series.getSeriesIndex(), barSettings.getBorderRadius());
            if (barSettings.getShowLabel()) {
                renderer.setSeriesItemLabelsVisible(series.getSeriesIndex(), true);
                renderer.setSeriesItemLabelFont(series.getSeriesIndex(), toAwtFont(barSettings.getLabelFont()));
                renderer.setSeriesItemLabelPaint(series.getSeriesIndex(), safeParseCssColor(barSettings.getLabelColor()));
                XYItemLabelGenerator labelGenerator = new TbXYItemLabelGenerator(series.getDataKey().getDecimals(), series.getDataKey().getUnits());
                renderer.setSeriesItemLabelGenerator(series.getSeriesIndex(), labelGenerator);
                ItemLabelPosition positiveItemLabelPosition;
                ItemLabelPosition negativeItemLabelPosition;
                if (ChartLabelPosition.top.equals(barSettings.getLabelPosition())) {
                    positiveItemLabelPosition = new ItemLabelPosition(
                            ItemLabelAnchor.OUTSIDE12, TextAnchor.BOTTOM_CENTER);
                    negativeItemLabelPosition = new ItemLabelPosition(
                            ItemLabelAnchor.OUTSIDE6, TextAnchor.TOP_CENTER);
                } else {
                    positiveItemLabelPosition = new ItemLabelPosition(
                            ItemLabelAnchor.OUTSIDE6, TextAnchor.TOP_CENTER);
                    negativeItemLabelPosition = new ItemLabelPosition(
                            ItemLabelAnchor.OUTSIDE12, TextAnchor.BOTTOM_CENTER);
                }
                renderer.setSeriesPositiveItemLabelPosition(series.getSeriesIndex(), positiveItemLabelPosition);
                renderer.setSeriesNegativeItemLabelPosition(series.getSeriesIndex(), negativeItemLabelPosition);
                if (barSettings.getEnableLabelBackground()) {
                    renderer.setSeriesItemLabelsBackgroundVisible(series.getSeriesIndex(), true);
                    renderer.setSeriesItemLabelsBackgroundPaint(series.getSeriesIndex(), safeParseCssColor(barSettings.getLabelBackground()));
                }
            }

        }
        plot.setRenderer(datasetKey.getDatasetIndex(), renderer);
    }

    private void createLinesRenderer(TbDatasetKey datasetKey, List<TsChartSeriesData> seriesList) {
        TbXYLineAndShapeRenderer renderer = createLineRenderer(datasetKey);
        for (TsChartSeriesData series : seriesList) {
            LineSeriesSettings lineSettings = getSeriesSettings(series).getLineSettings();
            Color seriesColor = safeParseCssColor(series.getDataKey().getColor());
            Paint fillPaint = createFillPaint(lineSettings.getFillAreaSettings(), seriesColor);
            renderer.setSeriesPaint(series.getSeriesIndex(), seriesColor);
            renderer.setSeriesFillPaint(series.getSeriesIndex(), fillPaint);

            if (lineSettings.getShowLine()) {
                renderer.setSeriesLinesVisible(series.getSeriesIndex(), true);
                Stroke lineStroke = createLineStroke(lineSettings.getLineType(), lineSettings.getLineWidth());
                renderer.setSeriesStroke(series.getSeriesIndex(), lineStroke);
            } else {
                renderer.setSeriesLinesVisible(series.getSeriesIndex(), false);
            }
            if (lineSettings.getShowPoints()) {
                Shape seriesShape = createSeriesShape(lineSettings.getPointShape(), lineSettings.getPointSize());
                if (seriesShape != null) {
                    renderer.setSeriesShapesVisible(series.getSeriesIndex(), true);
                    renderer.setSeriesShape(series.getSeriesIndex(), seriesShape);
                    if (ChartShape.emptyCircle.equals(lineSettings.getPointShape())) {
                        renderer.setSeriesShapesFillPaint(series.getSeriesIndex(), Color.WHITE);
                        renderer.setSeriesOutlineStroke(series.getSeriesIndex(), new BasicStroke(2.0f));
                    }
                    if (lineSettings.getShowPointLabel()) {
                        renderer.setSeriesItemLabelsVisible(series.getSeriesIndex(), true);
                        renderer.setSeriesItemLabelFont(series.getSeriesIndex(), toAwtFont(lineSettings.getPointLabelFont()));
                        renderer.setSeriesItemLabelPaint(series.getSeriesIndex(), safeParseCssColor(lineSettings.getPointLabelColor()));
                        XYItemLabelGenerator labelGenerator = new TbXYItemLabelGenerator(series.getDataKey().getDecimals(), series.getDataKey().getUnits());
                        renderer.setSeriesItemLabelGenerator(series.getSeriesIndex(), labelGenerator);
                        ItemLabelPosition itemLabelPosition;
                        if (ChartLabelPosition.top.equals(lineSettings.getPointLabelPosition())) {
                            itemLabelPosition = new ItemLabelPosition(
                                    ItemLabelAnchor.OUTSIDE12, TextAnchor.BOTTOM_CENTER);
                        } else {
                            itemLabelPosition = new ItemLabelPosition(
                                    ItemLabelAnchor.OUTSIDE6, TextAnchor.TOP_CENTER);
                        }
                        renderer.setSeriesPositiveItemLabelPosition(series.getSeriesIndex(), itemLabelPosition);
                        renderer.setSeriesNegativeItemLabelPosition(series.getSeriesIndex(), itemLabelPosition);
                        if (lineSettings.getEnablePointLabelBackground()) {
                            renderer.setSeriesItemLabelsBackgroundVisible(series.getSeriesIndex(), true);
                            renderer.setSeriesItemLabelsBackgroundPaint(series.getSeriesIndex(), safeParseCssColor(lineSettings.getPointLabelBackground()));
                        }
                    }
                } else {
                    renderer.setSeriesShapesVisible(series.getSeriesIndex(), false);
                }
            } else {
                renderer.setSeriesShapesVisible(series.getSeriesIndex(), false);
            }
        }
        plot.setRenderer(datasetKey.getDatasetIndex(), renderer);
    }

    private TbXYBarRenderer createBarRenderer() {
        TbXYBarRenderer barRenderer = new TbXYBarRenderer(this.stackMode);
        barRenderer.setShadowVisible(false);
        barRenderer.setDrawBarOutline(true);
        barRenderer.setDefaultOutlineStroke(new BasicStroke(0.0f));
        barRenderer.setLegendItemLabelGenerator(this);
        return barRenderer;
    }

    private TbXYLineAndShapeRenderer createLineRenderer(TbDatasetKey datasetKey) {
        TbXYLineAndShapeRenderer.LineInterpolationType interpolationType = TbXYLineAndShapeRenderer.LineInterpolationType.NONE;
        if (datasetKey.isStepLine()) {
            interpolationType = TbXYLineAndShapeRenderer.LineInterpolationType.STEP;
        } else if (datasetKey.isSmoothLine()) {
            interpolationType = TbXYLineAndShapeRenderer.LineInterpolationType.SMOOTH;
        }
        TbXYLineAndShapeRenderer lineRenderer = new TbXYLineAndShapeRenderer(interpolationType,
                datasetKey.isFillArea() ? TbXYLineAndShapeRenderer.FillType.TO_ZERO : TbXYLineAndShapeRenderer.FillType.NONE,
                this.stackMode);
        if (datasetKey.isStepLine()) {
            double stepPoint = 0.0;
            switch (datasetKey.getStepType()) {
                case start -> stepPoint = 0.0;
                case middle -> stepPoint = 0.5;
                case end -> stepPoint = 1.0;
            }
            lineRenderer.setStepPoint(stepPoint);
        } else if (datasetKey.isSmoothLine()) {
            lineRenderer.setPrecision(100);
            lineRenderer.setSmooth(0.25f);
        }
        lineRenderer.setLegendItemLabelGenerator(this);
        return lineRenderer;
    }

    @Override
    public String generateLabel(XYDataset dataset, int seriesIndex) {
        int datasetIndex = this.plot.indexOf(dataset);
        Optional<TsChartSeriesData> seriesOpt = findSeries(datasetIndex, seriesIndex);
        if (seriesOpt.isPresent()) {
            TsChartSeriesData series = seriesOpt.get();
            return ThymeleafUtil.renderFromTextString(series.getDataKey().getLabel(), series.getDataSource().getVariables());
        } else {
            return "Undefined";
        }
    }

    private int findSeriesIndex(int datasetIndex, int seriesIndex) {
        Optional<TsChartSeriesData> series = findSeries(datasetIndex, seriesIndex);
        return series.map(TsChartSeriesData::getIndex).orElse(-1);
    }

    private Optional<TsChartSeriesData> findSeries(int datasetIndex, int seriesIndex) {
        return this.seriesList.stream()
                .filter(s -> s.getDatasetIndex() == datasetIndex && s.getSeriesIndex() == seriesIndex)
                .findFirst();
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.TIME_SERIES_CHART;
    }

}
