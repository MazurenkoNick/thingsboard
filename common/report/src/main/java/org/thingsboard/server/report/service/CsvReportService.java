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
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.ReportRequest;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.configuration.CsvReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.queue.util.TbReportComponent;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static org.thingsboard.server.report.util.CsvUtils.generateCsv;
import static org.thingsboard.server.report.util.JasperReportBuilder.getSingleDataSource;
import static org.thingsboard.server.report.util.ReportUtils.prepareReportName;

@Service
@TbReportComponent
@RequiredArgsConstructor
@Slf4j
public class CsvReportService extends AbstractReportService {

    @Override
    public ReportData generateReport(ReportTask task, TbReportCtx ctx) throws ThingsboardException {
        TenantId tenantId = task.getTenantId();
        ReportRequest reportRequest = task.getReportRequest();

        log.trace("[{}] Executing generateReport, reportRequest [{}]", tenantId, reportRequest);
        ReportTemplate reportTemplate = task.getReportTemplate();
        CsvReportTemplateConfig configuration = (CsvReportTemplateConfig) reportTemplate.getConfiguration();

        try {
            List<Map<String, ?>> dataSource = buildDataSource(ctx, configuration.getComponent());

            byte[] csvBytes = generateCsv(dataSource);

            String requestTimeZone = reportRequest.getTimezone();
            TimeZone timeZone = (requestTimeZone == null) ? TimeZone.getDefault() : TimeZone.getTimeZone(requestTimeZone);
            String reportName = prepareReportName(configuration.getNamePattern(), new Date(), timeZone);

            return ReportData.builder()
                    .data(csvBytes)
                    .contentType(configuration.getFormat().getContentType())
                    .name(reportName)
                    .build();
        } catch (Exception e) {
            throw new ThingsboardException(ExceptionUtils.getRootCause(e), ThingsboardErrorCode.GENERAL);
        }
    }

    private List<Map<String, ?>> buildDataSource(TbReportCtx ctx, ReportComponent component) throws ThingsboardException {
        return switch (component.getType()) {
            case TIME_SERIES_TABLE -> buildTsDataSource(ctx, ((TimeseriesTableComponent) component));
            case ALARM_TABLE -> buildAlarmDataSource(ctx, ((AlarmTableComponent) component));
            case ENTITY_TABLE -> buildEntityDataSource(ctx, getSingleDataSource(component));
            default -> List.of(Map.of());
        };
    }

}
