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
package org.thingsboard.server.report.renderer.chart;

import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.plot.PieLabelLinkStyle;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.util.Rotation;
import org.jfree.chart.util.UnitType;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.thingsboard.server.common.data.report.configuration.chart.PieChartLabelPosition;
import org.thingsboard.server.common.data.report.configuration.chart.ReportPieChartSettings;
import org.thingsboard.server.report.context.chart.LatestChartData;
import org.thingsboard.server.report.context.chart.LatestChartDataItem;

import java.awt.BasicStroke;
import java.text.AttributedString;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.thingsboard.server.report.renderer.chart.ChartUtils.createValueFormatter;
import static org.thingsboard.server.report.util.AwtFontUtils.toAwtFont;
import static org.thingsboard.server.report.util.ColorUtils.safeParseCssColor;

public class TbPieChart extends TbLatestChart<ReportPieChartSettings, TbPiePlot<Integer>> implements PieSectionLabelGenerator  {

    private DefaultPieDataset<Integer> pieDataset;
    private Map<Integer, String> percents;

    public TbPieChart(ReportPieChartSettings settings, LatestChartData latestChartData, Map<String, Object> variables) {
        super(settings, latestChartData, variables);
    }

    @Override
    protected TbPiePlot<Integer> createPlot() {
        this.pieDataset = new DefaultPieDataset<>();
        return new TbPiePlot<>(this.pieDataset);
    }

    @Override
    protected void setupPlot(TbPiePlot<Integer> plot) {
        plot.setPieRadius(this.chartSettings.getRadius());
        if (this.chartSettings.getShowLabel()) {
            plot.setLabelGenerator(this);
            plot.setLabelFont(toAwtFont(this.chartSettings.getLabelFont()));
            plot.setLabelPaint(safeParseCssColor(this.chartSettings.getLabelColor()));
            plot.setLabelShadowPaint(null);
            plot.setLabelBackgroundPaint(null);
            plot.setLabelOutlinePaint(null);
            if (this.chartSettings.getLabelPosition() == PieChartLabelPosition.outside) {
                plot.setSimpleLabels(false);
                plot.setLabelLinkStyle(PieLabelLinkStyle.QUAD_CURVE);
            } else {
                plot.setSimpleLabels(true);
                plot.setSimpleLabelOffset(new RectangleInsets(UnitType.RELATIVE, 0.28,
                        0.28, 0.28, 0.28));
            }
        } else {
            plot.setLabelGenerator(null);
        }
        plot.setShadowPaint(null);

        plot.setDirection(this.chartSettings.getClockwise() ? Rotation.CLOCKWISE : Rotation.ANTICLOCKWISE);
        if (this.chartSettings.getBorderWidth() > 0) {
            plot.setSectionOutlinesVisible(true);
            plot.setDefaultSectionOutlineStroke(new BasicStroke(this.chartSettings.getBorderWidth()));
            plot.setDefaultSectionOutlinePaint(safeParseCssColor(this.chartSettings.getBorderColor()));
        } else {
            plot.setSectionOutlinesVisible(false);
        }

        this.percents = new HashMap<>();
        List<LatestChartDataItem> visibleDataItems = this.dataItems.stream().filter(item -> item.isHasValue() && item.getValue() >= 0).toList();
        double total = 0;
        for (LatestChartDataItem dataItem : visibleDataItems) {
            Integer key = dataItem.getSeriesIndex();
            this.pieDataset.setValue(key, dataItem.getValue());
            plot.setSectionPaint(key, safeParseCssColor(dataItem.getDataKey().getColor()));
            total += dataItem.getValue();
        }
        if (visibleDataItems.isEmpty()) {
            plot.setLabelGenerator(null);
            Integer key = 0;
            this.pieDataset.setValue(key, 1.0);
            plot.setSectionPaint(key, safeParseCssColor("lightgray"));
        }
        NumberFormat percentFormat = createValueFormatter(0, "%");
        for (LatestChartDataItem dataItem : visibleDataItems) {
            Integer key = dataItem.getSeriesIndex();
            double percent = 0;
            if (total == 0) {
                this.pieDataset.setValue(key, 1);
            } else {
                percent = dataItem.getValue() / total * 100;
            }
            this.percents.put(key, percentFormat.format(percent));
        }
    }

    @Override
    public String generateSectionLabel(PieDataset dataset, Comparable key) {
        return dataItems.get((int)key).getLabel() + "\n" + percents.get((int)key);
    }

    @Override
    public AttributedString generateAttributedSectionLabel(PieDataset dataset, Comparable key) {
        return null;
    }
}
