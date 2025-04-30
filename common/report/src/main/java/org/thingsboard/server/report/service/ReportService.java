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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.ReportTask;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.ReportRequest;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.TbReportType;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.EntityAlias;
import org.thingsboard.server.common.data.report.configuration.Filter;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfiguration;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.common.data.report.configuration.timewindow.History;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;
import org.thingsboard.server.report.JasperReportBuilder;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.SUB_REPORT;
import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.TIME_SERIES_TABLE;
import static org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator.getTimeRange;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toAlarmCountQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toAlarmDataQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toEntityCountQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toEntityDataQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toSingleDeviceQuery;
import static org.thingsboard.server.report.JasperReportBuilder.getComponentDataSource;

@Slf4j
@RequiredArgsConstructor
public class ReportService {

    private final SimpleDateFormat defaultDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss"); // fixme dasha make configurable (?)

    public ReportData generateReport(ReportTask task, RestClient restClient) throws ThingsboardException {
        TenantId tenantId = task.getTenantId();
        ReportRequest reportRequest = task.getReportRequest();

        log.trace("[{}] Executing generateReport, reportRequest [{}]", tenantId, reportRequest);
        ReportTemplate reportTemplate = task.getReportTemplate();
        ReportTemplateConfiguration configuration = reportTemplate.getConfiguration();

        try {
            TbReportCtx ctx = new TbReportCtx(tenantId, reportRequest.getCustomerId(), configuration, restClient);
            JasperReportBuilder reportBuilder = new JasperReportBuilder(configuration);

            Optional.ofNullable(configuration.getHeader()).ifPresent(reportBuilder::addPageHeader);
            Optional.ofNullable(configuration.getFooter()).ifPresent(reportBuilder::addPageFooter);

            renderContent(ctx, reportBuilder, configuration.getComponents());

            JasperReport mainReport = JasperCompileManager.compileReport(reportBuilder.getJasperDesign());
            JasperPrint print = JasperFillManager.fillReport(mainReport, ctx.getParams(), new JREmptyDataSource());

            TbReportType type = reportRequest.getType();
            return ReportData.builder()
                    .data(JasperExportManager.exportReportToPdf(print))
                    .contentType(type.getContentType())
                    .name(configuration.getFileName() + "-" + defaultDateFormat.format(new Date()) + type.getExtension())
                    .build();
        } catch (Exception e) {
            log.error("Unexpected error during report generation!", e);
            throw new ThingsboardException("Unable to generate report", ExceptionUtils.getRootCause(e), ThingsboardErrorCode.GENERAL);
        }
    }

    private void renderContent(TbReportCtx ctx, JasperReportBuilder parentBuilder, List<ReportComponent> components) throws Exception {
        for (ReportComponent component : components) {
            if (component.getType() == TIME_SERIES_TABLE || component.getType() == SUB_REPORT) { // check if component is complex
                List<EntityData> entityDatas = fetchEntities(ctx, component);
                for (EntityData entityData : entityDatas) {
                    renderComponent(ctx, parentBuilder, component, entityData);
                }
            } else {
                renderComponent(ctx, parentBuilder, component, null);
            }
        }
    }

    private void renderComponent(TbReportCtx ctx, JasperReportBuilder parentBuilder, ReportComponent component, EntityData entityData) throws Exception {
        JRMapCollectionDataSource dataSource = buildDataSource(ctx, component, entityData);

        String subReportExpr = "component_" + StringUtils.randomAlphabetic(10);
        String subReportDSExpr = "componentDS_" + StringUtils.randomAlphabetic(10);
        parentBuilder.addSubReport(subReportExpr, subReportDSExpr);

        JasperReport subReport = parentBuilder.buildComponent(component);

        Map<String, Object> params = ctx.getParams();
        params.put(subReportExpr, subReport);
        params.put(subReportDSExpr, dataSource);
    }

    private JRMapCollectionDataSource buildTsDataSource(TbReportCtx ctx, TimeseriesTableComponent component, EntityData entityData) {
        TimeWindowConfiguration timeWindowConf = component.getTimewindow();
        History historyConf = timeWindowConf.getHistory();
        TimeIntervalCalculator.TimeRange timeRange = getTimeRange(timeWindowConf);

        List<String> keys = getComponentDataSource(component).getDataKeys().stream()
                .map(DataKey::getName)
                .toList();

        // todo dasha make sort order configurable (?)
        List<TsKvEntry> result = ctx.getRestClient().getTimeseries(entityData.getEntityId(), keys, historyConf.getInterval(), timeWindowConf.getAggregation().getType(),
                SortOrder.Direction.DESC, timeRange.startTs, timeRange.endTs, timeWindowConf.getAggregation().getLimit(), false);
        return new JRMapCollectionDataSource(collectTsData(result));
    }

    private JRMapCollectionDataSource buildDataSource(TbReportCtx ctx, ReportComponent component, EntityData entityData) {
        return switch (component.getType()) {
            case TIME_SERIES_TABLE -> buildTsDataSource(ctx, ((TimeseriesTableComponent) component), entityData);
            case ALARM_TABLE -> buildAlarmDataSource(ctx, ((AlarmTableComponent) component));
            default -> buildEntityDataSource(ctx, component.getDataSources());
        };
    }

