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
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportConfig;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportData;
import org.thingsboard.server.common.data.id.EntityId;
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
import org.thingsboard.server.common.data.report.configuration.components.DataReportComponent;
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
import org.thingsboard.server.report.renderer.PdfReportComponentRenderer;
import org.thingsboard.server.report.util.ColorUtils;
import org.thingsboard.server.report.util.HtmlRenderUtils;
import org.thingsboard.server.report.util.ThymeleafUtil;
import org.thingsboard.server.report.util.WebReportClient;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.DASHBOARD;
import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.ERROR;
import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.SUB_REPORT;
import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.TIME_SERIES_TABLE;
import static org.thingsboard.server.common.data.report.configuration.style.PageSize.A4;
import static org.thingsboard.server.common.data.util.DataSourceUtils.entityDataFromEntityId;
import static org.thingsboard.server.report.util.ReportQueryUtils.toAlarmCountQuery;
import static org.thingsboard.server.report.util.ReportQueryUtils.toEntityCountQuery;
import static org.thingsboard.server.report.util.ReportUtils.getSingleDataSource;
import static org.thingsboard.server.report.util.ReportUtils.prepareReportComponent;
import static org.thingsboard.server.report.util.ReportUtils.prepareReportName;
import static org.thingsboard.server.report.util.ReportUtils.updateDashboardReportStateParamsWithEntity;

@Service
@Slf4j
public class PdfReportService extends AbstractReportService {

    private final Map<ReportComponentType, PdfReportComponentRenderer<ReportComponent>> componentsRenderers = new EnumMap<>(ReportComponentType.class);
    private final WebReportClient webReportClient;

