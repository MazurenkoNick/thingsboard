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
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.scheduler.ScheduledReportInfo;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_REPORT_EVENT_CUSTOMER_TTTLE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_REPORT_EVENT_TEMPLATE_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_REPORT_EVENT_TEMPLATE_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_REPORT_EVENT_USER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_REPORT_EVENT_USER_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_REPORT_EVENT_VIEW_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = SCHEDULER_REPORT_EVENT_VIEW_NAME)
public final class ScheduledReportInfoEntity extends AbstractSchedulerEventInfoEntity<ScheduledReportInfo> {

    @Column(name = SCHEDULER_REPORT_EVENT_TEMPLATE_ID_PROPERTY)
    private UUID reportTemplateId;
    @Column(name = SCHEDULER_REPORT_EVENT_TEMPLATE_NAME_PROPERTY)
    private String reportTemplateName;
    @Column(name = SCHEDULER_REPORT_EVENT_CUSTOMER_TTTLE_PROPERTY)
    private String customerTitle;
    @Column(name = SCHEDULER_REPORT_EVENT_USER_ID_PROPERTY)
    private UUID userId;
    @Column(name = SCHEDULER_REPORT_EVENT_USER_NAME_PROPERTY)
    private String userName;

    public ScheduledReportInfoEntity() {
        super();
    }

    @Override
    public ScheduledReportInfo toData() {
        ScheduledReportInfo schedulerReportEventInfo = new ScheduledReportInfo(super.toSchedulerEventInfo());
        schedulerReportEventInfo.setTemplateInfo(new EntityInfo(reportTemplateId, EntityType.REPORT_TEMPLATE.name(), reportTemplateName));
        schedulerReportEventInfo.setCustomerTitle(customerTitle);
        schedulerReportEventInfo.setUserName(userName);
        return schedulerReportEventInfo;
    }

}
