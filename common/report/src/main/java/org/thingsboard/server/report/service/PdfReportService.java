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
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
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
import net.sf.jasperreports.engine.type.EvaluationTimeEnum;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
//import net.sf.jasperreports.engine.util.HtmlPrintElementUtils;
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
import org.thingsboard.server.common.data.report.configuration.style.PageOrientation;
import org.thingsboard.server.common.data.report.configuration.style.PageSize;
import org.thingsboard.server.report.context.ReportLayout;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.datasource.AutoRewindableDataSource;
import org.thingsboard.server.report.renderer.ReportComponentRenderer;
import org.thingsboard.server.report.util.PdfReportUserAgent;
import org.thingsboard.server.report.util.RichTextHtmlPrintElementFactory;
import org.thingsboard.server.report.util.WebReportClient;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
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
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.lowagie.text.pdf.BaseFont.IDENTITY_H;
import static org.apache.commons.codec.CharEncoding.UTF_8;
import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.SUB_REPORT;
import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.TIME_SERIES_TABLE;
import static org.thingsboard.server.common.data.report.configuration.style.PageSize.A4;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toAlarmCountQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toEntityCountQuery;
import static org.thingsboard.server.report.context.ReportLayout.getSingleDataSource;
import static org.thingsboard.server.report.util.JasperReportUtils.createJRTextField;
import static org.thingsboard.server.report.util.JasperReportUtils.prepareReportName;
import static org.thymeleaf.templatemode.TemplateMode.HTML;
import static com.lowagie.text.pdf.BaseFont.EMBEDDED;
import static org.xhtmlrenderer.pdf.ITextRenderer.DEFAULT_DOTS_PER_POINT;

@Service
@Slf4j
public class PdfReportService extends AbstractReportService {

    //static {
    //    DefaultJasperReportsContext.getInstance().setProperty(HtmlPrintElementUtils.PROPERTY_HTML_PRINTELEMENT_FACTORY, RichTextHtmlPrintElementFactory.class.getName());
    //}

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

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(HTML);
        templateResolver.setCharacterEncoding(UTF_8);
        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        Map<String, Object> variables = new HashMap<>();
        PageSize pageSize = configuration.getPageSize();
        if (pageSize == null) {
            pageSize = A4;
        }
        if (configuration.getPageOrientation() == PageOrientation.LANDSCAPE) {
            variables.put("pageWidth", pageSize.getHeight() + "pt" );
            variables.put("pageHeight", pageSize.getWidth() + "pt" );
        } else {
            variables.put("pageWidth", pageSize.getWidth() + "pt" );
            variables.put("pageHeight", pageSize.getHeight() + "pt" );
        }
        String pageBackground = configuration.getPageBackground() != null ? configuration.getPageBackground() : "#fff";
        variables.put("pageBackground", pageBackground);
        if (configuration.getPageMargins() != null) {
            variables.put("pageMarginLeft", configuration.getPageMargins().getLeft() + "pt");
            variables.put("pageMarginRight", configuration.getPageMargins().getRight() + "pt");
            variables.put("pageMarginTop", configuration.getPageMargins().getTop() + "pt");
            variables.put("pageMarginBottom", configuration.getPageMargins().getBottom() + "pt");
        } else {
            variables.put("pageMarginLeft", "20pt");
            variables.put("pageMarginRight", "20pt");
            variables.put("pageMarginTop", "20pt");
            variables.put("pageMarginBottom", "20pt");
        }


        variables.put("pageContent", "<p>My super content</p><div class='page-break'></div><p class='page-break'>My next page super content</p>");

        variables.put("pageHeader", "<p>This is header</p>");

        variables.put("pageFooter", "<p>This is footer <span class='page-number'></span> / <span class='page-count'></span></p>");

        Context context = new Context(Locale.getDefault(), variables);
        String renderedHtmlContent = templateEngine.process("html/report-template", context);
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

     /*   ReportLayout layoutCtx = new ReportLayout(configuration);

        renderHeaderFooter(ctx, layoutCtx, configuration.getHeader(), true);
        renderHeaderFooter(ctx, layoutCtx, configuration.getFooter(), false);

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
                .build();*/
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

    private void renderHeaderFooter(TbReportCtx ctx,
                                    ReportLayout parentLayout,
                                    HeaderFooter headerFooter,
                                    boolean headerElseFooter) throws Exception {
        JRDesignFrame headerContainer = parentLayout.createHeaderFooter(headerElseFooter);
        boolean hasComponents = headerFooter.isEnabled() && headerFooter.getComponents() != null
                && !headerFooter.getComponents().isEmpty();
        boolean firstPageHeaderEnabled = headerFooter.getFirstPage() != null &&
                headerFooter.getFirstPage().isEnabled();
        if (firstPageHeaderEnabled && !headerFooter.getFirstPage().getComponents().isEmpty()) {
            JRExpression printWhenExpression = new JRDesignExpression("$V{PAGE_NUMBER} == 1");
            renderContent(ctx, parentLayout, headerFooter.getFirstPage().getComponents(), false, printWhenExpression, () -> headerContainer);

            JRDesignTextField pageNumberField = createJRTextField(parentLayout.getUsablePageWidth());
            pageNumberField.setPrintWhenExpression(printWhenExpression);
            pageNumberField.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
            pageNumberField.setEvaluationTime(EvaluationTimeEnum.MASTER);
            pageNumberField.setExpression(new JRDesignExpression("$V{PAGE_NUMBER} + \" / \" + $V{MASTER_TOTAL_PAGES}"));
            headerContainer.addElement(pageNumberField);
        }
        if (hasComponents) {
            JRExpression printWhenExpression = firstPageHeaderEnabled ? new JRDesignExpression("$V{PAGE_NUMBER} > 1") : null;
            renderContent(ctx, parentLayout, headerFooter.getComponents(), true, printWhenExpression, () -> headerContainer);

            JRDesignTextField pageNumberField = createJRTextField(parentLayout.getUsablePageWidth());
            pageNumberField.setPrintWhenExpression(printWhenExpression);
            pageNumberField.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
            pageNumberField.setEvaluationTime(EvaluationTimeEnum.MASTER);
            pageNumberField.setExpression(new JRDesignExpression("$V{PAGE_NUMBER} + \" / \" + $V{MASTER_TOTAL_PAGES}"));
            headerContainer.addElement(pageNumberField);
        }
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
