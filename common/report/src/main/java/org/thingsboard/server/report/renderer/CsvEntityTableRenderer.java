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
import org.thingsboard.server.common.data.report.configuration.components.EntityTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.report.context.ComponentData;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.report.util.ReportUtils.getSingleDataSource;


@Component
public class CsvEntityTableRenderer implements CsvReportComponentRenderer<EntityTableComponent> {


    @Override
    public List<List<String>> render(EntityTableComponent component, ComponentData reportDataSource) {
        List<List<String>> content = new LinkedList<>();

        Optional<DataSource> dataSource = getSingleDataSource(component);
        if (dataSource.isEmpty()) {
            return Collections.emptyList(); //renderError(usablePageWidthPx, "Data source is not configured for time series table");
        }
        List<DataKey> dataKeys = dataSource.get().getDataKeys();

        Map<String, String> labelToDataKey = dataKeys.stream()
                .collect(Collectors.toMap(
                        DataKey::getLabel,
                        DataKey::getName,
                        (s, s2) -> s2,
                        LinkedHashMap::new));
        List<String> headers = labelToDataKey.keySet().stream().toList();
        content.add(headers);

        List<Map<String, String>> entityDatas = reportDataSource.getEntityDatas();

        for (Map<String, String> row : entityDatas) {
            List<String> values = labelToDataKey.values().stream()
                    .map(h -> Objects.toString(row.get(h), "")).toList();
            content.add(values);
        }
        return content;
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.ENTITY_TABLE;
    }

}
