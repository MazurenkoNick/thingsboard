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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.CsvReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.report.context.TbReportCtx;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import static org.thingsboard.server.report.util.CsvUtils.generateCsv;
import static org.thingsboard.server.report.util.ReportUtils.getSingleDataSource;
import static org.thingsboard.server.report.util.ReportUtils.prepareReportName;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvReportService extends AbstractReportService {

    @Override
    public ReportData generateReport(ReportTask task, TbReportCtx ctx) {
        TenantId tenantId = task.getTenantId();

        log.trace("[{}] Executing generateReport, reportRequest [{}]", tenantId, task);
        CsvReportTemplateConfig configuration = (CsvReportTemplateConfig) task.getReportTemplateConfig();

        ReportComponent component = configuration.getComponent();
        List<DataKey> headers = getTableHeaders(component);
        List<Map<String, String>> dataSource = buildDataSource(ctx, component);

        byte[] csvBytes = generateCsv(headers, dataSource);

        String requestTimeZone = task.getTimezone();
        TimeZone timeZone = (requestTimeZone == null) ? TimeZone.getDefault() : TimeZone.getTimeZone(requestTimeZone);
        String reportName = prepareReportName(configuration.getNamePattern(), new Date(), timeZone);

        return ReportData.builder()
                .data(csvBytes)
                .contentType(configuration.getFormat().getContentType())
                .name(reportName)
                .build();
    }

    private static List<DataKey> getTableHeaders(ReportComponent component) {
        return component.getDataSources().get(0).getDataKeys();
    }

    private List<Map<String, String>> buildDataSource(TbReportCtx ctx, ReportComponent component) {
        return switch (component.getType()) {
            case TIME_SERIES_TABLE -> fetchEntityTsDatas(ctx, ((TimeseriesTableComponent) component));
            case ALARM_TABLE -> fetchAlarmDatas(ctx, ((AlarmTableComponent) component));
            case ENTITY_TABLE -> fetchEntityTableDatas(ctx, component);
            default -> List.of(Map.of());
        };
    }

    private List<Map<String, String>> fetchEntityTableDatas(TbReportCtx ctx, ReportComponent component) {
        Optional<DataSource> dataSource = getSingleDataSource(component);
        if (dataSource.isEmpty()) {
            return Collections.emptyList();
        }
        return fetchEntityDatas(ctx, dataSource.get(), null);
    }

    private List<Map<String, String>> fetchEntityTsDatas(TbReportCtx ctx, TimeseriesTableComponent component) {
        Optional<DataSource> dataSource = getSingleDataSource(component);
        if (dataSource.isEmpty()) {
            return Collections.emptyList();
        }
        String deviceId = dataSource.get().getDeviceId();
        if (deviceId == null) {
            return Collections.emptyList();
        }
        return fetchEntityTsData(ctx, component, DeviceId.fromString(deviceId));
    }

    @Override
    public TbReportFormat getFormat() {
        return TbReportFormat.CSV;
    }

}
