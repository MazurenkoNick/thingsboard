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
package org.thingsboard.server.report.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.EntityTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.common.data.report.configuration.timewindow.History;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.datasource.ReportDataService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator.getTimeRange;
import static org.thingsboard.server.common.data.util.DataSourceUtils.getAlarmLatestValue;
import static org.thingsboard.server.common.data.util.DataSourceUtils.getEntityLatestValue;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toAlarmCountQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toAlarmDataQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toEntityCountQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toEntityDataQuery;
import static org.thingsboard.server.report.util.ReportUtils.getSingleDataSource;

@Slf4j
public abstract class AbstractReportService implements ReportService {

    @Lazy
    @Autowired
    protected ReportDataService dataService;

    protected ComponentData buildSingleComponentData(int usablePageWidthPx, TbReportCtx ctx, DataSource dataSource, EntityData stateEntity) {
        ReportTemplateConfig configuration = ctx.getConfiguration();
        return switch (dataSource.getType()) {
            case "device", "entity" -> new ComponentData(usablePageWidthPx, dataSource, collectEntityDatas(ctx, dataSource, stateEntity));
            case "entityCount" -> buildEntityCountDataSource(usablePageWidthPx, ctx, dataSource, configuration);
            case "alarmCount" -> buildAlarmCountDataSource(usablePageWidthPx, ctx, dataSource, configuration);
            default -> throw new IllegalArgumentException("Unknown data source type: " + dataSource.getType());
        };
    }

    protected ComponentData buildEntityCountDataSource(int usablePageWidthPx, TbReportCtx ctx, DataSource dataSource, ReportTemplateConfig configuration) {
        Map<String, Object> map = new HashMap<>();
        String label = null;
        if (dataSource.getDataKeys() != null && !dataSource.getDataKeys().isEmpty()) {
            label = dataSource.getDataKeys().get(0).getLabel();
        }
        if (StringUtils.isBlank(label)) {
            label = "count";
        }
        map.put(label, dataService.countEntitiesByQuery(toEntityCountQuery(dataSource, configuration), ctx));
        return new ComponentData(usablePageWidthPx, map);
    }

    protected ComponentData buildAlarmCountDataSource(int usablePageWidthPx, TbReportCtx ctx, DataSource dataSource, ReportTemplateConfig configuration) {
        Map<String, Object> map = new HashMap<>();
        String label = null;
        if (dataSource.getDataKeys() != null && !dataSource.getDataKeys().isEmpty()) {
            label = dataSource.getDataKeys().get(0).getLabel();
        }
        if (StringUtils.isBlank(label)) {
            label = "count";
        }
        map.put(label, dataService.countAlarmsByQuery(toAlarmCountQuery(dataSource, configuration), ctx));
        return new ComponentData(usablePageWidthPx, map);
    }

    protected List<EntityData> fetchEntities(TbReportCtx ctx, DataSource dataSource, EntityData stateEntity) {
       return fetchEntityDataByQuery(pageLink -> toEntityDataQuery(dataSource, ctx.getConfiguration(), stateEntity, pageLink), ctx);
    }

    protected List<EntityData> fetchEntityDataByQuery(Function<PageLink, EntityDataQuery> querySupplier, TbReportCtx ctx) {
        List<EntityData> data = new ArrayList<>();
        for (EntityData entityData : new PageDataIterable<>(link -> dataService.findEntityDataByQuery(querySupplier.apply(link), ctx), 1024)) {
            data.add(entityData);
        }
        return data;
    }

    protected List<Map<String, String>> collectEntityDatas(TbReportCtx ctx, DataSource dataSource, EntityData stateEntity) {
        return switch (dataSource.getType()) {
            case "device", "entity" -> fetchEntities(ctx, dataSource, stateEntity)
                    .stream()
                    .map(entityData -> toStringMap(entityData, dataSource.getDataKeys(), ctx))
                    .collect(Collectors.toList());
            default -> throw new IllegalArgumentException("Unknown data source type: " + dataSource.getType());
        };
    }

    protected List<Map<String, String>> fetchEntityTableData(TbReportCtx ctx, EntityTableComponent component, EntityData stateEntity) {
        Optional<DataSource> singleDataSource = getSingleDataSource(component);
        if (singleDataSource.isEmpty()) {
            return Collections.emptyList();
        }
        return collectEntityDatas(ctx, singleDataSource.get(), stateEntity);
    }

    protected List<Map<String, String>> fetchEntityTsData(TbReportCtx ctx, TimeseriesTableComponent component, EntityData entity) {
        TimeWindowConfiguration timeWindowConf = component.getTimewindow();
        History historyConf = timeWindowConf.getHistory();
        TimeIntervalCalculator.TimeRange timeRange = getTimeRange(timeWindowConf);

        Optional<DataSource> singleDataSource = getSingleDataSource(component);
        if (singleDataSource.isEmpty()) {
            return Collections.emptyList();
        }
        List<DataKey> dataKeys = singleDataSource.get().getDataKeys();
        List<DataKey> latestDataKeys = singleDataSource.get().getLatestDataKeys();

        // todo: dasha make sort order configurable (?)
        List<String> keys = dataKeys.stream().map(DataKey::getName).collect(Collectors.toList());
        List<TsKvEntry> result = dataService.getTimeseries(entity.getEntityId(), keys, timeRange.startTs, timeRange.endTs,
                historyConf.getInterval(), timeWindowConf.getAggregation().getType(), SortOrder.Direction.DESC,
                timeWindowConf.getAggregation().getLimit(), false, ctx);
        SortOrder sortOrder = SortOrder.of("rawTs", SortOrder.Direction.DESC);
        return collectTsData(dataKeys, latestDataKeys, entity, result, component.isShowTimestamp(), component.getTimestampPattern(), sortOrder, ctx);
    }

