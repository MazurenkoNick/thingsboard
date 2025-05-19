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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
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
import org.thymeleaf.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator.getTimeRange;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toAlarmDataQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toEntityDataQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toSingleEntityQuery;
import static org.thingsboard.server.report.service.PdfReportService.getSingleDataSource;
import static org.thingsboard.server.report.util.ReportUtils.formatTimestamp;

public abstract class AbstractReportService implements ReportService {

    @Autowired
    protected ReportDataService dataService;

    protected List<EntityData> fetchEntities(TbReportCtx ctx, DataSource dataSource) {
        ReportTemplateConfig configuration = ctx.getConfiguration();
        return switch (dataSource.getType()) {
            case "device" -> fetchEntityDataByQuery(pageLink -> toSingleEntityQuery(dataSource, configuration, pageLink), ctx);
            case "entity" -> fetchEntityDataByQuery(pageLink -> toEntityDataQuery(dataSource, configuration, pageLink), ctx);
            default -> throw new IllegalArgumentException("Unknown data source type: " + dataSource.getType());
        };
    }

    protected List<EntityData> fetchEntityDataByQuery(Function<PageLink, EntityDataQuery> querySupplier, TbReportCtx ctx) {
        List<EntityData> data = new ArrayList<>();
        for (EntityData entityData : new PageDataIterable<>(link -> dataService.findEntityDataByQuery(querySupplier.apply(link), ctx), 1024)) {
            data.add(entityData);
        }
        return data;
    }

    protected List<Map<String, String>> buildTsDataSource(TbReportCtx ctx, TimeseriesTableComponent component) {
        String deviceId = component.getDataSources().get(0).getDeviceId();
        DeviceId entityId = DeviceId.fromString(deviceId);

        return buildTsDataSource(ctx, component, entityId);
    }

    protected List<Map<String, String>> buildTsDataSource(TbReportCtx ctx, TimeseriesTableComponent component, EntityId entityId) {
        TimeWindowConfiguration timeWindowConf = component.getTimewindow();
        History historyConf = timeWindowConf.getHistory();
        TimeIntervalCalculator.TimeRange timeRange = getTimeRange(timeWindowConf);

        List<String> keys = getSingleDataSource(component).getDataKeys().stream()
                .map(DataKey::getName)
                .toList();

        // todo dasha make sort order configurable (?)
        List<TsKvEntry> result = dataService.getTimeseries(entityId, keys, timeRange.startTs, timeRange.endTs,
                historyConf.getInterval(), timeWindowConf.getAggregation().getType(), SortOrder.Direction.DESC,
                timeWindowConf.getAggregation().getLimit(), false, ctx);
        return collectTsData(result);
    }

    protected List<Map<String, String>> buildAlarmDataSource(TbReportCtx ctx, AlarmTableComponent component) {
        List<String> keyList = component.getAlarmSource().getDataKeys().stream().map(DataKey::getName).toList();
        List<Map<String, String>> data = new ArrayList<>();
        for (AlarmData alarmData : new PageDataIterable<>(link -> dataService.findAlarmDataByQuery(toAlarmDataQuery(component, ctx.getConfiguration(), link), ctx), 1024)) {
            data.add(toMapData(alarmData, keyList));
        }
        return data;
    }

    protected Map<String, String> toMap(EntityData entityData) {
        HashMap<String, String> latestValues = new HashMap<>();
        if (entityData != null) {
            entityData.getLatest().forEach((keyType, keyValueMap) -> keyValueMap.forEach((key, tsValue) -> {
                if (tsValue.getValue() != null) {
                    latestValues.put(key, key.equals("createdTime") ? formatTimestamp(tsValue.getValue()): tsValue.getValue());
                }
            }));
            latestValues.put("id", entityData.getEntityId().toString());
        }
        return latestValues;
    }

    protected Map<String, String> toStateEntityMap(EntityData entityData) {
        HashMap<String, String> latestValues = new HashMap<>();
        if (entityData != null) {
            entityData.getLatest().forEach((keyType, keyValueMap) -> keyValueMap.forEach((key, tsValue) -> {
                if (tsValue.getValue() != null) {
                    latestValues.put("entity" + StringUtils.capitalize(key), tsValue.getValue());
                }
            }));
        }
        return latestValues;
    }

    protected Map<String, String> toMapData(AlarmData alarmData, List<String> keys) {
        Map<String, String> data = new HashMap<>();
        JsonNode alarmDataJson = JacksonUtil.valueToTree(alarmData);
        keys.forEach(key -> {
            JsonNode value = JacksonUtil.getByKeyPath(alarmDataJson, key);
            if (value != null) {
                data.put(key, value.asText());
            }
        });
        return data;
    }

    protected List<Map<String, String>> collectTsData(List<TsKvEntry> tsKvEntries) {
        List<Map<String, String>> tsData = new ArrayList<>();
        Map<Long, List<TsKvEntry>> groupedByTs = tsKvEntries.stream().collect(Collectors.groupingBy(TsKvEntry::getTs));

        groupedByTs.forEach((ts, entries) -> {
            Map<String, String> tsValues = new HashMap<>();
            tsValues.put("ts", ts.toString());
            for (TsKvEntry entry : entries) {
                tsValues.put(entry.getKey(), entry.getValueAsString());
            }
            tsData.add(tsValues);
        });
        return tsData;
    }
}
