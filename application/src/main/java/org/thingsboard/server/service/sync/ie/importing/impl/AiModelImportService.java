/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
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

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.ai.AiModelService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
class AiModelImportService extends BaseEntityImportService<AiModelId, AiModel, EntityExportData<AiModel>> {

    private final AiModelService aiModelService;

    @Override
    protected void setOwner(
            TenantId tenantId,
            AiModel model,
            BaseEntityImportService<AiModelId, AiModel, EntityExportData<AiModel>>.IdProvider idProvider
    ) {
        model.setTenantId(tenantId);
    }

    @Override
    protected AiModel prepare(
            EntitiesImportCtx ctx,
            AiModel model,
            AiModel oldModel,
            EntityExportData<AiModel> exportData,
            BaseEntityImportService<AiModelId, AiModel, EntityExportData<AiModel>>.IdProvider idProvider
    ) {
        return model;
    }

    @Override
    protected AiModel deepCopy(AiModel model) {
        return new AiModel(model);
    }

    @Override
    protected AiModel saveOrUpdate(
            EntitiesImportCtx ctx,
            AiModel model,
            EntityExportData<AiModel> exportData,
            BaseEntityImportService<AiModelId, AiModel, EntityExportData<AiModel>>.IdProvider idProvider,
            CompareResult compareResult
    ) {
        return aiModelService.save(model);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AI_MODEL;
    }

}
