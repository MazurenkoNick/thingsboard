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

import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRElementGroup;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExpression;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignFrame;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportData;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.HeaderFooter;
import org.thingsboard.server.common.data.report.configuration.PdfReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.DashboardComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.report.context.ReportLayout;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.datasource.AutoRewindableDataSource;
import org.thingsboard.server.report.renderer.ReportComponentRenderer;
import org.thingsboard.server.report.util.WebReportClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.SUB_REPORT;
import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.TIME_SERIES_TABLE;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toAlarmCountQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toEntityCountQuery;
import static org.thingsboard.server.report.context.ReportLayout.getSingleDataSource;
import static org.thingsboard.server.report.util.JasperReportUtils.createJRTextField;
import static org.thingsboard.server.report.util.JasperReportUtils.prepareReportName;

@Service
@Slf4j
public class PdfReportService extends AbstractReportService {

    private final Map<ReportComponentType, ReportComponentRenderer> componentsRenderers = new EnumMap<>(ReportComponentType.class);
    private final WebReportClient webReportClient;

    private PdfReportService(List<ReportComponentRenderer> renderers, WebReportClient webReportClient) {
        renderers.forEach(renderer -> {
            ReportComponentType type = renderer.getType();
            if (type != null) {
                this.componentsRenderers.put(type, renderer);
            }
        });
        this.webReportClient = webReportClient;
    }

    public ReportData generateReport(ReportTask task, TbReportCtx ctx) throws Exception {
        TenantId tenantId = task.getTenantId();

        log.trace("[{}] Executing generateReport, reportRequest [{}]", tenantId, task);
        PdfReportTemplateConfig configuration = (PdfReportTemplateConfig) task.getReportTemplateConfig();

        ReportLayout layoutCtx = new ReportLayout(configuration);

        renderHeaderFooter(ctx, layoutCtx, configuration.getHeader(), true);
        renderHeaderFooter(ctx, layoutCtx, configuration.getFooter(), false);

        //Optional.ofNullable(configuration.getHeader()).ifPresent(reportBuilder::addPageHeader);
        //Optional.ofNullable(configuration.getFooter()).ifPresent(reportBuilder::addPageFooter);

        renderContent(ctx, layoutCtx, configuration.getComponents(), false, null, () -> layoutCtx.createDetailsBand());

        JasperReport mainReport = JasperCompileManager.compileReport(layoutCtx.getJasperDesign());
        JasperPrint print = JasperFillManager.fillReport(mainReport, ctx.getParams(), new JREmptyDataSource());

        String requestTimeZone = task.getTimezone();
        TimeZone timeZone = (requestTimeZone == null) ? TimeZone.getDefault() : TimeZone.getTimeZone(requestTimeZone);
        String reportName = prepareReportName(configuration.getNamePattern(), new Date(), timeZone);

        return ReportData.builder()
                .data(JasperExportManager.exportReportToPdf(print))
                .contentType(configuration.getFormat().getContentType())
                .name(reportName)
                .build();
    }

    private void renderHeaderFooter(TbReportCtx ctx,
                                    ReportLayout parentLayout,
                                    HeaderFooter headerFooter,
                                    boolean headerElseFooter) throws Exception {
        JRDesignFrame headerContainer = parentLayout.createHeaderFooter(headerElseFooter);
        boolean hasComponents = headerFooter.isEnabled() && headerFooter.getComponents() != null
                && !headerFooter.getComponents().isEmpty();
        if (hasComponents) {
            // JRExpression printWhenExpression = new JRDesignExpression("true");
            renderContent(ctx, parentLayout, headerFooter.getComponents(), true, null, () -> headerContainer);
        }
        // simple way to add page numbering to header/footer
        //if (headerFooter.isPrintPageNumber()) {
            JRDesignTextField pageNumberField = createJRTextField(parentLayout.getUsablePageWidth());
            pageNumberField.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
            pageNumberField.setExpression(new JRDesignExpression("$V{PAGE_NUMBER}"));
            headerContainer.addElement(pageNumberField);
        //}
    }

