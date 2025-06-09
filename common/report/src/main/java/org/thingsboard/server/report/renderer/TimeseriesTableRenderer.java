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

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.report.context.ComponentData;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Component
public class TimeseriesTableRenderer extends TableComponentRenderer {

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.TIME_SERIES_TABLE;
    }

    protected String noDataMessage() {
        return "No time series data found";
    }

    protected Float defaultFontSize(Map.Entry<String, String> entry) {
        String key = entry.getKey();
        if ("ts".equals(key)) {
            return 9f;
        }
        return null;
    }

    protected boolean displayTitle(ComponentData reportDataSource) {
        return (boolean) reportDataSource.getVariables().getOrDefault("showTitle", false);
    }

    protected String title(ComponentData reportDataSource) {
        return (String) reportDataSource.getVariables().getOrDefault("title", "Time series table");
    }

    protected HashMap<String, CellVariables> getCellVariablesMap(ReportComponent component, DataSource dataSource, boolean isHeader) {
        TimeseriesTableComponent timeseriesTableComponent = (TimeseriesTableComponent) component;
        HashMap<String, CellVariables> variablesMap = new LinkedHashMap<>();
        if (timeseriesTableComponent.isShowTimestamp()) {
            String label = timeseriesTableComponent.getTimestampLabel();
            if (StringUtils.isBlank(label)) {
                label = "Timestamp";
            }
            variablesMap.put("ts", toCellVariables(label, timeseriesTableComponent.getTimestampColumnSettings(), isHeader));
        }
        variablesMap.putAll(super.getCellVariablesMap(component, dataSource, isHeader));
        List<DataKey> latestDataKeys = dataSource.getLatestDataKeys();
        if (latestDataKeys != null && !latestDataKeys.isEmpty()) {
            HashMap<String, CellVariables> latestVariablesMap = latestDataKeys.stream()
                    .collect(Collectors.toMap(
                            DataKey::getName,
                            dataKey -> toCellVariables(dataKey, isHeader),
                            (v1, v2) -> v1,
                            LinkedHashMap::new
                    ));
            variablesMap.putAll(latestVariablesMap);
        }
        return variablesMap;
    }

}
