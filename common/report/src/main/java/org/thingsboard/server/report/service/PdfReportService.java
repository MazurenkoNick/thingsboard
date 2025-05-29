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
import org.thingsboard.server.common.data.id.ReportTemplateId;
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
import org.thingsboard.server.common.data.report.configuration.components.ErrorComponent;
import org.thingsboard.server.common.data.report.configuration.components.ImageComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.SubReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.common.data.report.configuration.image.ImageSourceType;
import org.thingsboard.server.common.data.report.configuration.style.Insets;
import org.thingsboard.server.common.data.report.configuration.style.PageOrientation;
import org.thingsboard.server.common.data.report.configuration.style.PageSize;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.context.HeaderFooterRenderLayout;
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
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.ERROR;
import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.SUB_REPORT;
import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.TIME_SERIES_TABLE;
import static org.thingsboard.server.common.data.report.configuration.style.PageSize.A4;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toAlarmCountQuery;
import static org.thingsboard.server.common.data.util.ReportQueryUtils.toEntityCountQuery;
import static org.thingsboard.server.report.util.ReportUtils.getSingleDataSource;
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
        int usablePageWidthPx = (int)((pageSize.width - pageMargins.getLeft() - pageMargins.getRight()) * 4f / 3f);

        ITextRenderer renderer = HtmlRenderUtils.createRenderer(dataService, ctx, usablePageWidthPx);

        HeaderFooterRenderLayout headerLayout = renderHeaderFooter(renderer, ctx, configuration.getHeader(), usablePageWidthPx);
        HeaderFooterRenderLayout footerLayout = renderHeaderFooter(renderer, ctx, configuration.getFooter(), usablePageWidthPx);

        Map<String, Object> reportVariables = new HashMap<>();
        fillPageLayoutVariables(reportVariables, configuration, headerLayout, footerLayout, pageSize, pageMargins);

        reportVariables.put("pageContent", renderContent(ctx, configuration.getComponents(), null));

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
            String htmlContent = renderContent(ctx, headerFooter.getComponents(), null);
            headerFooterRenderLayout.setHtmlContent(htmlContent);
            int heightPx = HtmlRenderUtils.measureHtmlHeight(renderer, htmlContent, usablePageWidthPx);
            headerFooterRenderLayout.setHeightPx(heightPx);
        }
        headerFooterRenderLayout.setFirstPageEnabled(headerFooter.getFirstPage() != null && headerFooter.getFirstPage().isEnabled());
        if (headerFooterRenderLayout.isFirstPageEnabled()) {
            String htmlContent = renderContent(ctx, headerFooter.getFirstPage().getComponents(), null);
            headerFooterRenderLayout.setFirstPageHtmlContent(htmlContent);
            int heightPx = HtmlRenderUtils.measureHtmlHeight(renderer, htmlContent, usablePageWidthPx);
            headerFooterRenderLayout.setFirstPageHeightPx(heightPx);
        }
        return headerFooterRenderLayout;
    }

    private String renderContent(TbReportCtx ctx, List<ReportComponent> components, EntityData stateEntity) {
        StringBuilder content = new StringBuilder();
        for (ReportComponent component : components) {
            ReportComponentType type = component.getType();
            if (type == SUB_REPORT) {
                content.append(renderSubreport(ctx, component));
            } else if (type == TIME_SERIES_TABLE) {
                content.append(renderTimeseriesTables(ctx, stateEntity, component));
            } else {
                content.append(renderComponent(ctx, component, stateEntity));
            }
        }
        return content.toString();
    }

    private String renderComponent(TbReportCtx ctx, ReportComponent component, EntityData stateEntity) {
        try {
            ComponentData componentData = getComponentData(ctx, component, stateEntity);
            return componentsRenderers.get(component.getType()).render(component, componentData);
        } catch (Exception e) {
            log.error("Failed to render component of type [{}]", component.getType(), e);
            return renderError("Failed to render component of type: " + component.getType(), e);
        }
    }

    private ComponentData getComponentData(TbReportCtx ctx, ReportComponent component, EntityData stateEntity) {
        return switch (component.getType()) {
            case TIME_SERIES_TABLE ->
                    new ComponentData(fetchEntityTsData(ctx, (TimeseriesTableComponent) component, stateEntity.getEntityId()));
            case ALARM_TABLE -> new ComponentData(fetchAlarmDatas(ctx, (AlarmTableComponent) component));
            case DASHBOARD -> buildDashboardComponentData(ctx, ((DashboardComponent) component));
            case IMAGE -> buildImageComponentData(ctx, ((ImageComponent) component));
            default -> buildMultipleDataSourceData(ctx, component.getDataSources(), stateEntity);
        };
    }

    private String renderTimeseriesTables(TbReportCtx ctx, EntityData stateEntity, ReportComponent component) {
        StringBuilder content = new StringBuilder();
        Optional<DataSource> dataSource = getSingleDataSource(component);
        if (dataSource.isEmpty()) {
            return renderError("Data source is not configured for Subreport");
        }
        List<EntityData> entityDatas = fetchEntities(ctx, dataSource.get(), stateEntity);
        for (EntityData entity : entityDatas) {
            content.append(renderComponent(ctx, component, entity));
        }
        return content.toString();
    }

    private String renderSubreport(TbReportCtx ctx, ReportComponent component) {
        ReportTemplateId templateId = ((SubReportComponent) component).getTemplateId();
        if (templateId == null) {
            return renderError("Report template id is not configured for Subreport");
        }
        StringBuilder content = new StringBuilder();
        try {
            Optional<DataSource> dataSource = getSingleDataSource(component);
            if (dataSource.isEmpty()) {
                return renderError("Data source is not configured for Subreport");
            }
            ReportTemplate reportTemplate = dataService.findReportTemplate(templateId, ctx);
            if (reportTemplate == null) {
                return renderError("Template with id " + templateId + " not found. Please check the configuration.");
            }
            PdfReportTemplateConfig reportConfiguration = (PdfReportTemplateConfig) reportTemplate.getConfiguration();

            TbReportCtx subReportCtx = ctx.createSubReportCxt(reportConfiguration);
            List<EntityData> entityDatas = fetchEntities(ctx, dataSource.get(), null);
            for (EntityData entity : entityDatas) {
                content.append(renderContent(subReportCtx, reportConfiguration.getComponents(), entity));
            }
            return content.toString();
        } catch (Exception e) {
            log.error("Failed to render Subreport, template id: {}", templateId, e);
            return renderError("Failed to render sub-report " + templateId, e);
        }
    }

    private String renderError(String errorMessage) {
        return renderError(errorMessage, null);
    }

    private String renderError(String errorMessage, Exception e) {
        return componentsRenderers.get(ERROR).render(new ErrorComponent(errorMessage, e), null);
    }

    private ComponentData buildImageComponentData(TbReportCtx ctx, ImageComponent component) {
        if (ImageSourceType.entityKey.equals(component.getSourceType())) {
            Optional<DataSource> dataSource = getSingleDataSource(component);
            if (dataSource.isEmpty()) {
                return new ComponentData();
            }
            return buildSingleComponentData(ctx, dataSource.get(), null);
        } else {
            return new ComponentData();
        }
    }

    private ComponentData buildDashboardComponentData(TbReportCtx ctx, DashboardComponent component) {
        SettableFuture<DashboardReportData> futureToSet = SettableFuture.create();
        webReportClient.requestDashboardReport(component.getConfig(), null,
                ctx.getAccessToken(), ctx.getAccessTokenExpTs(),
                futureToSet::set, error -> {
                    log.error("Failed to generate dashboard report", error);
                    futureToSet.setException(error);
                });
        try {
            return new ComponentData(futureToSet.get().getData());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private ComponentData buildMultipleDataSourceData(TbReportCtx ctx, List<DataSource> dataSources, EntityData stateEntity) {
        if (dataSources == null || dataSources.isEmpty()) {
            return new ComponentData();
        }
        ComponentData mainDataSource = new ComponentData();
        for (DataSource dataSource : dataSources) {
            ComponentData singleDataSource = buildSingleComponentData(ctx, dataSource, stateEntity);
            mainDataSource.merge(singleDataSource);
        }
        return mainDataSource;
    }

    private ComponentData buildSingleComponentData(TbReportCtx ctx, DataSource dataSource, EntityData stateEntity) {
        ReportTemplateConfig configuration = ctx.getConfiguration();
        return switch (dataSource.getType()) {
            case "device", "entity" -> new ComponentData(dataSource, fetchEntityDatas(ctx, dataSource, stateEntity));
            case "entityCount" -> buildEntityCountDataSource(ctx, dataSource, configuration);
            case "alarmCount" -> buildAlarmCountDataSource(ctx, dataSource, configuration);
            default -> throw new IllegalArgumentException("Unknown data source type: " + dataSource.getType());
        };
    }

    private ComponentData buildEntityCountDataSource(TbReportCtx ctx, DataSource dataSource, ReportTemplateConfig configuration) {
        Map<String, Object> map = new HashMap<>();
        map.put("count", dataService.countEntitiesByQuery(toEntityCountQuery(dataSource, configuration), ctx));
        return new ComponentData(map);
    }

    private ComponentData buildAlarmCountDataSource(TbReportCtx ctx, DataSource dataSource, ReportTemplateConfig configuration) {
        Map<String, Object> map = new HashMap<>();
        map.put("count", dataService.countAlarmsByQuery(toAlarmCountQuery(dataSource, configuration), ctx));
        return new ComponentData(map);
    }

    private Dimension computePageSize(PdfReportTemplateConfig config) {
        PageSize pageSize = Optional.ofNullable(config.getPageSize()).orElse(A4);
        return config.getPageOrientation() == PageOrientation.LANDSCAPE
                ? new Dimension(pageSize.getHeight(), pageSize.getWidth())
                : new Dimension(pageSize.getWidth(), pageSize.getHeight());
    }

    private Insets computePageMargins(PdfReportTemplateConfig configuration) {
        if (configuration.getPageMargins() != null) {
            return configuration.getPageMargins();
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
        reportVariables.put("pageMarginLeft", pageMargins.getLeft() + "pt");
        reportVariables.put("pageMarginRight", pageMargins.getRight() + "pt");

        String pageBackground = configuration.getPageBackground() != null ? ColorUtils.normalizeCssColor(configuration.getPageBackground()) : "#fff";
        reportVariables.put("pageBackground", pageBackground);

        int minContentHeight = 100;

        int minHalfPageContentHeight = Math.max((pageSize.height - pageMargins.getTop() - pageMargins.getBottom() - minContentHeight) / 2, 0);
        int maxTopMargin = pageMargins.getTop() + minHalfPageContentHeight;
        int maxBottomMargin = pageMargins.getBottom() + minHalfPageContentHeight;

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
        int startMargin = headerElseFooter ? pageMargins.getTop() : pageMargins.getBottom();
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
