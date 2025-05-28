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

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.dao.report.ReportService;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.datasource.ReportDataService;
import org.thingsboard.server.service.query.EntityQueryService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.telemetry.TbTelemetryService;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Primary
@Service
public class LocalReportDataService implements ReportDataService {

    private final EntityQueryService entityQueryService;
    private final TbTelemetryService tbTelemetryService;
    private final AccessControlService accessControlService;
    private final ReportTemplateService reportTemplateService;
    private final ImageService imageService;
    private final ResourceService resourceService;
    private final ReportService reportService;

    @Override
    public ReportTemplate findReportTemplate(ReportTemplateId templateId, TbReportCtx ctx) throws ThingsboardException {
        SecurityUser securityUser = getSecurityUser(ctx);
        ReportTemplate reportTemplate = reportTemplateService.findReportTemplateById(securityUser.getTenantId(), templateId);
        accessControlService.checkPermission(securityUser, Resource.REPORT_TEMPLATE, Operation.READ, templateId, reportTemplate);
        return reportTemplate;
    }

    @Override
    public TbResource findImage(String type, String key, TbReportCtx ctx) throws ThingsboardException {
        SecurityUser securityUser = getSecurityUser(ctx);
        TenantId tenantId = "system".equals(type) ? TenantId.SYS_TENANT_ID : securityUser.getTenantId();
        TbResourceInfo imageInfo = checkNotNull(imageService.getImageInfoByTenantIdAndKey(tenantId, key));
        return checkNotNull(resourceService.findResourceById(tenantId, imageInfo.getId()));
    }

    @Override
    public TbResource findPublicImage(String publicKey, TbReportCtx ctx) throws ThingsboardException {
        SecurityUser securityUser = getSecurityUser(ctx);
        TbResourceInfo imageInfo = checkNotNull(imageService.getPublicImageInfoByKey(publicKey));
        return checkNotNull(resourceService.findResourceById(securityUser.getTenantId(), imageInfo.getId()));
    }

    @Override
    public TbResource findTbResource(TbResourceId resourceId, TbReportCtx ctx) throws ThingsboardException {
        SecurityUser securityUser = getSecurityUser(ctx);
        TbResource resource = resourceService.findResourceById(securityUser.getTenantId(), resourceId);
        accessControlService.checkPermission(securityUser, Resource.TB_RESOURCE, Operation.READ, resourceId, resource);
        return resource;
    }

    @Override
    public PageData<EntityData> findEntityDataByQuery(EntityDataQuery query, TbReportCtx ctx) {
        return entityQueryService.findEntityDataByQuery(getSecurityUser(ctx), query);
    }

    @Override
    public Long countEntitiesByQuery(EntityCountQuery query, TbReportCtx ctx) {
        return entityQueryService.countEntitiesByQuery(getSecurityUser(ctx), query);
    }

    @Override
    public PageData<AlarmData> findAlarmDataByQuery(AlarmDataQuery query, TbReportCtx ctx) {
        return entityQueryService.findAlarmDataByQuery(getSecurityUser(ctx), query);
    }

    @Override
    public Long countAlarmsByQuery(AlarmCountQuery query, TbReportCtx ctx) {
        return entityQueryService.countAlarmsByQuery(getSecurityUser(ctx), query);
    }

    @SneakyThrows
    @Override
    public List<TsKvEntry> getTimeseries(EntityId entityId, List<String> keys, Long startTs, Long endTs, Long interval, Aggregation agg, SortOrder.Direction sortOrder,
                                         Integer limit, boolean useStrictDataTypes, TbReportCtx ctx) {
        return tbTelemetryService.getTimeseries(entityId, keys, startTs, endTs, null, interval, null, limit, agg, sortOrder.name(), useStrictDataTypes, getSecurityUser(ctx)).get(); // .get() will be interrupted on task processing timeout
    }

    @SneakyThrows
    @Override
    public Report createReport(Report report, byte[] data, TbReportCtx ctx) {
        accessControlService.checkPermission(getSecurityUser(ctx), Resource.REPORT, Operation.CREATE);
        return reportService.createReport(report, data);
    }

    private SecurityUser getSecurityUser(TbReportCtx ctx) {
        return ((LocalTbReportCtxProvider.LocalTbReportCtx) ctx).getSecurityUser();
    }

    private <T> T checkNotNull(T reference) throws ThingsboardException {
        if (reference == null) {
            throw new ThingsboardException("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        return reference;
    }
}
