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
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
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
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.common.data.report.configuration.timewindow.History;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.datasource.ReportDataService;
import org.apache.commons.collections4.CollectionUtils;

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
import static org.thingsboard.server.report.util.ReportQueryUtils.toAlarmCountQuery;
import static org.thingsboard.server.report.util.ReportQueryUtils.toAlarmDataQuery;
import static org.thingsboard.server.report.util.ReportQueryUtils.toEntityCountQuery;
import static org.thingsboard.server.report.util.ReportQueryUtils.toEntityDataQuery;
import static org.thingsboard.server.report.util.ReportUtils.getSingleDataSource;

@Slf4j
public abstract class AbstractReportService implements ReportService {

    @Lazy
    @Autowired
    protected ReportDataService dataService;

    protected ComponentData buildSingleComponentData(int usablePageWidthPx, TbReportCtx ctx, DataSource dataSource, EntityData stateEntity) {
        ReportTemplateConfig configuration = ctx.getConfiguration();
        return switch (dataSource.getType()) {
            case DEVICE, ENTITY -> new ComponentData(usablePageWidthPx, dataSource, collectEntityDatas(ctx, dataSource, stateEntity));
            case ENTITY_COUNT -> buildEntityCountDataSource(usablePageWidthPx, ctx, dataSource, configuration);
            case ALARM_COUNT -> buildAlarmCountDataSource(usablePageWidthPx, ctx, dataSource, configuration);
            default -> throw new IllegalArgumentException("Unknown data source type: " + dataSource.getType());
        };
    }

    protected ComponentData buildEntityCountDataSource(int usablePageWidthPx, TbReportCtx ctx, DataSource dataSource, ReportTemplateConfig configuration) {
        Map<String, Object> map = new HashMap<>();
        String label = resolveSingleLabel(dataSource, "count");
        map.put(label, dataService.countEntitiesByQuery(toEntityCountQuery(dataSource, configuration), ctx));
        return new ComponentData(usablePageWidthPx, map);
    }

    protected ComponentData buildAlarmCountDataSource(int usablePageWidthPx, TbReportCtx ctx, DataSource dataSource, ReportTemplateConfig configuration) {
        Map<String, Object> map = new HashMap<>();
        String label = resolveSingleLabel(dataSource, "count");
        map.put(label, dataService.countAlarmsByQuery(toAlarmCountQuery(dataSource, configuration), ctx));
        return new ComponentData(usablePageWidthPx, map);
    }

    private String resolveSingleLabel(DataSource dataSource, String fallback) {
        String label = null;
        if (dataSource.getDataKeys() != null && !dataSource.getDataKeys().isEmpty()) {
            label = dataSource.getDataKeys().get(0).getLabel();
        }
        if (StringUtils.isNotBlank(label)) {
            return label;
        }
        return fallback;
    }

    protected List<EntityData> fetchEntities(TbReportCtx ctx, DataSource dataSource, EntityData stateEntity) {
       return fetchEntityDataByQuery(pageLink -> toEntityDataQuery(dataSource, ctx.getConfiguration(), stateEntity, pageLink), dataSource, ctx);
    }

    private List<EntityData> fetchEntityDataByQuery(Function<PageLink, EntityDataQuery> querySupplier, DataSource dataSource, TbReportCtx ctx) {
        List<DataKey> dataKeysWithAggr = getDataKeysWithAggr(dataSource);
        List<EntityData> data = new ArrayList<>();
        for (EntityData entityData : new PageDataIterable<>(link -> dataService.findEntityDataByQuery(querySupplier.apply(link), ctx), 1024)) {
            updateWithAggregatedData(ctx, dataKeysWithAggr, entityData);
            data.add(entityData);
        }
        return data;
    }

    private List<DataKey> getDataKeysWithAggr(DataSource dataSource) {
        List<DataKey> dataKeys = dataSource.getDataKeys();
        if (dataKeys != null) {
            return dataKeys.stream()
                    .filter(dataKey -> (dataKey.getAggregationType() != null && dataKey.getAggregationType() != Aggregation.NONE))
                    .toList();
        }
        return Collections.emptyList();
    }

    private void updateWithAggregatedData(TbReportCtx ctx, List<DataKey> dataKeysWithAggregation, EntityData entityData) {
        if (!dataKeysWithAggregation.isEmpty()) {
            List<ReadTsKvQuery> queries = buildReadTsKvQueries(dataKeysWithAggregation);
            List<ReadTsKvQueryResult> result = dataService.findTimeseriesByQueries(entityData.getEntityId(), queries, ctx);
            for (ReadTsKvQueryResult queryResult : result) {
                List<TsKvEntry> queryResultData = queryResult.getData();
                if (CollectionUtils.isNotEmpty(queryResultData)) {
                    entityData.getTimeseries().put(queryResultData.get(0).getKey(), queryResult.toTsValues());
                }
            }
        }
    }

