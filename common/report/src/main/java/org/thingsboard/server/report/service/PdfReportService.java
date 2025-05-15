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
import org.springframework.stereotype.Service;
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
import org.thingsboard.server.common.data.report.configuration.style.PageOrientation;
import org.thingsboard.server.common.data.report.configuration.style.PageSize;
import org.thingsboard.server.report.context.ComponentLayout;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.renderer.ReportComponentRenderer;
import org.thingsboard.server.report.util.PdfReportUserAgent;
import org.thingsboard.server.report.util.ThymeleafUtil;
import org.thingsboard.server.report.util.WebReportClient;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.lowagie.text.pdf.BaseFont.IDENTITY_H;
import static org.apache.commons.codec.CharEncoding.UTF_8;
import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.SUB_REPORT;
import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.TIME_SERIES_TABLE;
import static org.thingsboard.server.common.data.report.configuration.style.PageSize.A4;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toAlarmCountQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toEntityCountQuery;
import static org.thingsboard.server.report.context.ComponentLayout.getSingleDataSource;
import static org.thingsboard.server.report.util.ReportUtils.prepareReportName;
import static org.xhtmlrenderer.pdf.ITextRenderer.DEFAULT_DOTS_PER_POINT;

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

        Map<String, Object> reportVariables = new HashMap<>();
        fillReportVariables(reportVariables, configuration);

        reportVariables.put("pageHeader", renderHeader(configuration.getHeader()));
        reportVariables.put("pageFooter", renderFooter(configuration.getHeader()));
        reportVariables.put("pageContent", renderContent(ctx, new ComponentLayout(), configuration.getComponents()));

        String renderedHtmlContent = ThymeleafUtil.render("html/report-template", reportVariables);
        String xHtml = convertToXhtml(renderedHtmlContent);

        ITextRenderer renderer = new ITextRenderer(new ITextOutputDevice(DEFAULT_DOTS_PER_POINT), new PdfReportUserAgent());
        renderer.getFontResolver().addFont("fonts/roboto/roboto.ttf",
                "Roboto", IDENTITY_H, true, null);
        renderer.getFontResolver().addFont("fonts/roboto/robotoitalic.ttf",
                "Roboto", IDENTITY_H, true, null);
        renderer.getFontResolver().addFont("fonts/monospace/dejavusansmono.ttf",
                "monospace", IDENTITY_H, true, null);
        renderer.getFontResolver().addFont("fonts/monospace/dejavusansmonobold.ttf",
                "monospace", IDENTITY_H, true, null);

        renderer.setDocumentFromString(xHtml);
        renderer.layout();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            renderer.createPDF(outputStream);
            byte[] reportBytes = outputStream.toByteArray();
            String requestTimeZone = task.getTimezone();
            TimeZone timeZone = (requestTimeZone == null) ? TimeZone.getDefault() : TimeZone.getTimeZone(requestTimeZone);
            String reportName = prepareReportName(configuration.getNamePattern(), new Date(), timeZone);

            return ReportData.builder()
                    .data(reportBytes)
                    .contentType(configuration.getFormat().getContentType())
                    .name(reportName)
                    .build();
        }
    }

    private String convertToXhtml(String html) throws UnsupportedEncodingException {
        Tidy tidy = new Tidy();
        tidy.setInputEncoding(UTF_8);
        tidy.setOutputEncoding(UTF_8);
        tidy.setXHTML(true);
        tidy.setTrimEmptyElements(false);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(html.getBytes(UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        tidy.parseDOM(inputStream, outputStream);
        return outputStream.toString(UTF_8);
    }

    private String renderHeader(HeaderFooter headerFooter) throws Exception {
        boolean hasComponents = headerFooter.isEnabled() && headerFooter.getComponents() != null
                && !headerFooter.getComponents().isEmpty();
        boolean firstPageHeaderEnabled = headerFooter.getFirstPage() != null &&
                headerFooter.getFirstPage().isEnabled();
//        if (firstPageHeaderEnabled && !headerFooter.getFirstPage().getComponents().isEmpty()) {
//            renderContent(ctx, parentLayout, headerFooter.getFirstPage().getComponents(), false, printWhenExpression, () -> headerContainer);
//
//            JRDesignTextField pageNumberField = createJRTextField(parentLayout.getUsablePageWidth());
//            pageNumberField.setPrintWhenExpression(printWhenExpression);
//            pageNumberField.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
//            pageNumberField.setEvaluationTime(EvaluationTimeEnum.MASTER);
//            pageNumberField.setExpression(new JRDesignExpression("$V{PAGE_NUMBER} + \" / \" + $V{MASTER_TOTAL_PAGES}"));
//            headerContainer.addElement(pageNumberField);
//        }
//        if (hasComponents) {
//            JRExpression printWhenExpression = firstPageHeaderEnabled ? new JRDesignExpression("$V{PAGE_NUMBER} > 1") : null;
//            renderContent(ctx, parentLayout, headerFooter.getComponents(), true, printWhenExpression, () -> headerContainer);
//
//            JRDesignTextField pageNumberField = createJRTextField(parentLayout.getUsablePageWidth());
//            pageNumberField.setPrintWhenExpression(printWhenExpression);
//            pageNumberField.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
//            pageNumberField.setEvaluationTime(EvaluationTimeEnum.MASTER);
//            pageNumberField.setExpression(new JRDesignExpression("$V{PAGE_NUMBER} + \" / \" + $V{MASTER_TOTAL_PAGES}"));
//            headerContainer.addElement(pageNumberField);
//        }
        return "<p>This is header</p>";
    }

    private String renderFooter(HeaderFooter headerFooter) {
        return "<p>This is footer <span class='page-number'></span> / <span class='page-count'></span></p>";
    }


    private String renderContent(TbReportCtx ctx, ComponentLayout componentLayout, List<ReportComponent> components) {
        StringBuilder content = new StringBuilder();
        for (ReportComponent component : components) {
            if (component.getType() == TIME_SERIES_TABLE || component.getType() == SUB_REPORT) { // check if component is complex
                List<EntityData> entityDatas = fetchEntities(ctx, getSingleDataSource(component));
                for (EntityData entityData : entityDatas) {
                    content.append(renderComponent(ctx, componentLayout, component, entityData));
                }
            } else {
                content.append(renderComponent(ctx, componentLayout, component, null));
            }
        }
        return content.toString();
    }

    private String renderComponent(TbReportCtx ctx, ComponentLayout parentComponentLayout, ReportComponent component, EntityData entityData)  {
        Map<String, Object> variables = buildComponentContext(ctx, component, entityData);
        return componentsRenderers.get(component.getType()).render(parentComponentLayout, component, variables);
    }

    private Map<String, Object> buildComponentContext(TbReportCtx ctx, ReportComponent component, EntityData entityData) {
        return switch (component.getType()) {
            case TIME_SERIES_TABLE ->
                    Map.of("data", buildTsDataSource(ctx, ((TimeseriesTableComponent) component), entityData.getEntityId()));
            case ALARM_TABLE ->  Map.of("data", buildAlarmDataSource(ctx, ((AlarmTableComponent) component)));
            case DASHBOARD -> buildDashboardDataSource(ctx, ((DashboardComponent) component));
            default -> buildMultipleDataSource(ctx, component.getDataSources());
        };
    }

    private Map<String, Object> buildDashboardDataSource(TbReportCtx ctx, DashboardComponent component) {
        SettableFuture<DashboardReportData> futureToSet = SettableFuture.create();
        webReportClient.requestDashboardReport(component.getConfig(), null,
                ctx.getAccessToken(), ctx.getAccessTokenExpTs(),
                futureToSet::set, error -> {
                    log.error("Failed to generate dashboard report", error);
                    futureToSet.setException(error);
                });
        try {
            return Map.of("dashboardData", futureToSet.get());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> buildMultipleDataSource(TbReportCtx ctx, List<DataSource> dataSources) {
        if (dataSources == null || dataSources.isEmpty()) {
            return new HashMap<>();
        }
        Collection<Map<String, ?>> entryList = new ArrayList<>();
        for (DataSource dataSource : dataSources) {
            Collection<Map<String, ?>> dataMap = buildSingleDataSource(ctx, dataSource);
            entryList.addAll(dataMap);
        }
        return Map.of("data", entryList);
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

    private void fillReportVariables(Map<String, Object> reportVariables, PdfReportTemplateConfig configuration) {
        PageSize pageSize = configuration.getPageSize();
        if (pageSize == null) {
            pageSize = A4;
        }
        if (configuration.getPageOrientation() == PageOrientation.LANDSCAPE) {
            reportVariables.put("pageWidth", pageSize.getHeight() + "pt" );
            reportVariables.put("pageHeight", pageSize.getWidth() + "pt" );
        } else {
            reportVariables.put("pageWidth", pageSize.getWidth() + "pt" );
            reportVariables.put("pageHeight", pageSize.getHeight() + "pt" );
        }
        String pageBackground = configuration.getPageBackground() != null ? configuration.getPageBackground() : "#fff";
        reportVariables.put("pageBackground", pageBackground);
        if (configuration.getPageMargins() != null) {
            reportVariables.put("pageMarginLeft", configuration.getPageMargins().getLeft() + "pt");
            reportVariables.put("pageMarginRight", configuration.getPageMargins().getRight() + "pt");
            reportVariables.put("pageMarginTop", configuration.getPageMargins().getTop() + "pt");
            reportVariables.put("pageMarginBottom", configuration.getPageMargins().getBottom() + "pt");
        } else {
            reportVariables.put("pageMarginLeft", "20pt");
            reportVariables.put("pageMarginRight", "20pt");
            reportVariables.put("pageMarginTop", "20pt");
            reportVariables.put("pageMarginBottom", "20pt");
        }
    }

    @Override
    public TbReportFormat getFormat() {
        return TbReportFormat.PDF;
    }

}
