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
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.report.context.ComponentData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.report.util.ReportUtils.getSingleDataSource;


@Component
public class CsvTimeseriesTableRenderer extends AbstractCsvComponentRenderer<TimeseriesTableComponent> {


    @Override
    public List<List<String>> render(TimeseriesTableComponent component, ComponentData reportDataSource) {
        Optional<DataSource> dataSourceOpt = getSingleDataSource(component);
        if (dataSourceOpt.isEmpty()) {
            return List.of(List.of("Data source is not configured for alarm table"));
        }

        DataSource dataSource = dataSourceOpt.get();

        Map<String, String> labelToLatestKey = buildLabelToKeyMap(dataSource.getLatestDataKeys());
        Map<String, String> labelToKey = buildLabelToKeyMap(dataSource.getDataKeys());

        List<List<String>> content = new ArrayList<>();

        // add heading
        addOptionalHeading(component, reportDataSource, content);

        // Build and add headers
        List<String> headers = new ArrayList<>(labelToLatestKey.keySet());
        headers.add("TIMESTAMP");
        headers.addAll(labelToKey.keySet());
        content.add(headers);

        // Build and add rows
        for (Map<String, String> row : reportDataSource.getEntityDatas()) {
            List<String> values = new ArrayList<>();
            values.addAll(extractValues(row, labelToLatestKey));
            values.add(row.getOrDefault("rawTs", ""));
            values.addAll(extractValues(row, labelToKey));
            content.add(values);
        }

        return content;
    }

    private Map<String, String> buildLabelToKeyMap(List<DataKey> dataKeys) {
        if (dataKeys == null) return Collections.emptyMap();
        return dataKeys.stream()
                .collect(Collectors.toMap(
                        DataKey::getLabel,
                        DataKey::getName,
                        (existing, replacement) -> replacement,
                        LinkedHashMap::new));
    }

    private List<String> extractValues(Map<String, String> row, Map<String, String> labelToKey) {
        return labelToKey.values().stream()
                .map(key -> row.getOrDefault(key, ""))
                .toList();
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.TIME_SERIES_TABLE;
    }

}
