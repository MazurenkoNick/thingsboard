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

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.report.context.RemoteTbReportCtxProvider;
import org.thingsboard.server.report.context.TbReportCtx;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

@ConditionalOnMissingBean(value = ReportDataService.class, ignored = RemoteReportDataService.class)
@Service
public class RemoteReportDataService implements ReportDataService {

    @Override
    public ReportTemplate findReportTemplate(ReportTemplateId templateId, TbReportCtx ctx) {
        return getRestClient(ctx).findReportTemplate(templateId);
    }

    @Override
    public TbResource findImage(String type, String key, TbReportCtx ctx) {
        RestClient restClient = getRestClient(ctx);
        TbResourceInfo info = restClient.getImageInfo(type, key);
        return restClient.getResourceId(info.getId());
    }

    @Override
    public TbResource findPublicImage(String publicKey, TbReportCtx ctx) throws ThingsboardException {
        RestClient restClient = getRestClient(ctx);
        try {
            byte[] data = restClient.downloadPublicImage(publicKey);
            // TODO:
            TbResource tbResource = new TbResource();
            tbResource.setData(data);
            return tbResource;
        } catch (IOException e) {
            throw new ThingsboardException("Failed to download public image", e, ThingsboardErrorCode.GENERAL);
        }
    }

    @Override
    public TbResource findTbResource(TbResourceId resourceId, TbReportCtx ctx) {
        return getRestClient(ctx).getResourceId(resourceId);
    }

    @Override
    public PageData<EntityData> findEntityDataByQuery(EntityDataQuery query, TbReportCtx ctx) {
        return getRestClient(ctx).findEntityDataByQuery(query);
    }

    @Override
    public Long countEntitiesByQuery(EntityCountQuery query, TbReportCtx ctx) {
        return getRestClient(ctx).countEntitiesByQuery(query);
    }

    @Override
    public PageData<AlarmData> findAlarmDataByQuery(AlarmDataQuery query, TbReportCtx ctx) {
        return getRestClient(ctx).findAlarmDataByQuery(query);
    }

    @Override
    public PageData<AlarmData> findAlarmDataByQueryForEntities(AlarmDataQuery query, Collection<EntityId> entityIds, TbReportCtx ctx) {
        //TODO: retrieve alarms for specific entities
        return getRestClient(ctx).findAlarmDataByQuery(query);
    }

    @Override
    public Long countAlarmsByQuery(AlarmCountQuery query, TbReportCtx ctx) {
        return getRestClient(ctx).countAlarmsByQuery(query);
    }

    @Override
    public List<TsKvEntry> getTimeseries(EntityId entityId, List<String> keys, Long startTs, Long endTs, Long interval, Aggregation agg, SortOrder.Direction sortOrder,
                                         Integer limit, boolean useStrictDataTypes, TbReportCtx ctx) {
        return getRestClient(ctx).getTimeseries(entityId, keys, interval, agg, sortOrder, startTs, endTs, limit, useStrictDataTypes);
    }

    @Override
    public List<ReadTsKvQueryResult> findTimeseriesByQueries(EntityId entityId, List<ReadTsKvQuery> queries, TbReportCtx ctx) {
        return getRestClient(ctx).getTimeseriesByQueries(entityId, queries);
    }

    @Override
    public Report createReport(Report report, byte[] data, TbReportCtx ctx) {
        return getRestClient(ctx).createReport(report, data);
    }

    private RestClient getRestClient(TbReportCtx ctx) {
        return ((RemoteTbReportCtxProvider.RemoteTbReportCtx) ctx).getRestClient();
    }

}
