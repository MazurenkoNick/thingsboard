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
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.report.ReportTemplateDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Component
@AllArgsConstructor
public class ReportTemplateDataValidator extends DataValidator<ReportTemplate> {

    private final TenantService tenantService;
    private final CustomerDao customerDao;
    private final ReportTemplateDao reportTemplateDao;

    @Override
    protected ReportTemplate validateUpdate(TenantId tenantId, ReportTemplate reportTemplate) {
        ReportTemplate old = reportTemplateDao.findById(reportTemplate.getTenantId(), reportTemplate.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing report template!");
        }
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, ReportTemplate reportTemplate) {
        validateString("Report template name", reportTemplate.getName());
        if (reportTemplate.getFormat() == null) {
            throw new DataValidationException("Report template format should be specified!");
        }
        if (reportTemplate.getType() == null) {
            throw new DataValidationException("Report template type should be specified!");
        }
        if (reportTemplate.getTenantId() == null) {
            throw new DataValidationException("Report template should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(reportTemplate.getTenantId())) {
                throw new DataValidationException("Report template is referencing to non-existent tenant!");
            }
        }
        if (reportTemplate.getCustomerId() == null) {
            reportTemplate.setCustomerId(new CustomerId(NULL_UUID));
        } else if (!reportTemplate.getCustomerId().isNullUid()) {
            Customer customer = customerDao.findById(reportTemplate.getTenantId(), reportTemplate.getCustomerId().getId());
            if (customer == null) {
                throw new DataValidationException("Can't assign report template to non-existent customer!");
            }
            if (!customer.getTenantId().getId().equals(reportTemplate.getTenantId().getId())) {
                throw new DataValidationException("Can't assign report template to customer from different tenant!");
            }
        }
    }
}