    private List<ReadTsKvQuery> buildReadTsKvQueries(List<DataKey> dataKeysWithAggregation) {
        List<ReadTsKvQuery> queries = new ArrayList<>();
        for (DataKey key : dataKeysWithAggregation) {
            TimeIntervalCalculator.TimeRange timeRange = getTimeRange(key.getTimewindow());
            var query = new BaseReadTsKvQuery(key.getName(), timeRange.startTs, timeRange.endTs, timeRange.endTs - timeRange.startTs, 1, key.getAggregationType());
            queries.add(query);
        }
        return queries;
    }

    protected List<Map<String, String>> collectEntityDatas(TbReportCtx ctx, DataSource dataSource, EntityData stateEntity) {
        return switch (dataSource.getType()) {
            case DEVICE, ENTITY -> fetchEntities(ctx, dataSource, stateEntity)
                    .stream()
                    .map(entityData -> toStringMap(entityData, dataSource.getDataKeys(), ctx))
                    .collect(Collectors.toList());
            default -> throw new IllegalArgumentException("Unknown data source type: " + dataSource.getType());
        };
    }


    protected ComponentData buildTsComponentData(int usablePageWidthPx, TbReportCtx ctx, TimeseriesTableComponent component, EntityData entity) {
        TimeWindowConfiguration timeWindowConf = component.getTimewindow();
        History historyConf = timeWindowConf.getHistory();
        TimeIntervalCalculator.TimeRange timeRange = getTimeRange(timeWindowConf);

        Optional<DataSource> singleDataSource = getSingleDataSource(component);
        if (singleDataSource.isEmpty()) {
            return new ComponentData(usablePageWidthPx);
        }
        List<DataKey> dataKeys = singleDataSource.get().getDataKeys();
        List<DataKey> latestDataKeys = singleDataSource.get().getLatestDataKeys();

        // todo: dasha make sort order configurable (?)
        List<String> keys = dataKeys.stream().map(DataKey::getName).collect(Collectors.toList());
        List<TsKvEntry> result = dataService.getTimeseries(entity.getEntityId(), keys, timeRange.startTs, timeRange.endTs,
                historyConf.getInterval(), timeWindowConf.getAggregation().getType(), SortOrder.Direction.DESC,
                timeWindowConf.getAggregation().getLimit(), false, ctx);
        SortOrder sortOrder = SortOrder.of("rawTs", SortOrder.Direction.DESC);
        List<Map<String, String>> entityDatas = collectTsData(dataKeys, latestDataKeys, entity, result, component, sortOrder, ctx);
        Map<String, Object> variables = new HashMap<>(toStringMap(entity, dataKeys, ctx));
        return new ComponentData(usablePageWidthPx, null, entityDatas, variables);
    }

    protected ComponentData buildAlarmComponentData(int usablePageWidthPx, TbReportCtx ctx, AlarmTableComponent component, EntityData stateEntity) {
        DataSource alarmSource = component.getAlarmSource();
        if (alarmSource == null) {
            return new ComponentData(usablePageWidthPx);
        }
        switch (alarmSource.getType()) {
            case DEVICE:
                if (alarmSource.getDeviceId() == null) {
                    return new ComponentData(usablePageWidthPx);
                }
                break;
            case ENTITY:
                 if (alarmSource.getEntityAliasId() == null) {
                     return new ComponentData(usablePageWidthPx);
                 }
                 break;
            default:
                return new ComponentData(usablePageWidthPx);
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
        List<Map<String, String>> entityDatas = new ArrayList<>();
        for (AlarmData alarmData : new PageDataIterable<>(link -> dataService.findAlarmDataByQueryForEntities(toAlarmDataQuery(component, ctx.getConfiguration(), stateEntity, link), entityDataMap.keySet(), ctx), 1024)) {
            Map<String, String> mergedData = toStringMap(alarmData, alarmDataKeys, ctx);
            EntityData entityData = entityDataMap.get(alarmData.getEntityId());
            mergedData.putAll(toStringMap(entityData, latestDataKeys, ctx));
            entityDatas.add(mergedData);
        }
        Map<String, Object> variables = new HashMap<>(toStringMap(stateEntity, latestDataKeys, ctx));
        return new ComponentData(usablePageWidthPx, null, entityDatas, variables);
    }

    protected Map<String, String> toStringMap(EntityData entityData, List<DataKey> dataKeys, TbReportCtx ctx) {
        HashMap<String, String> data = new HashMap<>();
        if (entityData != null) {
            putLatestValues(dataKeys, data, entityData.getLatest(), ctx);
            putTimeseriesValues(dataKeys, data, entityData.getTimeseries(), ctx);
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
                data.put(alarmKey.getLabel(), formatValue(ctx, alarmKey, 0, value));
            }
        }
        Optional<String> entityName = getAlarmLatestValue(alarmData, EntityKeyType.ENTITY_FIELD, "name");
        Optional<String> entityLabel = getAlarmLatestValue(alarmData, EntityKeyType.ENTITY_FIELD, "label");
        data.put("entityName", entityName.orElse(""));
        data.put("entityLabel", entityLabel.orElse(""));
        return data;
    }

