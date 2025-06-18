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

import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.components.TableReportComponent;
import org.thingsboard.server.common.data.report.configuration.style.Heading;
import org.thingsboard.server.report.context.ComponentData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.report.util.ReportUtils.formatNumericValue;
import static org.thingsboard.server.report.util.ReportUtils.getSingleDataSource;
import static org.thingsboard.server.report.util.ReportUtils.tableHeadingText;

public abstract class AbstractCsvComponentRenderer<C extends TableReportComponent> implements CsvReportComponentRenderer<C> {

    protected List<List<String>> renderTable(TableReportComponent component, ComponentData reportDataSource) {
        Optional<DataSource> dataSourceOpt = getSingleDataSource(component);
        if (dataSourceOpt.isEmpty()) {
            return List.of(List.of("Data source is not configured for alarm table"));
        }

        DataSource dataSource = dataSourceOpt.get();
        Map<String, DataKey> labelToDataKeyMap = buildLabelToDataKeyMap(dataSource.getDataKeys());
        List<List<String>> content = new ArrayList<>();

        // add heading
        addOptionalHeading(component, reportDataSource, content);

        // add headers
        ArrayList<String> headers = new ArrayList<>(labelToDataKeyMap.keySet());
        content.add(headers);

        // add data rows
        for (Map<String, String> row : reportDataSource.getEntityDatas()) {
            content.add(extractValues(row, labelToDataKeyMap));
        }
        return content;
    }

    protected Map<String, DataKey> buildLabelToDataKeyMap(List<DataKey> dataKeys) {
        if (dataKeys == null) return Collections.emptyMap();
        return dataKeys.stream()
                .collect(Collectors.toMap(
                        DataKey::getLabel,
                        dataKey -> dataKey,
                        (existing, replacement) -> replacement,
                        LinkedHashMap::new));
    }

    protected List<String> extractValues(Map<String, String> row, Map<String, DataKey> labelToDataKey) {
        return labelToDataKey.values().stream()
                .map(dataKey -> formatNumericValue(row.get(dataKey.getLabel()), dataKey))
                .toList();
    }

    protected void addOptionalHeading(TableReportComponent component, ComponentData reportDataSource, List<List<String>> content) {
        if (component.isShowTableHeading() && component.getTableHeading() != null) {
            Heading tableHeading = component.getTableHeading();
            String headingText = tableHeadingText(tableHeading, reportDataSource);
            content.add(List.of(headingText));
        }
    }

}
