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
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReportQueryUtils {

    public static EntityDataQuery toEntityDataQuery(DataSource dataSource, List<EntityAlias> entityAliases, List<Filter> filters) {
        EntityFilter entityFilter = getEntityFilter(dataSource, entityAliases);

        return buildEntityDataQuery(entityFilter, dataSource, filters);
    }

    public static EntityDataQuery toSingleDeviceQuery(DataSource dataSource, List<Filter> filters) {
        SingleEntityFilter singleEntityFilter = new SingleEntityFilter();
        singleEntityFilter.setSingleEntity(new DeviceId(UUID.fromString(dataSource.getDeviceId())));

        return buildEntityDataQuery(singleEntityFilter, dataSource, filters);
    }

    public static EntityCountQuery toEntityCountQuery(DataSource dataSource, List<EntityAlias> entityAliases, List<Filter> filters) {
        EntityFilter entityFilter = getEntityFilter(dataSource, entityAliases);
        List<KeyFilter> keyFilters = getKeyFilters(dataSource, filters);

        return new EntityCountQuery(entityFilter, keyFilters);
    }

    public static AlarmCountQuery toAlarmCountQuery(DataSource dataSource, List<EntityAlias> entityAliases, List<Filter> filters) {
        List<KeyFilter> keyFilters = getKeyFilters(dataSource, filters);
        AlarmCountQuery alarmCountQuery = new AlarmCountQuery(getEntityFilter(dataSource, entityAliases), keyFilters);

        AlarmFilterConfig alarmFilterConfig = dataSource.getAlarmFilterConfig();
        if (alarmFilterConfig != null) {
            alarmCountQuery.setStatusList(alarmFilterConfig.getStatusList());
            alarmCountQuery.setSeverityList(alarmFilterConfig.getSeverityList());
            alarmCountQuery.setTypeList(alarmFilterConfig.getTypeList());
            alarmCountQuery.setAssigneeId(alarmFilterConfig.getAssigneeId());
        }
        return alarmCountQuery;
    }

    private static EntityFilter getEntityFilter(DataSource dataSource, List<EntityAlias> entityAliases) {
        return entityAliases.stream()
                .filter(alias -> alias.getId().equals(dataSource.getEntityAliasId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Entity alias not found: " + dataSource.getEntityAliasId()))
                .getFilter();
    }

    private static List<KeyFilter> getKeyFilters(DataSource dataSource, List<Filter> filters) {
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

    private static EntityDataQuery buildEntityDataQuery(EntityFilter filter, DataSource dataSource, List<Filter> filters) {
        List<KeyFilter> keyFilters = getKeyFilters(dataSource, filters);

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
        EntityDataPageLink pageLink = new EntityDataPageLink(Integer.MAX_VALUE, 0, null, null);
        return new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFilters);
    }

}
