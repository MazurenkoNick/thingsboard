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
import lombok.Getter;
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
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.ReportRequest;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.HeadingComponent;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfiguration;
import org.thingsboard.server.common.data.report.configuration.EntityTableComponent;
import org.thingsboard.server.common.data.report.configuration.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.RichTextComponent;
import org.thingsboard.server.common.data.util.EntityDataQueryUtils;
import org.thingsboard.server.common.data.util.JasperReportUtils;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.report.ReportTemplateService;
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
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.util.JasperReportUtils.addColumnHeader;
import static org.thingsboard.server.common.data.util.JasperReportUtils.addDesignTitle;
import static org.thingsboard.server.common.data.util.JasperReportUtils.addHeading;
import static org.thingsboard.server.common.data.util.JasperReportUtils.addPageFooter;
import static org.thingsboard.server.common.data.util.JasperReportUtils.addPageHeader;
import static org.thingsboard.server.common.data.util.JasperReportUtils.addRichText;
import static org.thingsboard.server.common.data.util.JasperReportUtils.addSubReportBand;
import static org.thingsboard.server.common.data.util.JasperReportUtils.addTableDetailBand;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultReportService extends AbstractTbEntityService implements ReportService {

    private SimpleDateFormat defaultDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
    private Map<ReportComponentType, JasperComponentRenderer<? extends ReportComponent>> rendererMap;

    private final ReportTemplateService reportTemplateService;
    private final DashboardReportService dashboardReportService;
    private final EntityService entityService;

    @PostConstruct
    public void init() {
        rendererMap = new EnumMap<>(ReportComponentType.class);
        rendererMap.put(ReportComponentType.HEADING, newJasperComponentRenderer(this::renderHeading));
        rendererMap.put(ReportComponentType.RICH_TEXT, newJasperComponentRenderer(this::renderRichText));
        rendererMap.put(ReportComponentType.ENTITY_TABLE, newJasperComponentRenderer(this::renderEntityTable));
    }

    @Override
    public ListenableFuture<ReportData> generateReport(TenantId tenantId, CustomerId customerId, ReportRequest reportRequest, MergedUserPermissions userPermissions) throws ThingsboardException {
        log.trace("[{}] Executing generateReport, reportRequest [{}]", tenantId, reportRequest);
        ReportTemplate reportTemplate = checkNotNull(reportTemplateService.findReportTemplateById(tenantId, reportRequest.getTemplateId()));
        ReportTemplateConfiguration configuration = reportTemplate.getConfiguration();

        SettableFuture<ReportData> resultFuture = SettableFuture.create();
        try {
            TbReportCtx tbReportCtx = new TbReportCtx(tenantId, customerId, userPermissions, configuration);
            JasperDesign jasperDesign = tbReportCtx.getJasperDesign();

            // add page header and footer
            Optional.ofNullable(configuration.getHeader())
                    .ifPresent(header -> addPageHeader(jasperDesign, configuration.getHeader()));
            Optional.ofNullable(configuration.getFooter())
                    .ifPresent(header -> addPageFooter(jasperDesign, configuration.getHeader()));

            // render components
            for (ReportComponent component : configuration.getComponents()) {
                Optional.ofNullable(rendererMap.get(component.getType()))
                        .ifPresent(renderer -> renderer.render(tbReportCtx, component));
            }

            JasperReport mainReport = JasperCompileManager.compileReport(jasperDesign);
            JasperPrint print = JasperFillManager.fillReport(mainReport, tbReportCtx.getParams(), new JREmptyDataSource());

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

    private void renderHeading(TbReportCtx tbReportCtx, HeadingComponent component) {
        addHeading(tbReportCtx.getJasperDesign(), component.getValue());
    }

    private void renderRichText(TbReportCtx tbReportCtx, RichTextComponent component) {
        addRichText(tbReportCtx.getJasperDesign(), component.getValue());
    }

    private void renderEntityTable(TbReportCtx tbReportCtx, EntityTableComponent component) {
        Collection<Map<String, ?>> entryList = fetchEntityData(tbReportCtx, component.getDataSource());
        JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource(entryList);

        JasperDesign mainDesign = tbReportCtx.getJasperDesign();
        try {
            JasperReport tableReport = buildTableReport(component.getDataSource());

            // add table report to the main report
            String subReportExpression = "SubReport_" + RandomStringUtils.randomAlphabetic(5);
            String subReportDSExpression = "SubReportDS_" + RandomStringUtils.randomAlphabetic(5);
            addSubReportBand(mainDesign, subReportExpression, subReportDSExpression);

            Map<String, Object> params = tbReportCtx.getParams();
            params.put(subReportExpression, tableReport);
            params.put(subReportDSExpression, dataSource);
        } catch (JRException e) {
            throw new RuntimeException(e);
        }
    }

    private Collection<Map<String, ?>> fetchEntityData(TbReportCtx tbReportCtx, DataSource dataSource) {
        EntityDataQuery entityDataQuery = EntityDataQueryUtils.toEntityDataQuery(dataSource, tbReportCtx.getEntityAliases(), tbReportCtx.getFilters());
        PageData<EntityData> result = entityService.findEntityDataByQuery(tbReportCtx.getTenantId(), tbReportCtx.getCustomerId(),
                tbReportCtx.getUserPermissions(), entityDataQuery);

        Collection<Map<String, ?>> entryList = new ArrayList<>(result.getData().size());
        for (EntityData entityData : result.getData()) {
            HashMap<String, String> fields = new HashMap<>();
            entryList.add(fields);
            entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).forEach((key, value) -> {
                if (value.getValue() != null) {
                    fields.put(key, value.getValue());
                }
            });
        }
        return entryList;
    }

    private JasperReport buildTableReport(DataSource dataSource) throws JRException {
        List<String> entityKeys = dataSource.getDataKeys().stream().map(DataKey::getName).collect(Collectors.toList());
        List<String> columsHeaders = dataSource.getDataKeys().stream().map(DataKey::getLabel).collect(Collectors.toList());

        JasperDesign tableDesign = JasperReportUtils.buildTableDesign();
        addColumnHeader(tableDesign, columsHeaders);
        addTableDetailBand(tableDesign, entityKeys);
        return JasperCompileManager.compileReport(tableDesign);
    }

    public static <C extends ReportComponent> DefaultReportService.JasperComponentRenderer<C> newJasperComponentRenderer(BiConsumer<TbReportCtx, C> renderer) {
        return new DefaultReportService.JasperComponentRenderer<>(renderer);
    }


    @RequiredArgsConstructor
    @Getter
    @SuppressWarnings("unchecked")
    public static class JasperComponentRenderer<C extends ReportComponent> {
        protected final BiConsumer<TbReportCtx, C> handler;

        public void render(TbReportCtx jasperDesign, ReportComponent component) {
            handler.accept(jasperDesign, (C) component);
        }
    }

}
