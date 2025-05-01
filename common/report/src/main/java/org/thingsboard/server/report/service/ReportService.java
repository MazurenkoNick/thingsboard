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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.SettableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportConfig;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportData;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.ReportRequest;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.EntityAlias;
import org.thingsboard.server.common.data.report.configuration.Filter;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfiguration;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.DashboardComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.common.data.report.configuration.timewindow.History;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;
import org.thingsboard.server.queue.util.TbReportComponent;
import org.thingsboard.server.report.util.JasperReportBuilder;
import org.thingsboard.server.report.util.WebReportClient;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.parseMediaType;
import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.SUB_REPORT;
import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.TIME_SERIES_TABLE;
import static org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator.getTimeRange;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toAlarmCountQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toAlarmDataQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toEntityCountQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toEntityDataQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toSingleDeviceQuery;
import static org.thingsboard.server.report.util.JasperReportBuilder.getComponentDataSource;

@TbReportComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {

    private static final JRMapCollectionDataSource EMPTY_DATA_SOURCE = new JRMapCollectionDataSource(List.of(Map.of()));
    private final SimpleDateFormat defaultDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss"); // fixme dasha make configurable (?)
    private static final Pattern reportNameDatePattern = Pattern.compile("%d\\{([^\\}]*)\\}");

    private final WebReportClient webReportClient;

    public ReportData generateReport(ReportTask task, TbReportCtx ctx) throws ThingsboardException {
        TenantId tenantId = task.getTenantId();
        ReportRequest reportRequest = task.getReportRequest();

        log.trace("[{}] Executing generateReport, reportRequest [{}]", tenantId, reportRequest);
        ReportTemplate reportTemplate = task.getReportTemplate();
        ReportTemplateConfiguration configuration = reportTemplate.getConfiguration();

        try {
            JasperReportBuilder reportBuilder = new JasperReportBuilder(configuration);

            Optional.ofNullable(configuration.getHeader()).ifPresent(reportBuilder::addPageHeader);
            Optional.ofNullable(configuration.getFooter()).ifPresent(reportBuilder::addPageFooter);

            renderContent(ctx, reportBuilder, configuration.getComponents());

            JasperReport mainReport = JasperCompileManager.compileReport(reportBuilder.getJasperDesign());
            JasperPrint print = JasperFillManager.fillReport(mainReport, ctx.getParams(), new JREmptyDataSource());

            TbReportFormat format = reportRequest.getFormat();
            return ReportData.builder()
                    .data(JasperExportManager.exportReportToPdf(print))
                    .contentType(format.getContentType())
                    .name(configuration.getFileName() + "-" + defaultDateFormat.format(new Date()) + format.getExtension())
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
        String subReportId = "component_" + StringUtils.randomAlphabetic(10);
        String subReportDSId = "componentDS_" + StringUtils.randomAlphabetic(10);

        parentBuilder.buildSubReportBand(subReportId, subReportDSId);

        JasperReport subReport = parentBuilder.buildComponent(component);
        JRDataSource subReportDS = buildDataSource(ctx, component, entityData);

        Map<String, Object> params = ctx.getParams();
        params.put(subReportId, subReport);
        params.put(subReportDSId, subReportDS);
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

    private JRDataSource buildDataSource(TbReportCtx ctx, ReportComponent component, EntityData entityData) throws ThingsboardException {
        return switch (component.getType()) {
            case TIME_SERIES_TABLE -> buildTsDataSource(ctx, ((TimeseriesTableComponent) component), entityData);
            case ALARM_TABLE -> buildAlarmDataSource(ctx, ((AlarmTableComponent) component));
            case DASHBOARD -> buildDashboardDataSource(ctx, ((DashboardComponent) component));
            default -> buildEntityDataSource(ctx, component.getDataSources());
        };
    }

    private JRDataSource buildDashboardDataSource(TbReportCtx ctx, DashboardComponent component) {
        SettableFuture<DashboardReportData> futureToSet = SettableFuture.create();
        webReportClient.requestDashboardReport(component.getConfig(), null,
                ctx.getAccessToken(), ctx.getAccessTokenExpTs(),
                futureToSet::set, error -> {
                    log.error("Failed to generate dashboard report", error);
                    futureToSet.setException(error);
                });
        try {
            return new JRBeanCollectionDataSource(List.of(futureToSet.get()));
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
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

    private Consumer<DashboardReportData> onSuccess(DeferredResult<ResponseEntity<Resource>> result) {
        return reportData -> {
            ByteArrayResource resource = new ByteArrayResource(reportData.getData());
            ResponseEntity<Resource> response = ResponseEntity.ok().
                    header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + reportData.getName())
                    .header("x-filename", reportData.getName())
                    .contentLength(resource.contentLength())
                    .contentType(parseMediaType(reportData.getContentType()))
                    .body(resource);
            result.setResult(response);
        };
    }

    private JsonNode createDashboardReportRequest(TenantId tenantId, DashboardReportConfig reportConfig) throws ThingsboardException {
//        AccessJwtToken accessToken = systemSecurityService.createUserAccessToken(tenantId, new UserId(UUID.fromString(reportConfig.getUserId())));
//        String token = accessToken.getToken();
//        long expiration = accessToken.getClaims().getExpiration().getTime();
        TimeZone tz = TimeZone.getTimeZone(reportConfig.getTimezone());
        String reportName = prepareReportName(reportConfig.getNamePattern(), new Date(), tz);
        ObjectNode dashboardReportRequest = JacksonUtil.newObjectNode();
        dashboardReportRequest.put("baseUrl", reportConfig.getBaseUrl());
        dashboardReportRequest.put("dashboardId", reportConfig.getDashboardId());
        // dashboardReportRequest.put("token", token);
        //dashboardReportRequest.put("expiration", expiration);
        dashboardReportRequest.put("name", reportName);
        dashboardReportRequest.set("reportParams", createReportParams(reportConfig));
        return dashboardReportRequest;
    }

    private JsonNode createReportParams(DashboardReportConfig reportConfig) {
        ObjectNode reportParams = JacksonUtil.newObjectNode();
        reportParams.put("type", reportConfig.getType());
        reportParams.put("state", reportConfig.getState());
        if (!reportConfig.isUseDashboardTimewindow()) {
            reportParams.set("timewindow", reportConfig.getTimewindow());
        }
        reportParams.put("timezone", reportConfig.getTimezone());
        return reportParams;
    }

    private DashboardReportData extractResponse(ResponseEntity<byte[]> responseEntity) throws UnsupportedEncodingException {
        DashboardReportData reportData = new DashboardReportData();
        reportData.setData(responseEntity.getBody());
        reportData.setContentType(responseEntity.getHeaders().getContentType().toString());
        String disposition = responseEntity.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        String fileName = disposition.replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1");
        fileName = URLDecoder.decode(fileName, "ISO_8859_1");
        reportData.setName(fileName);
        return reportData;
    }

    private String prepareReportName(String namePattern, Date reportDate, TimeZone tz) {
        String name = namePattern;
        Matcher matcher = reportNameDatePattern.matcher(namePattern);
        while (matcher.find()) {
            String toReplace = matcher.group(0);
            SimpleDateFormat dateFormat = new SimpleDateFormat(matcher.group(1));
            dateFormat.setTimeZone(tz);
            String replacement = dateFormat.format(reportDate);
            name = name.replace(toReplace, replacement);
        }
        return name;
    }

    private void prepareHeaders(HttpHeaders headers, byte[] json) {
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentLength(json.length);
        headers.setConnection("keep-alive");
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
