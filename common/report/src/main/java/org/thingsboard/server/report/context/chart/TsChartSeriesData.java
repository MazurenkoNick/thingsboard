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
package org.thingsboard.server.report.context.chart;

import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.DataKey;

import java.util.List;

@Data
public class TsChartSeriesData {

    private TsChartDataSource dataSource;
    private DataKey dataKey;
    private List<TsChartSeriesEntry> data;
    private List<Double> numericData;
    private int index;

    private int keyIndex;

    private int datasetIndex;
    private int seriesIndex;

    public boolean isEmpty() {
        return data == null || data.isEmpty();
    }

    public Double calcMin() {
        if (!numericData.isEmpty()) {
            double result = numericData.get(0);
            for (int i = 1; i < numericData.size(); i++) {
                double value = numericData.get(i);
                result = Math.min(result, value);
            }
            return result;
        } else {
            return null;
        }
    }

    public Double calcMax() {
        if (!numericData.isEmpty()) {
            double result = numericData.get(0);
            for (int i = 1; i < numericData.size(); i++) {
                double value = numericData.get(i);
                result = Math.max(result, value);
            }
            return result;
        } else {
            return null;
        }
    }

    public Double calcTotal() {
        if (!numericData.isEmpty()) {
            double result = 0;
            for (int i = 0; i < numericData.size(); i++) {
                double value = numericData.get(i);
                result += value;
            }
            return result;
        } else {
            return null;
        }
    }

    public int calcCount() {
        return numericData.size();
    }

    public Double calcAvg() {
        Double total = calcTotal();
        if (total != null) {
            int count = calcCount();
            return total / count;
        } else {
            return null;
        }
    }

    public Double calcLatest() {
        if (!numericData.isEmpty()) {
            return numericData.get(numericData.size() - 1);
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "TsChartSeriesData{" + "dataKey=" + dataKey +
                ", data=" + data +
                ", index=" + index +
                ", keyIndex=" + keyIndex +
                ", datasetIndex=" + datasetIndex +
                ", seriesIndex=" + seriesIndex +
                '}';
    }
}