    protected List<Map<String, String>> fetchAlarmDatas(TbReportCtx ctx, AlarmTableComponent component, EntityData stateEntity) {
        DataSource alarmSource = component.getAlarmSource();
        if (alarmSource == null) {
            return Collections.emptyList();
        }
        switch (alarmSource.getType()) {
            case "device":
                if (alarmSource.getDeviceId() == null) {
                    return Collections.emptyList();
                }
                break;
             case "entity":
                 if (alarmSource.getEntityAliasId() == null) {
                     return Collections.emptyList();
                 }
                 break;
            default:
                return Collections.emptyList();
        }
        List<DataKey> alarmDataKeys = alarmSource.getDataKeys()
                .stream()
                .filter(dataKey -> dataKey.getType().equals("alarm"))
                .collect(Collectors.toList());
        List<DataKey> latestDataKeys = alarmSource.getDataKeys()
                .stream()
                .filter(dataKey -> !dataKey.getType().equals("alarm"))
                .collect(Collectors.toList());
        List<EntityData> entityDataList = fetchEntities(ctx, alarmSource, stateEntity);
        Map<EntityId, EntityData> entityDataMap = entityDataList.stream()
                .collect(Collectors.toMap(EntityData::getEntityId, Function.identity()));
        List<Map<String, String>> data = new ArrayList<>();
        for (AlarmData alarmData : new PageDataIterable<>(link -> dataService.findAlarmDataByQueryForEntities(toAlarmDataQuery(component, ctx.getConfiguration(), stateEntity, link), entityDataMap.keySet(), ctx), 1024)) {
            Map<String, String> mergedData = toStringMap(alarmData, alarmDataKeys, ctx);
            EntityData entityData = entityDataMap.get(alarmData.getEntityId());
            mergedData.putAll(toStringMap(entityData, latestDataKeys, ctx));
            data.add(mergedData);
        }
        return data;
    }

    protected Map<String, String> toStringMap(EntityData entityData, List<DataKey> dataKeys, TbReportCtx ctx) {
        HashMap<String, String> data = new HashMap<>();
        if (entityData != null) {
            putLatestValues(dataKeys, data, entityData.getLatest(), ctx);
            data.put("id", entityData.getEntityId().toString());
            Optional<String> entityName = getEntityLatestValue(entityData, EntityKeyType.ENTITY_FIELD, "name");
            Optional<String> entityLabel = getEntityLatestValue(entityData, EntityKeyType.ENTITY_FIELD, "label");
            data.put("entityName", entityName.orElse(""));
            data.put("entityLabel", entityLabel.orElse(""));
        }
        return data;
    }

    protected Map<String, String> toStringMap(AlarmData alarmData, List<DataKey> alarmDataKeys, TbReportCtx ctx) {
        Map<String, String> data = new HashMap<>();
        JsonNode alarmDataJson = JacksonUtil.valueToTree(alarmData);

        for (DataKey alarmKey : alarmDataKeys) {
            String targetKey = alarmKey.getName();
            String value = null;
            if (targetKey.equals("assignee")) {
                value = getAssigneeDisplayName(alarmData);
            } else {
                if (targetKey.equals("originator")) {
                    targetKey = "originatorName";
                }
                JsonNode jsonValue = JacksonUtil.getByKeyPath(alarmDataJson, targetKey);
                if (jsonValue != null) {
                    value = jsonValue.asText();
                }
            }
            if (value != null) {
                data.put(alarmKey.getLabel(), formatData(ctx, alarmKey, 0, value));
            }
        }
        Optional<String> entityName = getAlarmLatestValue(alarmData, EntityKeyType.ENTITY_FIELD, "name");
        Optional<String> entityLabel = getAlarmLatestValue(alarmData, EntityKeyType.ENTITY_FIELD, "label");
        data.put("entityName", entityName.orElse(""));
        data.put("entityLabel", entityLabel.orElse(""));
        return data;
    }

    private void putLatestValues(List<DataKey> dataKeys, Map<String, String> data, Map<EntityKeyType, Map<String, TsValue>> latest, TbReportCtx ctx) {
        for (DataKey dataKey : dataKeys) {
            Map<String, TsValue> keyValueMap = latest.get(EntityKeyType.fromName(dataKey.getType()));
            if (keyValueMap != null) {
                TsValue tsValue = keyValueMap.get(dataKey.getName());
                if (tsValue != null && tsValue.getValue() != null) {
                    data.put(dataKey.getLabel(), formatData(ctx, dataKey, tsValue.getTs(), tsValue.getValue()));
                }
            }
        }
    }

