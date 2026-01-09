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
package org.thingsboard.server.service.entitiy.report;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTbReportTemplateService extends AbstractTbEntityService implements TbReportTemplateService {

    private final ReportTemplateService reportTemplateService;

    @Override
    public ReportTemplate save(ReportTemplate reportTemplate, User user) throws Exception {
        try {
            ReportTemplate savedReportTemplate = checkNotNull(reportTemplateService.saveReportTemplate(reportTemplate));
            autoCommit(user, savedReportTemplate.getId());
            logEntityActionService.logEntityAction(user.getTenantId(), savedReportTemplate.getId(), savedReportTemplate,
                    savedReportTemplate.getCustomerId(),
                    reportTemplate.getId() == null ? ActionType.ADDED : ActionType.UPDATED, user);
            return savedReportTemplate;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(user.getTenantId(), emptyId(EntityType.REPORT_TEMPLATE), reportTemplate,
                    reportTemplate.getId() == null ? ActionType.ADDED : ActionType.UPDATED, user, e);
            throw e;
        }
    }

    @Override
    public void delete(ReportTemplate reportTemplate, User user) {
        ActionType actionType = ActionType.DELETED;
        ReportTemplateId reportTemplateId = reportTemplate.getId();
        try {
            reportTemplateService.deleteReportTemplate(user.getTenantId(), reportTemplateId);
            logEntityActionService.logEntityAction(user.getTenantId(), reportTemplateId, reportTemplate,
                    reportTemplate.getCustomerId(), actionType, user, reportTemplateId.getId().toString());

        } catch (Exception e) {
            logEntityActionService.logEntityAction(user.getTenantId(), emptyId(EntityType.REPORT_TEMPLATE),
                    actionType, user, e, reportTemplateId.getId().toString());
            throw e;
        }
    }
}
