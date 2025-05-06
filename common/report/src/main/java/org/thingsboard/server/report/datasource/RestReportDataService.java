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
package org.thingsboard.server.report.datasource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;

import java.util.List;

@ConditionalOnMissingBean(value = ReportDataService.class, ignored = RestReportDataService.class)
@Service
public class RestReportDataService implements ReportDataService {

    @Value("${service.tb_core.base_url:http://localhost:${server.port}}")
    private String tbCoreBaseUrl;

    @Override
    public ReportDataServiceContext newContext(String accessToken) {
        return new RestReportDataServiceContext(new RestClient(new RestTemplate(), tbCoreBaseUrl, accessToken));
    }

    @Override
    public PageData<EntityData> findEntityDataByQuery(EntityDataQuery query, ReportDataServiceContext ctx) {
        return getRestClient(ctx).findEntityDataByQuery(query);
    }

    @Override
    public Long countEntitiesByQuery(EntityCountQuery query, ReportDataServiceContext ctx) {
        return getRestClient(ctx).countEntitiesByQuery(query);
    }

    @Override
    public PageData<AlarmData> findAlarmDataByQuery(AlarmDataQuery query, ReportDataServiceContext ctx) {
        return getRestClient(ctx).findAlarmDataByQuery(query);
    }

    @Override
    public Long countAlarmsByQuery(AlarmCountQuery query, ReportDataServiceContext ctx) {
        return getRestClient(ctx).countAlarmsByQuery(query);
    }

    @Override
    public List<TsKvEntry> getTimeseries(EntityId entityId, List<String> keys, Long startTs, Long endTs, Long interval, Aggregation agg, SortOrder.Direction sortOrder,
                                         Integer limit, boolean useStrictDataTypes, ReportDataServiceContext ctx) {
        return getRestClient(ctx).getTimeseries(entityId, keys, interval, agg, sortOrder, startTs, endTs, limit, useStrictDataTypes);
    }

    @Override
    public BlobEntityInfo createBlobEntity(BlobEntity blobEntity, ReportDataServiceContext ctx) {
        return getRestClient(ctx).createBlobEntity(blobEntity);
    }

    private RestClient getRestClient(ReportDataServiceContext ctx) {
        return ((RestReportDataServiceContext) ctx).restClient();
    }

    private record RestReportDataServiceContext(RestClient restClient) implements ReportDataServiceContext {
        @Override
        public void close() {
            restClient.close();
        }
    }

}
