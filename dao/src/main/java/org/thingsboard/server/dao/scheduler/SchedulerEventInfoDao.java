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
package org.thingsboard.server.dao.scheduler;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.ScheduledReportQuery;
import org.thingsboard.server.common.data.scheduler.ScheduledReportInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEventFilter;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEventTimeFilter;
import org.thingsboard.server.common.data.scheduler.SchedulerEventWithCustomerInfo;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

public interface SchedulerEventInfoDao extends Dao<SchedulerEventInfo> {

    SchedulerEventWithCustomerInfo findSchedulerEventWithCustomerInfoById(UUID tenantId, UUID schedulerEventId);

    List<SchedulerEventInfo> findSchedulerEventsByTenantId(UUID tenantId);

    List<SchedulerEventInfo> findSchedulerEventsByTenantIdAndEnabled(UUID tenantId, boolean enabled);

    PageData<SchedulerEventWithCustomerInfo> findSchedulerEventsByTenantIdAndFilter(UUID tenantId, SchedulerEventFilter filter, PageLink pageLink);

    List<SchedulerEventWithCustomerInfo> findAllSchedulerEventsByTenantIdAndEventTimeFilter(UUID tenantId, SchedulerEventTimeFilter filter, String searchText);

    List<SchedulerEventId> findSchedulerEventsIdsByTenantIdAndCustomerId(UUID tenantId, UUID customerId);

    List<SchedulerEventId> findSchedulerEventsIdsByTenantId(UUID tenantId);

    ListenableFuture<List<SchedulerEventInfo>> findSchedulerEventsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> schedulerEventIds);

    PageData<SchedulerEventInfo> findSchedulerEventInfosByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, PageLink pageLink);

    PageData<SchedulerEventInfo> findSchedulerEventInfosByTenantIdAndEdgeIdAndCustomerId(UUID tenantId, UUID edgeId, UUID customerId, PageLink pageLink);

    PageData<ScheduledReportInfo> findScheduledReportEvents(UUID tenantId, ScheduledReportQuery query);

    PageData<ScheduledReportInfo> findScheduledReportEvents(UUID tenantId, UUID customerId, ScheduledReportQuery query);

    int countScheduledReportEventsByTemplateId(UUID tenantId, UUID templateId);
}
