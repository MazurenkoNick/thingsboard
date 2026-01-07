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
package org.thingsboard.server.report.context;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.report.context.chart.LatestChartData;
import org.thingsboard.server.report.context.chart.TsChartData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.thingsboard.server.report.util.ThymeleafUtil.normalizeVariableName;

@Data
@Slf4j
public class ComponentData {

    private final int usablePageWidthPx;
    private List<Map<String, String>> entityDatas;
    private LatestChartData latestChartData;
    private TsChartData tsChartData;
    private Map<String, Object> variables;
    private byte[] image;
    private String error;

    public ComponentData(int usablePageWidthPx) {
        this(usablePageWidthPx,null, new ArrayList<>(), new HashMap<>());
    }

    public ComponentData(int usablePageWidthPx, String error) {
        this(usablePageWidthPx);
        this.error = error;
    }

    public ComponentData(int usablePageWidthPx, byte[] image) {
        this(usablePageWidthPx);
        this.image = image;
    }

    public ComponentData(int usablePageWidthPx, LatestChartData latestChartData, Map<String, Object> variables) {
        this(usablePageWidthPx);
        this.latestChartData = latestChartData;
        this.variables = variables;
    }

    public ComponentData(int usablePageWidthPx, TsChartData tsChartData, Map<String, Object> variables) {
        this(usablePageWidthPx);
        this.tsChartData = tsChartData;
        this.variables = variables;
    }

    public ComponentData(int usablePageWidthPx, List<Map<String, String>> entityDatas) {
        this(usablePageWidthPx, null, entityDatas, new HashMap<>());
    }

    public ComponentData(int usablePageWidthPx, DataSource dataSource, List<Map<String, String>> entityDatas) {
        this(usablePageWidthPx, dataSource, entityDatas, new HashMap<>());
    }

    public ComponentData(int usablePageWidthPx, Map<String, Object> variables) {
        this(usablePageWidthPx, null, new ArrayList<>(), variables);
    }

    public ComponentData(int usablePageWidthPx, DataSource dataSource, List<Map<String, String>> entityDatas, Map<String, Object> variables) {
        this.usablePageWidthPx = usablePageWidthPx;
        this.entityDatas = entityDatas;
        this.variables = variables;

        if (dataSource != null) {
            injectVariablesFromEntityDatas(dataSource);
        }
        variables.put("rowCount", this.entityDatas.size());
    }

    private void injectVariablesFromEntityDatas(DataSource dataSource) {
        Map<String, DataKey> labelToDataKey = Stream.concat(
                        Optional.ofNullable(dataSource.getDataKeys()).orElse(Collections.emptyList()).stream(),
                        Optional.ofNullable(dataSource.getLatestDataKeys()).orElse(Collections.emptyList()).stream())
                .collect(Collectors.toMap(
                        dataKey -> normalizeVariableName(dataKey.getLabel()),
                        Function.identity(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        for (Map.Entry<String, DataKey> entry : labelToDataKey.entrySet()) {
            for (Map<String, String> entityData : this.entityDatas) {
                String label = entry.getKey();
                DataKey dataKey = entry.getValue();
                this.variables.put(label, entityData.getOrDefault(dataKey.getLabel(), ""));
            }
        }
    }

    public ComponentData merge(ComponentData other) {
        this.entityDatas = mergeEntityDatas(this.entityDatas, other.getEntityDatas());
        this.variables.putAll(other.getVariables());
        return this;
    }

    private List<Map<String, String>> mergeEntityDatas(List<Map<String, String>> dataList, List<Map<String, String>> otherDataList) {
        Map<String, Map<String, String>> merged = new LinkedHashMap<>();

        Stream.of(dataList, otherDataList)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .forEach(map -> {
                    String id = map.get("id");
                    if (id != null) {
                        merged.merge(id, new HashMap<>(map), (existing, incoming) -> {
                            existing.putAll(incoming);
                            return existing;
                        });
                    }
                });

        return new ArrayList<>(merged.values());
    }

}
