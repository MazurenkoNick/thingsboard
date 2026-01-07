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
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.OtaPackageExportData;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class OtaPackageImportService extends BaseEntityImportService<OtaPackageId, OtaPackage, OtaPackageExportData> {

    private final OtaPackageService otaPackageService;

    @Override
    protected void setOwner(TenantId tenantId, OtaPackage otaPackage, IdProvider idProvider) {
        otaPackage.setTenantId(tenantId);
    }

    @Override
    protected OtaPackage prepare(EntitiesImportCtx ctx, OtaPackage otaPackage, OtaPackage oldOtaPackage, OtaPackageExportData exportData, IdProvider idProvider) {
        otaPackage.setDeviceProfileId(idProvider.getInternalId(otaPackage.getDeviceProfileId()));
        return otaPackage;
    }

    @Override
    protected OtaPackage findExistingEntity(EntitiesImportCtx ctx, OtaPackage otaPackage, IdProvider idProvider) {
        OtaPackage existingOtaPackage = super.findExistingEntity(ctx, otaPackage, idProvider);
        if (existingOtaPackage == null && ctx.isFindExistingByName()) {
            existingOtaPackage = otaPackageService.findOtaPackageByTenantIdAndTitleAndVersion(ctx.getTenantId(), otaPackage.getTitle(), otaPackage.getVersion());
        }
        return existingOtaPackage;
    }

    @Override
    protected OtaPackage deepCopy(OtaPackage otaPackage) {
        return new OtaPackage(otaPackage);
    }

    @Override
    protected OtaPackage saveOrUpdate(EntitiesImportCtx ctx, OtaPackage otaPackage, OtaPackageExportData exportData, IdProvider idProvider, CompareResult compareResult) {
        if (otaPackage.hasUrl()) {
            OtaPackageInfo info = new OtaPackageInfo(otaPackage);
            return new OtaPackage(otaPackageService.saveOtaPackageInfo(info, info.hasUrl()));
        }
        return otaPackageService.saveOtaPackage(otaPackage);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.OTA_PACKAGE;
    }

}
