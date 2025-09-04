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

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.thingsboard.server.common.data.report.configuration.chart.ChartFillType;
import org.thingsboard.server.common.data.report.configuration.chart.LineSeriesSettings;
import org.thingsboard.server.common.data.report.configuration.chart.LineSeriesStepType;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartKeySettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartSeriesType;

import java.util.Objects;

@Data
public class TbDatasetKey implements Comparable<TbDatasetKey> {

    private String yAxisId;
    private boolean comparison;
    private TimeSeriesChartSeriesType seriesType;
    private boolean stepLine;
    private LineSeriesStepType stepType;
    private boolean smoothLine;
    private boolean fillArea;

    private int datasetIndex;

    public TbDatasetKey(TimeSeriesChartKeySettings keySettings, boolean comparison) {
        this.comparison = comparison;
        this.yAxisId = keySettings.getYAxisId();
        this.seriesType = keySettings.getSeriesType();
        if (this.seriesType == TimeSeriesChartSeriesType.line) {
            LineSeriesSettings lineSettings = keySettings.getLineSettings();
            this.stepLine = lineSettings.getStep();
            if (this.stepLine) {
                this.stepType = lineSettings.getStepType();
            }
            this.smoothLine = lineSettings.getSmooth();
            this.fillArea = lineSettings.getFillAreaSettings().getType() != ChartFillType.none;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TbDatasetKey that)) return false;
        if (comparison != that.comparison) return false;
        if (!Objects.equals(yAxisId, that.yAxisId)) return false;
        if (seriesType == that.seriesType) {
            if (seriesType == TimeSeriesChartSeriesType.bar) {
                return true;
            } else {
                return stepLine == that.stepLine && stepType == that.stepType && smoothLine == that.smoothLine && fillArea == that.fillArea;
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        if (seriesType == TimeSeriesChartSeriesType.bar) {
            return Objects.hash(comparison, yAxisId, seriesType);
        } else {
            return Objects.hash(comparison, yAxisId, stepLine, stepType, smoothLine, fillArea);
        }
    }

    @Override
    public int compareTo(@NotNull TbDatasetKey tbDatasetKey) {
        int result = 0;
        if (this.comparison && !tbDatasetKey.comparison) {
            result = 1;
        } else if (!this.comparison && tbDatasetKey.comparison) {
            result = -1;
        }
        if (result == 0) {
            if (this.seriesType == tbDatasetKey.seriesType) {
                if (this.seriesType != TimeSeriesChartSeriesType.bar) {
                    if (this.fillArea && !tbDatasetKey.fillArea) {
                        result = -1;
                    } else if (!this.fillArea && tbDatasetKey.fillArea) {
                        result = 1;
                    }
                }
            } else if (this.seriesType == TimeSeriesChartSeriesType.bar) {
                result = -1;
            } else {
                result = 1;
            }
        }
        if (result == 0 && !Objects.equals(yAxisId, tbDatasetKey.yAxisId)) {
            if (this.yAxisId.equals("default")) {
                result = -1;
            } else {
                result = 1;
            }
        }
        return result;
    }
}
