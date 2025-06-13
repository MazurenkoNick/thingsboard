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
package org.thingsboard.server.service.sync.ie.importing.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.UUID;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class SchedulerEventImportService extends BaseEntityImportService<SchedulerEventId, SchedulerEvent, EntityExportData<SchedulerEvent>> {

    private final SchedulerEventService schedulerEventService;

    @Override
    protected void setOwner(TenantId tenantId, SchedulerEvent schedulerEvent, IdProvider idProvider) {
        schedulerEvent.setTenantId(tenantId);
        if (schedulerEvent.getOwnerId() instanceof TenantId) {
            schedulerEvent.setOwnerId(tenantId);
        } else {
            schedulerEvent.setOwnerId(idProvider.getInternalId(schedulerEvent.getOwnerId()));
        }
    }

    @Override
    protected SchedulerEvent prepare(EntitiesImportCtx ctx, SchedulerEvent schedulerEvent, SchedulerEvent oldSchedulerEvent, EntityExportData<SchedulerEvent> exportData, IdProvider idProvider) {
        schedulerEvent.setOriginatorId(idProvider.getInternalId(schedulerEvent.getOriginatorId()));
        prepareConfigurationByType(ctx, schedulerEvent, idProvider);
        return schedulerEvent;
    }

    @Override
    protected SchedulerEvent deepCopy(SchedulerEvent schedulerEvent) {
        return new SchedulerEvent(schedulerEvent);
    }

    @Override
    protected SchedulerEvent saveOrUpdate(EntitiesImportCtx ctx, SchedulerEvent schedulerEvent, EntityExportData<SchedulerEvent> exportData, IdProvider idProvider, CompareResult compareResult) throws Exception {
        return schedulerEventService.saveSchedulerEvent(schedulerEvent);
    }

    @Override
    protected void cleanupForComparison(SchedulerEvent schedulerEvent) {
        super.cleanupForComparison(schedulerEvent);
        if (schedulerEvent.getCustomerId() != null && schedulerEvent.getCustomerId().isNullUid()) {
            schedulerEvent.setCustomerId(null);
        }
    }

    private void prepareConfigurationByType(EntitiesImportCtx ctx, SchedulerEvent schedulerEvent, IdProvider idProvider) {
        var configuration = (ObjectNode) schedulerEvent.getConfiguration();
        switch (schedulerEvent.getType()) {
            case "updateFirmware", "updateSoftware" -> patchOtaPackageConfig(configuration, idProvider);
            case "generateReport" -> patchGenerateReportConfig(configuration, ctx, idProvider);
        }
        schedulerEvent.setConfiguration(configuration);
    }

    private void patchOtaPackageConfig(ObjectNode configuration, IdProvider idProvider) {
        ObjectNode config = (ObjectNode) configuration.get("msgBody");
        OtaPackageId otaPackageId = JacksonUtil.convertValue(config, OtaPackageId.class);
        if (otaPackageId != null) {
            OtaPackageId internalId = idProvider.getInternalId(otaPackageId);
            config.put("id", internalId.getId().toString());
        }
    }

    private void patchGenerateReportConfig(ObjectNode configuration, EntitiesImportCtx ctx, IdProvider idProvider) {
        ObjectNode config = (ObjectNode) configuration.path("msgBody").path("reportConfig");
        config.put("userId", ctx.getUser().getUuidId().toString()); // user entities are not supported by VC; replacing with current user id
        String oldDash = config.path("dashboardId").asText(null);
        if (oldDash != null) {
            DashboardId oldDashboardId = new DashboardId(UUID.fromString(oldDash));
            DashboardId dashboardId = idProvider.getInternalId(oldDashboardId);
            config.put("dashboardId", dashboardId.getId().toString());
        }
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SCHEDULER_EVENT;
    }

}
