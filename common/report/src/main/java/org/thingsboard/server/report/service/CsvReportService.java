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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.CsvReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.DataReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.EntityTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.renderer.CsvReportComponentRenderer;

import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.TIME_SERIES_TABLE;
import static org.thingsboard.server.report.util.CsvUtils.generateCsv;
import static org.thingsboard.server.report.util.ReportUtils.getSingleDataSource;
import static org.thingsboard.server.report.util.ReportUtils.prepareReportName;

@Service
@Slf4j
public class CsvReportService extends AbstractReportService {

    private final Map<ReportComponentType, CsvReportComponentRenderer<ReportComponent>> componentsRenderers = new EnumMap<>(ReportComponentType.class);

    private CsvReportService(List<CsvReportComponentRenderer> renderers) {
        renderers.forEach(renderer -> {
            ReportComponentType type = renderer.getType();
            if (type != null) {
                this.componentsRenderers.put(type, renderer);
            }
        });
    }

    @Override
    public ReportData generateReport(ReportTask task, TbReportCtx ctx) {
        TenantId tenantId = task.getTenantId();

        log.trace("[{}] Executing generateReport, reportRequest [{}]", tenantId, task);
        CsvReportTemplateConfig configuration = (CsvReportTemplateConfig) task.getReportTemplateConfig();

        List<List<String>> content = renderContent(ctx, configuration.getComponents());
        byte[] csvBytes = generateCsv(content);

        String requestTimeZone = task.getTimezone();
        TimeZone timeZone = (requestTimeZone == null) ? TimeZone.getDefault() : TimeZone.getTimeZone(requestTimeZone);
        String reportName = prepareReportName(configuration.getNamePattern(), new Date(), timeZone);

        return ReportData.builder()
                .data(csvBytes)
                .contentType(configuration.getFormat().getContentType())
                .name(reportName)
                .build();
    }

    private List<List<String>> renderContent(TbReportCtx ctx, List<ReportComponent> components) {
        List<List<String>> content = new LinkedList<>();
        for (ReportComponent component : components) {
            ReportComponentType type = component.getType();
             if (type == TIME_SERIES_TABLE) {
                content.addAll(renderTimeseriesTables(ctx, component));
            } else {
                content.addAll(renderComponent(ctx, component, null));
            }
        }
        return content;
    }

    private List<List<String>> renderComponent(TbReportCtx ctx, ReportComponent component, EntityData stateEntity) {
        try {
            ComponentData componentData = getComponentData(ctx, component, stateEntity);
            return componentsRenderers.get(component.getType()).render(component, componentData);
        } catch (Exception e) {
            log.error("Failed to render component of type [{}]", component.getType(), e);
            return Collections.emptyList(); //renderError(usablePageWidthPx, "Failed to render component of type: " + component.getType(), e);
        }
    }

    private List<List<String>> renderTimeseriesTables(TbReportCtx ctx, ReportComponent component) {
        List<List<String>> content = new LinkedList<>();
        Optional<DataSource> dataSource = getSingleDataSource((DataReportComponent)component);
        if (dataSource.isEmpty()) {
            return Collections.emptyList(); //renderError(usablePageWidthPx, "Data source is not configured for time series table");
        }
        DataSource ds = dataSource.get();
        if (ds.getDataKeys().isEmpty()) {
            return Collections.emptyList(); //renderError(usablePageWidthPx, "At least one time series column should be specified for time series table");
        }
        List<DataKey> latestDataKeys = ds.getLatestDataKeys();
        latestDataKeys.add(new DataKey("name", "entityField", "NAME"));
        DataSource latestDataSource = DataSource.builder()
                .type(ds.getType())
                .deviceId(ds.getDeviceId())
                .entityAliasId(ds.getEntityAliasId())
                .filterId(ds.getFilterId())
                .sortOrder(ds.getSortOrder())
                .dataKeys(latestDataKeys).build();
        List<EntityData> entityDatas = fetchEntities(ctx, latestDataSource, null);
        for (EntityData entity : entityDatas) {
            content.addAll(renderComponent(ctx, component, entity));
        }
        return content;
    }

    private ComponentData getComponentData(TbReportCtx ctx, ReportComponent component, EntityData stateEntity) {
        return switch (component.getType()) {
            case TIME_SERIES_TABLE ->
                    new ComponentData(0, fetchEntityTsData(ctx, (TimeseriesTableComponent) component, stateEntity));
            case ALARM_TABLE -> new ComponentData(0, fetchAlarmDatas(ctx, (AlarmTableComponent) component));
            case ENTITY_TABLE -> new ComponentData(0, fetchEntityTableData(ctx, (EntityTableComponent) component, null));
            default -> throw new IllegalArgumentException("Unsupported component type: " + component.getType());
        };
    }

    @Override
    public TbReportFormat getFormat() {
        return TbReportFormat.CSV;
    }

}
