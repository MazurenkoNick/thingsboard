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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class ReportTemplateImportService extends BaseEntityImportService<ReportTemplateId, ReportTemplate, EntityExportData<ReportTemplate>> {

    private static final LinkedHashSet<EntityType> HINTS = new LinkedHashSet<>(Arrays.asList(EntityType.DASHBOARD, EntityType.DEVICE, EntityType.ASSET));
    private final ReportTemplateService reportTemplateService;

    @Override
    protected void setOwner(TenantId tenantId, ReportTemplate reportTemplate, IdProvider idProvider) {
        reportTemplate.setTenantId(tenantId);
        if (reportTemplate.getOwnerId() instanceof TenantId) {
            reportTemplate.setOwnerId(tenantId);
        } else {
            reportTemplate.setOwnerId(idProvider.getInternalId(reportTemplate.getOwnerId()));
        }
    }

    @Override
    protected ReportTemplate prepare(EntitiesImportCtx ctx, ReportTemplate reportTemplate, ReportTemplate oldReportTemplate, EntityExportData<ReportTemplate> exportData, IdProvider idProvider) {
        for (JsonNode entityAlias : reportTemplate.getEntityAliasesConfig()) {
            replaceIdsRecursively(ctx, idProvider, entityAlias, Set.of("id"), null, HINTS);
        }
        for (JsonNode dataSource : reportTemplate.getComponentDataSources()) {
            replaceIdsRecursively(ctx, idProvider, dataSource, Set.of("entityAlias"), null, HINTS);
        }
        return reportTemplate;
    }

    @Override
    protected ReportTemplate deepCopy(ReportTemplate reportTemplate) {
        return new ReportTemplate(reportTemplate);
    }

    @Override
    protected ReportTemplate saveOrUpdate(EntitiesImportCtx ctx, ReportTemplate reportTemplate, EntityExportData<ReportTemplate> exportData, IdProvider idProvider, CompareResult compareResult) throws Exception {
        return reportTemplateService.saveReportTemplate(reportTemplate);
    }

    @Override
    protected void cleanupForComparison(ReportTemplate reportTemplate) {
        super.cleanupForComparison(reportTemplate);
        if (reportTemplate.getCustomerId() != null && reportTemplate.getCustomerId().isNullUid()) {
            reportTemplate.setCustomerId(null);
        }
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.REPORT_TEMPLATE;
    }

}
