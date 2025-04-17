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
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.report.BaseReportTemplate;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractReportTemplateEntity<T extends BaseReportTemplate> extends BaseVersionedEntity<T> {

    @Column(name = ModelConstants.REPORT_TEMPLATE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.REPORT_TEMPLATE_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @Column(name = ModelConstants.REPORT_TEMPLATE_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.REPORT_TEMPLATE_DESCRIPTION_PROPERTY)
    private String description;

    @Column(name = ModelConstants.REPORT_TEMPLATE_SCHEDULER_EVENT_ID_PROPERTY)
    private UUID schedulerEventId;

    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    public AbstractReportTemplateEntity() {
        super();
    }

    public AbstractReportTemplateEntity(T reportTemplate) {
        super(reportTemplate);
        if (reportTemplate.getTenantId() != null) {
            this.tenantId = reportTemplate.getTenantId().getId();
        }
        if (reportTemplate.getCustomerId() != null) {
            this.customerId = reportTemplate.getCustomerId().getId();
        }
        this.name = reportTemplate.getName();
        this.description = reportTemplate.getDescription();
        if (reportTemplate.getSchedulerEventId() != null) {
            this.schedulerEventId = reportTemplate.getSchedulerEventId().getId();
        }
        if (reportTemplate.getExternalId() != null) {
            this.externalId = reportTemplate.getExternalId().getId();
        }
    }

    public AbstractReportTemplateEntity(AbstractReportTemplateEntity reportTemplateEntity) {
        super(reportTemplateEntity);
        this.tenantId = reportTemplateEntity.getTenantId();
        this.customerId = reportTemplateEntity.getCustomerId();
        this.name = reportTemplateEntity.getName();
        this.description = reportTemplateEntity.getDescription();
        this.schedulerEventId = reportTemplateEntity.getSchedulerEventId();
        this.externalId = reportTemplateEntity.getExternalId();
    }

    protected BaseReportTemplate toBaseReportTemplate() {
        BaseReportTemplate reportTemplate = new BaseReportTemplate(new ReportTemplateId(id));
        reportTemplate.setCreatedTime(getCreatedTime());
        if (tenantId != null) {
            reportTemplate.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            reportTemplate.setCustomerId(new CustomerId(customerId));
        }
        reportTemplate.setName(name);
        reportTemplate.setDescription(description);
        if (schedulerEventId != null) {
            reportTemplate.setSchedulerEventId(new SchedulerEventId(schedulerEventId));
        }
        if (externalId != null) {
            reportTemplate.setExternalId(new ReportTemplateId(externalId));
        }
        return reportTemplate;
    }

}
