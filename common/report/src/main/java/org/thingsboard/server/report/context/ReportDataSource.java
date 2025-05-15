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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ReportDataSource {

    private List<Map<String, ?>> entityDatas;
    private Map<String, Object> data;

    public ReportDataSource() {
        this.entityDatas = new ArrayList<>();
        this.data = new HashMap<>();
    }
    public ReportDataSource(List<Map<String, ?>> entityDatas) {
        this.entityDatas = entityDatas;
        this.data = new HashMap<>();
    }

    public ReportDataSource(Map<String, Object> data) {
        this.data = data;
        this.entityDatas = new ArrayList<>();
    }

    public Map<String, Object> getContextVariables() {
        HashMap<String, Object> contextVariables = new HashMap<>(data);
        if (!entityDatas.isEmpty()) {
            contextVariables.putAll(entityDatas.get(0));
        }
        return contextVariables;
    }

    public ReportDataSource merge(ReportDataSource otherDataSource) {
        this.entityDatas = mergeEntityDatas(this.entityDatas, otherDataSource.getEntityDatas());
        this.data = mergeVariables(this.data, otherDataSource.getData());
        return this;
    }

    private Map<String, Object> mergeVariables(Map<String, Object> variables, Map<String, Object> variables1) {
        for (String key : variables1.keySet()) {
            variables.put(key, variables1.get(key));
        }
        return variables;
    }

    private List<Map<String, ?>> mergeEntityDatas(List<Map<String, ?>> list1, List<Map<String, ?>> list2) {
        Map<Object, Map<String, Object>> mergedMap = new HashMap<>();

        for (Map<String, ?> map : list1) {
            Object id = map.get("id");
            if (id != null) {
                mergedMap.put(id, new HashMap<>(map));
            }
        }

        for (Map<String, ?> map : list2) {
            Object id = map.get("id");
            if (id != null) {
                mergedMap.compute(id, (key, existing) -> {
                    if (existing == null) {
                        return new HashMap<>(map);
                    } else {
                        existing.putAll(map);
                        return existing;
                    }
                });
            }
        }

        return new ArrayList<>(mergedMap.values());
    }
}