    private String getAssigneeDisplayName(AlarmData alarmData) {
        if (alarmData.getAssignee() != null) {
            return alarmData.getAssignee().getTitle();
        } else if (alarmData.getAssigneeId() != null) {
            return "User deleted";
        } else {
            return "Unassigned";
        }
    }

    protected List<Map<String, String>> collectTsData(List<DataKey> dataKeys, List<DataKey> latestDataKeys, EntityData entity,
                                                      List<TsKvEntry> tsKvEntries, boolean showTs, String tsPattern,
                                                      SortOrder sortOrder, TbReportCtx ctx) {
        Optional<String> entityName = getEntityLatestValue(entity, EntityKeyType.ENTITY_FIELD, "name");
        Optional<String> entityLabel = getEntityLatestValue(entity, EntityKeyType.ENTITY_FIELD, "label");
        List<Map<String, String>> tsData = new ArrayList<>();

        Comparator<Long> tsComparator = sortOrder.getDirection() == SortOrder.Direction.ASC
                ? Comparator.naturalOrder()
                : Comparator.reverseOrder();

        Map<Long, List<TsKvEntry>> groupedByTs = tsKvEntries.stream()
                .collect(Collectors.groupingBy(
                        TsKvEntry::getTs,
                        () -> new TreeMap<>(tsComparator),
                        Collectors.toList()
                ));

        groupedByTs.forEach((ts, entries) -> {
            Map<String, String> tsValues = new HashMap<>();
            tsValues.put("rawTs", ts.toString());
            if (showTs) {
                tsValues.put("Timestamp", formatTimestamp(ts.toString(), tsPattern, ctx));
            }
            for (DataKey dataKey : dataKeys) {
                entries.stream().filter(tsKvEntry -> tsKvEntry.getKey().equals(dataKey.getName()))
                        .findFirst()
                        .ifPresentOrElse(tsKvEntry -> {
                                    String value = tsKvEntry.getValueAsString();
                                    if (value != null) {
                                        tsValues.put(dataKey.getLabel(), formatData(ctx, dataKey, tsKvEntry.getTs(), value));
                                    }
                                },
                                () -> tsValues.putIfAbsent(dataKey.getLabel(), null));
            }
            putLatestValues(latestDataKeys, tsValues, entity.getLatest(), ctx);
            tsValues.put("entityName", entityName.orElse(""));
            tsValues.put("entityLabel", entityLabel.orElse(""));
            tsData.add(tsValues);
        });
        return tsData;
    }

    protected String formatData(TbReportCtx ctx, DataKey dataKey, long timestamp, String value) {
        if (dataKey != null) {
            String processedData = postProcessData(ctx, dataKey, timestamp, value);
            return "createdTime".equals(dataKey.getName()) ? formatTimestamp(processedData, ctx) : processedData;
        }
        return value;
    }

    protected String formatTimestamp(String timestampStr, TbReportCtx ctx) {
        return formatTimestamp(timestampStr, null, ctx);
    }

    protected String formatTimestamp(String timestampStr, String timeDataPattern, TbReportCtx ctx) {
        if (timeDataPattern == null || timeDataPattern.isEmpty()) {
            timeDataPattern = ctx.getConfiguration().getTimeDataPattern();
        }
        if (timeDataPattern == null || timeDataPattern.isEmpty()) {
            return timestampStr;
        }
        try {
            long timestamp = Long.parseLong(timestampStr);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeDataPattern)
                    .withZone(ctx.getTimeZone() != null ? ZoneId.of(ctx.getTimeZone()) : ZoneId.systemDefault());

            return formatter.format(Instant.ofEpochMilli(timestamp));
        } catch (NumberFormatException e) {
            return "Invalid timestamp: " + timestampStr;
        }
    }

    private String postProcessData(TbReportCtx ctx, DataKey dataKey, long timestamp, String value) {
        if (dataKey != null && dataKey.isUsePostProcessing()) {
            UUID scriptId = ctx.getScripts().computeIfAbsent(dataKey.getPostFuncBody(), s -> evalScript(ctx, s));
            if (scriptId != null) {
                value = evalData(ctx, timestamp, value, scriptId);
            }
        }
        return value;
    }

    private String evalData(TbReportCtx ctx, long timestamp, String value, UUID scriptId) {
        try {
            return ctx.getTbelInvokeService().invokeScript(ctx.getTenantId(), null, scriptId, timestamp, value).get().toString();
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to evaluate data: " + value, e);
        } catch (ExecutionException e) {
            log.error("Failed to evaluate data {}", value, e);
            return value;
        }
    }

    private UUID evalScript(TbReportCtx ctx, String script)  {
        try {
            return ctx.getTbelInvokeService().eval(ctx.getTenantId(), ScriptType.REPORT_DATA_KEY_SCRIPT, script, "time", "value").get();
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to compile script: " + script, e);
        } catch (ExecutionException e) {
            log.error("Failed to compile script {} ", script, e);
            return null;
        }
    }
}