    private JRMapCollectionDataSource buildAlarmDataSource(TbReportCtx ctx, AlarmTableComponent component) {
//        List<EntityId> entityIds = restClient.findEntityDataByQuery(toEntityDataQuery(component.getAlarmSource(), ctx.getEntityAliases(), ctx.getFilters()))
//                .getData()
//                .stream()
//                .map(EntityData::getEntityId).toList();
        PageData<AlarmData> alarmDatas = ctx.getRestClient().findAlarmDataByQuery(toAlarmDataQuery(component, ctx.getEntityAliases(), ctx.getFilters())); // FIXME Dasha check, why previously used alarmService.findAlarmDataByQueryForEntities(ctx.getTenantId(), ctx.getUserPermissions(), entityIds) ?
        Collection<Map<String, ?>> alarmList = new ArrayList<>();
        for (AlarmData alarmData : alarmDatas.getData()) {
            Map<String, String> mapped = toStringMap(alarmData);
            mapped.put("status", alarmData.getStatus().name());
            alarmList.add(mapped);
        }
        return new JRMapCollectionDataSource(alarmList);
    }

    private JRMapCollectionDataSource buildEntityDataSource(TbReportCtx ctx, List<DataSource> dataSources) {
        if (dataSources == null) {
            return new JRMapCollectionDataSource(List.of(Map.of()));
        }
        Collection<Map<String, ?>> entryList = new ArrayList<>();
        for (DataSource dataSource : dataSources) {
            Collection<Map<String, ?>> dataMap = buildEntityDataSource(ctx, dataSource);
            entryList.addAll(dataMap);
        }
        return new JRMapCollectionDataSource(entryList);
    }

    private Collection<Map<String, ?>> buildEntityDataSource(TbReportCtx ctx, DataSource dataSource) {
        List<EntityAlias> entityAliases = ctx.getEntityAliases();
        List<Filter> filters = ctx.getFilters();
        return switch (dataSource.getType()) {
            case "device" -> collectEntityData(ctx.getRestClient().findEntityDataByQuery(toSingleDeviceQuery(dataSource, filters)));
            case "entity" -> collectEntityData(ctx.getRestClient().findEntityDataByQuery(toEntityDataQuery(dataSource, entityAliases, filters)));
            case "entityCount" ->
                    List.of(Map.of("count", ctx.getRestClient().countEntitiesByQuery(toEntityCountQuery(dataSource, entityAliases, filters))));
            case "alarmCount" ->
                    List.of(Map.of("count", ctx.getRestClient().countAlarmsByQuery(toAlarmCountQuery(dataSource, entityAliases, filters))));
            default -> throw new IllegalArgumentException("Unknown data source type: " + dataSource.getType());
        };
    }

    private List<EntityData> fetchEntities(TbReportCtx ctx, ReportComponent component) {
        DataSource dataSource = getComponentDataSource(component);
        List<EntityAlias> entityAliases = ctx.getEntityAliases();
        List<Filter> filters = ctx.getFilters();
        return switch (dataSource.getType()) {
            case "device" -> ctx.getRestClient().findEntityDataByQuery(toSingleDeviceQuery(dataSource, filters)).getData();
            case "entity" -> ctx.getRestClient().findEntityDataByQuery(toEntityDataQuery(dataSource, entityAliases, filters)).getData();
            default -> throw new IllegalArgumentException("Unknown data source type: " + dataSource.getType());
        };
    }

    private static Collection<Map<String, ?>> collectEntityData(PageData<EntityData> result) {
        Collection<Map<String, ?>> entityList = new ArrayList<>();
        for (EntityData entityData : result.getData()) {
            HashMap<String, String> latestValues = new HashMap<>();
            entityList.add(latestValues);
            entityData.getLatest().forEach((keyType, keyValueMap) -> keyValueMap.forEach((key, tsValue) -> {
                if (tsValue.getValue() != null) {
                    latestValues.put(key, tsValue.getValue());
                }
            }));
        }
        return entityList;
    }

    private static Collection<Map<String, ?>> collectTsData(List<TsKvEntry> tsKvEntries) {
        Collection<Map<String, ?>> tsData = new ArrayList<>();
        Map<Long, List<TsKvEntry>> groupedByTs = tsKvEntries.stream().collect(Collectors.groupingBy(TsKvEntry::getTs));

        groupedByTs.forEach((ts, entries) -> {
            Map<String, Object> tsValues = new HashMap<>();
            tsValues.put("ts", ts.toString());
            for (TsKvEntry entry : entries) {
                tsValues.put(entry.getKey(), entry.getValueAsString());
            }
            tsData.add(tsValues);
        });
        return tsData;
    }

    public static Map<String, String> toStringMap(Object obj) {
        Map<String, String> map = new HashMap<>();
        Class<?> current = obj.getClass();

        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object value = field.get(obj);
                    map.put(field.getName(), value != null ? value.toString() : null);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Error accessing field: " + field.getName(), e);
                }
            }
            current = current.getSuperclass();
        }

        return map;
    }
}
