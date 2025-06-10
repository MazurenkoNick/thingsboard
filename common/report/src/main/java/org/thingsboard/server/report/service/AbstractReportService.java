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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntitySearchQueryFilter;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.query.StateEntityFilter;
import org.thingsboard.server.common.data.query.StateEntityOwnerFilter;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.common.data.report.configuration.timewindow.History;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.datasource.ReportDataService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator.getTimeRange;
import static org.thingsboard.server.common.data.util.DataSourceUtils.getAlarmLatestValue;
import static org.thingsboard.server.common.data.util.DataSourceUtils.getEntityLatestValue;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.buildEntityDataQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.findEntityFilterByAliasId;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.findKeyFilters;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.singleDeviceFilter;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toAlarmDataQuery;
import static org.thingsboard.server.common.data.util.DataSourceUtils.setDataKeyIfNotExists;
import static org.thingsboard.server.report.util.ReportUtils.getSingleDataSource;

public abstract class AbstractReportService implements ReportService {

    @Lazy
    @Autowired
    protected ReportDataService dataService;

    protected List<EntityData> fetchEntities(TbReportCtx ctx, DataSource dataSource, EntityData stateEntity) {
        return switch (dataSource.getType()) {
            case "device" -> fetchDevice(ctx, dataSource);
            case "entity" -> fetchEntitiesByDataSource(ctx, dataSource, stateEntity);
            default -> throw new IllegalArgumentException("Unknown data source type: " + dataSource.getType());
        };
    }

    private List<EntityData> fetchEntitiesByDataSource(TbReportCtx ctx, DataSource dataSource, EntityData stateEntity) {
        if (dataSource.getEntityAliasId() == null) {
            return Collections.emptyList();
        } else {
            EntityFilter entityFilter = findEntityFilterByAliasId(dataSource, ctx.getConfiguration());
            List<KeyFilter> keyFilters = findKeyFilters(dataSource, ctx.getConfiguration());

            if (entityFilter instanceof StateEntityFilter stateFilter) {
                return fetchStateEntity(stateFilter, dataSource, keyFilters, stateEntity, ctx);
            }
            if (entityFilter instanceof StateEntityOwnerFilter ownerFilter) {
                return fetchStateEntityOwner(ownerFilter, dataSource, keyFilters, stateEntity, ctx);
            }
            if (entityFilter instanceof EntitySearchQueryFilter searchFilter && searchFilter.isRootStateEntity()) {
                return fetchSearchQueryEntities(searchFilter, dataSource, keyFilters, stateEntity, ctx);
            }
            return fetchEntityDataByQuery(pageLink -> buildEntityDataQuery(entityFilter, dataSource, keyFilters, pageLink), ctx);
        }
    }

    private List<EntityData> fetchSearchQueryEntities(EntitySearchQueryFilter entityFilter, DataSource dataSource, List<KeyFilter> keyFilters, EntityData stateEntity, TbReportCtx ctx) {
        EntityId entityId = (stateEntity != null)
                ? stateEntity.getEntityId()
                : entityFilter.getDefaultStateEntity();
        if (entityId == null) {
            return Collections.emptyList();
        }
        entityFilter.setRootEntity(entityId);
        return fetchEntityDataByQuery(pageLink -> buildEntityDataQuery(entityFilter, dataSource, keyFilters, pageLink), ctx);
    }

    private List<EntityData> fetchStateEntity(StateEntityFilter stateEntityFilter, DataSource dataSource, List<KeyFilter> keyFilters, EntityData stateEntity, TbReportCtx ctx) {
        EntityId entityId = (stateEntity != null)
                ? stateEntity.getEntityId()
                : stateEntityFilter.getDefaultStateEntity();

        if (entityId == null) {
            return Collections.emptyList();
        }
        SingleEntityFilter singleEntityFilter = new SingleEntityFilter();
        singleEntityFilter.setSingleEntity(entityId);
        return fetchEntityDataByQuery(pageLink -> buildEntityDataQuery(singleEntityFilter, dataSource, keyFilters, pageLink), ctx);
    }