    private void putLatestValues(List<DataKey> dataKeys, Map<String, String> data, Map<EntityKeyType, Map<String, TsValue>> latest, TbReportCtx ctx) {
        if (dataKeys == null) {
            return;
        }
        for (DataKey dataKey : dataKeys) {
            Map<String, TsValue> keyValueMap = latest.get(EntityKeyType.fromName(dataKey.getType()));
            if (keyValueMap != null) {
                TsValue tsValue = keyValueMap.get(dataKey.getName());
                if (tsValue != null && tsValue.getValue() != null) {
                    data.put(dataKey.getLabel(), formatValue(ctx, dataKey, tsValue.getTs(), tsValue.getValue()));
                }
            }
        }
    }

    private void putTimeseriesValues(List<DataKey> dataKeys, HashMap<String, String> data, Map<String, TsValue[]> timeseries, TbReportCtx ctx) {
        if (dataKeys == null) {
            return;
        }
        List<DataKey> dataKeysWithAggregation = dataKeys.stream()
                .filter(dataKey -> dataKey.getAggregationType() != null && dataKey.getAggregationType() != Aggregation.NONE)
                .toList();
        for (DataKey dataKey : dataKeysWithAggregation) {
            timeseries.computeIfPresent(dataKey.getName(), (s, tsValues) -> {
                for (TsValue tsValue : tsValues) {
                    data.put(dataKey.getLabel(), formatValue(ctx, dataKey, tsValue.getTs(), tsValue.getValue()));
                }
                return tsValues;
            });
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
                                                      List<TsKvEntry> tsKvEntries, TimeseriesTableComponent component,
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
            if (component.isShowTimestamp()) {
                tsValues.put(component.getTimestampLabel(), formatTimestamp(ts.toString(), component.getTimestampPattern(), ctx));
            }
            for (DataKey dataKey : dataKeys) {
                entries.stream().filter(tsKvEntry -> tsKvEntry.getKey().equals(dataKey.getName()))
                        .findFirst()
                        .ifPresentOrElse(tsKvEntry -> {
                                    String value = tsKvEntry.getValueAsString();
                                    if (value != null) {
                                        tsValues.put(dataKey.getLabel(), formatValue(ctx, dataKey, tsKvEntry.getTs(), tsKvEntry.getValue(), false));
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

    protected List<EntityData> getSubReportEntities(TbReportCtx ctx, Optional<DataSource> dataSource) {
        List<EntityData> entities;
        if (dataSource.isEmpty()) {
            entities = new ArrayList<>();
            entities.add(null);
        } else {
            entities = fetchEntities(ctx, dataSource.get(), null);
        }
        return entities;
    }

    protected void populateReportVars(ComponentData componentData, TbReportCtx ctx) {
        Map<String, Object> variables = componentData.getVariables();
        variables.put("reportCreatedTime", formatTimestamp(String.valueOf(System.currentTimeMillis()), ctx));
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
            if (timestampStr.equals("milliseconds")) {
                return String.valueOf(timestamp);
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeDataPattern)
                    .withZone(ctx.getTimeZone() != null ? ZoneId.of(ctx.getTimeZone()) : ZoneId.systemDefault());

            return formatter.format(Instant.ofEpochMilli(timestamp));
        } catch (NumberFormatException e) {
            return "Invalid timestamp: " + timestampStr;
        }
    }

    private String formatValue(TbReportCtx ctx, DataKey dataKey, long timestamp, Object value) {
        return formatValue(ctx, dataKey, timestamp, value, true);
    }

    private String formatValue(TbReportCtx ctx, DataKey dataKey, long timestamp, Object value, boolean parseString) {
        if (dataKey == null) {
            return value != null ? value.toString() : null;
        }

        Object processed = postProcess(ctx, dataKey, timestamp, value, parseString);
        if (processed == null) {
            return null;
        }

        return "createdTime".equals(dataKey.getName())
                ? formatTimestamp(processed.toString(), ctx)
                : processed.toString();
    }

    private Object postProcess(TbReportCtx ctx, DataKey dataKey, long timestamp, Object value, boolean parseString) {
        if (dataKey.isUsePostProcessing()) {
            Object input = parseString && value instanceof String ? parseStringValue((String) value) : value;
            UUID scriptId = ctx.getScripts().computeIfAbsent(dataKey.getPostFuncBody(), s -> evalScript(ctx, s));
            if (scriptId != null) {
                return evalData(ctx, timestamp, input, scriptId);
            }
        }
        return value;
    }

    public static Object parseStringValue(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        if (NumberUtils.isParsable(value)) {
            return Double.parseDouble(value);
        }
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        return value;
    }

    private Object evalData(TbReportCtx ctx, long timestamp, Object value, UUID scriptId) {
        try {
            return ctx.getTbelInvokeService().invokeScript(ctx.getTenantId(), null, scriptId, timestamp, value).get();
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
