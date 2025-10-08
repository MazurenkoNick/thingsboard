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
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.chart.ReportBarChartSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ReportDoughnutChartSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ReportPieChartSettings;
import org.thingsboard.server.common.data.report.configuration.components.LatestChartComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.context.chart.LatestChartData;
import org.thingsboard.server.report.renderer.chart.TbBarChart;
import org.thingsboard.server.report.renderer.chart.TbDoughnutChart;
import org.thingsboard.server.report.renderer.chart.TbLatestChart;
import org.thingsboard.server.report.renderer.chart.TbPieChart;

import java.awt.Graphics2D;
import java.util.Map;

import static org.thingsboard.server.common.data.report.configuration.chart.ReportComponentSubType.DOUGHNUT_CHART;
import static org.thingsboard.server.common.data.report.configuration.chart.ReportComponentSubType.HORIZONTAL_DOUGHNUT_CHART;
import static org.thingsboard.server.common.data.report.configuration.chart.ReportComponentSubType.LATEST_BAR_CHART;
import static org.thingsboard.server.common.data.report.configuration.chart.ReportComponentSubType.PIE_CHART;

@Component
public class LatestChartRenderer extends ChartRenderer<LatestChartComponent> {

    @Override
    protected JFreeChart createChart(Graphics2D g2, LatestChartComponent component, ComponentData reportDataSource) {
        TbLatestChart<?,?> latestChart = createLatestChart(component, reportDataSource.getLatestChartData(), reportDataSource.getVariables());
        return latestChart.createChart(g2);
    }

    private TbLatestChart<?,?> createLatestChart(LatestChartComponent component, LatestChartData latestChartData, Map<String, Object> variables) {
        if (LATEST_BAR_CHART == component.getSubType()) {
            ReportBarChartSettings reportBarChartSettings = new ReportBarChartSettings((ReportBarChartSettings) component.getLatestChartSettings());
            return new TbBarChart(reportBarChartSettings, latestChartData, variables);
        } else if (PIE_CHART == component.getSubType()) {
            ReportPieChartSettings reportPieChartSettings = new ReportPieChartSettings((ReportPieChartSettings) component.getLatestChartSettings());
            return new TbPieChart(reportPieChartSettings, latestChartData, variables);
        } else if (DOUGHNUT_CHART == component.getSubType() || HORIZONTAL_DOUGHNUT_CHART == component.getSubType()) {
            ReportDoughnutChartSettings reportDoughnutChartSettings = new ReportDoughnutChartSettings((ReportDoughnutChartSettings) component.getLatestChartSettings());
            return new TbDoughnutChart(reportDoughnutChartSettings, latestChartData, variables);
        } else {
            throw new IllegalArgumentException("Latest chart with subType '" + component.getSubType() + "' is not supported");
        }
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.LATEST_CHART;
    }
}
