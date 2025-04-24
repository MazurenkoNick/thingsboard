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
import net.sf.jasperreports.engine.design.JasperDesign;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.DashboardReportService;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.ReportRequest;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.EntityTableComponent;
import org.thingsboard.server.common.data.report.configuration.HeadingComponent;
import org.thingsboard.server.common.data.report.configuration.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfiguration;
import org.thingsboard.server.common.data.report.configuration.RichTextComponent;
import org.thingsboard.server.common.data.util.EntityDataQueryUtils;
import org.thingsboard.server.common.data.util.JasperReportUtils;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.thingsboard.server.common.data.util.EntityDataQueryUtils.toEntityDataQuery;
import static org.thingsboard.server.common.data.util.EntityDataQueryUtils.toSingleDeviceQuery;
import static org.thingsboard.server.common.data.util.JasperReportUtils.addSubReport;
import static org.thingsboard.server.common.data.util.JasperReportUtils.addColumnHeader;
import static org.thingsboard.server.common.data.util.JasperReportUtils.addHeading;
import static org.thingsboard.server.common.data.util.JasperReportUtils.addPageFooter;
import static org.thingsboard.server.common.data.util.JasperReportUtils.addPageHeader;
import static org.thingsboard.server.common.data.util.JasperReportUtils.addRichText;
import static org.thingsboard.server.common.data.util.JasperReportUtils.addTableDetailBand;
import static org.thingsboard.server.common.data.util.JasperReportUtils.createField;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultReportService extends AbstractTbEntityService implements ReportService {

    private SimpleDateFormat defaultDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
    private Map<ReportComponentType, Function<ReportComponent, JasperReport>> rendererMap;

    private final ReportTemplateService reportTemplateService;
    private final DashboardReportService dashboardReportService;
    private final EntityService entityService;

    @PostConstruct
    public void init() {
        rendererMap = new EnumMap<>(ReportComponentType.class);
        rendererMap.put(ReportComponentType.HEADING, component -> renderHeading((HeadingComponent) component));
        rendererMap.put(ReportComponentType.RICH_TEXT, component -> renderRichText((RichTextComponent) component));
        rendererMap.put(ReportComponentType.ENTITY_TABLE, component -> renderEntityTable((EntityTableComponent) component));
    }

    @Override
    public ListenableFuture<ReportData> generateReport(TenantId tenantId, CustomerId customerId, ReportRequest reportRequest, MergedUserPermissions userPermissions) throws ThingsboardException {
        log.trace("[{}] Executing generateReport, reportRequest [{}]", tenantId, reportRequest);
        ReportTemplate reportTemplate = checkNotNull(reportTemplateService.findReportTemplateById(tenantId, reportRequest.getTemplateId()));
        ReportTemplateConfiguration configuration = reportTemplate.getConfiguration();

        SettableFuture<ReportData> resultFuture = SettableFuture.create();
        try {
            TbReportCtx tbReportCtx = new TbReportCtx(tenantId, customerId, userPermissions, configuration);
            JasperDesign jasperDesign = JasperReportUtils.initMainDesign(configuration);
            Map<String, Object> params = new HashMap<>();

            Optional.ofNullable(configuration.getHeader())
                    .ifPresent(header -> addPageHeader(jasperDesign, configuration.getHeader()));
            Optional.ofNullable(configuration.getFooter())
                    .ifPresent(header -> addPageFooter(jasperDesign, configuration.getFooter()));

            int componentIndex = 0;
            for (ReportComponent component : configuration.getComponents()) {
                Function<ReportComponent, JasperReport> renderFunction = rendererMap.get(component.getType());
                if (renderFunction != null) {
                    JRMapCollectionDataSource dataSource = fetchDataSource(tbReportCtx, component.getDataSources());

                    JasperReport subReport = renderFunction.apply(component);

                    String subReportExpression = "component_" + component.getType().name() + componentIndex;
                    String subReportDSExpression = "componentDS_" + component.getType().name() + componentIndex;

                    params.put(subReportExpression, subReport);
                    params.put(subReportDSExpression, dataSource);

                    addSubReport(jasperDesign, subReportExpression, subReportDSExpression);
                }
            }

            JasperReport mainReport = JasperCompileManager.compileReport(jasperDesign);
            JasperPrint print = JasperFillManager.fillReport(mainReport, params, new JREmptyDataSource());

            ReportData report = ReportData.builder()
                    .data(JasperExportManager.exportReportToPdf(print))
                    .contentType(reportRequest.getReportType().getContentType())
                    .name(configuration.getFileName() + defaultDateFormat.format(new Date()) + ".pdf")
                    .build();
            resultFuture.set(report);
        } catch (JRException e) {
            log.error("Unexpected error during report generation!", e);
            throw new ThingsboardException("Unable to generate report", ExceptionUtils.getRootCause(e), ThingsboardErrorCode.GENERAL);
        }
        return resultFuture;
    }

    private JasperReport renderHeading(HeadingComponent component) {
        JasperDesign headingDesign = JasperReportUtils.initComponentDesign();
        // define fields
        List<DataKey> dataKeys = getDataKeys(component.getDataSources());
        try {
            for (DataKey dataKey : dataKeys) {
                headingDesign.addField(createField(dataKey.getName(), String.class));
            }
            addHeading(headingDesign, component.getValue());
            return JasperCompileManager.compileReport(headingDesign);
        } catch (JRException e) {
            throw new RuntimeException(e);
        }
    }

    private JasperReport renderRichText(RichTextComponent component) {
        try {
            JasperDesign richTextDesign = JasperReportUtils.initComponentDesign();
            addRichText(richTextDesign, component.getValue());
            return JasperCompileManager.compileReport(richTextDesign);
        } catch (JRException e) {
            throw new RuntimeException(e);
        }
    }

    private JasperReport renderEntityTable(EntityTableComponent component) {
        List<DataKey> dataKeys = getDataKeys(component.getDataSources());
        List<String> entityKeys = dataKeys.stream().map(DataKey::getName).toList();
        List<String> columsHeaders = dataKeys.stream().map(DataKey::getLabel).toList();

        try {
            JasperDesign tableDesign = JasperReportUtils.initComponentDesign();
            addColumnHeader(tableDesign, columsHeaders);
            addTableDetailBand(tableDesign, entityKeys);
            return JasperCompileManager.compileReport(tableDesign);
        } catch (JRException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<DataKey> getDataKeys(List<DataSource> dataSources) {
        return dataSources.stream()
                .map(DataSource::getDataKeys)
                .flatMap(Collection::stream)
                .toList();
    }

    private JRMapCollectionDataSource fetchDataSource(TbReportCtx tbReportCtx, List<DataSource> dataSources) {
        if (dataSources == null) {
            return new JRMapCollectionDataSource(new ArrayList<>());
        }
        Collection<Map<String, ?>> entryList = new ArrayList<>();
        for (DataSource dataSource : dataSources) {
            Collection<Map<String, ?>> dataMap = fetchDataSource(tbReportCtx, dataSource);
            entryList.addAll(dataMap);
        }
        return new JRMapCollectionDataSource(entryList);
    }

    private Collection<Map<String, ?>> fetchDataSource(TbReportCtx tbReportCtx, DataSource dataSource) {
        return switch (dataSource.getType()) {
            case "device" -> fetchEntityData(tbReportCtx, toSingleDeviceQuery(dataSource, tbReportCtx.getFilters()));
            case "entity" -> fetchEntityData(tbReportCtx, toEntityDataQuery(dataSource, tbReportCtx.getEntityAliases(), tbReportCtx.getFilters()));
            case "entityCount" -> Collections.emptyList();
            case "alarmCount" -> Collections.emptyList();
            default -> throw new IllegalArgumentException("Unknown data source type: " + dataSource.getType());
        };
    }

    private Collection<Map<String, ?>> fetchEntityData(TbReportCtx tbReportCtx, EntityDataQuery entityDataQuery) {
        PageData<EntityData> queryResult = entityService.findEntityDataByQuery(tbReportCtx.getTenantId(), tbReportCtx.getCustomerId(),
                    tbReportCtx.getUserPermissions(), entityDataQuery);
        return collectData(queryResult);
    }

    private static Collection<Map<String, ?>> collectData(PageData<EntityData> result) {
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

}
