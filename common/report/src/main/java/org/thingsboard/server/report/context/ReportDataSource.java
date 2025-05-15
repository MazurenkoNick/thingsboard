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
