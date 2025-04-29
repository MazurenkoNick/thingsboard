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
package org.thingsboard.server.service.report;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
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
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.DashboardReportService;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.ReportRequest;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.EntityAlias;
import org.thingsboard.server.common.data.report.configuration.Filter;
import org.thingsboard.server.common.data.report.configuration.JasperReportBuilder;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfiguration;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.report.configuration.JasperReportBuilder.getComponentDataSource;
import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.TIME_SERIES_TABLE;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toAlarmCountQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toAlarmDataQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toEntityCountQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toEntityDataQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toReadTsKvQueries;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toSingleDeviceQuery;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultReportService extends AbstractTbEntityService implements ReportService {

    private SimpleDateFormat defaultDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");

    private final ReportTemplateService reportTemplateService;
    private final DashboardReportService dashboardReportService;
    private final EntityService entityService;
    private final TimeseriesService tsService;
    private final AlarmService alarmService;

    @Override
    public ListenableFuture<ReportData> generateReport(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, ReportRequest reportRequest) throws ThingsboardException {
        log.trace("[{}] Executing generateReport, reportRequest [{}]", tenantId, reportRequest);
        ReportTemplate reportTemplate = checkNotNull(reportTemplateService.findReportTemplateById(tenantId, reportRequest.getTemplateId()));
        ReportTemplateConfiguration configuration = reportTemplate.getConfiguration();

        SettableFuture<ReportData> resultFuture = SettableFuture.create();
        try {
            TbReportCtx ctx = new TbReportCtx(tenantId, customerId, userPermissions, configuration);
            JasperReportBuilder reportBuilder = new JasperReportBuilder(configuration);

            Optional.ofNullable(configuration.getHeader()).ifPresent(reportBuilder::addPageHeader);
            Optional.ofNullable(configuration.getFooter()).ifPresent(reportBuilder::addPageFooter);

            renderContent(ctx, reportBuilder, configuration.getComponents());

            JasperReport mainReport = JasperCompileManager.compileReport(reportBuilder.getJasperDesign());
            JasperPrint print = JasperFillManager.fillReport(mainReport, ctx.getParams(), new JREmptyDataSource());

            ReportData report = ReportData.builder()
                    .data(JasperExportManager.exportReportToPdf(print))
                    .contentType(reportRequest.getReportType().getContentType())
                    .name(configuration.getFileName() + "-" + defaultDateFormat.format(new Date()) + ".pdf")
                    .build();
            resultFuture.set(report);
        } catch (Exception e) {
            log.error("Unexpected error during report generation!", e);
            throw new ThingsboardException("Unable to generate report", ExceptionUtils.getRootCause(e), ThingsboardErrorCode.GENERAL);
        }
        return resultFuture;
    }

    private void renderContent(TbReportCtx ctx, JasperReportBuilder parentBuilder, List<ReportComponent> components) throws Exception {
        for (ReportComponent component : components) {
            if (component.getType() == TIME_SERIES_TABLE) { // check if component is complex
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

        JasperReport subReport = parentBuilder.addComponent(component);

        Map<String, Object> params = ctx.getParams();
        params.put(subReportExpr, subReport);
        params.put(subReportDSExpr, dataSource);
    }

    private JRMapCollectionDataSource buildTsDataSource(TbReportCtx tbReportCtx, EntityData entityData, TimeseriesTableComponent component) {
        List<ReadTsKvQuery> queries = toReadTsKvQueries(component);
        ListenableFuture<List<TsKvEntry>> queryResult = tsService.findAll(tbReportCtx.getTenantId(), entityData.getEntityId(), queries);
        try {
            return new JRMapCollectionDataSource(collectTsData(queryResult.get()));
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private JRMapCollectionDataSource buildDataSource(TbReportCtx ctx, ReportComponent component, EntityData entityData) {
        return switch (component.getType()) {
            case TIME_SERIES_TABLE -> buildTsDataSource(ctx, entityData, ((TimeseriesTableComponent) component));
            case ALARM_TABLE -> buildAlarmDataSource(ctx, ((AlarmTableComponent) component));
            default -> buildEntityDataSource(ctx, component.getDataSources());
        };
    }

    private JRMapCollectionDataSource buildAlarmDataSource(TbReportCtx ctx, AlarmTableComponent component) {
        List<EntityId> entityIds = entityService.findEntityDataByQuery(ctx.getTenantId(), ctx.getCustomerId(),
                        ctx.getUserPermissions(), toEntityDataQuery(component.getAlarmSource(), ctx.getEntityAliases(), ctx.getFilters()))
                .getData()
                .stream()
                .map(EntityData::getEntityId).toList();
        PageData<AlarmData> alarmDatas = alarmService.findAlarmDataByQueryForEntities(ctx.getTenantId(), ctx.getUserPermissions(),
                toAlarmDataQuery(component, ctx.getEntityAliases(), ctx.getFilters()), entityIds);
        Collection<Map<String, ?>> alarmList = new ArrayList<>();
        for (AlarmData alarmData : alarmDatas.getData()) {
            Map<String, String> mapped = toStringMap(alarmData);
            mapped.put("status", alarmData.getStatus().name());
            alarmList.add(mapped);
        }
        return new JRMapCollectionDataSource(alarmList);
    }

    private JRMapCollectionDataSource buildEntityDataSource(TbReportCtx tbReportCtx, List<DataSource> dataSources) {
        if (dataSources == null) {
            return new JRMapCollectionDataSource(List.of(Map.of()));
        }
        Collection<Map<String, ?>> entryList = new ArrayList<>();
        for (DataSource dataSource : dataSources) {
            Collection<Map<String, ?>> dataMap = buildEntityDataSource(tbReportCtx, dataSource);
            entryList.addAll(dataMap);
        }
        return new JRMapCollectionDataSource(entryList);
    }

    private Collection<Map<String, ?>> buildEntityDataSource(TbReportCtx ctx, DataSource dataSource) {
        List<EntityAlias> entityAliases = ctx.getEntityAliases();
        List<Filter> filters = ctx.getFilters();
        return switch (dataSource.getType()) {
            case "device" ->
                    collectEntityData(entityService.findEntityDataByQuery(ctx.getTenantId(), ctx.getCustomerId(), ctx.getUserPermissions(), toSingleDeviceQuery(dataSource, filters)));
            case "entity" ->
                    collectEntityData(entityService.findEntityDataByQuery(ctx.getTenantId(), ctx.getCustomerId(), ctx.getUserPermissions(), toEntityDataQuery(dataSource, entityAliases, filters)));
            case "entityCount" ->
                    List.of(Map.of("count", entityService.countEntitiesByQuery(ctx.getTenantId(), ctx.getCustomerId(), ctx.getUserPermissions(), toEntityCountQuery(dataSource, entityAliases, filters))));
            case "alarmCount" ->
                    List.of(Map.of("count", alarmService.countAlarmsByQuery(ctx.getTenantId(), ctx.getCustomerId(), ctx.getUserPermissions(), toAlarmCountQuery(dataSource, entityAliases, filters))));
            default -> throw new IllegalArgumentException("Unknown data source type: " + dataSource.getType());
        };
    }

    private List<EntityData> fetchEntities(TbReportCtx ctx, ReportComponent component) {
        DataSource dataSource = getComponentDataSource(component);
        List<EntityAlias> entityAliases = ctx.getEntityAliases();
        List<Filter> filters = ctx.getFilters();
        return switch (dataSource.getType()) {
            case "device" ->
                    entityService.findEntityDataByQuery(ctx.getTenantId(), ctx.getCustomerId(), ctx.getUserPermissions(), toSingleDeviceQuery(dataSource, filters))
                            .getData();
            case "entity" ->
                    entityService.findEntityDataByQuery(ctx.getTenantId(), ctx.getCustomerId(), ctx.getUserPermissions(), toEntityDataQuery(dataSource, entityAliases, filters))
                            .getData();
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