    private List<EntityData> fetchStateEntityOwner(StateEntityOwnerFilter ownerFilter, DataSource dataSource, List<KeyFilter> keyFilters, EntityData stateEntity, TbReportCtx ctx) {
        EntityId entityId = (stateEntity != null)
                ? stateEntity.getEntityId()
                : ownerFilter.getDefaultStateEntity();

        if (entityId == null) {
            return Collections.emptyList();
        }
        StateEntityOwnerFilter stateEntityOwnerFilter = new StateEntityOwnerFilter();
        stateEntityOwnerFilter.setSingleEntity(entityId);
        return fetchEntityDataByQuery(pageLink -> buildEntityDataQuery(stateEntityOwnerFilter, dataSource, keyFilters, pageLink), ctx);
    }

    private List<EntityData> fetchDevice(TbReportCtx ctx, DataSource dataSource) {
        ReportTemplateConfig configuration = ctx.getConfiguration();
        if (dataSource.getDeviceId() == null) {
            return Collections.emptyList();
        } else {
            EntityFilter entityFilter = singleDeviceFilter(dataSource);
            List<KeyFilter> keyFilters = findKeyFilters(dataSource, configuration);
            return fetchEntityDataByQuery(pageLink -> buildEntityDataQuery(entityFilter, dataSource, keyFilters, pageLink), ctx);
        }
    }

    protected List<EntityData> fetchEntityDataByQuery(Function<PageLink, EntityDataQuery> querySupplier, TbReportCtx ctx) {
        List<EntityData> data = new ArrayList<>();
        for (EntityData entityData : new PageDataIterable<>(link -> dataService.findEntityDataByQuery(querySupplier.apply(link), ctx), 1024)) {
            data.add(entityData);
        }
        return data;
    }

    protected List<Map<String, String>> fetchEntityDatas(TbReportCtx ctx, DataSource dataSource, EntityData stateEntity) {
        return switch (dataSource.getType()) {
            case "device", "entity" -> fetchEntities(ctx, dataSource, stateEntity).stream().map(entityData -> toStringMap(entityData, ctx)).collect(Collectors.toList());
            default -> throw new IllegalArgumentException("Unknown data source type: " + dataSource.getType());
        };
    }