    private void renderContent(TbReportCtx ctx, ReportLayout parentBuilder, List<ReportComponent> components,
                               boolean autoRewind, JRExpression printWhenExpression, Supplier<JRElementGroup> subreportContainerSupplier) throws Exception {
        for (ReportComponent component : components) {
            if (component.getType() == TIME_SERIES_TABLE || component.getType() == SUB_REPORT) { // check if component is complex
                List<EntityData> entityDatas = fetchEntities(ctx, getSingleDataSource(component));
                for (EntityData entityData : entityDatas) {
                    renderComponent(ctx, parentBuilder, component, subreportContainerSupplier.get(), autoRewind, printWhenExpression, entityData);
                }
            } else {
                renderComponent(ctx, parentBuilder, component, subreportContainerSupplier.get(), autoRewind, printWhenExpression, null);
            }
        }
    }

    private void renderComponent(TbReportCtx ctx, ReportLayout layoutCtx,
                                 ReportComponent component, JRElementGroup container,
                                 boolean autoRewind,
                                 JRExpression printWhenExpression,
                                 EntityData entityData) throws Exception {

        String subReportId = "component_" + StringUtils.randomAlphabetic(10);
        String subReportDSId = "componentDS_" + StringUtils.randomAlphabetic(10);

        layoutCtx.addSubReport(subReportId, subReportDSId, container, printWhenExpression);

        JasperReport subReport = buildJasperReport(layoutCtx, component);
        JRDataSource subReportDS = buildDataSource(ctx, component, entityData);
        if (autoRewind) {
            subReportDS = new AutoRewindableDataSource((JRRewindableDataSource) subReportDS);
        }

        Map<String, Object> params = ctx.getParams();
        params.put(subReportId, subReport);
        params.put(subReportDSId, subReportDS);
    }

    public JasperReport buildJasperReport(ReportLayout parentLayoutCtx, ReportComponent component) throws JRException {
        ReportLayout layoutCtx = new ReportLayout(component, parentLayoutCtx);
        componentsRenderers.get(component.getType()).render(layoutCtx, component);
        return JasperCompileManager.compileReport(layoutCtx.getJasperDesign());
    }

    private JRDataSource buildDataSource(TbReportCtx ctx, ReportComponent component, EntityData entityData) {
        return switch (component.getType()) {
            case TIME_SERIES_TABLE ->
                    new JRMapCollectionDataSource(buildTsDataSource(ctx, ((TimeseriesTableComponent) component), entityData.getEntityId()));
            case ALARM_TABLE -> new JRMapCollectionDataSource(buildAlarmDataSource(ctx, ((AlarmTableComponent) component)));
            case DASHBOARD -> buildDashboardDataSource(ctx, ((DashboardComponent) component));
            default -> buildMultipleDataSource(ctx, component.getDataSources());
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

    private JRMapCollectionDataSource buildMultipleDataSource(TbReportCtx ctx, List<DataSource> dataSources) {
        if (dataSources == null || dataSources.isEmpty()) {
            return new JRMapCollectionDataSource(List.of(Map.of()));
        }
        Collection<Map<String, ?>> entryList = new ArrayList<>();
        for (DataSource dataSource : dataSources) {
            Collection<Map<String, ?>> dataMap = buildSingleDataSource(ctx, dataSource);
            entryList.addAll(dataMap);
        }
        return new JRMapCollectionDataSource(entryList);
    }

    private Collection<Map<String, ?>> buildSingleDataSource(TbReportCtx ctx, DataSource dataSource) {
        ReportTemplateConfig configuration = ctx.getConfiguration();
        return switch (dataSource.getType()) {
            case "device", "entity" -> fetchEntities(ctx, dataSource).stream().map(this::toMap).collect(Collectors.toList());
            case "entityCount" -> List.of(Map.of("count", dataService.countEntitiesByQuery(toEntityCountQuery(dataSource, configuration), ctx)));
            case "alarmCount" -> List.of(Map.of("count", dataService.countAlarmsByQuery(toAlarmCountQuery(dataSource, configuration), ctx)));
            default -> throw new IllegalArgumentException("Unknown data source type: " + dataSource.getType());
        };
    }

    @Override
    public TbReportFormat getFormat() {
        return TbReportFormat.PDF;
    }

}
