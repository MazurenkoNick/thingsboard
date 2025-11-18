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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.sync.ie.SchedulerEventExportData;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class SchedulerEventImportService extends BaseEntityImportService<SchedulerEventId, SchedulerEvent, SchedulerEventExportData> {

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
    protected SchedulerEvent prepare(EntitiesImportCtx ctx, SchedulerEvent schedulerEvent, SchedulerEvent oldSchedulerEvent, SchedulerEventExportData exportData, IdProvider idProvider) {
        // Groups are imported after entities, so a group-originator lookup may return null. Validation forbids a null originator,
        // so we assign a temporary originator id and rely on reimport to correct it later
        EntityId originatorId = schedulerEvent.getOriginatorId();
        boolean isEntityGroup = originatorId != null && originatorId.getEntityType() == EntityType.ENTITY_GROUP;
        EntityId internalId = idProvider.getInternalId(originatorId, !isEntityGroup || ctx.isFinalImportAttempt());
        schedulerEvent.setOriginatorId(internalId != null ? internalId : originatorId);
        JsonNode configuration = exportData.prepareConfiguration(schedulerEvent.getConfiguration(), schedulerEvent.getType(),
                idProvider::getInternalId, ctx.getUser().getId());
        schedulerEvent.setConfiguration(configuration);
        return schedulerEvent;
    }

    @Override
    protected SchedulerEvent deepCopy(SchedulerEvent schedulerEvent) {
        return new SchedulerEvent(schedulerEvent);
    }

    @Override
    protected SchedulerEvent saveOrUpdate(EntitiesImportCtx ctx, SchedulerEvent schedulerEvent, SchedulerEventExportData exportData, IdProvider idProvider, CompareResult compareResult) throws Exception {
        return schedulerEventService.saveSchedulerEvent(schedulerEvent);
    }

    @Override
    protected void cleanupForComparison(SchedulerEvent schedulerEvent) {
        super.cleanupForComparison(schedulerEvent);
        if (schedulerEvent.getCustomerId() != null && schedulerEvent.getCustomerId().isNullUid()) {
            schedulerEvent.setCustomerId(null);
        }
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SCHEDULER_EVENT;
    }

}
