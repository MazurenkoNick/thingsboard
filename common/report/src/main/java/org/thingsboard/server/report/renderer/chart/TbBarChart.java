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

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.thingsboard.server.common.data.report.configuration.chart.BarSeriesSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ChartFillType;
import org.thingsboard.server.common.data.report.configuration.chart.ChartLabelPosition;
import org.thingsboard.server.common.data.report.configuration.chart.ReportBarChartSettings;
import org.thingsboard.server.report.context.chart.LatestChartData;
import org.thingsboard.server.report.context.chart.LatestChartDataItem;

import java.awt.*;

import static org.thingsboard.server.report.renderer.chart.ChartUtils.createFillPaint;
import static org.thingsboard.server.report.util.AwtFontUtils.toAwtFont;
import static org.thingsboard.server.report.util.ColorUtils.safeParseCssColor;

public class TbBarChart extends TbLatestChart<ReportBarChartSettings> implements CategoryItemLabelGenerator {

    public TbBarChart(ReportBarChartSettings chartSettings, LatestChartData latestChartData) {
        super(chartSettings, latestChartData);
    }

    @Override
    protected Plot createPlot() {
        CategoryAxis categoryAxis = new CategoryAxis(null);
        categoryAxis.setVisible(false);

        TbNumberAxis valueAxis = new TbNumberAxis(null, null);
        if (this.chartSettings.getAxisMin() != null) {
            valueAxis.setAxisMin(this.chartSettings.getAxisMin());
        }
        if (this.chartSettings.getAxisMax() != null) {
            valueAxis.setAxisMax(this.chartSettings.getAxisMax());
        }
        valueAxis.setNumberFormatOverride(this.valueFormatter);
        valueAxis.setTickLabelFont(toAwtFont(this.chartSettings.getAxisTickLabelFont()));
        valueAxis.setTickLabelPaint(safeParseCssColor(this.chartSettings.getAxisTickLabelColor()));

        BarRenderer renderer = new BarRenderer();
        renderer.setShadowVisible(false);
        renderer.setDrawBarOutline(true);
        renderer.setDefaultOutlineStroke(new BasicStroke(0.0f));

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        BarSeriesSettings barSettings = this.chartSettings.getBarSettings();
        // TODO: renderer.setMaximumBarWidth(barSettings.getBarWidth() / 100.0);
        for (LatestChartDataItem dataItem : this.dataItems) {
            dataset.addValue(dataItem.getValue(), dataItem.getLabel(), "Latest");
            Color seriesColor = safeParseCssColor(dataItem.getDataKey().getColor());
            Paint seriesPaint = seriesColor;
            if (!ChartFillType.none.equals(barSettings.getBackgroundSettings().getType())) {
                seriesPaint = createFillPaint(barSettings.getBackgroundSettings(), seriesColor);
            }
            renderer.setSeriesPaint(dataItem.getSeriesIndex(), seriesPaint);
            if (barSettings.getShowBorder()) {
                renderer.setSeriesOutlineStroke(dataItem.getSeriesIndex(), new BasicStroke(barSettings.getBorderWidth()));
                renderer.setSeriesOutlinePaint(dataItem.getSeriesIndex(), seriesColor);
            }
            if (barSettings.getShowLabel()) {
                renderer.setSeriesItemLabelsVisible(dataItem.getSeriesIndex(), true);
                renderer.setSeriesItemLabelFont(dataItem.getSeriesIndex(), toAwtFont(barSettings.getLabelFont()));
                renderer.setSeriesItemLabelPaint(dataItem.getSeriesIndex(), safeParseCssColor(barSettings.getLabelColor()));
                renderer.setSeriesItemLabelGenerator(dataItem.getSeriesIndex(), this);
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
                renderer.setSeriesPositiveItemLabelPosition(dataItem.getSeriesIndex(), positiveItemLabelPosition);
                renderer.setSeriesNegativeItemLabelPosition(dataItem.getSeriesIndex(), negativeItemLabelPosition);
                if (barSettings.getEnableLabelBackground()) {
                   // TODO: renderer.setSeriesItemLabelsBackgroundVisible(dataItem.getSeriesIndex(), true);
                   // TODO: renderer.setSeriesItemLabelsBackgroundPaint(dataItem.getSeriesIndex(), safeParseCssColor(barSettings.getLabelBackground()));
                }
            }
            // TODO: renderer.setSeriesItemBorderRadius(dataItem.getSeriesIndex(), barSettings.getBorderRadius());
        }

        CategoryPlot plot = new CategoryPlot(dataset, categoryAxis, valueAxis,
                renderer);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
//        plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);
        plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);
        return plot;
    }


    @Override
    public String generateRowLabel(CategoryDataset dataset, int row) {
        return "";
    }

    @Override
    public String generateColumnLabel(CategoryDataset dataset, int column) {
        return "";
    }

    @Override
    public String generateLabel(CategoryDataset dataset, int row, int column) {
        return dataItems.get(row).getLabel();
    }
}
