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
package org.thingsboard.server.common.data.util;

import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmDataPageLink;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.report.configuration.AlarmFilterConfig;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.EntityAlias;
import org.thingsboard.server.common.data.report.configuration.Filter;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator.getTimeRange;

public class ReportQueryUtils {

    private static final EntityDataSortOrder DEFAULT_SORT_ORDER = new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, "id"), EntityDataSortOrder.Direction.ASC);

    public static EntityDataQuery toSingleEntityQuery(DataSource dataSource, List<Filter> filters, PageLink pageLink) {
        SingleEntityFilter singleEntityFilter = new SingleEntityFilter();
        String deviceId = Optional.ofNullable(dataSource.getDeviceId())
                .orElseThrow(() -> new IllegalArgumentException("Device ID is null"));
        singleEntityFilter.setSingleEntity(new DeviceId(UUID.fromString(deviceId)));

        return buildEntityDataQuery(singleEntityFilter, dataSource, filters, pageLink);
    }

    public static EntityDataQuery toEntityDataQuery(DataSource dataSource, List<EntityAlias> entityAliases, List<Filter> filters, PageLink pageLink) {
        EntityFilter entityFilter = findEntityFilterByAliasId(dataSource, entityAliases);

        return buildEntityDataQuery(entityFilter, dataSource, filters, pageLink);
    }

    public static EntityCountQuery toEntityCountQuery(DataSource dataSource, List<EntityAlias> entityAliases, List<Filter> filters) {
        EntityFilter entityFilter = findEntityFilterByAliasId(dataSource, entityAliases);
        List<KeyFilter> keyFilters = findKeyFilters(dataSource, filters);

        return new EntityCountQuery(entityFilter, keyFilters);
    }

    public static AlarmCountQuery toAlarmCountQuery(DataSource dataSource, List<EntityAlias> entityAliases, List<Filter> filters) {
        List<KeyFilter> keyFilters = findKeyFilters(dataSource, filters);
        AlarmCountQuery alarmCountQuery = new AlarmCountQuery(findEntityFilterByAliasId(dataSource, entityAliases), keyFilters);

        AlarmFilterConfig alarmFilterConfig = dataSource.getAlarmFilterConfig();
        if (alarmFilterConfig != null) {
            alarmCountQuery.setStatusList(alarmFilterConfig.getStatusList());
            alarmCountQuery.setSeverityList(alarmFilterConfig.getSeverityList());
            alarmCountQuery.setTypeList(alarmFilterConfig.getTypeList());
            alarmCountQuery.setAssigneeId(alarmFilterConfig.getAssigneeId());
        }
        return alarmCountQuery;
    }

    public static AlarmDataQuery toAlarmDataQuery(AlarmTableComponent component, List<EntityAlias> entityAliases, List<Filter> filters) {
        DataSource alarmSource = component.getAlarmSource();

        EntityFilter entityFilter = findEntityFilterByAliasId(alarmSource, entityAliases);
        List<KeyFilter> keyFilters = findKeyFilters(alarmSource, filters);

        List<EntityKey> alarmFields = new ArrayList<>();
        for (DataKey dataKey : alarmSource.getDataKeys()) {
            alarmFields.add(new EntityKey(EntityKeyType.ALARM_FIELD, dataKey.getName()));
        }
        AlarmFilterConfig alarmFilterConfig = alarmSource.getAlarmFilterConfig();
        AlarmDataPageLink pageLink = new AlarmDataPageLink();
        pageLink.setPage(0);
        pageLink.setPageSize(Integer.MAX_VALUE);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "createdTime")));

        TimeIntervalCalculator.TimeRange timeRange = getTimeRange(component.getTimewindow());
        pageLink.setStartTs(timeRange.startTs);
        pageLink.setEndTs(timeRange.endTs);
        pageLink.setSearchPropagatedAlarms(alarmFilterConfig.isSearchPropagatedAlarms());
        pageLink.setSeverityList(alarmFilterConfig.getSeverityList());
        pageLink.setStatusList(alarmFilterConfig.getStatusList());
        pageLink.setTypeList(alarmFilterConfig.getTypeList());
        return new AlarmDataQuery(entityFilter, pageLink, null, null, keyFilters, alarmFields);
    }

    private static EntityFilter findEntityFilterByAliasId(DataSource dataSource, List<EntityAlias> entityAliases) {
        return entityAliases.stream()
                .filter(alias -> alias.getId().equals(dataSource.getEntityAliasId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Entity alias not found: " + dataSource.getEntityAliasId()))
                .getFilter();
    }

    private static List<KeyFilter> findKeyFilters(DataSource dataSource, List<Filter> filters) {
        if (dataSource.getFilterId() == null) {
            return null;
        } else {
            return filters.stream()
                    .filter(filter -> filter.getId().equals(dataSource.getFilterId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Entity filter not found: " + dataSource.getFilterId()))
                    .getKeyFilters();
        }
    }

    private static EntityDataQuery buildEntityDataQuery(EntityFilter filter, DataSource dataSource, List<Filter> filters, PageLink pageLink) {
        EntityDataSortOrder sortOrder = Optional.ofNullable(dataSource.getSortOrder()).orElse(DEFAULT_SORT_ORDER);
        EntityDataPageLink entityDataPageLink = new EntityDataPageLink(pageLink.getPageSize(), pageLink.getPage(), pageLink.getTextSearch(), sortOrder);

        List<KeyFilter> keyFilters = findKeyFilters(dataSource, filters);

        List<EntityKey> entityFields = new ArrayList<>();
        List<EntityKey> latestValues = new ArrayList<>();
        for (DataKey dataKey : dataSource.getDataKeys()) {
            switch (dataKey.getType()) {
                case "attribute" -> {
                    latestValues.add(new EntityKey(EntityKeyType.ATTRIBUTE, dataKey.getName()));
                }
                case "timeseries" -> {
                    latestValues.add(new EntityKey(EntityKeyType.TIME_SERIES, dataKey.getName()));
                }
                case "entityField" -> {
                    entityFields.add(new EntityKey(EntityKeyType.ENTITY_FIELD, dataKey.getName()));
                }
            }
        }
        return new EntityDataQuery(filter, entityDataPageLink, entityFields, latestValues, keyFilters);
    }

}
