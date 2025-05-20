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
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportData;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.HeaderFooter;
import org.thingsboard.server.common.data.report.configuration.PdfReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.DashboardComponent;
import org.thingsboard.server.common.data.report.configuration.components.ImageComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.SubReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.common.data.report.configuration.style.Margins;
import org.thingsboard.server.common.data.report.configuration.style.PageOrientation;
import org.thingsboard.server.common.data.report.configuration.style.PageSize;
import org.thingsboard.server.report.context.ComponentLayout;
import org.thingsboard.server.report.context.HeaderFooterRenderLayout;
import org.thingsboard.server.report.context.ReportDataSource;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.renderer.ReportComponentRenderer;
import org.thingsboard.server.report.util.ColorUtils;
import org.thingsboard.server.report.util.HtmlRenderUtils;
import org.thingsboard.server.report.util.ThymeleafUtil;
import org.thingsboard.server.report.util.WebReportClient;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.SUB_REPORT;
import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.TIME_SERIES_TABLE;
import static org.thingsboard.server.common.data.report.configuration.style.PageSize.A4;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toAlarmCountQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toEntityCountQuery;
import static org.thingsboard.server.report.util.ReportUtils.prepareReportName;

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

        Dimension pageSize = computePageSize(configuration);
        Insets pageMargins = computePageMargins(configuration);
        int usablePageWidthPx = (int)((pageSize.width - pageMargins.left - pageMargins.right) * 4f / 3f);

        ITextRenderer renderer = HtmlRenderUtils.createRenderer();

        HeaderFooterRenderLayout headerLayout = renderHeaderFooter(renderer, ctx, configuration.getHeader(), usablePageWidthPx);
        HeaderFooterRenderLayout footerLayout = renderHeaderFooter(renderer, ctx, configuration.getFooter(), usablePageWidthPx);

        Map<String, Object> reportVariables = new HashMap<>();
        fillPageLayoutVariables(reportVariables, configuration, headerLayout, footerLayout, pageSize, pageMargins);

        reportVariables.put("pageContent", renderContent(ctx, new ComponentLayout(), configuration.getComponents(), null));

        String renderedHtmlContent = ThymeleafUtil.render("html/report-template", reportVariables);
        String xHtml = HtmlRenderUtils.convertToXhtml(renderedHtmlContent);

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

    private HeaderFooterRenderLayout renderHeaderFooter(ITextRenderer renderer,
                                                        TbReportCtx ctx, HeaderFooter headerFooter,
                                                        int usablePageWidthPx) throws Exception {
        HeaderFooterRenderLayout headerFooterRenderLayout = new HeaderFooterRenderLayout();
        headerFooterRenderLayout.setEnabled(headerFooter.isEnabled());
        if (headerFooter.isEnabled()) {
            String htmlContent = renderContent(ctx, new ComponentLayout(), headerFooter.getComponents(), null);
            headerFooterRenderLayout.setHtmlContent(htmlContent);
            int heightPx = HtmlRenderUtils.measureHtmlHeight(renderer, htmlContent, usablePageWidthPx);
            headerFooterRenderLayout.setHeightPx(heightPx);
        }
        headerFooterRenderLayout.setFirstPageEnabled(headerFooter.getFirstPage() != null && headerFooter.getFirstPage().isEnabled());
        if (headerFooterRenderLayout.isFirstPageEnabled()) {
            String htmlContent = renderContent(ctx, new ComponentLayout(), headerFooter.getFirstPage().getComponents(), null);
            headerFooterRenderLayout.setFirstPageHtmlContent(htmlContent);
            int heightPx = HtmlRenderUtils.measureHtmlHeight(renderer, htmlContent, usablePageWidthPx);
            headerFooterRenderLayout.setFirstPageHeightPx(heightPx);
        }
        return headerFooterRenderLayout;
    }

    private String renderContent(TbReportCtx ctx, ComponentLayout componentLayout, List<ReportComponent> components, EntityData stateEntity) throws ThingsboardException {
        StringBuilder content = new StringBuilder();
        for (ReportComponent component : components) {
            if (component.getType() == SUB_REPORT) {
                ReportTemplateId templateId = ((SubReportComponent) component).getTemplateId();
                ReportTemplate reportTemplate = dataService.findReportTemplate(templateId, ctx).orElseThrow(() -> new IllegalArgumentException("Report template was not found: " + templateId));
                PdfReportTemplateConfig reportConfiguration = (PdfReportTemplateConfig) reportTemplate.getConfiguration();

                List<EntityData> entityDatas = fetchEntities(ctx, getSingleDataSource(component));
                for (EntityData entity : entityDatas) {
                    content.append(renderContent(ctx, componentLayout, reportConfiguration.getComponents(), entity));
                }
            } else if (component.getType() == TIME_SERIES_TABLE) {
                List<EntityData> entityDatas = fetchEntities(ctx, getSingleDataSource(component));
                for (EntityData entity : entityDatas) {
                    content.append(renderComponent(ctx, componentLayout, component, entity));
                }
            } else {
                content.append(renderComponent(ctx, componentLayout, component, stateEntity));
            }
        }
        return content.toString();
    }

    private String renderComponent(TbReportCtx ctx, ComponentLayout parentComponentLayout, ReportComponent component, EntityData stateEntity)  {
        ReportDataSource reportDataSource = buildComponentDataSource(ctx, component, stateEntity);
        ComponentLayout componentLayout = new ComponentLayout(component, parentComponentLayout);
        return componentsRenderers.get(component.getType()).render(componentLayout, component, reportDataSource);
    }

    private ReportDataSource buildComponentDataSource(TbReportCtx ctx, ReportComponent component, EntityData stateEntity) {
        ReportDataSource reportDataSource = switch (component.getType()) {
            case TIME_SERIES_TABLE ->
                    new ReportDataSource(buildTsDataSource(ctx, (TimeseriesTableComponent) component, stateEntity.getEntityId()));
            case ALARM_TABLE -> new ReportDataSource(buildAlarmDataSource(ctx, (AlarmTableComponent) component));
            case DASHBOARD -> buildDashboardDataSource(ctx, ((DashboardComponent) component));
            case IMAGE -> buildImageDataSource(ctx, ((ImageComponent) component));
            default -> buildMultipleDataSource(ctx, component.getDataSources());
        };
        // Merge state entity data into the report variables
        reportDataSource.getVariables().putAll(toStateEntityMap(stateEntity));
        return reportDataSource;
    }

    private ReportDataSource buildImageDataSource(TbReportCtx ctx, ImageComponent component) {
        TbResourceId tbResourceId = component.getTbResourceId();
        TbResource tbResource;
        try {
            tbResource = dataService.findTbResource(tbResourceId, ctx);
        } catch (ThingsboardException e) {
            log.error("Failed to download resource by id: {}", tbResourceId, e);
            throw new RuntimeException("Failed to find resource by id: " + tbResourceId, e);
        }
        return new ReportDataSource(tbResource.getData());
    }

    private ReportDataSource buildDashboardDataSource(TbReportCtx ctx, DashboardComponent component) {
        SettableFuture<DashboardReportData> futureToSet = SettableFuture.create();
        webReportClient.requestDashboardReport(component.getConfig(), null,
                ctx.getAccessToken(), ctx.getAccessTokenExpTs(),
                futureToSet::set, error -> {
                    log.error("Failed to generate dashboard report", error);
                    futureToSet.setException(error);
                });
        try {
            return new ReportDataSource(futureToSet.get().getData());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private ReportDataSource buildMultipleDataSource(TbReportCtx ctx, List<DataSource> dataSources) {
        if (dataSources == null || dataSources.isEmpty()) {
            return new ReportDataSource();
        }
        ReportDataSource mainDataSource = new ReportDataSource();
        for (DataSource dataSource : dataSources) {
            ReportDataSource singleDataSource = buildSingleDataSource(ctx, dataSource);
            mainDataSource.merge(singleDataSource);
        }
        return mainDataSource;
    }

    private ReportDataSource buildSingleDataSource(TbReportCtx ctx, DataSource dataSource) {
        ReportTemplateConfig configuration = ctx.getConfiguration();
        return switch (dataSource.getType()) {
            case "device", "entity" -> buildEntityDataSource(ctx, dataSource);
            case "entityCount" -> buildEntityCountDataSource(ctx, dataSource, configuration);
            case "alarmCount" -> buildAlarmCountDataSource(ctx, dataSource, configuration);
            default -> throw new IllegalArgumentException("Unknown data source type: " + dataSource.getType());
        };
    }

    private ReportDataSource buildEntityDataSource(TbReportCtx ctx, DataSource dataSource) {
        List<Map<String, String>> entityDatas = fetchEntities(ctx, dataSource).stream().map(this::toMap).collect(Collectors.toList());
        return new ReportDataSource(entityDatas);
    }

    private ReportDataSource buildEntityCountDataSource(TbReportCtx ctx, DataSource dataSource, ReportTemplateConfig configuration) {
        Map<String, String> map = new HashMap<>();
        map.put("count", dataService.countEntitiesByQuery(toEntityCountQuery(dataSource, configuration), ctx).toString());
        return new ReportDataSource(map);
    }

    private ReportDataSource buildAlarmCountDataSource(TbReportCtx ctx, DataSource dataSource, ReportTemplateConfig configuration) {
        Map<String, String> map = new HashMap<>();
        map.put("count", dataService.countAlarmsByQuery(toAlarmCountQuery(dataSource, configuration), ctx).toString());
        return new ReportDataSource(map);
    }

    public static DataSource getSingleDataSource(ReportComponent component) {
        List<DataSource> dataSources = component.getDataSources();
        if (dataSources == null || dataSources.isEmpty()) {
            throw new IllegalArgumentException("Data source is required for component: " + component.getType());
        }
        return component.getDataSources().get(0);
    }

    private Dimension computePageSize(PdfReportTemplateConfig configuration) {
        PageSize pageSize = configuration.getPageSize();
        if (pageSize == null) {
            pageSize = A4;
        }
        if (configuration.getPageOrientation() == PageOrientation.LANDSCAPE) {
            return new Dimension(pageSize.getHeight(), pageSize.getWidth());
        } else {
            return new Dimension(pageSize.getWidth(), pageSize.getHeight());
        }
    }

    private Insets computePageMargins(PdfReportTemplateConfig configuration) {
        if (configuration.getPageMargins() != null) {
            Margins margins = configuration.getPageMargins();
            return new Insets(margins.getTop(), margins.getLeft(), margins.getBottom(), margins.getRight());
        } else {
            return new Insets(20, 20, 20, 20);
        }
    }

    private void fillPageLayoutVariables(Map<String, Object> reportVariables,
                                         PdfReportTemplateConfig configuration,
                                         HeaderFooterRenderLayout headerLayout,
                                         HeaderFooterRenderLayout footerLayout,
                                         Dimension pageSize, Insets pageMargins) {
        reportVariables.put("pageWidth", pageSize.getWidth() + "pt" );
        reportVariables.put("pageHeight", pageSize.getHeight() + "pt" );
        reportVariables.put("pageMarginLeft", pageMargins.left + "pt");
        reportVariables.put("pageMarginRight", pageMargins.right + "pt");

        String pageBackground = configuration.getPageBackground() != null ? ColorUtils.normalizeCssColor(configuration.getPageBackground()) : "#fff";
        reportVariables.put("pageBackground", pageBackground);

        int minContentHeight = 100;

        int minHalfPageContentHeight = Math.max((pageSize.height - pageMargins.top - pageMargins.bottom - minContentHeight) / 2, 0);
        int maxTopMargin = pageMargins.top + minHalfPageContentHeight;
        int maxBottomMargin = pageMargins.bottom + minHalfPageContentHeight;

        int pageMarginTop = this.fillHeaderFooterVariables(reportVariables, headerLayout, pageMargins, maxTopMargin, true);
        reportVariables.put("pageMarginTop", pageMarginTop + "pt");

        int pageMarginBottom = this.fillHeaderFooterVariables(reportVariables, footerLayout, pageMargins, maxBottomMargin, false);
        reportVariables.put("pageMarginBottom", pageMarginBottom + "pt");
    }

    private int fillHeaderFooterVariables(Map<String, Object> reportVariables,
                                          HeaderFooterRenderLayout headerFooterLayout,
                                          Insets pageMargins,
                                          int maxMargin,
                                          boolean headerElseFooter) {
        String prefix = headerElseFooter ? "Header" : "Footer";
        String marginPrefix = headerElseFooter ? "Top" : "Bottom";
        reportVariables.put("enable" + prefix, headerFooterLayout.isEnabled());
        int startMargin = headerElseFooter ? pageMargins.top : pageMargins.bottom;
        int margin = startMargin;
        reportVariables.put("page" + prefix + "Padding", startMargin + "pt");
        if (headerFooterLayout.isEnabled()) {
            reportVariables.put("page" + prefix, headerFooterLayout.getHtmlContent());
            margin = Math.min((int)(startMargin + headerFooterLayout.getHeightPx() * 3f / 4f), maxMargin);
            int height = margin - startMargin;
            reportVariables.put("page" + prefix + "Height", height + "pt");
        }
        reportVariables.put("enableFirstPage" + prefix, headerFooterLayout.isFirstPageEnabled());
        if (headerFooterLayout.isFirstPageEnabled()) {
            int firstPageMargin = Math.min((int)(startMargin + headerFooterLayout.getFirstPageHeightPx() * 3f / 4f), maxMargin);
            reportVariables.put("firstPageMargin" + marginPrefix, firstPageMargin + "pt");
            reportVariables.put("firstPage" + prefix, headerFooterLayout.getFirstPageHtmlContent());
            int height = firstPageMargin - startMargin;
            reportVariables.put("firstPage" + prefix + "Height", height + "pt");
        }
        return margin;
    }

    @Override
    public TbReportFormat getFormat() {
        return TbReportFormat.PDF;
    }

}