    private PdfReportService(List<PdfReportComponentRenderer> renderers, WebReportClient webReportClient) {
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

        EntityData stateEntity = task.getOriginator() != null ? entityDataFromEntityId(task.getOriginator()) : null;

        HeaderFooterRenderLayout headerLayout = renderHeaderFooter(renderer, ctx, configuration.getHeader(), usablePageWidthPx, stateEntity);
        HeaderFooterRenderLayout footerLayout = renderHeaderFooter(renderer, ctx, configuration.getFooter(), usablePageWidthPx, stateEntity);

        Map<String, Object> reportVariables = new HashMap<>();
        fillPageLayoutVariables(reportVariables, configuration, headerLayout, footerLayout, pageSize, pageMargins);

        reportVariables.put("pageContent", renderContent(usablePageWidthPx, ctx, configuration.getComponents(), stateEntity));

        String renderedHtmlContent = ThymeleafUtil.renderFromHtmlTemplate("html/report-template", reportVariables);
        String xHtml = HtmlRenderUtils.convertToXhtml(renderedHtmlContent);

        renderer.setDocumentFromString(xHtml);
        renderer.layout();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            renderer.createPDF(outputStream);
            byte[] reportBytes = outputStream.toByteArray();
            String reportName = prepareReportName(configuration.getNamePattern(), new Date(), task.getTimezone());

            return ReportData.builder()
                    .data(reportBytes)
                    .contentType(configuration.getFormat().getContentType())
                    .name(reportName)
                    .build();
        }
    }

    private HeaderFooterRenderLayout renderHeaderFooter(ITextRenderer renderer,
                                                        TbReportCtx ctx, HeaderFooter headerFooter,
                                                        int usablePageWidthPx, EntityData stateEntity) throws Exception {
        if (headerFooter == null) {
            return new HeaderFooterRenderLayout();
        }
        HeaderFooterRenderLayout headerFooterRenderLayout = new HeaderFooterRenderLayout();
        headerFooterRenderLayout.setEnabled(headerFooter.isEnabled());
        if (headerFooter.isEnabled()) {
            String htmlContent = renderContent(usablePageWidthPx, ctx, headerFooter.getComponents(), stateEntity);
            headerFooterRenderLayout.setHtmlContent(htmlContent);
            int heightPx = HtmlRenderUtils.measureHtmlHeight(renderer, htmlContent, usablePageWidthPx);
            headerFooterRenderLayout.setHeightPx(heightPx);
        }
        headerFooterRenderLayout.setFirstPageEnabled(headerFooter.getFirstPage() != null && headerFooter.getFirstPage().isEnabled());
        if (headerFooterRenderLayout.isFirstPageEnabled()) {
            String htmlContent = renderContent(usablePageWidthPx, ctx, headerFooter.getFirstPage().getComponents(), stateEntity);
            headerFooterRenderLayout.setFirstPageHtmlContent(htmlContent);
            int heightPx = HtmlRenderUtils.measureHtmlHeight(renderer, htmlContent, usablePageWidthPx);
            headerFooterRenderLayout.setFirstPageHeightPx(heightPx);
        }
        return headerFooterRenderLayout;
    }

    private String renderContent(int usablePageWidthPx, TbReportCtx ctx, List<ReportComponent> components, EntityData stateEntity) {
        StringBuilder content = new StringBuilder();
        EntityId stateEntityId = stateEntity != null ? stateEntity.getEntityId() : null;
        for (ReportComponent component : components) {
            prepareReportComponent(component);
            ReportComponentType type = component.getType();
            if (type == SUB_REPORT) {
                content.append(renderSubReport(usablePageWidthPx, ctx, (SubReportComponent)component, stateEntityId));
            } else if (type == DASHBOARD) {
                content.append(renderDashboard(usablePageWidthPx, ctx, stateEntityId, (DataReportComponent) component));
            } else if (type == TIME_SERIES_TABLE) {
                content.append(renderTimeseriesTables(usablePageWidthPx, ctx, stateEntityId, (TimeseriesTableComponent) component));
            } else {
                content.append(renderComponent(usablePageWidthPx, ctx, component, stateEntity));
            }
        }
        return content.toString();
    }

    private String renderComponent(int usablePageWidthPx, TbReportCtx ctx, ReportComponent component, EntityData stateEntity) {
        try {
            ComponentData componentData = getComponentData(usablePageWidthPx, ctx, component, stateEntity);
            return componentsRenderers.get(component.getType()).render(component, componentData);
        } catch (Exception e) {
            log.error("Failed to render component of type [{}]", component.getType(), e);
            return renderError(usablePageWidthPx, "Failed to render component of type: " + component.getType(), e);
        }
    }

    private ComponentData getComponentData(int usablePageWidthPx, TbReportCtx ctx, ReportComponent component, EntityData stateEntity) {
        ComponentData componentData = switch (component.getType()) {
            case TIME_SERIES_TABLE ->
                    buildTsComponentData(usablePageWidthPx, ctx, (TimeseriesTableComponent) component, stateEntity);
            case ALARM_TABLE ->
                    buildAlarmComponentData(usablePageWidthPx, ctx, (AlarmTableComponent) component, stateEntity);
            case DASHBOARD ->
                    buildDashboardComponentData(usablePageWidthPx, ctx, ((DashboardComponent) component), stateEntity);
            case IMAGE -> buildImageComponentData(usablePageWidthPx, ctx, ((ImageComponent) component));
            default -> buildMultipleDataSourceData(usablePageWidthPx, ctx, component, stateEntity);
        };
        populateReportVars(componentData, ctx);
        return componentData;
    }

    private String renderTimeseriesTables(int usablePageWidthPx, TbReportCtx ctx, EntityId stateEntityId, DataReportComponent component) {
        StringBuilder content = new StringBuilder();
        Optional<DataSource> dataSource = getSingleDataSource(component);
        if (dataSource.isEmpty()) {
            return renderError(usablePageWidthPx, "Data source is not configured for time series table");
        }
        DataSource ds = dataSource.get();
        if (ds.getDataKeys().isEmpty()) {
            return renderError(usablePageWidthPx, "At least one time series column should be specified for time series table");
        }
        DataSource latestDataSource = DataSource.builder()
                .type(ds.getType())
                .deviceId(ds.getDeviceId())
                .entityAliasId(ds.getEntityAliasId())
                .filterId(ds.getFilterId())
                .dataKeys(ds.getLatestDataKeys()).build();
        List<EntityData> entityDatas = fetchEntities(ctx, latestDataSource, stateEntityId);
        for (EntityData entity : entityDatas) {
            content.append(renderComponent(usablePageWidthPx, ctx, component, entity));
        }
        return content.toString();
    }

    private String renderDashboard(int usablePageWidthPx, TbReportCtx ctx, EntityId stateEntityId, DataReportComponent component) {
        StringBuilder content = new StringBuilder();
        Optional<DataSource> dataSource = getSingleDataSource(component);
        List<EntityData> entityDatas;
        if (dataSource.isEmpty()) {
            entityDatas = new ArrayList<>();
            entityDatas.add(null);
        } else {
            DataSource dashboardDataSource = dataSource.get();
            entityDatas = fetchEntities(ctx, dashboardDataSource, stateEntityId);
        }
        for (EntityData entity : entityDatas) {
            content.append(renderComponent(usablePageWidthPx, ctx, component, entity));
        }
        return content.toString();
    }

    private String renderSubReport(int usablePageWidthPx, TbReportCtx ctx, DataReportComponent component, EntityId stateEntityId) {
        SubReportComponent subReportComponent = ((SubReportComponent) component);
        ReportTemplateId templateId = subReportComponent.getTemplateId();
        if (templateId == null) {
            return renderError(usablePageWidthPx, "Report template id is not configured for SubReport");
        }
        StringBuilder content = new StringBuilder();
        try {
            ReportTemplate reportTemplate = dataService.findReportTemplate(templateId, ctx);
            if (reportTemplate == null) {
                return renderError(usablePageWidthPx, "Template with id " + templateId + " not found. Please check the configuration.");
            }
            PdfReportTemplateConfig reportConfiguration = (PdfReportTemplateConfig) reportTemplate.getConfiguration();

            TbReportCtx subReportCtx = ctx.createSubReportCxt(reportConfiguration);
            List<EntityData> entities = getSubReportEntities(ctx, component, stateEntityId);
            for (EntityData entity : entities) {
                if (subReportComponent.isAvoidPageBreakInside()) {
                    content.append("<div class=\"no-page-break\">");
                }
                content.append(renderContent(usablePageWidthPx, subReportCtx, reportConfiguration.getComponents(), entity));
                if (subReportComponent.isAvoidPageBreakInside()) {
                    content.append("</div>");
                }
            }
            return content.toString();
        } catch (Exception e) {
            log.error("Failed to render Subreport, template id: {}", templateId, e);
            return renderError(usablePageWidthPx, "Failed to render sub-report " + templateId, e);
        }
    }

    private String renderError(int usablePageWidthPx, String errorMessage) {
        return renderError(usablePageWidthPx, errorMessage, null);
    }

    private String renderError(int usablePageWidthPx, String errorMessage, Exception e) {
        if (e instanceof InterruptedException || ExceptionUtils.getRootCause(e) instanceof InterruptedException) {
            throw new RuntimeException(e);
        }
        return componentsRenderers.get(ERROR).render(new ErrorComponent(errorMessage, e), new ComponentData(usablePageWidthPx));
    }

    private ComponentData buildImageComponentData(int usablePageWidthPx, TbReportCtx ctx, ImageComponent component) {
        if (ImageSourceType.ENTITY_KEY == component.getSourceType()) {
            Optional<DataSource> dataSource = getSingleDataSource(component);
            if (dataSource.isEmpty()) {
                return new ComponentData(usablePageWidthPx);
            }
            return buildSingleComponentData(usablePageWidthPx, ctx, dataSource.get(), null);
        } else {
            return new ComponentData(usablePageWidthPx);
        }
    }

    private ComponentData buildSingleComponentData(int usablePageWidthPx, TbReportCtx ctx, DataSource dataSource, EntityId stateEntityId) {
        return switch (dataSource.getType()) {
            case DEVICE, ENTITY -> new ComponentData(usablePageWidthPx, dataSource, collectEntityDatas(ctx, dataSource, stateEntityId));
            case ENTITY_COUNT -> buildEntityCountDataSource(usablePageWidthPx, ctx, dataSource);
            case ALARM_COUNT -> buildAlarmCountDataSource(usablePageWidthPx, ctx, dataSource);
            default -> throw new IllegalArgumentException("Unknown data source type: " + dataSource.getType());
        };
    }

    private ComponentData buildEntityCountDataSource(int usablePageWidthPx, TbReportCtx ctx, DataSource dataSource) {
        Map<String, Object> map = new HashMap<>();
        String label = resolveSingleLabel(dataSource, "count");
        map.put(label, dataService.countEntitiesByQuery(toEntityCountQuery(dataSource, ctx), ctx));
        return new ComponentData(usablePageWidthPx, map);
    }

    private ComponentData buildAlarmCountDataSource(int usablePageWidthPx, TbReportCtx ctx, DataSource dataSource) {
        Map<String, Object> map = new HashMap<>();
        String label = resolveSingleLabel(dataSource, "count");
        map.put(label, dataService.countAlarmsByQuery(toAlarmCountQuery(dataSource, ctx), ctx));
        return new ComponentData(usablePageWidthPx, map);
    }

    private String resolveSingleLabel(DataSource dataSource, String fallback) {
        String label = null;
        if (dataSource.getDataKeys() != null && !dataSource.getDataKeys().isEmpty()) {
            label = dataSource.getDataKeys().get(0).getLabel();
        }
        if (StringUtils.isNotBlank(label)) {
            return label;
        }
        return fallback;
    }

    private ComponentData buildDashboardComponentData(int usablePageWidthPx, TbReportCtx ctx, DashboardComponent component, EntityData stateEntity) {
        if (component.getConfig() == null) {
            return new ComponentData(usablePageWidthPx, "Dashboard report config is empty");
        }
        if (StringUtils.isBlank(component.getConfig().getBaseUrl())) {
            return new ComponentData(usablePageWidthPx, "Base URL is not configured for dashboard report");
        }
        if (StringUtils.isBlank(component.getConfig().getDashboardId())) {
            return new ComponentData(usablePageWidthPx, "Dashboard id is not configured for dashboard report");
        }
        SettableFuture<DashboardReportData> futureToSet = SettableFuture.create();
        DashboardReportConfig config = component.getConfig();
        config.setType("png");
        if (stateEntity != null) {
            config.setState(updateDashboardReportStateParamsWithEntity(config.getState(), stateEntity));
        }
        webReportClient.requestDashboardReport(config, null,
                ctx.getAccessToken(), ctx.getAccessTokenExpTs(),
                futureToSet::set, error -> {
                    log.error("Failed to generate dashboard report", error);
                    futureToSet.setException(error);
                });
        try {
            return new ComponentData(usablePageWidthPx, futureToSet.get().getData());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    private ComponentData buildMultipleDataSourceData(int usablePageWidthPx, TbReportCtx ctx, ReportComponent component, EntityData stateEntity) {
        List<DataSource> dataSources = null;
        if (component instanceof DataReportComponent) {
            dataSources = ((DataReportComponent) component).getDataSources();
        }
        if (dataSources == null || dataSources.isEmpty()) {
            return new ComponentData(usablePageWidthPx);
        }
        ComponentData mainDataSource = new ComponentData(usablePageWidthPx);
        for (DataSource dataSource : dataSources) {
            ComponentData singleDataSource = buildSingleComponentData(usablePageWidthPx, ctx, dataSource, stateEntity != null ? stateEntity.getEntityId() : null);
            mainDataSource.merge(singleDataSource);
        }
        return mainDataSource;
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
