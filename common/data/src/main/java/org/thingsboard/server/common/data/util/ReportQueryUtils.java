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
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmDataPageLink;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntitySearchQueryFilter;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.query.StateEntityFilter;
import org.thingsboard.server.common.data.query.StateEntityOwnerFilter;
import org.thingsboard.server.common.data.report.configuration.AlarmFilterConfig;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator.getTimeRange;
import static org.thingsboard.server.common.data.util.DataSourceUtils.setEntityKeyIfNotExists;

public class ReportQueryUtils {

    private static final EntityDataSortOrder DEFAULT_SORT_ORDER = new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, "id"), EntityDataSortOrder.Direction.ASC);
    private static final EntityDataSortOrder DEFAULT_ALARM_SORT_ORDER = new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "createdTime"), EntityDataSortOrder.Direction.DESC);

    public static EntityCountQuery toEntityCountQuery(DataSource dataSource, ReportTemplateConfig reportTemplateConfig) {
        EntityFilter entityFilter = buildEntityFilter(dataSource, reportTemplateConfig, null);
        List<KeyFilter> keyFilters = findKeyFilters(dataSource, reportTemplateConfig);

        return new EntityCountQuery(entityFilter, keyFilters);
    }

    public static AlarmCountQuery toAlarmCountQuery(DataSource dataSource, ReportTemplateConfig reportTemplateConfig) {
        EntityFilter entityFilter = buildEntityFilter(dataSource, reportTemplateConfig, null);
        List<KeyFilter> keyFilters = findKeyFilters(dataSource, reportTemplateConfig);
        AlarmCountQuery alarmCountQuery = new AlarmCountQuery(entityFilter, keyFilters);

        AlarmFilterConfig alarmFilterConfig = dataSource.getAlarmFilterConfig();
        if (alarmFilterConfig != null) {
            alarmCountQuery.setStatusList(alarmFilterConfig.getStatusList());
            alarmCountQuery.setSeverityList(alarmFilterConfig.getSeverityList());
            alarmCountQuery.setTypeList(alarmFilterConfig.getTypeList());
            alarmCountQuery.setAssigneeId(alarmFilterConfig.getAssigneeId());
        }
        return alarmCountQuery;
    }

    public static AlarmDataQuery toAlarmDataQuery(AlarmTableComponent component, ReportTemplateConfig reportTemplateConfig, EntityData stateEntity, PageLink pageLink) {
        DataSource alarmSource = component.getAlarmSource();
        EntityFilter entityFilter = buildEntityFilter(alarmSource, reportTemplateConfig, stateEntity);
        List<KeyFilter> keyFilters = findKeyFilters(alarmSource, reportTemplateConfig);

        List<EntityKey> alarmFields = alarmSource.getDataKeys().stream().filter(dataKey -> "alarm".equals(dataKey.getType())).map(dataKey ->
                new EntityKey(EntityKeyType.ALARM_FIELD, dataKey.getName())).toList();

        List<EntityKey> entityFields = alarmSource.getDataKeys().stream().filter(dataKey -> "entityField".equals(dataKey.getType())).map(dataKey ->
                new EntityKey(EntityKeyType.ENTITY_FIELD, dataKey.getName())).toList();

        entityFields = setEntityKeyIfNotExists(entityFields, EntityKeyType.ENTITY_FIELD, "name");
        entityFields = setEntityKeyIfNotExists(entityFields, EntityKeyType.ENTITY_FIELD, "label");

        List<EntityKey> attrFields = alarmSource.getDataKeys().stream().filter(dataKey -> "attribute".equals(dataKey.getType())).map(dataKey ->
                new EntityKey(EntityKeyType.ATTRIBUTE, dataKey.getName())).toList();

        List<EntityKey> tsFields = alarmSource.getDataKeys().stream().filter(dataKey -> "timeseries".equals(dataKey.getType())).map(dataKey ->
                new EntityKey(EntityKeyType.TIME_SERIES, dataKey.getName())).toList();

        List<EntityKey> latestValues = Stream.concat(attrFields.stream(), tsFields.stream()).toList();

        AlarmFilterConfig alarmFilterConfig = alarmSource.getAlarmFilterConfig();
        AlarmDataPageLink alarmDataPageLink = new AlarmDataPageLink();
        alarmDataPageLink.setPage(pageLink.getPage());
        alarmDataPageLink.setPageSize(pageLink.getPageSize());
        alarmDataPageLink.setSortOrder(Optional.ofNullable(alarmSource.getSortOrder()).orElse(DEFAULT_ALARM_SORT_ORDER));

        TimeIntervalCalculator.TimeRange timeRange = getTimeRange(component.getTimewindow());
        alarmDataPageLink.setStartTs(timeRange.startTs);
        alarmDataPageLink.setEndTs(timeRange.endTs);
        alarmDataPageLink.setSearchPropagatedAlarms(alarmFilterConfig.isSearchPropagatedAlarms());
        alarmDataPageLink.setSeverityList(alarmFilterConfig.getSeverityList());
        alarmDataPageLink.setStatusList(alarmFilterConfig.getStatusList());
        alarmDataPageLink.setTypeList(alarmFilterConfig.getTypeList());
        alarmDataPageLink.setAssigneeId(alarmFilterConfig.getAssigneeId());
        return new AlarmDataQuery(entityFilter, alarmDataPageLink, entityFields, latestValues, keyFilters, alarmFields);
    }

    public static EntityDataQuery toEntityDataQuery(DataSource dataSource, ReportTemplateConfig reportTemplateConfig, EntityData stateEntity, PageLink pageLink) {
        EntityDataSortOrder sortOrder = Optional.ofNullable(dataSource.getSortOrder()).orElse(DEFAULT_SORT_ORDER);
        EntityDataPageLink entityDataPageLink = new EntityDataPageLink(pageLink.getPageSize(), pageLink.getPage(), pageLink.getTextSearch(), sortOrder);

        EntityFilter filter = buildEntityFilter(dataSource, reportTemplateConfig, stateEntity);
        List<KeyFilter> keyFilters = findKeyFilters(dataSource, reportTemplateConfig);

        List<EntityKey> entityFields = new ArrayList<>();
        List<EntityKey> latestValues = new ArrayList<>();
        if (dataSource.getDataKeys() != null) {
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
        }
        entityFields = setEntityKeyIfNotExists(entityFields, EntityKeyType.ENTITY_FIELD, "name");
        entityFields = setEntityKeyIfNotExists(entityFields, EntityKeyType.ENTITY_FIELD, "label");
        return new EntityDataQuery(filter, entityDataPageLink, entityFields, latestValues, keyFilters);
    }

    private static EntityFilter buildEntityFilter(DataSource dataSource, ReportTemplateConfig config, EntityData stateEntity) {
        if (dataSource.getType().equals("device")) {
            return buildSingleEntityFilter(DeviceId.fromString(dataSource.getDeviceId()));
        }
        return buildAliasBasedFilter(dataSource, config, stateEntity);
    }

    private static EntityFilter buildSingleEntityFilter(EntityId entityId) {
        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(entityId);
        return filter;
    }

    private static EntityFilter buildAliasBasedFilter(DataSource dataSource, ReportTemplateConfig config, EntityData stateEntity) {
        EntityFilter filter = config.getEntityAliases().stream()
                .filter(alias -> alias.getId().equals(dataSource.getEntityAliasId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Entity alias not found: " + dataSource.getEntityAliasId()))
                .getFilter();

        EntityId resolvedEntity = resolveEntityId(stateEntity, filter);

        if (filter instanceof StateEntityFilter) {
            return buildSingleEntityFilter(resolvedEntity);
        } else if (filter instanceof StateEntityOwnerFilter) {
            StateEntityOwnerFilter ownerFilter = new StateEntityOwnerFilter();
            ownerFilter.setSingleEntity(resolvedEntity);
            return ownerFilter;
        } else if (filter instanceof EntitySearchQueryFilter queryFilter && queryFilter.isRootStateEntity()) {
            queryFilter.setRootEntity(resolvedEntity);
        }

        return filter;
    }

    private static EntityId resolveEntityId(EntityData stateEntity, EntityFilter filter) {
        if (stateEntity != null) {
            return stateEntity.getEntityId();
        }

        if (filter instanceof StateEntityFilter stateFilter) {
            return stateFilter.getDefaultStateEntity();
        } else if (filter instanceof StateEntityOwnerFilter ownerFilter) {
            return ownerFilter.getDefaultStateEntity();
        } else if (filter instanceof EntitySearchQueryFilter queryFilter) {
            return queryFilter.getDefaultStateEntity();
        }

        return null;
    }

    private static List<KeyFilter> findKeyFilters(DataSource dataSource, ReportTemplateConfig reportTemplateConfig) {
        if (dataSource.getFilterId() != null) {
            return reportTemplateConfig.getFilters().stream()
                    .filter(filter -> filter.getId().equals(dataSource.getFilterId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Entity filter not found: " + dataSource.getFilterId()))
                    .getKeyFilters();
        } else {
            return null;
        }
    }

}
