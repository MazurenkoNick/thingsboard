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
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
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
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.ReportRequest;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.EntityAlias;
import org.thingsboard.server.common.data.report.configuration.components.EntityTableComponent;
import org.thingsboard.server.common.data.report.configuration.Filter;
import org.thingsboard.server.common.data.report.configuration.components.HeadingComponent;
import org.thingsboard.server.common.data.report.configuration.components.PageBreakComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfiguration;
import org.thingsboard.server.common.data.report.configuration.components.RichTextComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.common.data.report.configuration.timewindow.History;
import org.thingsboard.server.common.data.report.configuration.timewindow.QuickTimeIntervalCalculator;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;
import org.thingsboard.server.common.data.report.configuration.JasperReportBuilder;
import org.thingsboard.server.common.data.util.ThrowingBiFunction;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.ENTITY_TABLE;
import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.HEADING;
import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.PAGE_BREAK;
import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.RICH_TEXT;
import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.TIME_SERIES_TABLE;
import static org.thingsboard.server.common.data.report.configuration.timewindow.QuickTimeIntervalCalculator.getTimeRange;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toAlarmCountQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toEntityCountQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toEntityDataQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toSingleDeviceQuery;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultReportService extends AbstractTbEntityService implements ReportService {

    private SimpleDateFormat defaultDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
    private Map<ReportComponentType, ThrowingBiFunction<JasperReportBuilder, ReportComponent, JasperReport>> componentRendererMap;

    private final ReportTemplateService reportTemplateService;
    private final DashboardReportService dashboardReportService;
    private final EntityService entityService;
    private final TimeseriesService tsService;
    private final AlarmService alarmService;

    @PostConstruct
    public void init() {
        componentRendererMap = new EnumMap<>(ReportComponentType.class);
        componentRendererMap.put(HEADING, (parent, component) -> renderHeadingComponent(parent, (HeadingComponent) component));
        componentRendererMap.put(RICH_TEXT, (parent, component) -> renderRichTextComponent(parent, (RichTextComponent) component));
        componentRendererMap.put(ENTITY_TABLE, (parent, component) -> renderEntityTableComponent(parent, (EntityTableComponent) component));
        componentRendererMap.put(TIME_SERIES_TABLE, (parent, component) -> renderTimeSeriesTableComponent(parent, (TimeseriesTableComponent) component));
        componentRendererMap.put(PAGE_BREAK, (parent, component) -> renderPageBreakComponent(parent, (PageBreakComponent) component));
    }

    @Override
    public ListenableFuture<ReportData> generateReport(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, ReportRequest reportRequest) throws ThingsboardException {
        log.trace("[{}] Executing generateReport, reportRequest [{}]", tenantId, reportRequest);
        ReportTemplate reportTemplate = checkNotNull(reportTemplateService.findReportTemplateById(tenantId, reportRequest.getTemplateId()));
        ReportTemplateConfiguration configuration = reportTemplate.getConfiguration();

        SettableFuture<ReportData> resultFuture = SettableFuture.create();
        try {
            TbReportCtx tbReportCtx = new TbReportCtx(tenantId, customerId, userPermissions, configuration);
            JasperReportBuilder parentBuilder = new JasperReportBuilder(configuration);

            Optional.ofNullable(configuration.getHeader()).ifPresent(parentBuilder::addPageHeader);
            Optional.ofNullable(configuration.getFooter()).ifPresent(parentBuilder::addPageFooter);

            renderComponents(tbReportCtx, parentBuilder, configuration.getComponents());

            JasperReport mainReport = JasperCompileManager.compileReport(parentBuilder.getJasperDesign());
            JasperPrint print = JasperFillManager.fillReport(mainReport, tbReportCtx.getParams(), new JREmptyDataSource());

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

    private void renderComponents(TbReportCtx tbReportCtx, JasperReportBuilder parentBuilder, List<ReportComponent> components) throws Exception {
        for (ReportComponent component : components) {
            ThrowingBiFunction<JasperReportBuilder, ReportComponent, JasperReport> renderer = componentRendererMap.get(component.getType());
            if (renderer != null) {
                JRMapCollectionDataSource dataSource = fetchDataSource(tbReportCtx, component);

                JasperReport subReport = renderer.apply(parentBuilder, component);

                String subReportExpr = "component_" + StringUtils.randomAlphabetic(10);
                String subReportDSExpr = "componentDS_" + StringUtils.randomAlphabetic(10);
                parentBuilder.addSubReport(subReportExpr, subReportDSExpr);

                Map<String, Object> params = tbReportCtx.getParams();
                params.put(subReportExpr, subReport);
                params.put(subReportDSExpr, dataSource);
            }
        }
    }

    private JasperReport renderHeadingComponent(JasperReportBuilder parentReport, HeadingComponent component) throws JRException {
        JasperReportBuilder heading = new JasperReportBuilder(component, parentReport.getUsablePageWidth());
        heading.addHeading(component.getValue());
        return JasperCompileManager.compileReport(heading.getJasperDesign());
    }

    private JasperReport renderRichTextComponent(JasperReportBuilder parentReport, RichTextComponent component) throws JRException {
        JasperReportBuilder richTextDesign = new JasperReportBuilder(component, parentReport.getUsablePageWidth());
        richTextDesign.addRichText(component.getValue());
        return JasperCompileManager.compileReport(richTextDesign.getJasperDesign());
    }

    private JasperReport renderEntityTableComponent(JasperReportBuilder parentReport, EntityTableComponent component) throws JRException {
        JasperReportBuilder table = new JasperReportBuilder(component, parentReport.getUsablePageWidth());

        List<DataKey> dataKeys = component.getDataSources().get(0).getDataKeys();
        List<String> entityKeys = dataKeys.stream().map(DataKey::getName).toList();
        List<String> columsHeaders = dataKeys.stream().map(DataKey::getLabel).toList();

        table.addColumnHeader(columsHeaders);
        table.addTableDetailBand(entityKeys);
        return JasperCompileManager.compileReport(table.getJasperDesign());
    }

    private JasperReport renderTimeSeriesTableComponent(JasperReportBuilder parentReport, TimeseriesTableComponent component) throws JRException {
            JasperReportBuilder table = new JasperReportBuilder(component, parentReport.getUsablePageWidth());

            List<DataKey> dataKeys = component.getDataSources().get(0).getDataKeys();
            List<String> entityKeys = dataKeys.stream().map(DataKey::getName).collect(Collectors.toList());
            List<String> columsHeaders = dataKeys.stream().map(DataKey::getLabel).collect(Collectors.toList());

            entityKeys.add(0, "ts");
            columsHeaders.add(0, "Timestamp");

            table.addColumnHeader(columsHeaders);
            table.addTableDetailBand(entityKeys);
            return JasperCompileManager.compileReport(table.getJasperDesign());
    }

    private JasperReport renderPageBreakComponent(JasperReportBuilder parentBuilder, PageBreakComponent component) {
        try {
            JasperReportBuilder pageBreak = new JasperReportBuilder(component, parentBuilder.getUsablePageWidth());
            pageBreak.addPageBreak();
            return JasperCompileManager.compileReport(pageBreak.getJasperDesign());
        } catch (JRException e) {
            throw new RuntimeException(e);
        }
    }

    private JRMapCollectionDataSource fetchDataSource(TbReportCtx tbReportCtx, ReportComponent component) {
        return switch (component.getType()) {
            case HEADING, RICH_TEXT, PAGE_BREAK, ENTITY_TABLE -> fetchDataSource(tbReportCtx, component.getDataSources());
            case TIME_SERIES_TABLE -> fetchTsDataSource(tbReportCtx, (TimeseriesTableComponent) component);
            default -> throw new IllegalArgumentException("Unknown report component type: " + component.getType());
        };
    }

    private JRMapCollectionDataSource fetchTsDataSource(TbReportCtx tbReportCtx, TimeseriesTableComponent component) {
        DataSource dataSource = component.getDataSources().get(0);
        String deviceId = dataSource.getDeviceId();
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID is required for time series table component");
        }
        TimeWindowConfiguration timeWindowConf = component.getTimewindow();
        History historyConf = timeWindowConf.getHistory();

        long startTs;
        long endTs;
        switch (historyConf.getHistoryType()) {
            case 0 -> {
                startTs = System.currentTimeMillis() - historyConf.getTimewindowMs();
                endTs = System.currentTimeMillis();
            }
            case 1 -> {
                startTs = historyConf.getFixedTimeWindow().getStartTimeMs();
                endTs = historyConf.getFixedTimeWindow().getEndTimeMs();
            }
            case 2 -> {
                QuickTimeIntervalCalculator.TimeRange timeRange = getTimeRange(historyConf.getQuickInterval(), timeWindowConf.getTimezone());
                startTs = timeRange.startTs;
                endTs = timeRange.endTs;
            }
            default -> throw new IllegalArgumentException("Unknown history type: " + historyConf.getHistoryType());
        }
        List<ReadTsKvQuery> queries = dataSource.getDataKeys()
                .stream()
                .map(dataKey -> new BaseReadTsKvQuery(dataKey.getName(), startTs, endTs, historyConf.getInterval(), timeWindowConf.getAggregation().getLimit(), timeWindowConf.getAggregation().getType()))
                .collect(Collectors.toList());

        ListenableFuture<List<TsKvEntry>> queryResult = tsService.findAll(tbReportCtx.getTenantId(), DeviceId.fromString(deviceId), queries);
        try {
            return new JRMapCollectionDataSource(collectTsData(queryResult.get()));
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private JRMapCollectionDataSource fetchDataSource(TbReportCtx tbReportCtx, List<DataSource> dataSources) {
        if (dataSources == null) {
            return new JRMapCollectionDataSource(List.of(Map.of()));
        }
        Collection<Map<String, ?>> entryList = new ArrayList<>();
        for (DataSource dataSource : dataSources) {
            Collection<Map<String, ?>> dataMap = fetchDataSource(tbReportCtx, dataSource);
            entryList.addAll(dataMap);
        }
        return new JRMapCollectionDataSource(entryList);
    }

    private Collection<Map<String, ?>> fetchDataSource(TbReportCtx tbReportCtx, DataSource dataSource) {
        List<EntityAlias> entityAliases = tbReportCtx.getEntityAliases();
        List<Filter> filters = tbReportCtx.getFilters();
        return switch (dataSource.getType()) {
            case "device" -> fetchEntityData(tbReportCtx, toSingleDeviceQuery(dataSource, filters));
            case "entity" -> fetchEntityData(tbReportCtx, toEntityDataQuery(dataSource, entityAliases, filters));
            case "entityCount" -> fetchEntityCount(tbReportCtx, toEntityCountQuery(dataSource, entityAliases, filters));
            case "alarmCount" -> fetchAlarmCount(tbReportCtx, toAlarmCountQuery(dataSource, entityAliases, filters));
            default -> throw new IllegalArgumentException("Unknown data source type: " + dataSource.getType());
        };
    }

    private Collection<Map<String, ?>> fetchEntityData(TbReportCtx tbReportCtx, EntityDataQuery entityDataQuery) {
        PageData<EntityData> queryResult = entityService.findEntityDataByQuery(tbReportCtx.getTenantId(), tbReportCtx.getCustomerId(),
                    tbReportCtx.getUserPermissions(), entityDataQuery);
        return collectEntityData(queryResult);
    }

    private Collection<Map<String, ?>> fetchEntityCount(TbReportCtx tbReportCtx, EntityCountQuery entityCountQuery) {
        long count = entityService.countEntitiesByQuery(tbReportCtx.getTenantId(), tbReportCtx.getCustomerId(),
                tbReportCtx.getUserPermissions(), entityCountQuery);
        return List.of(Map.of("count", count));
    }

    private Collection<Map<String, ?>> fetchAlarmCount(TbReportCtx tbReportCtx, AlarmCountQuery alarmCountQuery) {
        long count = alarmService.countAlarmsByQuery(tbReportCtx.getTenantId(), tbReportCtx.getCustomerId(),
                tbReportCtx.getUserPermissions(), alarmCountQuery);
        return List.of(Map.of("count", count));
    }

    private static Collection<Map<String, ?>> collectEntityData(PageData<EntityData> result) {
        Collection<Map<String, ?>> entryList = new ArrayList<>();
        for (EntityData entityData : result.getData()) {
            HashMap<String, String> EntityFields = new HashMap<>();
            entryList.add(EntityFields);
            entityData.getLatest().forEach((keyType, keyValueMap) -> {
                keyValueMap.forEach((key, tsValue) -> {
                    if (tsValue.getValue() != null) {
                        EntityFields.put(key, tsValue.getValue());
                    }
                });
            });
        }
        return entryList;
    }

    private static Collection<Map<String, ?>> collectTsData(List<TsKvEntry> tsKvEntries) {
        Collection<Map<String, ?>> entryList = new ArrayList<>();
        Map<Long, List<TsKvEntry>> groupedByTs = tsKvEntries.stream().collect(Collectors.groupingBy(TsKvEntry::getTs));

        groupedByTs.forEach((ts, entries) -> {
            Map<String, Object> tsValues = new HashMap<>();
            tsValues.put("ts", ts.toString());
            for (TsKvEntry entry : entries) {
                tsValues.put(entry.getKey(), entry.getValueAsString());
            }
            entryList.add(tsValues);
        });
        return entryList;
    }

}
