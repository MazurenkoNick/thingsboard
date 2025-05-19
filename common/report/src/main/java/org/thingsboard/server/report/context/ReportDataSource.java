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
package org.thingsboard.server.report.context;

import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class ReportDataSource {

    private List<Map<String, String>> entityDatas;
    private Map<String, String> variables;
    private byte[] image;

    public ReportDataSource() {
        this.entityDatas = new ArrayList<>();
        this.variables = new HashMap<>();
    }

    public ReportDataSource(byte[] image) {
        this.image = image;
    }

    public ReportDataSource(List<Map<String, String>> entityDatas) {
        this.entityDatas = entityDatas;
        this.variables = new HashMap<>();
    }

    public ReportDataSource(List<Map<String, String>> entityDatas, Map<String, String> variables) {
        this.entityDatas = entityDatas;
        this.variables = variables;
    }

    public ReportDataSource(Map<String, String> variables) {
        this.variables = variables;
        this.entityDatas = new ArrayList<>();
    }

    public HashMap<String, Object> buildContextVariables(ReportComponent component) {
        Map<String, String> labelKeyMap = component.getDataSources()
                .stream()
                .map(DataSource::getDataKeys)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(DataKey::getLabel, DataKey::getName));

        HashMap<String, Object> contextVariables = new HashMap<>(variables);
        if (!entityDatas.isEmpty()) {
            Map<String, String> entityData = entityDatas.get(0);
            for (String label : labelKeyMap.keySet()) {
                contextVariables.put(label.trim().replaceAll("\\s+", "_"), entityData.get(labelKeyMap.get(label)));
            }
        }
        return contextVariables;
    }

    public ReportDataSource merge(ReportDataSource otherDataSource) {
        this.entityDatas = mergeEntityDatas(this.entityDatas, otherDataSource.getEntityDatas());
        this.variables = mergeVariables(this.variables, otherDataSource.getVariables());
        return this;
    }

    private Map<String, String> mergeVariables(Map<String, String> variables, Map<String, String> otherVariables) {
        for (String key : otherVariables.keySet()) {
            variables.put(key, otherVariables.get(key));
        }
        return variables;
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