    protected List<Map<String, String>> fetchEntityTsData(TbReportCtx ctx, TimeseriesTableComponent component, EntityData entity) {
        TimeWindowConfiguration timeWindowConf = component.getTimewindow();
        History historyConf = timeWindowConf.getHistory();
        TimeIntervalCalculator.TimeRange timeRange = getTimeRange(timeWindowConf);

        Optional<DataSource> singleDataSource = getSingleDataSource(component);
        if (singleDataSource.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> keys = singleDataSource.get().getDataKeys().stream()
                .map(DataKey::getName)
                .toList();

        // todo: dasha make sort order configurable (?)
        List<TsKvEntry> result = dataService.getTimeseries(entity.getEntityId(), keys, timeRange.startTs, timeRange.endTs,
                historyConf.getInterval(), timeWindowConf.getAggregation().getType(), SortOrder.Direction.DESC,
                timeWindowConf.getAggregation().getLimit(), false, ctx);
        SortOrder sortOrder = SortOrder.of("rawTs", SortOrder.Direction.DESC);
        return collectTsData(keys, entity, result, component.isShowTimestamp(), component.getTimestampPattern(), sortOrder, ctx);
    }

    protected List<Map<String, String>> fetchAlarmDatas(TbReportCtx ctx, AlarmTableComponent component) {
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
        List<String> alarmKeys = alarmSource.getDataKeys().stream().filter(dataKey -> dataKey.getType().equals("alarm")).map(DataKey::getName).toList();
        List<Map<String, String>> data = new ArrayList<>();
        for (AlarmData alarmData : new PageDataIterable<>(link -> dataService.findAlarmDataByQuery(toAlarmDataQuery(component, ctx.getConfiguration(), link), ctx), 1024)) {
            data.add(toStringMap(alarmData, alarmKeys, ctx));
        }
        return data;
    }

    protected Map<String, String> toStringMap(EntityData entityData, TbReportCtx ctx) {
        HashMap<String, String> data = new HashMap<>();
        if (entityData != null) {
            entityData.getLatest().forEach((keyType, keyValueMap) -> keyValueMap.forEach((key, tsValue) -> {
                if (tsValue.getValue() != null) {
                    data.put(key, formatData(ctx, key, tsValue.getValue()));
                }
            }));
            data.put("id", entityData.getEntityId().toString());
            Optional<String> entityName = getEntityLatestValue(entityData, EntityKeyType.ENTITY_FIELD, "name");
            Optional<String> entityLabel = getEntityLatestValue(entityData, EntityKeyType.ENTITY_FIELD, "label");
            data.put("entityName", entityName.orElse(""));
            data.put("entityLabel", entityLabel.orElse(""));
        }
        return data;
    }

    protected Map<String, String> toStringMap(AlarmData alarmData, List<String> alarmKeys, TbReportCtx ctx) {
        Map<String, String> data = new HashMap<>();
        JsonNode alarmDataJson = JacksonUtil.valueToTree(alarmData);
        alarmKeys.forEach(key -> {
            String value = null;
            if (key.equals("assignee")) {
                value = getAssigneeDisplayName(alarmData);
            } else {
                String targetKey = key;
                if (key.equals("originator")) {
                    targetKey = "originatorName";
                }
                JsonNode jsonValue = JacksonUtil.getByKeyPath(alarmDataJson, targetKey);
                if (jsonValue != null) {
                    value = jsonValue.asText();
                }
            }
            if (value != null) {
                data.put(key, formatData(ctx, key, value));
            }
        });
        alarmData.getLatest().forEach((keyType, keyValueMap) -> keyValueMap.forEach((key, tsValue) -> {
            if (tsValue.getValue() != null) {
                data.put(key, formatData(ctx, key, tsValue.getValue()));
            }
        }));
        Optional<String> entityName = getAlarmLatestValue(alarmData, EntityKeyType.ENTITY_FIELD, "name");
        Optional<String> entityLabel = getAlarmLatestValue(alarmData, EntityKeyType.ENTITY_FIELD, "label");
        data.put("entityName", entityName.orElse(""));
        data.put("entityLabel", entityLabel.orElse(""));
        return data;
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

    protected List<Map<String, String>> collectTsData(List<String> keys, EntityData entity,
                                                      List<TsKvEntry> tsKvEntries, boolean showTs, String tsPattern,
                                                      SortOrder sortOrder, TbReportCtx ctx) {
        Optional<String> entityName = getEntityLatestValue(entity, EntityKeyType.ENTITY_FIELD, "name");
        Optional<String> entityLabel = getEntityLatestValue(entity, EntityKeyType.ENTITY_FIELD, "label");
        List<Map<String, String>> tsData = new ArrayList<>();
        Map<Long, List<TsKvEntry>> groupedByTs = tsKvEntries.stream().collect(Collectors.groupingBy(TsKvEntry::getTs));
        groupedByTs.forEach((ts, entries) -> {
            Map<String, String> tsValues = new HashMap<>();
            tsValues.put("rawTs", ts.toString());
            if (showTs) {
                tsValues.put("ts", formatTimestamp(ts.toString(), tsPattern, ctx));
            }
            for (TsKvEntry entry : entries) {
                tsValues.put(entry.getKey(), formatData(ctx, entry.getKey(), entry.getValueAsString()));
            }
            keys.forEach(key -> tsValues.putIfAbsent(key, null));
            entity.getLatest().forEach((keyType, keyValueMap) -> keyValueMap.forEach((key, tsValue) -> {
                if (tsValue.getValue() != null) {
                    tsValues.put(key, formatData(ctx, key, tsValue.getValue()));
                }
            }));
            tsValues.put("entityName", entityName.orElse(""));
            tsValues.put("entityLabel", entityLabel.orElse(""));
            tsData.add(tsValues);
        });
        tsData.sort((row1, row2) -> {
            String p1 = row1.get(sortOrder.getProperty());
            if (p1 == null) {
                p1 = "";
            }
            String p2 = row2.get(sortOrder.getProperty());
            if (p2 == null) {
                p2 = "";
            }
            return p1.compareTo(p2) * (sortOrder.getDirection() == SortOrder.Direction.ASC ? 1 : -1 );
        });
        return tsData;
    }

    protected String formatData(TbReportCtx ctx, String key, String value) {
        return key.equals("createdTime") ? formatTimestamp(value, ctx) : value;
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
}
